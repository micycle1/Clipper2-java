package clipper2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.InternalClipper;
import clipper2.core.PathType;
import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.core.Rect64;
import clipper2.core.RectD;
import clipper2.engine.Clipper64;
import clipper2.engine.ClipperD;
import clipper2.engine.PointInPolygonResult;
import clipper2.engine.PolyPath64;
import clipper2.engine.PolyTree64;
import clipper2.offset.ClipperOffset;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;

public final class Clipper {

	public static final Rect64 MaxInvalidRect64 = new Rect64(Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);

	public static final RectD MaxInvalidRectD = new RectD(Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

	public static List<List<Point64>> Intersect(List<List<Point64>> subject, List<List<Point64>> clip, FillRule fillRule) {
		return BooleanOp(ClipType.Intersection, subject, clip, fillRule);
	}

//	public static List<List<PointD>> Intersect(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule) {
//		return Intersect(subject, clip, fillRule, 2);
//	}

	public static List<List<PointD>> Intersect(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule,
			int roundingDecimalPrecision) {
		return BooleanOp(ClipType.Intersection, subject, clip, fillRule, roundingDecimalPrecision);
	}

	public static List<List<Point64>> Union(List<List<Point64>> subject, FillRule fillRule) {
		return BooleanOp(ClipType.Union, subject, null, fillRule);
	}

	public static List<List<Point64>> Union(List<List<Point64>> subject, List<List<Point64>> clip, FillRule fillRule) {
		return BooleanOp(ClipType.Union, subject, clip, fillRule);
	}

//	public static List<List<PointD>> Union(List<List<PointD>> subject, FillRule fillRule) {
//		return BooleanOp(ClipType.Union, subject, null, fillRule);
//	}
//
//	public static List<List<PointD>> Union(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule) {
//		return Union(subject, clip, fillRule, 2);
//	}

	public static List<List<PointD>> Union(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule,
			int roundingDecimalPrecision) {
		return BooleanOp(ClipType.Union, subject, clip, fillRule, roundingDecimalPrecision);
	}

	public static List<List<Point64>> Difference(List<List<Point64>> subject, List<List<Point64>> clip, FillRule fillRule) {
		return BooleanOp(ClipType.Difference, subject, clip, fillRule);
	}

//	public static List<List<PointD>> Difference(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule) {
//		return Difference(subject, clip, fillRule, 2);
//	}

	public static List<List<PointD>> Difference(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule,
			int roundingDecimalPrecision) {
		return BooleanOp(ClipType.Difference, subject, clip, fillRule, roundingDecimalPrecision);
	}

	public static List<List<Point64>> Xor(List<List<Point64>> subject, List<List<Point64>> clip, FillRule fillRule) {
		return BooleanOp(ClipType.Xor, subject, clip, fillRule);
	}

//	public static List<List<PointD>> Xor(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule) {
//		return Xor(subject, clip, fillRule, 2);
//	}

	public static List<List<PointD>> Xor(List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule,
			int roundingDecimalPrecision) {
		return BooleanOp(ClipType.Xor, subject, clip, fillRule, roundingDecimalPrecision);
	}

	public static List<List<Point64>> BooleanOp(ClipType clipType, List<List<Point64>> subject, List<List<Point64>> clip,
			FillRule fillRule) {
		List<List<Point64>> solution = new ArrayList<>();
		if (subject == null) {
			return solution;
		}
		Clipper64 c = new Clipper64();
		c.AddPaths(subject, PathType.Subject);
		if (clip != null) {
			c.AddPaths(clip, PathType.Clip);
		}
		c.Execute(clipType, fillRule, solution);
		return solution;
	}

//	public static List<List<PointD>> BooleanOp(ClipType clipType, List<List<PointD>> subject, List<List<PointD>> clip, FillRule fillRule) {
//		return BooleanOp(clipType, subject, clip, fillRule, 2);
//	}

	public static List<List<PointD>> BooleanOp(ClipType clipType, List<List<PointD>> subject, @Nullable List<List<PointD>> clip,
			FillRule fillRule, int roundingDecimalPrecision) {
		List<List<PointD>> solution = new ArrayList<>();
		ClipperD c = new ClipperD(roundingDecimalPrecision);
		c.AddSubjectsD(subject);
		if (clip != null) {
			c.AddClipsD(clip);
		}
		c.Execute(clipType, fillRule, solution);
		return solution;
	}

	public static List<List<Point64>> InflatePaths(List<List<Point64>> paths, double delta, JoinType joinType, EndType endType) {
		return InflatePaths(paths, delta, joinType, endType, 2.0);
	}

	public static List<List<Point64>> InflatePaths(List<List<Point64>> paths, double delta, JoinType joinType, EndType endType,
			double miterLimit) {
		ClipperOffset co = new ClipperOffset(miterLimit);
		co.AddPaths(paths, joinType, endType);
		return co.Execute(delta);
	}

//	public static List<List<PointD>> InflatePaths(List<List<PointD>> paths, double delta, JoinType joinType, EndType endType, double miterLimit) { // NOTE
//		return InflatePaths(paths, delta, joinType, endType, miterLimit, 2);
//	}
//
//	public static List<List<PointD>> InflatePaths(List<List<PointD>> paths, double delta, JoinType joinType, EndType endType) {
//		return InflatePaths(paths, delta, joinType, endType, 2.0, 2);
//	}

	public static List<List<PointD>> InflatePaths(List<List<PointD>> paths, double delta, JoinType joinType, EndType endType,
			double miterLimit, int precision) {
		if (precision < -8 || precision > 8) {
			throw new RuntimeException("Error: Precision is out of range.");
		}
		double scale = Math.pow(10, precision);
		List<List<Point64>> tmp = ScalePaths64(paths, scale);
		ClipperOffset co = new ClipperOffset(miterLimit);
		co.AddPaths(tmp, joinType, endType);
		tmp = co.Execute(delta * scale);
		return ScalePathsD(tmp, 1 / scale);
	}

	public static List<List<Point64>> MinkowskiSum(List<Point64> pattern, List<Point64> path, boolean isClosed) {
		return Minkowski.Sum(pattern, path, isClosed);
	}

	public static List<List<Point64>> MinkowskiDiff(List<Point64> pattern, List<Point64> path, boolean isClosed) {
		return Minkowski.Diff(pattern, path, isClosed);
	}

	public static double AreaPath(List<Point64> path) {
		// https://en.wikipedia.org/wiki/Shoelace_formula
		double a = 0.0;
		int cnt = path.size();
		if (cnt < 3) {
			return 0.0;
		}
		Point64 prevPt = path.get(cnt - 1).clone();
		for (Point64 pt : path) {
			a += (double) (prevPt.Y + pt.Y) * (prevPt.X - pt.X);
			prevPt = pt.clone();
		}
		return a * 0.5;
	}

	public static double Area(List<List<Point64>> paths) {
		double a = 0.0;
		for (List<Point64> path : paths) {
			a += AreaPath(path);
		}
		return a;
	}

//	public static double AreaPath(List<PointD> path) {
//	  double a = 0.0;
//	  int cnt = path.size();
//	  if (cnt < 3) {
//		  return 0.0;
//	  }
//	  PointD prevPt = path.get(cnt - 1).clone();
//	  for (PointD pt : path) {
//		a += (prevPt.y + pt.y) * (prevPt.x - pt.x);
//		prevPt = pt.clone();
//	  }
//	  return a * 0.5;
//	}
//
//	public static double Area(List<List<PointD>> paths) {
//	  double a = 0.0;
//	  for (List<PointD> path : paths) {
//		a += Area(path);
//	  }
//	  return a;
//	}

	public static boolean IsPositive(List<Point64> poly) {
		return AreaPath(poly) >= 0;
	}

//	public static boolean IsPositive(List<PointD> poly) {
//		return AreaPath(poly) >= 0;
//	}

	public static String Path64ToString(List<Point64> path) {
		String result = "";
		for (Point64 pt : path) {
			result = result + pt.toString();
		}
		return result + '\n';
	}

	public static String Paths64ToString(List<List<Point64>> paths) {
		String result = "";
		for (List<Point64> path : paths) {
			result = result + Path64ToString(path);
		}
		return result;
	}

	public static String PathDToString(List<PointD> path) {
		String result = "";
		for (PointD pt : path) {
			result = result + pt.toString();
		}
		return result + '\n';
	}

	public static String PathsDToString(List<List<PointD>> paths) {
		String result = "";
		for (List<PointD> path : paths) {
			result = result + PathDToString(path);
		}
		return result;
	}

	public static List<Point64> OffsetPath(List<Point64> path, long dx, long dy) {
		List<Point64> result = new ArrayList<>(path.size());
		for (Point64 pt : path) {
			result.add(new Point64(pt.X + dx, pt.Y + dy));
		}
		return result;
	}

	public static Point64 ScalePoint64(Point64 pt, double scale) {
		Point64 result = new Point64();
		result.X = (long) (pt.X * scale);
		result.Y = (long) (pt.Y * scale);
		return result.clone();
	}

	public static PointD ScalePointD(Point64 pt, double scale) {
		PointD result = new PointD();
		result.x = pt.X * scale;
		result.y = pt.Y * scale;
		return result.clone();
	}

	public static List<Point64> ScalePath(List<Point64> path, double scale) {
		if (InternalClipper.IsAlmostZero(scale - 1)) {
			return path;
		}
		List<Point64> result = new ArrayList<>(path.size());
		for (Point64 pt : path) {
			result.add(new Point64(pt.X * scale, pt.Y * scale));
		}
		return result;
	}

	public static List<List<Point64>> ScalePaths(List<List<Point64>> paths, double scale) {
		if (InternalClipper.IsAlmostZero(scale - 1)) {
			return paths;
		}
		List<List<Point64>> result = new ArrayList<>(paths.size());
		for (List<Point64> path : paths) {
			result.add(ScalePath(path, scale));
		}
		return result;
	}

//	public static List<PointD> ScalePath(List<PointD> path, double scale) {
//		if (InternalClipper.IsAlmostZero(scale - 1)) {
//			return path;
//		}
//		List<PointD> result = new ArrayList<>(path.size());
//		for (PointD pt : path) {
//			result.add(new PointD(pt.clone(), scale));
//		}
//		return result;
//	}
//
//	public static List<List<PointD>> ScalePaths(List<List<PointD>> paths, double scale) {
//		if (InternalClipper.IsAlmostZero(scale - 1)) {
//			return paths;
//		}
//		List<List<PointD>> result = new ArrayList<>(paths.size());
//		for (List<PointD> path : paths) {
//			result.add(ScalePath(path, scale));
//		}
//		return result;
//	}

	// Unlike ScalePath, both ScalePath64 & ScalePathD also involve type conversion
	public static List<Point64> ScalePath64(List<PointD> path, double scale) {
		int cnt = path.size();
		List<Point64> res = new ArrayList<>(cnt);
		for (PointD pt : path) {
			res.add(new Point64(pt.clone(), scale));
		}
		return res;
	}

	public static List<List<Point64>> ScalePaths64(List<List<PointD>> paths, double scale) {
		int cnt = paths.size();
		List<List<Point64>> res = new ArrayList<>(cnt);
		for (List<PointD> path : paths) {
			res.add(ScalePath64(path, scale));
		}
		return res;
	}

	public static List<PointD> ScalePathD(List<Point64> path, double scale) {
		int cnt = path.size();
		List<PointD> res = new ArrayList<>(cnt);
		for (Point64 pt : path) {
			res.add(new PointD(pt.clone(), scale));
		}
		return res;
	}

	public static List<List<PointD>> ScalePathsD(List<List<Point64>> paths, double scale) {
		int cnt = paths.size();
		List<List<PointD>> res = new ArrayList<>(cnt);
		for (List<Point64> path : paths) {
			res.add(ScalePathD(path, scale));
		}
		return res;
	}

	// The static functions Path64 and PathD convert path types without scaling
	public static List<Point64> Path64(List<PointD> path) {
		List<Point64> result = new ArrayList<>(path.size());
		for (PointD pt : path) {
			result.add(new Point64(pt.clone()));
		}
		return result;
	}

	public static List<List<Point64>> Paths64(List<List<PointD>> paths) {
		List<List<Point64>> result = new ArrayList<>(paths.size());
		for (List<PointD> path : paths) {
			result.add(Path64(path));
		}
		return result;
	}

	public static List<List<PointD>> PathsD(List<List<Point64>> paths) {
		List<List<PointD>> result = new ArrayList<>(paths.size());
		for (List<Point64> path : paths) {
			result.add(PathD(path));
		}
		return result;
	}

	public static List<PointD> PathD(List<Point64> path) {
		List<PointD> result = new ArrayList<>(path.size());
		for (Point64 pt : path) {
			result.add(new PointD(pt.clone()));
		}
		return result;
	}

	public static List<Point64> TranslatePath(List<Point64> path, long dx, long dy) {
		List<Point64> result = new ArrayList<>(path.size());
		for (Point64 pt : path) {
			result.add(new Point64(pt.X + dx, pt.Y + dy));
		}
		return result;
	}

	public static List<List<Point64>> TranslatePaths(List<List<Point64>> paths, long dx, long dy) {
		List<List<Point64>> result = new ArrayList<>(paths.size());
		for (List<Point64> path : paths) {
			result.add(OffsetPath(path, dx, dy));
		}
		return result;
	}

	public static List<PointD> TranslatePath(List<PointD> path, double dx, double dy) {
		List<PointD> result = new ArrayList<>(path.size());
		for (PointD pt : path) {
			result.add(new PointD(pt.x + dx, pt.y + dy));
		}
		return result;
	}

	public static List<List<PointD>> TranslatePaths(List<List<PointD>> paths, double dx, double dy) {
		List<List<PointD>> result = new ArrayList<>(paths.size());
		for (List<PointD> path : paths) {
			result.add(TranslatePath(path, dx, dy));
		}
		return result;
	}

	public static List<Point64> ReversePath(List<Point64> path) {
		List<Point64> result = new ArrayList<>(path);
		Collections.reverse(result);
		return result;
	}

//	public static List<PointD> ReversePath(List<PointD> path) {
//		List<PointD> result = new ArrayList<>(path);
//		Collections.reverse(result);
//		return result;
//	}

	public static List<List<Point64>> ReversePaths(List<List<Point64>> paths) {
		List<List<Point64>> result = new ArrayList<>(paths.size());
		for (List<Point64> t : paths) {
			result.add(ReversePath(t));
		}

		return result;
	}

//	public static List<List<PointD>> ReversePaths(List<List<PointD>> paths) {
//		List<List<PointD>> result = new ArrayList<>(paths.size());
//		for (List<PointD> path : paths) {
//			result.add(ReversePath(path));
//		}
//		return result;
//	}

	public static Rect64 GetBounds(List<List<Point64>> paths) {
		Rect64 result = MaxInvalidRect64;
		for (List<Point64> path : paths) {
			for (Point64 pt : path) {
				if (pt.X < result.left) {
					result.left = pt.X;
				}
				if (pt.X > result.right) {
					result.right = pt.X;
				}
				if (pt.Y < result.top) {
					result.top = pt.Y;
				}
				if (pt.Y > result.bottom) {
					result.bottom = pt.Y;
				}
			}
		}
		return result.IsEmpty() ? new Rect64() : result;
	}

//	public static RectD GetBounds(List<List<PointD>> paths) {
//		RectD result = MaxInvalidRectD;
//		for (List<PointD> path : paths) {
//			for (PointD pt : path) {
//				if (pt.x < result.left) {
//					result.left = pt.x;
//				}
//				if (pt.x > result.right) {
//					result.right = pt.x;
//				}
//				if (pt.y < result.top) {
//					result.top = pt.y;
//				}
//				if (pt.y > result.bottom) {
//					result.bottom = pt.y;
//				}
//			}
//		}
//		return result.IsEmpty() ? new RectD() : result;
//	}

	public static List<Point64> MakePath(int[] arr) {
		int len = arr.length / 2;
		List<Point64> p = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			p.add(new Point64(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static List<Point64> MakePath(long[] arr) {
		int len = arr.length / 2;
		List<Point64> p = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			p.add(new Point64(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static List<PointD> MakePath(double[] arr) {
		int len = arr.length / 2;
		List<PointD> p = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			p.add(new PointD(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static double Sqr(double value) {
		return value * value;
	}

	public static boolean PointsNearEqual(PointD pt1, PointD pt2, double distanceSqrd) {
		return Sqr(pt1.x - pt2.x) + Sqr(pt1.y - pt2.y) < distanceSqrd;
	}

	public static List<PointD> StripNearDuplicates(List<PointD> path, double minEdgeLenSqrd, boolean isClosedPath) {
		int cnt = path.size();
		List<PointD> result = new ArrayList<>(cnt);
		if (cnt == 0) {
			return result;
		}
		PointD lastPt = path.get(0).clone();
		result.add(lastPt.clone());
		for (int i = 1; i < cnt; i++) {
			if (!PointsNearEqual(lastPt.clone(), path.get(i).clone(), minEdgeLenSqrd)) {
				lastPt = path.get(i).clone();
				result.add(lastPt.clone());
			}
		}

		if (isClosedPath && PointsNearEqual(lastPt.clone(), result.get(0).clone(), minEdgeLenSqrd)) {
			result.remove(result.size() - 1);
		}

		return result;
	}

	public static List<Point64> StripDuplicates(List<Point64> path, boolean isClosedPath) {
		int cnt = path.size();
		List<Point64> result = new ArrayList<>(cnt);
		if (cnt == 0) {
			return result;
		}
		Point64 lastPt = path.get(0).clone();
		result.add(lastPt.clone());
		for (int i = 1; i < cnt; i++) {
			if (!path.get(i).equals(lastPt.clone())) {
				lastPt = path.get(i).clone();
				result.add(lastPt.clone());
			}
		}
		if (isClosedPath && result.get(0).equals(lastPt.clone())) {
			result.remove(result.size() - 1);
		}
		return result;
	}

	private static void AddPolyNodeToPaths(PolyPath64 polyPath, List<List<Point64>> paths) {
		if (!polyPath.getPolygon().isEmpty()) {
			paths.add(polyPath.getPolygon());
		}
		for (int i = 0; i < polyPath.getCount(); i++) {
			AddPolyNodeToPaths((PolyPath64) polyPath._childs.get(i), paths);
		}
	}

	public static List<List<Point64>> PolyTreeToPaths64(PolyTree64 polyTree) {
		List<List<Point64>> result = new ArrayList<>();
		for (int i = 0; i < polyTree.getCount(); i++) {
			AddPolyNodeToPaths((PolyPath64) polyTree._childs.get(i), result);
		}
		return result;
	}

//	public static void AddPolyNodeToPathsD(PolyPathD polyPath, List<List<PointD>> paths) {
//	  if (polyPath.getPolygon().size() > 0) {
//		paths.add(polyPath.getPolygon());
//	  }
//	  for (int i = 0; i < polyPath.getCount(); i++) {
//		AddPolyNodeToPathsD((PolyPathD) polyPath._childs.get(i), paths);
//	  }
//	}
//
//	public static List<List<PointD>> PolyTreeToPathsD(PolyTreeD polyTree) {
//		List<List<PointD>> result = new ArrayList<>();
//		for (var polyPathBase : polyTree) {
//			PolyPathD p = (PolyPathD) polyPathBase;
//			AddPolyNodeToPathsD(p, result);
//		}
//
//		return result;
//	}

	public static double PerpendicDistFromLineSqrd(PointD pt, PointD line1, PointD line2) {
		double a = pt.x - line1.x;
		double b = pt.y - line1.y;
		double c = line2.x - line1.x;
		double d = line2.y - line1.y;
		if (c == 0 && d == 0) {
			return 0;
		}
		return Sqr(a * d - c * b) / (c * c + d * d);
	}

	public static double PerpendicDistFromLineSqrd(Point64 pt, Point64 line1, Point64 line2) {
		double a = (double) pt.X - line1.X;
		double b = (double) pt.Y - line1.Y;
		double c = (double) line2.X - line1.X;
		double d = (double) line2.Y - line1.Y;
		if (c == 0 && d == 0) {
			return 0;
		}
		return Sqr(a * d - c * b) / (c * c + d * d);
	}

	public static void RDP(List<Point64> path, int begin, int end, double epsSqrd, List<Boolean> flags) {
		int idx = 0;
		double max_d = 0;
		while (end > begin && path.get(begin).equals(path.get(end).clone())) {
			flags.set(end--, false);
		}
		for (int i = begin + 1; i < end; ++i) {
			// PerpendicDistFromLineSqrd - avoids expensive Sqrt()
			double d = PerpendicDistFromLineSqrd(path.get(i).clone(), path.get(begin).clone(), path.get(end).clone());
			if (d <= max_d) {
				continue;
			}
			max_d = d;
			idx = i;
		}
		if (max_d <= epsSqrd) {
			return;
		}
		flags.set(idx, true);
		if (idx > begin + 1) {
			RDP(path, begin, idx, epsSqrd, flags);
		}
		if (idx < end - 1) {
			RDP(path, idx, end, epsSqrd, flags);
		}
	}

	public static List<Point64> RamerDouglasPeuckerPath(List<Point64> path, double epsilon) {
		int len = path.size();
		if (len < 5) {
			return path;
		}
		List<Boolean> flags = new ArrayList<>(Arrays.asList(new Boolean[len]));
		flags.set(0, true);
		flags.set(len - 1, true);
		RDP(path, 0, len - 1, Sqr(epsilon), flags);
		List<Point64> result = new ArrayList<>(len);
		for (int i = 0; i < len; ++i) {
			if (flags.get(i).booleanValue()) {
				result.add(path.get(i).clone());
			}
		}
		return result;
	}

	public static List<List<Point64>> RamerDouglasPeucker(List<List<Point64>> paths, double epsilon) {
		List<List<Point64>> result = new ArrayList<>(paths.size());
		for (List<Point64> path : paths) {
			result.add(RamerDouglasPeuckerPath(path, epsilon));
		}
		return result;
	}

//	public static void RDP(List<PointD> path, int begin, int end, double epsSqrd, List<Boolean> flags) {
//		int idx = 0;
//		double max_d = 0;
//		while (end > begin && path.get(begin).equals(path.get(end).clone())) {
//			flags.set(end--, false);
//		}
//		for (int i = begin + 1; i < end; ++i) {
//			// PerpendicDistFromLineSqrd - avoids expensive Sqrt()
//			double d = PerpendicDistFromLineSqrd(path.get(i).clone(), path.get(begin).clone(), path.get(end).clone());
//			if (d <= max_d) {
//				continue;
//			}
//			max_d = d;
//			idx = i;
//		}
//		if (max_d <= epsSqrd) {
//			return;
//		}
//		flags.set(idx, true);
//		if (idx > begin + 1) {
//			RDP(path, begin, idx, epsSqrd, flags);
//		}
//		if (idx < end - 1) {
//			RDP(path, idx, end, epsSqrd, flags);
//		}
//	}

//	public static List<PointD> RamerDouglasPeucker(List<PointD> path, double epsilon) {
//	  int len = path.size();
//	  if (len < 5) {
//		  return path;
//	  }
//	  List<Boolean> flags = new ArrayList<>(Arrays.asList(new Boolean[len]));
//	  flags.[0] = true;
//	  flags.[len - 1] = true;
//	  RDP(path, 0, len - 1, Sqr(epsilon), flags);
//	  List<PointD> result = new ArrayList<>(len);
//	  for (int i = 0; i < len; ++i) {
//		if (flags.get(i)) {
//			result.add(path.get(i).clone());
//		}
//	  }
//	  return result;
//	}

//	public static List<List<PointD>> RamerDouglasPeucker(List<List<PointD>> paths, double epsilon) {
//		List<List<PointD>> result = new ArrayList<>(paths.size());
//		for (List<PointD> path : paths) {
//			result.add(RamerDouglasPeucker(path, epsilon));
//		}
//		return result;
//	}

	public static List<Point64> TrimCollinear(List<Point64> path) {
		return TrimCollinear(path, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public static List<Point64> TrimCollinear(List<Point64> path, bool isOpen = false)
	public static List<Point64> TrimCollinear(List<Point64> path, boolean isOpen) {
		int len = path.size();
		int i = 0;
		if (!isOpen) {
			while (i < len - 1
					&& InternalClipper.CrossProduct(path.get(len - 1).clone(), path.get(i).clone(), path.get(i + 1).clone()) == 0) {
				i++;
			}
			while (i < len - 1
					&& InternalClipper.CrossProduct(path.get(len - 2).clone(), path.get(len - 1).clone(), path.get(i).clone()) == 0) {
				len--;
			}
		}

		if (len - i < 3) {
			if (!isOpen || len < 2 || path.get(0).equals(path.get(1).clone())) {
				return new ArrayList<>();
			}
			return path;
		}

		List<Point64> result = new ArrayList<>(len - i);
		Point64 last = path.get(i).clone();
		result.add(last.clone());
		for (i++; i < len - 1; i++) {
			if (InternalClipper.CrossProduct(last.clone(), path.get(i).clone(), path.get(i + 1).clone()) == 0) {
				continue;
			}
			last = path.get(i).clone();
			result.add(last.clone());
		}

		if (isOpen) {
			result.add(path.get(len - 1).clone());
		} else if (InternalClipper.CrossProduct(last.clone(), path.get(len - 1).clone(), result.get(0).clone()) != 0) {
			result.add(path.get(len - 1).clone());
		} else {
			while (result.size() > 2 && InternalClipper.CrossProduct(result.get(result.size() - 1).clone(),
					result.get(result.size() - 2).clone(), result.get(0).clone()) == 0) {
				result.remove(result.size() - 1);
			}
			if (result.size() < 3) {
				result.clear();
			}
		}
		return result;
	}

	public static List<PointD> TrimCollinear(List<PointD> path, int precision) {
		return TrimCollinear(path, precision, false);
	}

	public static List<PointD> TrimCollinear(List<PointD> path, int precision, boolean isOpen) {
		if (precision < -8 || precision > 8) {
			throw new RuntimeException("Error: Precision is out of range.");
		}
		double scale = Math.pow(10, precision);
		List<Point64> p = ScalePath64(path, scale);
		p = TrimCollinear(p, isOpen);
		return ScalePathD(p, 1 / scale);
	}

	public static PointInPolygonResult PointInPolygon(Point64 pt, List<Point64> polygon) {
		return InternalClipper.PointInPolygon(pt.clone(), polygon);
	}

}