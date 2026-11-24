package protocolsupport.api.utils;

import java.util.Objects;

import protocolsupport.utils.Validate;

import protocolsupport.utils.Utils;

public class ProfileProperty {

	protected final String name;
	protected final String value;
	protected final String signature;

	public ProfileProperty(String name, String value, String signature) {
		Validate.notNull(name, "Name cannot be null");
		Validate.notNull(value, "Value cannot be null");
		this.name = name;
		this.value = value;
		this.signature = signature;
	}

	public ProfileProperty(String name, String value) {
		this(name, value, null);
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public String getSignature() {
		return signature;
	}

	public boolean hasSignature() {
		return signature != null;
	}

	@Override
	public String toString() {
		return Utils.toStringAllFields(this);
	}

	@Override
	public int hashCode() {
		int nvHash = getName().hashCode() ^ getValue().hashCode();
		return hasSignature() ? nvHash ^ getSignature().hashCode() : nvHash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ProfileProperty otherProperty = (ProfileProperty) obj;
		return
			getName().equals(otherProperty.getName()) &&
			getValue().equalsIgnoreCase(otherProperty.getValue()) &&
			Objects.equals(getSignature(), otherProperty.getSignature());
	}

}