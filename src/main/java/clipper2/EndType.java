package clipper2;

public enum EndType {
	Polygon, Joined, Butt, Square, Round;

	public static final int SIZE = java.lang.Integer.SIZE;

	public int getValue() {
		return this.ordinal();
	}

	public static EndType forValue(int value) {
		return values()[value];
	}
}