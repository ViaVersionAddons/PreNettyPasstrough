package protocolsupport.api;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import protocolsupport.api.utils.Profile;
import protocolsupport.protocol.utils.GameProfile;

public abstract class Connection {

	protected volatile ProtocolVersion version = ProtocolVersion.UNKNOWN;
	protected Profile profile = new GameProfile();

	public abstract Object getNetworkManager();

	public abstract boolean isConnected();

	public abstract InetSocketAddress getRawAddress();

	public abstract InetSocketAddress getAddress();

	public abstract void changeAddress(InetSocketAddress newRemote);

	public Profile getProfile() {
		return profile;
	}

	public abstract ProxiedPlayer getPlayer();

	public ProtocolVersion getVersion() {
		return version;
	}

	public abstract void sendPacketToClient(Object packet);

	public abstract void sendPacketToServer(Object packet);

	protected final CopyOnWriteArrayList<PacketListener> clientConnectionPacketListeners = new CopyOnWriteArrayList<>();

	public void addClientConnectionPacketListener(PacketListener listener) {
		clientConnectionPacketListeners.add(listener);
	}

	public void removeClientConnectionPacketListener(PacketListener listener) {
		clientConnectionPacketListeners.remove(listener);
	}

	protected final CopyOnWriteArrayList<PacketListener> serverConnectionPacketListeners = new CopyOnWriteArrayList<>();

	public void addServerConnectionPacketListener(PacketListener listener) {
		serverConnectionPacketListeners.add(listener);
	}

	public void removeServerConnectionPacketListener(PacketListener listener) {
		serverConnectionPacketListeners.remove(listener);
	}

	public abstract static class PacketListener {

		public void onPacketSending(PacketEvent event) {
		}

		public void onPacketReceiving(PacketEvent event) {
		}

		public static class PacketEvent {

			protected Object packet;
			protected boolean cancelled;

			public Object getPacket() {
				return packet;
			}

			public void setPacket(Object packet) {
				this.packet = packet;
			}

			public boolean isCancelled() {
				return cancelled;
			}

			public void setCancelled(boolean cancelled) {
				this.cancelled = cancelled;
			}
		}

	}

	protected final ConcurrentHashMap<String, Object> metadata = new ConcurrentHashMap<>();

	public void addMetadata(String key, Object obj) {
		metadata.put(key, obj);
	}

	public Object getMetadata(String key) {
		return metadata.get(key);
	}

	public Object removeMetadata(String key) {
		return metadata.remove(key);
	}

	public boolean hasMetadata(String key) {
		return metadata.containsKey(key);
	}

}
