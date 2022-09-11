package clipper2.engine;

import java.util.HashMap;

public enum VertexFlags {

//	public static final int None = 0;
//	public static final int OpenStart = 1;
//	public static final int OpenEnd = 2;
//	public static final int LocalMax = 4;
//	public static final int LocalMin = 8;

	None(0), OpenStart(1), OpenEnd(2), LocalMax(4), LocalMin(8);

	private final int intValue;
	private static HashMap<Integer, VertexFlags> mappings;

	private VertexFlags(int value) {
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue() {
		return intValue;
	}

	public static VertexFlags forValue(int value) {
		return mappings.get(value);
	}

	private static HashMap<Integer, VertexFlags> getMappings() {
		if (mappings == null) {
			mappings = new HashMap<>();
		}
		return mappings;
	}

}
