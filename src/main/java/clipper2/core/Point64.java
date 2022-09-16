package clipper2.core;

public final class Point64 {

	public long x;
	public long y;

	public Point64() {
	}

	public Point64(Point64 pt) {
		this.x = pt.x;
		this.y = pt.y;
	}

	public Point64(long x, long y) {
		this.x = x;
		this.y = y;
	}

	public Point64(double x, double y) {
		this.x = (long) Math.rint(x);
		this.y = (long) Math.rint(y);
	}

	public Point64(PointD pt) {
		x = (long) Math.rint(pt.x);
		y = (long) Math.rint(pt.y);
	}

	public Point64(Point64 pt, double scale) {
		x = (long) Math.rint(pt.x * scale);
		y = (long) Math.rint(pt.y * scale);
	}

	public Point64(PointD pt, double scale) {
		x = (long) Math.rint(pt.x * scale);
		y = (long) Math.rint(pt.y * scale);
	}

	public boolean opEquals(Point64 o) {
		return x == o.x && y == o.y;
	}

	public static boolean opEquals(Point64 lhs, Point64 rhs) {
		return lhs.x == rhs.x && lhs.y == rhs.y;
	}

	public boolean opNotEquals(Point64 o) {
		return x != o.x || y != o.y;
	}

	public static boolean opNotEquals(Point64 lhs, Point64 rhs) {
		return lhs.x != rhs.x || lhs.y != rhs.y;
	}

	public static Point64 opAdd(Point64 lhs, Point64 rhs) {
		return new Point64(lhs.x + rhs.x, lhs.y + rhs.y);
	}

	public static Point64 opSubtract(Point64 lhs, Point64 rhs) {
		return new Point64(lhs.x - rhs.x, lhs.y - rhs.y);
	}

	@Override
	public String toString() {
		return String.format("(%1$s,%2$s) ", x, y); // nb: trailing space
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Point64 p) {
			return opEquals(this, p);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(x * 31 + y);
	}

	@Override
	public Point64 clone() {
		return new Point64(x, y);
	}
}