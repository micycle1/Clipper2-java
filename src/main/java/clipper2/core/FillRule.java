package clipper2.core;

// By far the most widely used filling rules for polygons are EvenOdd
// and NonZero, sometimes called Alternate and Winding respectively.
// https://en.wikipedia.org/wiki/Nonzero-rule
public enum FillRule {

	EvenOdd, NonZero, Positive, Negative;

	public int getValue() {
		return this.ordinal();
	}

	public static FillRule forValue(int value) {
		return values()[value];
	}
}