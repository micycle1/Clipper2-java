package clipper2.engine;

public class PointInPolygonResult {

	public static final PointInPolygonResult IsOn = new PointInPolygonResult(0);
	public static final PointInPolygonResult IsInside = new PointInPolygonResult(1);
	public static final PointInPolygonResult IsOutside = new PointInPolygonResult(2);

	private final int intValue;

	private PointInPolygonResult(int value) {
		intValue = value;
	}

	public int getValue() {
		return intValue;
	}

}