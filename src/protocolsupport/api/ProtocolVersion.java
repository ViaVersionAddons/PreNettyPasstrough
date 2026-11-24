package protocolsupport.api;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.stream.Collectors;

import protocolsupport.utils.Validate;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import protocolsupport.utils.Utils;

public enum ProtocolVersion {

	MINECRAFT_FUTURE(-1, new OrderId(ProtocolType.PC, 36)),
	MINECRAFT_1_16_4(754, new OrderId(ProtocolType.PC, 35), "1.16.4"),
	MINECRAFT_1_16_3(753, new OrderId(ProtocolType.PC, 34), "1.16.3"),
	MINECRAFT_1_16_2(751, new OrderId(ProtocolType.PC, 33), "1.16.2"),
	MINECRAFT_1_16_1(736, new OrderId(ProtocolType.PC, 32), "1.16.1"),
	MINECRAFT_1_16(735, new OrderId(ProtocolType.PC, 31), "1.16"),
	MINECRAFT_1_15_2(578, new OrderId(ProtocolType.PC, 30), "1.15.2"),
	MINECRAFT_1_15_1(575, new OrderId(ProtocolType.PC, 29), "1.15.1"),
	MINECRAFT_1_15(573, new OrderId(ProtocolType.PC, 28), "1.15"),
	MINECRAFT_1_14_4(498, new OrderId(ProtocolType.PC, 27), "1.14.4"),
	MINECRAFT_1_14_3(490, new OrderId(ProtocolType.PC, 26), "1.14.3"),
	MINECRAFT_1_14_2(485, new OrderId(ProtocolType.PC, 25), "1.14.2"),
	MINECRAFT_1_14_1(480, new OrderId(ProtocolType.PC, 24), "1.14.1"),
	MINECRAFT_1_14(477, new OrderId(ProtocolType.PC, 23), "1.14"),
	MINECRAFT_1_13_2(404, new OrderId(ProtocolType.PC, 22), "1.13.2"),
	MINECRAFT_1_13_1(401, new OrderId(ProtocolType.PC, 21), "1.13.1"),
	MINECRAFT_1_13(393, new OrderId(ProtocolType.PC, 20), "1.13"),
	MINECRAFT_1_12_2(340, new OrderId(ProtocolType.PC, 19), "1.12.2"),
	MINECRAFT_1_12_1(338, new OrderId(ProtocolType.PC, 18), "1.12.1"),
	MINECRAFT_1_12(335, new OrderId(ProtocolType.PC, 17), "1.12"),
	MINECRAFT_1_11_1(316, new OrderId(ProtocolType.PC, 16), "1.11.2"),
	MINECRAFT_1_11(315, new OrderId(ProtocolType.PC, 15), "1.11"),
	MINECRAFT_1_10(210, new OrderId(ProtocolType.PC, 14), "1.10"),
	MINECRAFT_1_9_4(110, new OrderId(ProtocolType.PC, 13), "1.9.4"),
	MINECRAFT_1_9_2(109, new OrderId(ProtocolType.PC, 12), "1.9.2"),
	MINECRAFT_1_9_1(108, new OrderId(ProtocolType.PC, 11), "1.9.1"),
	MINECRAFT_1_9(107, new OrderId(ProtocolType.PC, 10), "1.9"),
	MINECRAFT_1_8(47, new OrderId(ProtocolType.PC, 9), "1.8"),
	MINECRAFT_1_7_10(5, new OrderId(ProtocolType.PC, 8), "1.7.10"),
	MINECRAFT_1_7_5(4, new OrderId(ProtocolType.PC, 7), "1.7.5"),
	MINECRAFT_1_6_4(78, new OrderId(ProtocolType.PC, 6), "1.6.4"),
	MINECRAFT_1_6_2(74, new OrderId(ProtocolType.PC, 5), "1.6.2"),
	MINECRAFT_1_6_1(73, new OrderId(ProtocolType.PC, 4), "1.6.1"),
	MINECRAFT_1_5_2(61, new OrderId(ProtocolType.PC, 3), "1.5.2"),
	MINECRAFT_1_5_1(60, new OrderId(ProtocolType.PC, 2), "1.5.1"),
	MINECRAFT_1_4_7(51, new OrderId(ProtocolType.PC, 1), "1.4.7"),
	MINECRAFT_LEGACY(-1, new OrderId(ProtocolType.PC, 0)),
	UNKNOWN(-1, new OrderId(ProtocolType.UNKNOWN, 0));

	private final int id;
	private final OrderId orderId;
	private final String name;

	ProtocolVersion(int id, OrderId orderid) {
		this(id, orderid, null);
	}

	ProtocolVersion(int id, OrderId orderId, String name) {
		this.id = id;
		this.orderId = orderId;
		this.name = name;
	}

	private static final ProtocolVersion[] allSupported = Arrays.stream(ProtocolVersion.values())
	.filter(ProtocolVersion::isSupported)
	.collect(Collectors.toList())
	.toArray(new ProtocolVersion[0]);

	private static final Int2ObjectOpenHashMap<ProtocolVersion> byProtocolId = new Int2ObjectOpenHashMap<>();
	static {
		Arrays.stream(ProtocolVersion.getAllSupported()).forEach(version -> byProtocolId.put(version.id, version));
	}

	private static final EnumMap<ProtocolType, ProtocolVersion[]> byOrderId = new EnumMap<>(ProtocolType.class);
	static {
		for (ProtocolType type : ProtocolType.values()) {
			if (type != ProtocolType.UNKNOWN) {
				byOrderId.put(type,
					Arrays.stream(ProtocolVersion.values())
					.filter(version -> version.getProtocolType() == type)
					.sorted((o1, o2) -> o1.orderId.compareTo(o2.orderId))
					.toArray(size -> new ProtocolVersion[size])
				);
			}
		}
	}

	public ProtocolType getProtocolType() {
		return orderId.type;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isSupported() {
		return name != null;
	}

	public boolean isAfter(ProtocolVersion another) {
		return orderId.compareTo(another.orderId) > 0;
	}

	public boolean isAfterOrEq(ProtocolVersion another) {
		return orderId.compareTo(another.orderId) >= 0;
	}

	public boolean isBefore(ProtocolVersion another) {
		return orderId.compareTo(another.orderId) < 0;
	}

	public boolean isBeforeOrEq(ProtocolVersion another) {
		return orderId.compareTo(another.orderId) <= 0;
	}

	public boolean isBetween(ProtocolVersion one, ProtocolVersion another) {
		return (isAfterOrEq(one) && isBeforeOrEq(another)) || (isBeforeOrEq(one) && isAfterOrEq(another));
	}

	@Deprecated
	public static ProtocolVersion fromId(int id) {
		ProtocolVersion version = byProtocolId.get(id);
		return version != null ? version : UNKNOWN;
	}

	public ProtocolVersion next() {
		Validate.isTrue(getProtocolType() != ProtocolType.UNKNOWN, "Can't get next version for unknown protocol type");
		return Utils.getFromArrayOrNull(byOrderId.get(getProtocolType()), orderId.id + 1);
	}

	public ProtocolVersion previous() {
		Validate.isTrue(getProtocolType() != ProtocolType.UNKNOWN, "Can't get next version for unknown protocol type");
		return Utils.getFromArrayOrNull(byOrderId.get(getProtocolType()), orderId.id - 1);
	}

	public static ProtocolVersion[] getAllBetween(ProtocolVersion one, ProtocolVersion another) {
		ProtocolType type = one.getProtocolType();
		Validate.isTrue(type == another.getProtocolType(), "Can't get versions between different protocol types");
		Validate.isTrue(type != ProtocolType.UNKNOWN, "Can't get versions for unknown protocol type");
		ProtocolVersion[] versions = byOrderId.get(type);
		int startId = Math.min(one.orderId.id, another.orderId.id);
		int endId = Math.max(one.orderId.id, another.orderId.id);
		ProtocolVersion[] between = new ProtocolVersion[(endId - startId) + 1];
		for (int i = startId; i <= endId; i++) {
			between[i - startId] = versions[i];
		}
		return between;
	}

	public static ProtocolVersion[] getAllAfterI(ProtocolVersion version) {
		return getAllBetween(version, getLatest(version.getProtocolType()));
	}

	public static ProtocolVersion[] getAllAfterE(ProtocolVersion version) {
		ProtocolVersion next = version.next();
		if ((next == null) || !next.isSupported()) {
			return new ProtocolVersion[0];
		} else {
			return getAllAfterI(next);
		}
	}

	public static ProtocolVersion[] getAllBeforeI(ProtocolVersion version) {
		return getAllBetween(getOldest(version.getProtocolType()), version);
	}

	public static ProtocolVersion[] getAllBeforeE(ProtocolVersion version) {
		ProtocolVersion prev = version.previous();
		if ((prev == null) || !prev.isSupported()) {
			return new ProtocolVersion[0];
		} else {
			return getAllBeforeI(prev);
		}
	}

	public static ProtocolVersion getLatest(ProtocolType type) {
		switch (type) {
			case PC: {
				return MINECRAFT_1_16_4;
			}
			default: {
				throw new IllegalArgumentException(MessageFormat.format("No supported versions for protocol type {0}", type));
			}
		}
	}

	public static ProtocolVersion getOldest(ProtocolType type) {
		switch (type) {
			case PC: {
				return MINECRAFT_1_4_7;
			}
			default: {
				throw new IllegalArgumentException(MessageFormat.format("No supported versions for protocol type {0}", type));
			}
		}
	}

	public static ProtocolVersion[] getAllSupported() {
		return allSupported.clone();
	}

	private static class OrderId implements Comparable<OrderId> {

		private final ProtocolType type;
		private final int id;

		public OrderId(ProtocolType type, int id) {
			this.type = type;
			this.id = id;
		}

		@Override
		public int compareTo(OrderId o) {
			Validate.isTrue(this.type != ProtocolType.UNKNOWN, "Can't compare unknown protocol type");
			Validate.isTrue(o.type != ProtocolType.UNKNOWN, "Can't compare with unknown protocol type");
			Validate.isTrue(this.type == o.type, "Cant compare order from different types");
			return Integer.compare(id, o.id);
		}

	}

}
