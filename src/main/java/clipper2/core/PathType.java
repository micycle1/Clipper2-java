package clipper2.core;

public enum PathType {
	Subject, Clip;

	public static final int SIZE = java.lang.Integer.SIZE;

	public int getValue() {
		return this.ordinal();
	}

	public static PathType forValue(int value) {
		return values()[value];
	}
}