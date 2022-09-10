package clipper2.offset;

public enum JoinType {
	Square, Round, Miter;

	public static final int SIZE = java.lang.Integer.SIZE;

	public int getValue() {
		return this.ordinal();
	}

	public static JoinType forValue(int value) {
		return values()[value];
	}
}