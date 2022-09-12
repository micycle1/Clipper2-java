package clipper2;

import java.util.ArrayList;
import java.util.List;

import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.core.Rect64;
import clipper2.core.RectD;

public class Clipper {

	public static final Rect64 MaxInvalidRect64 = new Rect64(Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
	public static final RectD MaxInvalidRectD = new RectD(Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

	/**
	 * Returns the area of the supplied polygon. It's assumed that the path is
	 * closed and does not self-intersect. Depending on the path's winding
	 * orientation, this value may be positive or negative. If the winding is
	 * clockwise, then the area will be positive and conversely, if winding is
	 * counter-clockwise, then the area will be negative.
	 * 
	 * @param path
	 * @return
	 */
	public static double area(List<Point64> path) {
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
			a += area(path);
		}
		return a;
	}

	public static List<Point64> MakePath(int[] arr) {
		int len = arr.length / 2;
		ArrayList<Point64> p = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			p.add(new Point64(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static List<Point64> MakePath(long[] arr) {
		int len = arr.length / 2;
		ArrayList<Point64> p = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			p.add(new Point64(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static List<PointD> MakePath(double[] arr) {
		int len = arr.length / 2;
		ArrayList<PointD> p = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			p.add(new PointD(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

}
