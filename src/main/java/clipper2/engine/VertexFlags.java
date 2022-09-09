package clipper2.engine;

public enum VertexFlags {

	None(0), OpenStart(1), OpenEnd(2), LocalMax(4), LocalMin(8);

	private final int intValue;

	private VertexFlags(int value) {
		intValue = value;
	}

	public int getValue() {
		return intValue;
	}

}
