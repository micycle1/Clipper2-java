package clipper2.rectclip;

import clipper2.Clipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;

/**
 * RectClipLines64 intersects subject open paths (polylines) with the specified
 * rectangular clipping region.
 * <p>
 * This function is extremely fast when compared to the Library's general
 * purpose Intersect clipper. Where Intersect has roughly O(n³) performance,
 * RectClipLines64 has O(n) performance.
 * 
 * @since 1.0.6
 */
public class RectClipLines64 extends RectClip64 {

	public RectClipLines64(Rect64 rect) {
		super(rect);
	}

	public Paths64 Execute(Paths64 paths) {
		Paths64 res = new Paths64();
		if (rect_.IsEmpty()) {
			return res;
		}
		for (Path64 path : paths) {
			if (path.size() < 2) {
				continue;
			}
			pathBounds_ = Clipper.GetBounds(path);
			if (!rect_.Intersects(pathBounds_)) {
				continue;
			}
			executeInternal(path);
			for (OutPt2 op : results_) {
				Path64 tmp = getPath(op);
				if (!tmp.isEmpty()) {
					res.add(tmp);
				}
			}
			results_.clear();
			for (int i = 0; i < 8; i++) {
				edges_[i].clear();
			}
		}
		return res;
	}

	private static Path64 getPath(OutPt2 op) {
		Path64 res = new Path64();
		if (op == null || op == op.next) {
			return res;
		}
		op = op.next;
		res.add(op.pt);
		OutPt2 p2 = op.next;
		while (p2 != op) {
			res.add(p2.pt);
			p2 = p2.next;
		}
		return res;
	}

	@Override
	protected void executeInternal(Path64 path) {
		results_.clear();
		if (path.size() < 2 || rect_.IsEmpty()) {
			return;
		}
		Location loc = Location.inside;
		int i = 1, highI = path.size() - 1;
		LocationResult locRes = getLocation(rect_, path.get(0));
		loc = locRes.location;
		if (!locRes.outside) {
			LocationResult prevLocRes = new LocationResult(false, loc);
			while (i <= highI) {
				prevLocRes = getLocation(rect_, path.get(i));
				if (prevLocRes.outside) {
					break;
				}
				i++;
			}
			if (i > highI) {
				for (Point64 pt : path) {
					add(pt);
				}
				return;
			}
			if (prevLocRes.location == Location.inside) {
				loc = Location.inside;
			}
			i = 1;
		}
		if (loc == Location.inside) {
			add(path.get(0));
		}
		while (i <= highI) {
			Location prev = loc;
			NextLocationResult nextLoc = getNextLocation(path, loc, i, highI);
			loc = nextLoc.location;
			i = nextLoc.index;
			if (i > highI) {
				break;
			}
			Point64 prevPt = path.get(i - 1);
			Point64 ipRefObject = new Point64();
			IntersectionResult crossRes = getIntersection(rectPath_, path.get(i), prevPt, loc, ipRefObject);
			if (!crossRes.intersects) {
				i++;
				continue;
			}
			Point64 ip = ipRefObject;
			if (loc == Location.inside) {
				add(ip, true);
			} else if (prev != Location.inside) {
				Point64 ip2RefObject = new Point64();
				getIntersection(rectPath_, prevPt, path.get(i), prev, ip2RefObject);
				add(ip2RefObject, true);
				add(ip, true);
			} else {
				add(ip);
			}
			i++;
		}
	}
}
