package protocolsupport.api.events;

import protocolsupport.api.Connection;

public abstract class PlayerAbstractLoginEvent extends ConnectionEvent {

	public PlayerAbstractLoginEvent(Connection connection) {
		super(connection);
	}

	private String denyLoginMessage;

	public boolean isLoginDenied() {
		return denyLoginMessage != null;
	}

	public String getDenyLoginMessage() {
		return denyLoginMessage;
	}

	public void denyLogin(String message) {
		this.denyLoginMessage = message;
	}

}
