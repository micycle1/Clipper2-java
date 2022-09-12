package clipper2.core;

import java.util.List;

import clipper2.engine.PointInPolygonResult;

public final class InternalClipper {

	public static final double floatingPointTolerance = 1E-12;
	public static final double defaultMinimumEdgeLength = 0.1;

	public static boolean IsAlmostZero(double value) {
		return (Math.abs(value) <= floatingPointTolerance);
	}

	public static double CrossProduct(Point64 pt1, Point64 pt2, Point64 pt3) {
		// typecast to double to avoid potential int overflow
		return ((double) (pt2.X - pt1.X) * (pt3.Y - pt2.Y) - (double) (pt2.Y - pt1.Y) * (pt3.X - pt2.X));
	}

	public static double DotProduct(Point64 pt1, Point64 pt2, Point64 pt3) {
		// typecast to double to avoid potential int overflow
		return ((double) (pt2.X - pt1.X) * (pt3.X - pt2.X) + (double) (pt2.Y - pt1.Y) * (pt3.Y - pt2.Y));
	}

	public static double CrossProduct(PointD vec1, PointD vec2) {
		return (vec1.y * vec2.x - vec2.y * vec1.x);
	}

	public static double DotProduct(PointD vec1, PointD vec2) {
		return (vec1.x * vec2.x + vec1.y * vec2.y);
	}

	public static boolean GetIntersectPoint(Point64 ln1a, Point64 ln1b, Point64 ln2a, Point64 ln2b, PointD ip) {
		ip.x = 0;
		ip.y = 0;
		double m1, b1, m2, b2;
		if (ln1b.X == ln1a.X) {
			if (ln2b.X == ln2a.X) {
				return false;
			}
			m2 = (double) (ln2b.Y - ln2a.Y) / (ln2b.X - ln2a.X);
			b2 = ln2a.Y - m2 * ln2a.X;
			ip.x = ln1a.X;
			ip.y = m2 * ln1a.X + b2;
		} else if (ln2b.X == ln2a.X) {
			m1 = (double) (ln1b.Y - ln1a.Y) / (ln1b.X - ln1a.X);
			b1 = ln1a.Y - m1 * ln1a.X;
			ip.x = ln2a.X;
			ip.y = m1 * ln2a.X + b1;
		} else {
			m1 = (double) (ln1b.Y - ln1a.Y) / (ln1b.X - ln1a.X);
			b1 = ln1a.Y - m1 * ln1a.X;
			m2 = (double) (ln2b.Y - ln2a.Y) / (ln2b.X - ln2a.X);
			b2 = ln2a.Y - m2 * ln2a.X;
			if (Math.abs(m1 - m2) > floatingPointTolerance) {
				ip.x = (b2 - b1) / (m1 - m2);
				ip.y = m1 * ip.x + b1;
			} else {
				ip.x = (ln1a.X + ln1b.X) * 0.5;
				ip.y = (ln1a.Y + ln1b.Y) * 0.5;
			}
		}

		return true;
	}

	public static boolean SegmentsIntersect(Point64 seg1a, Point64 seg1b, Point64 seg2a, Point64 seg2b) {
		double dx1 = seg1a.X - seg1b.X;
		double dy1 = seg1a.Y - seg1b.Y;
		double dx2 = seg2a.X - seg2b.X;
		double dy2 = seg2a.Y - seg2b.Y;
		return (((dy1 * (seg2a.X - seg1a.X) - dx1 * (seg2a.Y - seg1a.Y)) * (dy1 * (seg2b.X - seg1a.X) - dx1 * (seg2b.Y - seg1a.Y)) < 0)
				&& ((dy2 * (seg1a.X - seg2a.X) - dx2 * (seg1a.Y - seg2a.Y)) * (dy2 * (seg1b.X - seg2a.X) - dx2 * (seg1b.Y - seg2a.Y)) < 0));
	}

	public static PointInPolygonResult PointInPolygon(Point64 pt, List<Point64> polygon) {
		int len = polygon.size(), i = len - 1;

		if (len < 3) {
			return PointInPolygonResult.IsOutside;
		}

		while (i >= 0 && polygon.get(i).Y == pt.Y) {
			--i;
		}
		if (i < 0) {
			return PointInPolygonResult.IsOutside;
		}

		int val = 0;
		boolean isAbove = polygon.get(i).Y < pt.Y;
		i = 0;

		while (i < len) {
			if (isAbove) {
				while (i < len && polygon.get(i).Y < pt.Y) {
					i++;
				}
				if (i == len) {
					break;
				}
			} else {
				while (i < len && polygon.get(i).Y > pt.Y) {
					i++;
				}
				if (i == len) {
					break;
				}
			}

			Point64 prev = new Point64();

			Point64 curr = polygon.get(i).clone();
			if (i > 0) {
				prev = polygon.get(i - 1).clone();
			} else {
				prev = polygon.get(len - 1).clone();
			}

			if (curr.Y == pt.Y) {
				if (curr.X == pt.X || (curr.Y == prev.Y && ((pt.X < prev.X) != (pt.X < curr.X)))) {
					return PointInPolygonResult.IsOn;
				}
				i++;
				continue;
			}

			if (pt.X < curr.X && pt.X < prev.X) {
				// we're only interested in edges crossing on the left
			} else if (pt.X > prev.X && pt.X > curr.X) {
				val = 1 - val; // toggle val
			} else {
				double d = CrossProduct(prev.clone(), curr.clone(), pt.clone());
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