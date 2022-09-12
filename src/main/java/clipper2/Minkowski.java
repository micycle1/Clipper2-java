package clipper2;

import java.util.*;

import clipper2.core.FillRule;
import clipper2.core.Point64;
import clipper2.core.PointD;

public class Minkowski {

	public static List<List<Point64>> Sum(List<Point64> pattern, List<Point64> path, boolean isClosed) {
		return Clipper.Union(MinkowskiInternal(pattern, path, true, isClosed), FillRule.NonZero);
	}

//	public static List<List<PointD>> Sum(List<PointD> pattern, List<PointD> path, boolean isClosed) { // NOTE
//		return Sum(pattern, path, isClosed, 2);
//	}

	public static List<List<PointD>> Sum(List<PointD> pattern, List<PointD> path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		List<List<Point64>> tmp = Clipper.Union(
				MinkowskiInternal(Clipper.ScalePath64(pattern, scale), Clipper.ScalePath64(path, scale), true, isClosed), FillRule.NonZero);
		return Clipper.ScalePathsD(tmp, 1 / scale);
	}

	public static List<List<Point64>> Diff(List<Point64> pattern, List<Point64> path, boolean isClosed) {
		return Clipper.Union(MinkowskiInternal(pattern, path, false, isClosed), FillRule.NonZero);
	}

//	public static List<List<PointD>> Diff(List<PointD> pattern, List<PointD> path, boolean isClosed) { // NOTE
//		return Diff(pattern, path, isClosed, 2);
//	}

	public static List<List<PointD>> Diff(List<PointD> pattern, List<PointD> path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		List<List<Point64>> tmp = Clipper.Union(
				MinkowskiInternal(Clipper.ScalePath64(pattern, scale), Clipper.ScalePath64(path, scale), false, isClosed),
				FillRule.NonZero);
		return Clipper.ScalePathsD(tmp, 1 / scale);
	}

	private static List<List<Point64>> MinkowskiInternal(List<Point64> pattern, List<Point64> path, boolean isSum, boolean isClosed) {
		int delta = isClosed ? 0 : 1;
		int patLen = pattern.size(), pathLen = path.size();
		List<List<Point64>> tmp = new ArrayList<>(pathLen);

		for (Point64 pathPt : path) {
			List<Point64> path2 = new ArrayList<>(patLen);
			if (isSum) {
				for (Point64 basePt : pattern) {
					path2.add(Point64.opAdd(pathPt.clone(), basePt.clone()));
				}
			} else {
				for (Point64 basePt : pattern) {
					path2.add(Point64.opSubtract(pathPt.clone(), basePt.clone()));
				}
			}
			tmp.add(path2);
		}

		List<List<Point64>> result = new ArrayList<>((pathLen - delta) * patLen);
		int g = isClosed ? pathLen - 1 : 0;

		int h = patLen - 1;
		for (int i = delta; i < pathLen; i++) {
			for (int j = 0; j < patLen; j++) {
				List<Point64> quad = new ArrayList<>(
						Arrays.asList(tmp.get(g).get(h), tmp.get(i).get(h), tmp.get(i).get(j), tmp.get(g).get(j)));
				if (!Clipper.IsPositive(quad)) {
					result.add(Clipper.ReversePath(quad));
				} else {
					result.add(quad);
				}
				h = j;
			}
			g = i;
		}
		return result;
	}

}