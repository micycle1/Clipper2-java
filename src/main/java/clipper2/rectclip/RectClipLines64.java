package clipper2.rectclip;

import clipper2.Clipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;
import tangible.RefObject;

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
		RefObject<Location> locRefObject = new RefObject<>(Location.inside);
		int i = 1, highI = path.size() - 1;
		if (!getLocation(rect_, path.get(0), locRefObject)) {
			RefObject<Location> prevRefObject = new RefObject<>(locRefObject.argValue);
			while (i <= highI && !getLocation(rect_, path.get(i), prevRefObject)) {
				i++;
			}
			if (i > highI) {
				for (Point64 pt : path) {
					add(pt);
				}
				return;
			}
			if (prevRefObject.argValue == Location.inside) {
				locRefObject.argValue = Location.inside;
			}
			i = 1;
		}
		if (locRefObject.argValue == Location.inside) {
			add(path.get(0));
		}
		while (i <= highI) {
			Location prev = locRefObject.argValue;
			RefObject<Integer> iRefObject = new RefObject<>(i);
			getNextLocation(path, locRefObject, iRefObject, highI);
			i = iRefObject.argValue;
			if (i > highI) {
				break;
			}
			Point64 prevPt = path.get(i - 1);
			RefObject<Location> crossRefObject = new RefObject<>(locRefObject.argValue);
			Point64 ipRefObject = new Point64();
			if (!getIntersection(rectPath_, path.get(i), prevPt, crossRefObject, ipRefObject)) {
				i++;
				continue;
			}
			Point64 ip = ipRefObject;
			if (locRefObject.argValue == Location.inside) {
				add(ip, true);
			} else if (prev != Location.inside) {
				Point64 ip2RefObject = new Point64();
				getIntersection(rectPath_, prevPt, path.get(i), new RefObject<>(prev), ip2RefObject);
				add(ip2RefObject, true);
				add(ip, true);
			} else {
				add(ip);
			}
			i++;
		}
	}
}