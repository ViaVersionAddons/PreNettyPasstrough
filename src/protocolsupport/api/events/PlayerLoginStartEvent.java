package protocolsupport.api.events;

import net.md_5.bungee.BungeeCord;
import protocolsupport.api.Connection;

public class PlayerLoginStartEvent extends PlayerAbstractLoginEvent {

	protected final String hostname;
	protected boolean onlinemode;

	public PlayerLoginStartEvent(Connection connection, String hostname) {
		super(connection);
		this.hostname = hostname;
		this.onlinemode = BungeeCord.getInstance().config.isOnlineMode();
	}

	public String getHostname() {
		return hostname;
	}

	public boolean isOnlineMode() {
		return onlinemode;
	}

	public void setOnlineMode(boolean onlinemode) {
		this.onlinemode = onlinemode;
	}

}
