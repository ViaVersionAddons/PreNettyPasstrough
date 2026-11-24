package protocolsupport.injector;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.channel.BungeeChannelInitializer;
import net.md_5.bungee.protocol.channel.ChannelAcceptor;
import net.md_5.bungee.util.PacketLimiter;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.handler.PSInitialHandler;
import protocolsupport.protocol.pipeline.ChannelHandlers;
import protocolsupport.protocol.pipeline.IPipeLineBuilder;
import protocolsupport.protocol.pipeline.common.LogicHandler;
import protocolsupport.protocol.pipeline.initial.InitialPacketDecoder;
import protocolsupport.protocol.storage.ProtocolStorage;
import protocolsupport.utils.ReflectionUtils;

public class BungeeNettyChannelInjector {

	private BungeeNettyChannelInjector() {
	}

	public static void inject() {
		ProxyServer.Unsafe unsafe = ProxyServer.getInstance().unsafe();

		ChannelAcceptor frontend = unsafe.getFrontendChannelInitializer().getChannelAcceptor();
		unsafe.setFrontendChannelInitializer(BungeeChannelInitializer.create(channel -> safeAccept(frontend, channel, true)));

		ChannelAcceptor backend = unsafe.getBackendChannelInitializer().getChannelAcceptor();
		unsafe.setBackendChannelInitializer(BungeeChannelInitializer.create(channel -> safeAccept(backend, channel, false)));
	}

	private static boolean safeAccept(ChannelAcceptor original, Channel channel, boolean frontend) {
		if (!original.accept(channel)) {
			return false;
		}
		if (!channel.isOpen()) {
			return true;
		}
		try {
			if (frontend) {
				hookFrontend(channel);
			} else {
				hookBackend(channel);
			}
			return true;
		} catch (Throwable t) {
			ProxyServer.getInstance().getLogger().log(Level.SEVERE, "Unable to inject into channel", t);
			return false;
		}
	}

	private static void hookFrontend(Channel channel) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		HandlerBoss oldBoss = pipeline.get(HandlerBoss.class);
		PacketHandler handler = ReflectionUtils.getFieldValue(oldBoss, "handler");
		if (!(handler instanceof InitialHandler)) {
			return;
		}
		CustomHandlerBoss boss = new CustomHandlerBoss(handler);
		PacketLimiter limiter = ReflectionUtils.getFieldValue(oldBoss, "limiter");
		if (limiter != null) {
			boss.setLimiter(limiter);
		}
		pipeline.replace(PipelineUtils.BOSS_HANDLER, PipelineUtils.BOSS_HANDLER, boss);

		Entry<String, ChannelHandler> firstHandler = pipeline.iterator().next();
		if (firstHandler.getValue() instanceof HAProxyMessageDecoder) {
			pipeline.addAfter(firstHandler.getKey(), ChannelHandlers.INITIAL_DECODER, new InitialPacketDecoder());
		} else {
			pipeline.addFirst(ChannelHandlers.INITIAL_DECODER, new InitialPacketDecoder());
		}
		PSInitialHandler initialhandler = new PSInitialHandler(BungeeCord.getInstance(), channel.attr(PipelineUtils.LISTENER).get());
		ConnectionImpl connection = new ConnectionImpl(boss, initialhandler);
		connection.storeInChannel(channel);
		ProtocolStorage.addConnection(channel.remoteAddress(), connection);
		pipeline.addBefore(PipelineUtils.BOSS_HANDLER, ChannelHandlers.LOGIC, new LogicHandler(connection, true));
		pipeline.remove(PipelineUtils.LEGACY_DECODER);
		boss.setHandler(initialhandler);
	}

	private static void hookBackend(Channel channel) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		HandlerBoss oldBoss = pipeline.get(HandlerBoss.class);
		PacketHandler handler = ReflectionUtils.getFieldValue(oldBoss, "handler");
		CustomHandlerBoss boss = new CustomHandlerBoss(handler);
		PacketLimiter limiter = ReflectionUtils.getFieldValue(oldBoss, "limiter");
		if (limiter != null) {
			boss.setLimiter(limiter);
		}
		pipeline.replace(PipelineUtils.BOSS_HANDLER, PipelineUtils.BOSS_HANDLER, boss);

		AtomicBoolean hooked = new AtomicBoolean(false);
		PacketHandlerChangeListener listener = newHandler -> {
			if ((newHandler instanceof ServerConnector) && hooked.compareAndSet(false, true)) {
				try {
					hookServerConnector(channel, newHandler);
				} catch (Throwable t) {
					ProxyServer.getInstance().getLogger().log(Level.SEVERE, "Unable to inject into server connection", t);
					channel.close();
				}
			}
			return newHandler;
		};
		boss.setHandlerChangeListener(listener);
		if (handler != null) {
			listener.onPacketHandlerChange(handler);
		}
	}

	private static void hookServerConnector(Channel channel, PacketHandler serverConnector) throws Exception {
		ChannelPipeline pipeline = channel.pipeline();
		UserConnection user = ReflectionUtils.getFieldValue(serverConnector, "user");
		ConnectionImpl connection = ConnectionImpl.getFromChannel(user.getCh().getHandle());
		if (connection == null) {
			return;
		}
		pipeline.addBefore(PipelineUtils.BOSS_HANDLER, ChannelHandlers.LOGIC, new LogicHandler(connection, false));
		connection.setServerConnectionChannel(channel);
		IPipeLineBuilder builder = InitialPacketDecoder.BUILDERS.get(connection.getVersion());
		if (builder != null) {
			builder.buildBungeeServer(channel, connection);
		}
	}

	@FunctionalInterface
	public static interface PacketHandlerChangeListener {
		public PacketHandler onPacketHandlerChange(PacketHandler handler);
	}

	public static class CustomHandlerBoss extends HandlerBoss {

		protected PacketHandler handler;
		protected PacketHandlerChangeListener listener = (handler) -> handler;

		public CustomHandlerBoss(PacketHandler handler) {
			if (handler != null) {
				setHandler(handler);
			}
		}

		@Override
		public void setHandler(PacketHandler handler) {
			handler = listener.onPacketHandlerChange(handler);
			super.setHandler(handler);
			this.handler = handler;
		}

		public PacketHandler getHandler() {
			return this.handler;
		}

		public void setHandlerChangeListener(PacketHandlerChangeListener listener) {
			this.listener = listener;
		}

	}

}
