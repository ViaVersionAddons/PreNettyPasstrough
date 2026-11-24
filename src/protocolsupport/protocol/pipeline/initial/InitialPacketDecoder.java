package protocolsupport.protocol.pipeline.initial;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.util.concurrent.Future;
import net.md_5.bungee.netty.PipelineUtils;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.pipeline.ChannelHandlers;
import protocolsupport.protocol.pipeline.IPipeLineBuilder;
import protocolsupport.protocol.pipeline.common.PacketCompressor;
import protocolsupport.protocol.pipeline.common.PacketDecompressor;
import protocolsupport.protocol.pipeline.common.VarIntFrameDecoder;
import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.utils.EncapsulatedProtocolInfo;
import protocolsupport.protocol.utils.EncapsulatedProtocolUtils;
import protocolsupport.tunnel.LegacyTunnel;
import protocolsupport.tunnel.TunnelConfig;
import protocolsupport.utils.Utils;
import protocolsupport.utils.netty.Decompressor;
import protocolsupport.utils.netty.ReplayingDecoderBuffer;
import protocolsupport.utils.netty.ReplayingDecoderBuffer.EOFSignal;


public class InitialPacketDecoder extends SimpleChannelInboundHandler<ByteBuf> {

	public static final Map<ProtocolVersion, IPipeLineBuilder> BUILDERS = new EnumMap<>(ProtocolVersion.class);
	static {
		BUILDERS.put(ProtocolVersion.MINECRAFT_FUTURE, IPipeLineBuilder.NOOP);
		Arrays.stream(ProtocolVersion.getAllAfterI(ProtocolVersion.MINECRAFT_1_7_5)).forEach(version -> InitialPacketDecoder.BUILDERS.put(version, IPipeLineBuilder.NOOP));
		IPipeLineBuilder v6builder = new protocolsupport.protocol.pipeline.version.v_1_6.PipeLineBuilder();
		InitialPacketDecoder.BUILDERS.put(ProtocolVersion.MINECRAFT_1_6_4, v6builder);
		InitialPacketDecoder.BUILDERS.put(ProtocolVersion.MINECRAFT_1_6_2, v6builder);
		InitialPacketDecoder.BUILDERS.put(ProtocolVersion.MINECRAFT_1_6_1, v6builder);
		IPipeLineBuilder v5builder = new protocolsupport.protocol.pipeline.version.v_1_5.PipeLineBuilder();
		InitialPacketDecoder.BUILDERS.put(ProtocolVersion.MINECRAFT_1_5_2, v5builder);
		InitialPacketDecoder.BUILDERS.put(ProtocolVersion.MINECRAFT_1_5_1, v5builder);
		InitialPacketDecoder.BUILDERS.put(ProtocolVersion.MINECRAFT_1_4_7, new protocolsupport.protocol.pipeline.version.v_1_4.PipeLineBuilder());
	}

	private static final int ping152delay = Utils.getJavaPropertyValue("ping152delay", 100, Integer::parseInt);
	private static final int pingLegacyDelay = Utils.getJavaPropertyValue("pinglegacydelay", 200, Integer::parseInt);

	protected final ReplayingDecoderBuffer buffer = new ReplayingDecoderBuffer(Unpooled.buffer());

	protected Future<?> responseTask;

	protected void scheduleTask(ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit tu) {
		responseTask = ctx.executor().schedule(task, delay, tu);
	}

	protected void cancelTask() {
		if (responseTask != null) {
			responseTask.cancel(true);
			responseTask = null;
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		cancelTask();
		super.channelInactive(ctx);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		cancelTask();
		super.handlerRemoved(ctx);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
		if (!buf.isReadable()) {
			return;
		}
		buffer.writeBytes(buf);
		buffer.readerIndex(0);
		decode(ctx);
	}


	private boolean firstread = true;
	private EncapsulatedProtocolInfo encapsulatedinfo = null;

	private void decode(ChannelHandlerContext ctx) throws Exception {
		cancelTask();
		if (firstread) {
			int firstbyte = buffer.readUnsignedByte();
			if (firstbyte == EncapsulatedProtocolUtils.FIRST_BYTE) {
				encapsulatedinfo = EncapsulatedProtocolUtils.readInfo(buffer);
				buffer.discardReadBytes();
			}
			buffer.readerIndex(0);
			firstread = false;
		}
		try {
			if (encapsulatedinfo == null) {
				decodeRaw(ctx);
			} else {
				decodeEncapsulated(ctx);
			}
		} catch (EOFSignal ex) {
		}
	}

	private void decodeRaw(ChannelHandlerContext ctx) {
		Channel channel = ctx.channel();
		int firstbyte = buffer.readUnsignedByte();
		switch (firstbyte) {
			case 0xFE: {
				if (buffer.readableBytes() == 0) {
					scheduleTask(ctx, new SetProtocolTask(this, channel, ProtocolVersion.MINECRAFT_LEGACY), pingLegacyDelay, TimeUnit.MILLISECONDS);
				} else if (buffer.readUnsignedByte() == 1) {
					if (buffer.readableBytes() == 0) {
						scheduleTask(ctx, new SetProtocolTask(this, channel, ProtocolVersion.MINECRAFT_1_5_2), ping152delay, TimeUnit.MILLISECONDS);
					} else if (
						(buffer.readUnsignedByte() == 0xFA) &&
						"MC|PingHost".equals(StringSerializer.readShortUTF16BEString(buffer))
					) {
						buffer.readUnsignedShort();
						setProtocol(channel, ProtocolUtils.get16PingVersion(buffer.readUnsignedByte()));
					} else {
						setProtocol(channel, attemptDecodeNewHandshake(buffer));
					}
				} else {
					setProtocol(channel, attemptDecodeNewHandshake(buffer));
				}
				break;
			}
			case 0x02: {
				int start = buffer.readerIndex();
				if (buffer.readUnsignedByte() == 0x00) {
					buffer.readerIndex(start);
					String[] info = LegacyTunnel.parseStringHandshake(StringSerializer.readShortUTF16BEString(buffer));
					startTunnel(channel, info[0], info[1], "1.0 - 1.3.2");
					break;
				}
				buffer.readerIndex(start);
				int protocolId = buffer.readUnsignedByte();
				if (TunnelConfig.tunnelsOldHandshakeProtocol(protocolId)) {
					String username = StringSerializer.readShortUTF16BEString(buffer);
					String host = StringSerializer.readShortUTF16BEString(buffer);
					startTunnel(channel, username, host, "protocol " + protocolId);
					break;
				}
				buffer.readerIndex(start);
				setProtocol(channel, ProtocolUtils.readOldHandshake(buffer));
				break;
			}
			default: {
				setProtocol(channel, attemptDecodeNewHandshake(buffer));
				break;
			}
		}
	}

	private static ProtocolVersion attemptDecodeNewHandshake(ByteBuf bytebuf) {
		bytebuf.readerIndex(0);
		return ProtocolUtils.readNewHandshake(bytebuf.readSlice(VarNumberSerializer.readVarInt(bytebuf)));
	}

	private void decodeEncapsulated(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		ByteBuf firstpacketdata = buffer.readSlice(VarNumberSerializer.readVarInt(buffer));
		if (encapsulatedinfo.hasCompression()) {
			int uncompressedlength = VarNumberSerializer.readVarInt(firstpacketdata);
			if (uncompressedlength != 0) {
				firstpacketdata = Unpooled.wrappedBuffer(Decompressor.decompressStatic(MiscSerializer.readAllBytes(firstpacketdata), uncompressedlength));
			}
		}
		int firstbyte = firstpacketdata.readUnsignedByte();
		switch (firstbyte) {
			case 0xFE: {
				if (firstpacketdata.readableBytes() == 0) {
					setProtocol(channel, ProtocolVersion.MINECRAFT_LEGACY);
				} else if (firstpacketdata.readUnsignedByte() == 1) {
					if (firstpacketdata.readableBytes() == 0) {
						setProtocol(channel, ProtocolVersion.MINECRAFT_1_5_2);
					} else if (
						(firstpacketdata.readUnsignedByte() == 0xFA) &&
						"MC|PingHost".equals(StringSerializer.readShortUTF16BEString(firstpacketdata))
					) {
						firstpacketdata.readUnsignedShort();
						setProtocol(channel, ProtocolUtils.get16PingVersion(firstpacketdata.readUnsignedByte()));
					} else {
						throw new DecoderException("Unable to detect incoming protocol");
					}
				} else {
					throw new DecoderException("Unable to detect incoming protocol");
				}
				break;
			}
			case 0x02: {
				setProtocol(channel, ProtocolUtils.readOldHandshake(firstpacketdata));
				break;
			}
			case 0x00: {
				setProtocol(channel, ProtocolUtils.readNewHandshake(firstpacketdata));
				break;
			}
			default: {
				throw new DecoderException("Unable to detect incoming protocol");
			}
		}
	}

	private void startTunnel(Channel channel, String username, String host, String versionName) {
		buffer.readerIndex(0);
		LegacyTunnel.start(channel, buffer.unwrap(), username, host, versionName);
	}

	protected void setProtocol(Channel channel, ProtocolVersion version) {
		if ((encapsulatedinfo == null) && ((version == ProtocolVersion.MINECRAFT_1_7_5) || (version == ProtocolVersion.MINECRAFT_1_7_10))) {
			buffer.readerIndex(0);
			VarNumberSerializer.readVarInt(buffer);
			VarNumberSerializer.readVarInt(buffer);
			VarNumberSerializer.readVarInt(buffer);
			String host = StringSerializer.readVarIntUTF8String(buffer);
			buffer.readUnsignedShort();
			VarNumberSerializer.readVarInt(buffer);
			startTunnel(channel, null, host, version.getName());
			return;
		}
		ConnectionImpl connection = prepare(channel, version);
		IPipeLineBuilder builder = InitialPacketDecoder.BUILDERS.get(connection.getVersion());
		builder.buildBungeeClientCodec(channel, connection);
		if (encapsulatedinfo == null) {
			builder.buildBungeeClientPipeLine(channel, connection);
		} else {
			ChannelPipeline pipeline = channel.pipeline();
			pipeline.replace(PipelineUtils.FRAME_DECODER, PipelineUtils.FRAME_DECODER, new VarIntFrameDecoder());
			if (encapsulatedinfo.hasCompression()) {
				pipeline.addAfter(PipelineUtils.FRAME_DECODER, "decompress", new PacketDecompressor());
				pipeline.addAfter(PipelineUtils.FRAME_PREPENDER_AND_COMPRESS, "compress", new PacketCompressor(256));
			}
			if ((encapsulatedinfo.getAddress() != null) && connection.getRawAddress().getAddress().isLoopbackAddress()) {
				connection.changeAddress(encapsulatedinfo.getAddress());
			}
		}
		buffer.readerIndex(0);
		channel.pipeline().firstContext().fireChannelRead(buffer.unwrap());
	}

	protected static ConnectionImpl prepare(Channel channel, ProtocolVersion version) {
		channel.pipeline().remove(ChannelHandlers.INITIAL_DECODER);
		ConnectionImpl connection = ConnectionImpl.getFromChannel(channel);
		connection.setVersion(version);
		return connection;
	}

}
