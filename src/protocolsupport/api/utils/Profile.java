package protocolsupport.api.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Charsets;

import protocolsupport.api.events.PlayerProfileCompleteEvent;

public abstract class Profile {

	public static UUID generateOfflineModeUUID(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
	}

	protected boolean onlineMode;

	protected String originalname;
	protected UUID originaluuid;

	protected String name;
	protected UUID uuid;
	protected final Map<String, Set<ProfileProperty>> properties = new HashMap<>();

	public boolean isOnlineMode() {
		return onlineMode;
	}

	public String getOriginalName() {
		return originalname;
	}

	public UUID getOriginalUUID() {
		return originaluuid;
	}

	public String getName() {
		return name;
	}

	public UUID getUUID() {
		return uuid;
	}

	public Set<String> getPropertiesNames() {
		return Collections.unmodifiableSet(properties.keySet());
	}

	public Set<ProfileProperty> getProperties(String name) {
		return Collections.unmodifiableSet(properties.getOrDefault(name, Collections.emptySet()));
	}

}
