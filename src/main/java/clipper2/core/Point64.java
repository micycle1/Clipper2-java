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

	public static boolean opEquals(Point64 lhs, Point64 rhs) {
		return lhs.X == rhs.X && lhs.Y == rhs.Y;
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
		return String.format("%1$s,%2$s ", X, Y); // nb: trailing space
	}

	@Override
	public boolean equals(Object obj) {
		boolean tempVar = obj instanceof Point64;
		Point64 p = tempVar ? (Point64) obj : null;
		if (tempVar) {
			return opEquals(this.clone(), p);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public Point64 clone() {
		Point64 varCopy = new Point64();

		varCopy.X = this.X;
		varCopy.Y = this.Y;

		return varCopy;
	}
}