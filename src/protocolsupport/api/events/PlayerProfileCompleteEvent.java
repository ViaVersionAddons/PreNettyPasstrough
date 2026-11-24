package protocolsupport.api.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import protocolsupport.api.Connection;
import protocolsupport.api.utils.ProfileProperty;

public class PlayerProfileCompleteEvent extends PlayerAbstractLoginEvent {

	public PlayerProfileCompleteEvent(Connection connection) {
		super(connection);
		connection.getProfile().getPropertiesNames().forEach(name -> properties.put(name, connection.getProfile().getProperties(name)));
	}

	protected String forcedName;
	protected UUID forcedUUID;

	public String getForcedName() {
		return forcedName;
	}

	public void setForcedName(String name) {
		this.forcedName = name;
	}

	public UUID getForcedUUID() {
		return forcedUUID;
	}

	public void setForcedUUID(UUID uuid) {
		this.forcedUUID = uuid;
	}

	protected final Map<String, Set<ProfileProperty>> properties = new HashMap<>();

	public Map<String, Set<ProfileProperty>> getProperties() {
		return properties;
	}

	public Set<ProfileProperty> getProperties(String name) {
		return properties.getOrDefault(name, Collections.emptySet());
	}

	public void removeProperties(String name) {
		properties.remove(name);
	}

	public void removeProperty(ProfileProperty property) {
		String propertyName = property.getName();
		Set<ProfileProperty> propertiesWithName = properties.get(propertyName);
		propertiesWithName.remove(property);
		if (propertiesWithName.isEmpty()) {
			properties.remove(propertyName);
		}
	}

	public void addProperty(ProfileProperty property) {
		properties.computeIfAbsent(property.getName(), k -> new HashSet<>()).add(property);
	}

}
