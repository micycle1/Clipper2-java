package clipper2.core;

import clipper2.engine.PointInPolygonResult;

public final class InternalClipper {

	private static final long MaxInt64 = 9223372036854775807L;
	private static final long MaxCoord = MaxInt64 / 4;
	private static final double max_coord = MaxCoord;
	private static final double min_coord = -MaxCoord;
	private static final long Invalid64 = MaxInt64;

	private static final double FLOATING_POINT_TOLERANCE = 1E-12;
	private static final double DEFAULT_MIN_EDGE_LENGTH = 0.1;

	private static final String PRECISION_RANGE_ERROR = "Error: Precision is out of range.";

	public static void CheckPrecision(int precision)
	{
		if (precision < -8 || precision > 8)
			throw new IllegalArgumentException(PRECISION_RANGE_ERROR);
	}

	private InternalClipper() {
	}

	public static boolean IsAlmostZero(double value) {
		return (Math.abs(value) <= FLOATING_POINT_TOLERANCE);
	}

	public static double CrossProduct(Point64 pt1, Point64 pt2, Point64 pt3) {
		return ((pt2.x - pt1.x) * (pt3.y - pt2.y) - (pt2.y - pt1.y) * (pt3.x - pt2.x));
	}

	public static double DotProduct(Point64 pt1, Point64 pt2, Point64 pt3) {
		return ((pt2.x - pt1.x) * (pt3.x - pt2.x) + (pt2.y - pt1.y) * (pt3.y - pt2.y));
	}

	public static double CrossProduct(PointD vec1, PointD vec2) {
		return (vec1.y * vec2.x - vec2.y * vec1.x);
	}

	public static double DotProduct(PointD vec1, PointD vec2) {
		return (vec1.x * vec2.x + vec1.y * vec2.y);
	}

	public static long CheckCastInt64(double val)
	{
		if ((val >= max_coord) || (val <= min_coord)) return Invalid64;
		return (long)Math.rint(val);
	}

	public static boolean GetIntersectPt(Point64 ln1a, Point64 ln1b, Point64 ln2a, Point64 ln2b, /*out*/ Point64 ip)
	{
		double dy1 = (ln1b.y - ln1a.y);
		double dx1 = (ln1b.x - ln1a.x);
		double dy2 = (ln2b.y - ln2a.y);
		double dx2 = (ln2b.x - ln2a.x);
		double cp = dy1 * dx2 - dy2 * dx1;
		if (cp == 0.0) {
			return false;
		}
		double qx = dx1 * ln1a.y - dy1 * ln1a.x;
		double qy = dx2 * ln2a.y - dy2 * ln2a.x;
		ip.x = CheckCastInt64((dx1 * qy - dx2 * qx) / cp);
		ip.y = CheckCastInt64((dy1 * qy - dy2 * qx) / cp);
		return (ip.x != Invalid64 && ip.y != Invalid64);
	}

	public static boolean GetIntersectPoint(Point64 ln1a, Point64 ln1b, Point64 ln2a, Point64 ln2b, /* out */ PointD ip) {
		double dy1 = (ln1b.y - ln1a.y);
		double dx1 = (ln1b.x - ln1a.x);
		double dy2 = (ln2b.y - ln2a.y);
		double dx2 = (ln2b.x - ln2a.x);
		double q1 = dy1 * ln1a.x - dx1 * ln1a.y;
		double q2 = dy2 * ln2a.x - dx2 * ln2a.y;
		double cross_prod = dy1 * dx2 - dy2 * dx1;
		if (cross_prod == 0.0) {
			return false;
		}
		ip.x = (dx2 * q1 - dx1 * q2) / cross_prod;
		ip.y = (dy2 * q1 - dy1 * q2) / cross_prod;
		return true;
	}

	public static boolean SegsIntersect(Point64 seg1a, Point64 seg1b, Point64 seg2a, Point64 seg2b) {
		return SegsIntersect(seg1a, seg1b, seg2a, seg2b, false);
	}

	public static boolean SegsIntersect(Point64 seg1a, Point64 seg1b, Point64 seg2a, Point64 seg2b, boolean inclusive) {
		if (inclusive) {
			double res1 = CrossProduct(seg1a, seg2a, seg2b);
			double res2 = CrossProduct(seg1b, seg2a, seg2b);
			if (res1 * res2 > 0) {
				return false;
			}
			double res3 = CrossProduct(seg2a, seg1a, seg1b);
			double res4 = CrossProduct(seg2b, seg1a, seg1b);
			if (res3 * res4 > 0) {
				return false;
			}
			// ensure NOT collinear
			return (res1 != 0 || res2 != 0 || res3 != 0 || res4 != 0);
		} else {
			return (CrossProduct(seg1a, seg2a, seg2b) *
					CrossProduct(seg1b, seg2a, seg2b) < 0) &&
					(CrossProduct(seg2a, seg1a, seg1b) *
							CrossProduct(seg2b, seg1a, seg1b) < 0);
		}
	}

	public static Point64 GetClosestPtOnSegment(Point64 offPt, Point64 seg1, Point64 seg2)
	{
		if (seg1.x == seg2.x && seg1.y == seg2.y) return seg1;
		double dx = (seg2.x - seg1.x);
		double dy = (seg2.y - seg1.y);
		double q = ((offPt.x - seg1.x) * dx +
				(offPt.y - seg1.y) * dy) / ((dx*dx) + (dy*dy));
		if (q < 0) q = 0; else if (q > 1) q = 1;
		return new Point64(
				seg1.x + Math.rint(q * dx), seg1.y + Math.rint(q* dy));
	}

	public static PointInPolygonResult PointInPolygon(Point64 pt, Path64 polygon) {
		int len = polygon.size(), i = len - 1;

		if (len < 3) {
			return PointInPolygonResult.IsOutside;
		}

		while (i >= 0 && polygon.get(i).y == pt.y) {
			--i;
		}
		if (i < 0) {
			return PointInPolygonResult.IsOutside;
		}

		int val = 0;
		boolean isAbove = polygon.get(i).y < pt.y;
		i = 0;

		while (i < len) {
			if (isAbove) {
				while (i < len && polygon.get(i).y < pt.y) {
					i++;
				}
				if (i == len) {
					break;
				}
			} else {
				while (i < len && polygon.get(i).y > pt.y) {
					i++;
				}
				if (i == len) {
					break;
				}
			}

			Point64 prev;

			Point64 curr = polygon.get(i);
			if (i > 0) {
				prev = polygon.get(i - 1);
			} else {
				prev = polygon.get(len - 1);
			}

			if (curr.y == pt.y) {
				if (curr.x == pt.x || (curr.y == prev.y && ((pt.x < prev.x) != (pt.x < curr.x)))) {
					return PointInPolygonResult.IsOn;
				}
				i++;
				continue;
			}

			if (pt.x < curr.x && pt.x < prev.x) {
				// we're only interested in edges crossing on the left
			} else if (pt.x > prev.x && pt.x > curr.x) {
				val = 1 - val; // toggle val
			} else {
				double d = CrossProduct(prev, curr, pt);
				if (d == 0) {
					return PointInPolygonResult.IsOn;
				}
				if ((d < 0) == isAbove) {
					val = 1 - val;
				}
			}
			isAbove = !isAbove;
			i++;
		}
		if (val == 0) {
			return PointInPolygonResult.IsOutside;
		}
		return PointInPolygonResult.IsInside;
	}

}