package clipper2.core;

public final class Point64 {

	public long X;
	public long Y;

	public Point64() {
	}

	public Point64(Point64 pt) {
		X = pt.X;
		Y = pt.Y;
	}

	public Point64(long x, long y) {
		X = x;
		Y = y;
	}

	public Point64(double x, double y) {
		X = (long) Math.rint(x);
		Y = (long) Math.rint(y);
	}

	public Point64(PointD pt) {
		X = (long) Math.rint(pt.x);
		Y = (long) Math.rint(pt.y);
	}

	public Point64(Point64 pt, double scale) {
		X = (long) Math.rint(pt.X * scale);
		Y = (long) Math.rint(pt.Y * scale);
	}

	public Point64(PointD pt, double scale) {
		X = (long) Math.rint(pt.x * scale);
		Y = (long) Math.rint(pt.y * scale);
	}

	public boolean opEquals(Point64 o) {
		return X == o.X && Y == o.Y;
	}

	public static boolean opEquals(Point64 lhs, Point64 rhs) {
		return lhs.X == rhs.X && lhs.Y == rhs.Y;
	}

	public boolean opNotEquals(Point64 o) {
		return X != o.X || Y != o.Y;
	}

	public static boolean opNotEquals(Point64 lhs, Point64 rhs) {
		return lhs.X != rhs.X || lhs.Y != rhs.Y;
	}

	public static Point64 opAdd(Point64 lhs, Point64 rhs) {
		return new Point64(lhs.X + rhs.X, lhs.Y + rhs.Y);
	}

	public static Point64 opSubtract(Point64 lhs, Point64 rhs) {
		return new Point64(lhs.X - rhs.X, lhs.Y - rhs.Y);
	}

	@Override
	public String toString() {
		return String.format("(%1$s,%2$s) ", X, Y); // nb: trailing space
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
		return Long.hashCode(X * 31 + Y);
	}

	@Override
	public Point64 clone() {
		return new Point64(X, Y);
	}
}