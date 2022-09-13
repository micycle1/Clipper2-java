package clipper2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.PointD;

public class Minkowski {

	public static Paths64 Sum(Path64 pattern, Path64 path, boolean isClosed) {
		return Clipper.Union(MinkowskiInternal(pattern, path, true, isClosed), FillRule.NonZero);
	}

//	public static List<List<PointD>> Sum(List<PointD> pattern, List<PointD> path, boolean isClosed) { // NOTE
//		return Sum(pattern, path, isClosed, 2);
//	}

	public static List<List<PointD>> Sum(List<PointD> pattern, List<PointD> path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		Paths64 tmp = Clipper.Union(
				MinkowskiInternal(Clipper.ScalePath64(pattern, scale), Clipper.ScalePath64(path, scale), true, isClosed), FillRule.NonZero);
		return Clipper.ScalePathsD(tmp, 1 / scale);
	}

	public static Paths64 Diff(Path64 pattern, Path64 path, boolean isClosed) {
		return Clipper.Union(MinkowskiInternal(pattern, path, false, isClosed), FillRule.NonZero);
	}

//	public static List<List<PointD>> Diff(List<PointD> pattern, List<PointD> path, boolean isClosed) { // NOTE
//		return Diff(pattern, path, isClosed, 2);
//	}

	public static List<List<PointD>> Diff(List<PointD> pattern, List<PointD> path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		Paths64 tmp = Clipper.Union(
				MinkowskiInternal(Clipper.ScalePath64(pattern, scale), Clipper.ScalePath64(path, scale), false, isClosed),
				FillRule.NonZero);
		return Clipper.ScalePathsD(tmp, 1 / scale);
	}

	private static Paths64 MinkowskiInternal(Path64 pattern, Path64 path, boolean isSum, boolean isClosed) {
		int delta = isClosed ? 0 : 1;
		int patLen = pattern.size(), pathLen = path.size();
		Paths64 tmp = new Paths64(pathLen);

		for (Point64 pathPt : path) {
			Path64 path2 = new Path64(patLen);
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

		Paths64 result = Paths64((pathLen - delta) * patLen);
		int g = isClosed ? pathLen - 1 : 0;

		int h = patLen - 1;
		for (int i = delta; i < pathLen; i++) {
			for (int j = 0; j < patLen; j++) {
				Path64 quad = Path64(
						List.of(tmp.get(g).get(h), tmp.get(i).get(h), tmp.get(i).get(j), tmp.get(g).get(j)));
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