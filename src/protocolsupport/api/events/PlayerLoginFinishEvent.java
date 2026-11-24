package protocolsupport.api.events;

import java.util.UUID;

import protocolsupport.api.Connection;
import protocolsupport.api.utils.Profile;

public class PlayerLoginFinishEvent extends PlayerAbstractLoginEvent {

	public PlayerLoginFinishEvent(Connection connection) {
		super(connection);
	}

	public UUID getUUID() {
		return getConnection().getProfile().getUUID();
	}

	public boolean isOnlineMode() {
		return getConnection().getProfile().isOnlineMode();
	}

}
