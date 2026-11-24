package protocolsupport.protocol.packet.middleimpl.readable.play.v_4_5_6;

import java.util.Collection;
import java.util.Collections;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.Kick;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyDefinedReadableMiddlePacket;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.utils.ChatCompat;
import protocolsupport.utils.Utils;

public class KickPacket extends LegacyDefinedReadableMiddlePacket {

	public KickPacket() {
		super(LegacyPacketId.Dualbound.PLAY_KICK);
	}

	protected String message;

	@Override
	protected void read0(ByteBuf from) {
		message = StringSerializer.readShortUTF16BEString(from);
	}

	@Override
	public Collection<PacketWrapper> toNative() {
		return Collections.singletonList(new PacketWrapper(new Kick(ChatCompat.fromLegacy(Utils.clampString(message, 256))), Unpooled.wrappedBuffer(readbytes), Protocol.GAME));
	}

}
