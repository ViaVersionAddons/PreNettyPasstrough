package protocolsupport.tunnel;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.netty.PipelineUtils;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.storage.ProtocolStorage;

public class LegacyTunnel {

	private static final String PREFIX = "[PreNettyPasstrough] ";

	public static void start(Channel client, ByteBuf initialData, String username, String host, String versionName) {
		ByteBuf replay = Unpooled.copiedBuffer(initialData);
		ProxyServer proxy = ProxyServer.getInstance();
		ServerInfo target = resolveTarget(client, host);
		if (target == null) {
			proxy.getLogger().log(Level.WARNING, PREFIX + "No server available for tunnelled client {0}", client.remoteAddress());
			replay.release();
			client.close();
			return;
		}
		if (TunnelConfig.isLog()) {
			proxy.getLogger().log(Level.INFO, PREFIX + "{0} ({1}, {2}) -> tunnel to {3}", new Object[] { client.remoteAddress(), username, versionName, target.getName() });
		}

		ConnectionImpl connection = ConnectionImpl.getFromChannel(client);
		if ((connection != null) && (ProtocolStorage.getConnection(connection.getRawAddress()) != null)) {
			ProtocolStorage.removeConnection(connection.getRawAddress());
		}

		client.config().setAutoRead(false);
		Relay clientRelay = new Relay();
		ChannelPipeline pipeline = client.pipeline();
		for (ChannelHandler handler : pipeline.toMap().values()) {
			pipeline.remove(handler);
		}
		pipeline.addLast("tunnel", clientRelay);

		SocketAddress address = target.getSocketAddress();
		Bootstrap bootstrap = new Bootstrap()
		.channel(PipelineUtils.getChannel(address))
		.group(client.eventLoop())
		.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) {
				ch.pipeline().addLast("tunnel", new Relay());
			}
		})
		.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
		.remoteAddress(address);
		bootstrap.connect().addListener((ChannelFutureListener) future -> {
			if (!future.isSuccess()) {
				proxy.getLogger().log(Level.WARNING, PREFIX + "Unable to connect tunnel of {0} to {1}: {2}", new Object[] { client.remoteAddress(), target.getName(), future.cause() });
				replay.release();
				clientRelay.discardPending();
				client.close();
				return;
			}
			Channel server = future.channel();
			if (!client.isActive()) {
				replay.release();
				clientRelay.discardPending();
				server.close();
				return;
			}
			((Relay) server.pipeline().get("tunnel")).target = client;
			server.writeAndFlush(replay);
			clientRelay.setTarget(server);
			client.config().setAutoRead(true);
		});
	}

	private static ServerInfo resolveTarget(Channel client, String host) {
		ProxyServer proxy = ProxyServer.getInstance();
		String configured = TunnelConfig.getServer();
		if ((configured != null) && !configured.isEmpty()) {
			return proxy.getServerInfo(configured);
		}
		ListenerInfo listener = client.attr(PipelineUtils.LISTENER).get();
		if (listener == null) {
			return null;
		}
		if (host != null) {
			String normalized = host.toLowerCase();
			while (normalized.endsWith(".")) {
				normalized = normalized.substring(0, normalized.length() - 1);
			}
			Map<String, String> forced = listener.getForcedHosts();
			String name = (forced != null) ? forced.get(normalized) : null;
			if (name != null) {
				ServerInfo info = proxy.getServerInfo(name);
				if (info != null) {
					return info;
				}
			}
		}
		for (String name : listener.getServerPriority()) {
			ServerInfo info = proxy.getServerInfo(name);
			if (info != null) {
				return info;
			}
		}
		return null;
	}

	public static String[] parseStringHandshake(String data) {
		String[] parts = data.split(";", 2);
		String host = null;
		if (parts.length > 1) {
			host = parts[1];
			int portSeparator = host.lastIndexOf(':');
			if (portSeparator >= 0) {
				host = host.substring(0, portSeparator);
			}
		}
		return new String[] { parts[0], host };
	}

	private static class Relay extends ChannelInboundHandlerAdapter {

		private volatile Channel target;
		private final Queue<Object> pending = new ArrayDeque<>();

		public void setTarget(Channel target) {
			this.target = target;
			Object msg;
			while ((msg = pending.poll()) != null) {
				forward(target, msg);
			}
		}

		public void discardPending() {
			Object msg;
			while ((msg = pending.poll()) != null) {
				ReferenceCountUtil.release(msg);
			}
		}

		private void forward(Channel to, Object msg) {
			to.writeAndFlush(msg).addListener(future -> {
				if (!future.isSuccess()) {
					to.close();
				}
			});
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			Channel to = target;
			if (to == null) {
				pending.add(msg);
				return;
			}
			if (!to.isActive()) {
				ReferenceCountUtil.release(msg);
				ctx.close();
				return;
			}
			forward(to, msg);
			if (!to.isWritable()) {
				ctx.channel().config().setAutoRead(false);
			}
		}

		@Override
		public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
			Channel other = target;
			if (ctx.channel().isWritable() && (other != null)) {
				other.config().setAutoRead(true);
			}
			super.channelWritabilityChanged(ctx);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			discardPending();
			Channel other = target;
			if ((other != null) && other.isActive()) {
				other.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
			}
			super.channelInactive(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			ctx.close();
			Channel other = target;
			if (other != null) {
				other.close();
			}
		}

	}

}
