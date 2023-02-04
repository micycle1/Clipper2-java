package clipper2.rectclip;

import static clipper2.Clipper.GetBounds;

import java.util.ArrayList;
import java.util.List;

import clipper2.Clipper;
import clipper2.core.InternalClipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;
import clipper2.engine.PointInPolygonResult;
import tangible.OutObject;
import tangible.RefObject;

/**
 * RectClip intersects subject polygons with the specified rectangular clipping
 * region. Polygons may be simple or complex (self-intersecting).
 * <p>
 * This function is extremely fast when compared to the Library's general
 * purpose Intersect clipper. Where Intersect has roughly O(n³) performance,
 * RectClip has O(n) performance.
 *
 * @since 1.0.6
 */
public class RectClip {

	protected enum Location {
		LEFT, TOP, RIGHT, BOTTOM, INSIDE
	}

	final protected Rect64 rect;
	final protected Point64 mp;
	final protected Path64 rectPath;
	protected Path64 result_;
	protected Location firstCross;
	protected List<Location> startLocs = new ArrayList<>();

	public RectClip(Rect64 rect) {
		this.rect = rect;
		mp = rect.MidPoint();
		rectPath = this.rect.AsPath();
		result_ = new Path64();
		firstCross = Location.INSIDE;
	}

	private static PointInPolygonResult Path1ContainsPath2(Path64 path1, Path64 path2) {
		PointInPolygonResult result = PointInPolygonResult.IsOn;
		for (Point64 pt : path2) {
			result = Clipper.PointInPolygon(pt, path1);
			if (result != PointInPolygonResult.IsOn) {
				break;
			}
		}
		return result;
	}

	private static boolean IsClockwise(Location prev, Location curr, Point64 prevPt, Point64 currPt, Point64 rectMidPoint) {
		if (AreOpposites(prev, curr)) {
			return InternalClipper.CrossProduct(prevPt, rectMidPoint, currPt) < 0;
		} else {
			return HeadingClockwise(prev, curr);
		}
	}

	private static boolean AreOpposites(Location prev, Location curr) {
		return Math.abs(prev.ordinal() - curr.ordinal()) == 2;
	}

	private static boolean HeadingClockwise(Location prev, Location curr) {
		return (prev.ordinal() + 1) % 4 == curr.ordinal();
	}

	private static Location GetAdjacentLocation(Location loc, boolean isClockwise) {
		int delta = (isClockwise) ? 1 : 3;
		return Location.values()[(loc.ordinal() + delta) % 4];
	}

	private void AddCorner(Location prev, Location curr) {
		if (HeadingClockwise(prev, curr)) {
			result_.add(rectPath.get(prev.ordinal()));
		} else {
			result_.add(rectPath.get(curr.ordinal()));
		}
	}

	private void AddCorner(RefObject<Location> loc, boolean isClockwise) {
		if (isClockwise) {
			result_.add(rectPath.get(loc.argValue.ordinal()));
			loc.argValue = GetAdjacentLocation(loc.argValue, true);
		} else {
			loc.argValue = GetAdjacentLocation(loc.argValue, false);
			result_.add(rectPath.get(loc.argValue.ordinal()));
		}
	}

	static protected boolean GetLocation(Rect64 rec, Point64 pt, OutObject<Location> loc) {
		if (pt.x == rec.left && pt.y >= rec.top && pt.y <= rec.bottom) {
			loc.argValue = Location.LEFT;
			return false; // pt on rec
		}
		if (pt.x == rec.right && pt.y >= rec.top && pt.y <= rec.bottom) {
			loc.argValue = Location.RIGHT;
			return false; // pt on rec
		}
		if (pt.y == rec.top && pt.x >= rec.left && pt.x <= rec.right) {
			loc.argValue = Location.TOP;
			return false; // pt on rec
		}
		if (pt.y == rec.bottom && pt.x >= rec.left && pt.x <= rec.right) {
			loc.argValue = Location.BOTTOM;
			return false; // pt on rec
		}
		if (pt.x < rec.left) {
			loc.argValue = Location.LEFT;
		} else if (pt.x > rec.right) {
			loc.argValue = Location.RIGHT;
		} else if (pt.y < rec.top) {
			loc.argValue = Location.TOP;
		} else if (pt.y > rec.bottom) {
			loc.argValue = Location.BOTTOM;
		} else {
			loc.argValue = Location.INSIDE;
		}
		return true;
	}

	static protected boolean GetIntersection(Path64 rectPath, Point64 p, Point64 p2, RefObject<Location> loc,
			/* out */ Point64 ip) {
		/*
		 * Gets the pt of intersection between rectPath and segment(p, p2) that's
		 * closest to 'p'. When result == false, loc will remain unchanged.
		 */
		switch (loc.argValue) {
			case LEFT:
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
				} else if (p.y < rectPath.get(0).y
						&& InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.TOP;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.BOTTOM;
				} else {
					return false;
				}
				break;

			case RIGHT:
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
				} else if (p.y < rectPath.get(0).y
						&& InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.TOP;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.BOTTOM;
				} else {
					return false;
				}
				break;

			case TOP:
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
				} else if (p.x < rectPath.get(0).x
						&& InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.LEFT;
				} else if (p.x > rectPath.get(1).x
						&& InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.RIGHT;
				} else {
					return false;
				}
				break;

			case BOTTOM:
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
				} else if (p.x < rectPath.get(3).x
						&& InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.LEFT;
				} else if (p.x > rectPath.get(2).x
						&& InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.RIGHT;
				} else {
					return false;
				}
				break;

			case INSIDE:
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.LEFT;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.TOP;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.RIGHT;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.BOTTOM;
				} else {
					return false;
				}
				break;
		}
		return true;
	}

	protected void GetNextLocation(Path64 path, RefObject<Location> loc, RefObject<Integer> i, int highI) {
		switch (loc.argValue) {
			case LEFT: {
				while (i.argValue <= highI && path.get(i.argValue).x <= rect.left) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).x >= rect.right) {
					loc.argValue = Location.RIGHT;
				} else if (path.get(i.argValue).y <= rect.top) {
					loc.argValue = Location.TOP;
				} else if (path.get(i.argValue).y >= rect.bottom) {
					loc.argValue = Location.BOTTOM;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case TOP: {
				while (i.argValue <= highI && path.get(i.argValue).y <= rect.top) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).y >= rect.bottom) {
					loc.argValue = Location.BOTTOM;
				} else if (path.get(i.argValue).x <= rect.left) {
					loc.argValue = Location.LEFT;
				} else if (path.get(i.argValue).x >= rect.right) {
					loc.argValue = Location.RIGHT;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case RIGHT: {
				while (i.argValue <= highI && path.get(i.argValue).x >= rect.right) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).x <= rect.left) {
					loc.argValue = Location.LEFT;
				} else if (path.get(i.argValue).y <= rect.top) {
					loc.argValue = Location.TOP;
				} else if (path.get(i.argValue).y >= rect.bottom) {
					loc.argValue = Location.BOTTOM;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case BOTTOM: {
				while (i.argValue <= highI && path.get(i.argValue).y >= rect.bottom) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).y <= rect.top) {
					loc.argValue = Location.TOP;
				} else if (path.get(i.argValue).x <= rect.left) {
					loc.argValue = Location.LEFT;
				} else if (path.get(i.argValue).x >= rect.right) {
					loc.argValue = Location.RIGHT;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case INSIDE: {
				while (i.argValue <= highI) {
					if (path.get(i.argValue).x < rect.left) {
						loc.argValue = Location.LEFT;
					} else if (path.get(i.argValue).x > rect.right) {
						loc.argValue = Location.RIGHT;
					} else if (path.get(i.argValue).y > rect.bottom) {
						loc.argValue = Location.BOTTOM;
					} else if (path.get(i.argValue).y < rect.top) {
						loc.argValue = Location.TOP;
					} else {
						result_.add(path.get(i.argValue));
						i.argValue++;
						continue;
					}
					break;
				}
			}
				break;
		} // switch
	}

	public Path64 ExecuteInternal(Path64 path) {
		/*
		 * NOTE: Would ideally be visible to Clipper class only (i.e. not public), but
		 * must be public since they reside in different packages (Java restriction).
		 */

		if (path.size() < 3 || rect.IsEmpty()) {
			return new Path64();
		}

		result_.clear();
		startLocs.clear();
		RefObject<Integer> i = new RefObject<>(0);
		int highI = path.size() - 1;
		firstCross = Location.INSIDE;
		RefObject<Location> crossingLoc = new RefObject<>(Location.INSIDE);
		RefObject<Location> prev = new RefObject<>(null);
		RefObject<Location> loc = new RefObject<>(null);
		if (!GetLocation(rect, path.get(highI), loc)) {
			prev.argValue = loc.argValue;
			i.argValue = highI - 1;
			while (i.argValue >= 0 && !GetLocation(rect, path.get(i.argValue), prev)) {
				i.argValue--;
			}
			if (i.argValue < 0) {
				return path;
			}
			if (prev.argValue == Location.INSIDE) {
				loc.argValue = Location.INSIDE;
			}
			i.argValue = 0;
		}
		Location startingLoc = loc.argValue;

		///////////////////////////////////////////////////
		while (i.argValue <= highI) {
			prev.argValue = loc.argValue;
			Location prevCrossLoc = crossingLoc.argValue;
			GetNextLocation(path, loc, i, highI);
			if (i.argValue > highI) {
				break;
			}

			Point64 prevPt = (i.argValue == 0) ? path.get(highI) : path.get(i.argValue - 1);
			crossingLoc.argValue = loc.argValue;
			Point64 ip = new Point64();
			if (!GetIntersection(rectPath, path.get(i.argValue), prevPt, crossingLoc, ip)) {
				// ie remaining outside (& crossingLoc still == loc)

				if (prevCrossLoc == Location.INSIDE) {
					boolean isClockw = IsClockwise(prev.argValue, loc.argValue, prevPt, path.get(i.argValue), mp);
					do {
						startLocs.add(prev.argValue);
						prev.argValue = GetAdjacentLocation(prev.argValue, isClockw);
					} while (prev.argValue != loc.argValue);
					crossingLoc.argValue = prevCrossLoc; // still not crossed
				} else if (prev.argValue != Location.INSIDE && prev.argValue != loc.argValue) {
					boolean isClockw = IsClockwise(prev.argValue, loc.argValue, prevPt, path.get(i.argValue), mp);
					do {
						AddCorner(prev, isClockw);
					} while (prev.argValue != loc.argValue);
				}
				++i.argValue;
				continue;
			}

			////////////////////////////////////////////////////
			// we must be crossing the rect boundary to get here
			////////////////////////////////////////////////////

			if (loc.argValue == Location.INSIDE) // path must be entering rect
			{
				if (firstCross == Location.INSIDE) {
					firstCross = crossingLoc.argValue;
					startLocs.add(prev.argValue);
				} else if (prev.argValue != crossingLoc.argValue) {
					boolean isClockw = IsClockwise(prev.argValue, crossingLoc.argValue, prevPt, path.get(i.argValue), mp);
					do {
						AddCorner(prev, isClockw);
					} while (prev.argValue != crossingLoc.argValue);
				}
			} else if (prev.argValue != Location.INSIDE) {
				// passing right through rect. 'ip' here will be the second
				// intersect pt but we'll also need the first intersect pt (ip2)
				loc.argValue = prev.argValue;
				Point64 ip2 = new Point64();
				GetIntersection(rectPath, prevPt, path.get(i.argValue), loc, ip2);
				if (prevCrossLoc != Location.INSIDE) {
					AddCorner(prevCrossLoc, loc.argValue);
				}

				if (firstCross == Location.INSIDE) {
					firstCross = loc.argValue;
					startLocs.add(prev.argValue);
				}

				loc.argValue = crossingLoc.argValue;
				result_.add(ip2);
				if (ip.opEquals(ip2)) {
					// it's very likely that path[i] is on rect
					GetLocation(rect, path.get(i.argValue), loc);
					AddCorner(crossingLoc.argValue, loc.argValue);
					crossingLoc.argValue = loc.argValue;
					continue;
				}
			} else // path must be exiting rect
			{
				loc.argValue = crossingLoc.argValue;
				if (firstCross == Location.INSIDE) {
					firstCross = crossingLoc.argValue;
				}
			}

			result_.add(ip);
		} // while i <= highI
			///////////////////////////////////////////////////

		// path must be entering rect
		if (firstCross == Location.INSIDE) {
			if (startingLoc == Location.INSIDE) {
				return path;
			}
			Rect64 tmp_rect = GetBounds(path);
			if (tmp_rect.Contains(rect) && Path1ContainsPath2(path, rectPath) != PointInPolygonResult.IsOutside) {
				return rectPath;
			}
			return new Path64();
		}

		if (loc.argValue != Location.INSIDE && (loc.argValue != firstCross || startLocs.size() > 2)) {
			if (startLocs.size() > 0) {
				prev.argValue = loc.argValue;
				for (Location loc2 : startLocs) {
					if (prev.argValue == loc2) {
						continue;
					}
					AddCorner(prev, HeadingClockwise(prev.argValue, loc2));
					prev.argValue = loc2;
				}
				loc.argValue = prev.argValue;
			}
			if (loc.argValue != firstCross) {
				AddCorner(loc, HeadingClockwise(loc.argValue, firstCross));
			}
		}

		if (result_.size() < 3) {
			return new Path64();
		}

		// finally, tidy up result
		int k = 0, len = result_.size();
		Point64 lastPt = result_.get(len - 1);
		Path64 result = new Path64(len);
		result.add(result_.get(0));
		for (int xxx = 1; xxx < result_.size(); xxx++) {
			Point64 pt = result_.get(xxx);
			if (InternalClipper.CrossProduct(lastPt, result.get(k), pt) != 0) {
				lastPt = result.get(k++);
				result.add(pt);
			} else {
				result.set(k, pt);
			}
		}

		if (k < 2) {
			result.clear();
		} else if (InternalClipper.CrossProduct(result.get(0), result.get(k - 1), result.get(k)) == 0) {
			result.remove(result.size() - 1);
		}
		return result;
	}

	private Paths64 ExecuteInternal(Paths64 paths) {
		Paths64 result = new Paths64(paths.size());
		for (Path64 path : paths) {
			if (rect.Intersects(GetBounds(path))) {
				result.add(ExecuteInternal(path));
			}
		}
		return result;
	}
}
