package com.github.micycle1.clipper2;

import com.github.micycle1.clipper2.core.FillRule;
import com.github.micycle1.clipper2.core.Path64;
import com.github.micycle1.clipper2.core.PathD;
import com.github.micycle1.clipper2.core.Paths64;
import com.github.micycle1.clipper2.core.PathsD;
import com.github.micycle1.clipper2.core.Point64;

public final class Minkowski {

	private Minkowski() {
	}

	public static Paths64 sum(Path64 pattern, Path64 path, boolean isClosed) {
		return Clipper.union(MinkowskiInternal(pattern, path, true, isClosed), FillRule.NonZero);
	}

	public static PathsD sum(PathD pattern, PathD path, boolean isClosed) {
		return sum(pattern, path, isClosed, 2);
	}

	public static PathsD sum(PathD pattern, PathD path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		Paths64 tmp = Clipper.union(
				MinkowskiInternal(Clipper.scalePath64(pattern, scale), Clipper.scalePath64(path, scale), true, isClosed), FillRule.NonZero);
		return Clipper.scalePathsD(tmp, 1 / scale);
	}

	public static Paths64 diff(Path64 pattern, Path64 path, boolean isClosed) {
		return Clipper.union(MinkowskiInternal(pattern, path, false, isClosed), FillRule.NonZero);
	}

	public static PathsD diff(PathD pattern, PathD path, boolean isClosed) {
		return diff(pattern, path, isClosed, 2);
	}

	public static PathsD diff(PathD pattern, PathD path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		Paths64 tmp = Clipper.union(
				MinkowskiInternal(Clipper.scalePath64(pattern, scale), Clipper.scalePath64(path, scale), false, isClosed),
				FillRule.NonZero);
		return Clipper.scalePathsD(tmp, 1 / scale);
	}

	private static Paths64 MinkowskiInternal(Path64 pattern, Path64 path, boolean isSum, boolean isClosed) {
		int delta = isClosed ? 0 : 1;
		int patLen = pattern.size(), pathLen = path.size();
		Paths64 tmp = new Paths64(pathLen);

		for (Point64 pathPt : path) {
			Path64 path2 = new Path64(patLen);
			if (isSum) {
				for (Point64 basePt : pattern) {
					path2.add(Point64.opAdd(pathPt, basePt));
				}
			} else {
				for (Point64 basePt : pattern) {
					path2.add(Point64.opSubtract(pathPt, basePt));
				}
			}
			tmp.add(path2);
		}

		Paths64 result = new Paths64((pathLen - delta) * patLen);
		int g = isClosed ? pathLen - 1 : 0;

		int h = patLen - 1;
		for (int i = delta; i < pathLen; i++) {
			for (int j = 0; j < patLen; j++) {
				Path64 quad = new Path64(tmp.get(g).get(h), tmp.get(i).get(h), tmp.get(i).get(j), tmp.get(g).get(j));
				if (!Clipper.isPositive(quad)) {
					result.add(Clipper.reversePath(quad));
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
