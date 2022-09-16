package clipper2.core;

public final class PointD {

	public double x;
	public double y;

	public PointD() {
	}

	public PointD(PointD pt) {
		x = pt.x;
		y = pt.y;
	}

	public PointD(Point64 pt) {
		x = pt.x;
		y = pt.y;
	}

	public PointD(PointD pt, double scale) {
		x = pt.x * scale;
		y = pt.y * scale;
	}

	public PointD(Point64 pt, double scale) {
		x = pt.x * scale;
		y = pt.y * scale;
	}

	public PointD(long x, long y) {
		this.x = x;
		this.y = y;
	}

	public PointD(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return String.format("(%1$f,%2$f) ", x, y);
	}

	public static boolean opEquals(PointD lhs, PointD rhs) {
		return InternalClipper.IsAlmostZero(lhs.x - rhs.x) && InternalClipper.IsAlmostZero(lhs.y - rhs.y);
	}

	public static boolean opNotEquals(PointD lhs, PointD rhs) {
		return !InternalClipper.IsAlmostZero(lhs.x - rhs.x) || !InternalClipper.IsAlmostZero(lhs.y - rhs.y);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PointD p) {
			return opEquals(this, p);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(x * 31 + y);
	}

	@Override
	public PointD clone() {
		return new PointD(x, y);
	}
}