package clipper2.rectclip;

import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;
import tangible.RefObject;

/**
 * RectClipLines intersects subject open paths (polylines) with the specified
 * rectangular clipping region.
 * <p>
 * This function is extremely fast when compared to the Library's general
 * purpose Intersect clipper. Where Intersect has roughly O(n³) performance,
 * RectClipLines has O(n) performance.
 * 
 * @since 1.0.6
 */
public class RectClipLines extends RectClip {

	public RectClipLines(Rect64 rect) {
		super(rect);
	}

	public Paths64 NewExecuteInternal(Path64 path) {
		result_.clear();
		Paths64 result = new Paths64();
		if (path.size() < 2 || rect.IsEmpty()) {
			return result;
		}

		RefObject<Location> prev = new RefObject<>(Location.INSIDE);
		RefObject<Integer> i = new RefObject<>(1);
		int highI = path.size() - 1;
		RefObject<Location> loc = new RefObject<>(null);
		if (!GetLocation(rect, path.get(0), loc)) {
			while (i.argValue <= highI && !GetLocation(rect, path.get(i.argValue), prev)) {
				i.argValue++;
			}
			if (i.argValue > highI) {
				result.add(path);
				return result;
			}
			if (prev.argValue == Location.INSIDE) {
				loc.argValue = Location.INSIDE;
			}
			i.argValue = 1;
		}
		if (loc.argValue == Location.INSIDE) {
			result_.add(path.get(0));
		}

		///////////////////////////////////////////////////
		while (i.argValue <= highI) {
			prev.argValue = loc.argValue;
			GetNextLocation(path, loc, i, highI);
			if (i.argValue > highI) {
				break;
			}
			Point64 prevPt = path.get(i.argValue - 1);

			RefObject<Location> crossingLoc = new RefObject<>(loc.argValue);
			Point64 ip = new Point64();
			if (!GetIntersection(rectPath, path.get(i.argValue), prevPt, crossingLoc, ip)) {
				// ie remaining outside (& crossingLoc still == loc)
				++i.argValue;
				continue;
			}

			////////////////////////////////////////////////////
			// we must be crossing the rect boundary to get here
			////////////////////////////////////////////////////

			if (loc.argValue == Location.INSIDE) // path must be entering rect
			{
				result_.add(ip);
			} else if (prev.argValue != Location.INSIDE) {
				// passing right through rect. 'ip' here will be the second
				// intersect pt but we'll also need the first intersect pt (ip2)
				crossingLoc.argValue = prev.argValue;
				Point64 ip2 = new Point64();
				GetIntersection(rectPath, prevPt, path.get(i.argValue), crossingLoc, ip2);
				result_.add(ip2);
				result_.add(ip);
				result.add(result_);
				result_ = new Path64();
			} else // path must be exiting rect
			{
				result_.add(ip);
				result.add(result_);
				result_ = new Path64();
			}
		}

		if (result_.size() > 1) {
			result.add(result_);
			result_ = new Path64();
		}
		return result;
	}

}
