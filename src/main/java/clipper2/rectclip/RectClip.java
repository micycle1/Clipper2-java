package clipper2.rectclip;

import static clipper2.Clipper.GetBounds;

import clipper2.Clipper;
import clipper2.core.InternalClipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;
import clipper2.engine.PointInPolygonResult;
import tangible.OutObject;
import tangible.RefObject;

import java.util.ArrayList;
import java.util.List;

public class RectClip {
	protected enum Location {
		left, top, right, bottom, inside
	}

	final protected Rect64 rect_;
	final protected Point64 mp_;
	final protected Path64 rectPath_;
	protected Path64 result_;
	protected Location firstCross_;
	protected List<Location> startLocs_ = new ArrayList<>();

	public RectClip(Rect64 rect) {
		rect_ = rect;
		mp_ = rect.MidPoint();
		rectPath_ = rect_.AsPath();
		result_ = new Path64();
		firstCross_ = Location.inside;
	}

	private static PointInPolygonResult Path1ContainsPath2(Path64 path1, Path64 path2) {
		PointInPolygonResult result = PointInPolygonResult.IsOn;
		for (Point64 pt : path2) {
			result = Clipper.PointInPolygon(pt, path1);
			if (result != PointInPolygonResult.IsOn)
				break;
		}
		return result;
	}

	private static boolean IsClockwise(Location prev, Location curr,
			Point64 prevPt, Point64 currPt, Point64 rectMidPoint) {
		if (AreOpposites(prev, curr))
			return InternalClipper.CrossProduct(prevPt, rectMidPoint, currPt) < 0;
		else
			return HeadingClockwise(prev, curr);
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
		if (HeadingClockwise(prev, curr))
			result_.add(rectPath_.get(prev.ordinal()));
		else
			result_.add(rectPath_.get(curr.ordinal()));
	}

	private void AddCorner(RefObject<Location> loc, boolean isClockwise) {
		if (isClockwise) {
			result_.add(rectPath_.get(loc.argValue.ordinal()));
			loc.argValue = GetAdjacentLocation(loc.argValue, true);
		} else {
			loc.argValue = GetAdjacentLocation(loc.argValue, false);
			result_.add(rectPath_.get(loc.argValue.ordinal()));
		}
	}

	static protected boolean GetLocation(Rect64 rec, Point64 pt, OutObject<Location> loc) {
		if (pt.x == rec.left && pt.y >= rec.top && pt.y <= rec.bottom) {
			loc.argValue = Location.left;
			return false; // pt on rec
		}
		if (pt.x == rec.right && pt.y >= rec.top && pt.y <= rec.bottom) {
			loc.argValue = Location.right;
			return false; // pt on rec
		}
		if (pt.y == rec.top && pt.x >= rec.left && pt.x <= rec.right) {
			loc.argValue = Location.top;
			return false; // pt on rec
		}
		if (pt.y == rec.bottom && pt.x >= rec.left && pt.x <= rec.right) {
			loc.argValue = Location.bottom;
			return false; // pt on rec
		}
		if (pt.x < rec.left)
			loc.argValue = Location.left;
		else if (pt.x > rec.right)
			loc.argValue = Location.right;
		else if (pt.y < rec.top)
			loc.argValue = Location.top;
		else if (pt.y > rec.bottom)
			loc.argValue = Location.bottom;
		else
			loc.argValue = Location.inside;
		return true;
	}

	static protected boolean GetIntersection(Path64 rectPath, Point64 p, Point64 p2, RefObject<Location> loc, /*out*/ Point64 ip) {
		// gets the pt of intersection between rectPath and segment(p, p2) that's closest to 'p'
		// when result == false, loc will remain unchanged
		switch (loc.argValue) {
			case left:
				if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(3), ip);
				} else if (p.y < rectPath.get(0).y &&
						InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.top;
				} else if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.bottom;
				} else
					return false;
				break;

			case right:
				if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(1), rectPath.get(2), ip);
				} else if (p.y < rectPath.get(0).y &&
						InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.top;
				} else if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.bottom;
				} else
					return false;
				break;

			case top:
				if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(1), ip);
				} else if (p.x < rectPath.get(0).x &&
						InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.left;
				} else if (p.x > rectPath.get(1).x &&
						InternalClipper.SegmentsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.right;
				} else
					return false;
				break;

			case bottom:
				if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(2), rectPath.get(3), ip);
				} else if (p.x < rectPath.get(3).x &&
						InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.left;
				} else if (p.x > rectPath.get(2).x &&
						InternalClipper.SegmentsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.right;
				} else
					return false;
				break;

			case inside:
				if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.left;
				} else if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.top;
				} else if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.right;
				} else if (InternalClipper.SegmentsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPoint64(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.bottom;
				} else
					return false;
				break;
		}
		return true;
	}

	protected void GetNextLocation(Path64 path, RefObject<Location> loc, RefObject<Integer> i, int highI) {
		switch (loc.argValue) {
			case left: {
				while (i.argValue <= highI && path.get(i.argValue).x <= rect_.left)
					i.argValue++;
				if (i.argValue > highI)
					break;
				if (path.get(i.argValue).x >= rect_.right)
					loc.argValue = Location.right;
				else if (path.get(i.argValue).y <= rect_.top)
					loc.argValue = Location.top;
				else if (path.get(i.argValue).y >= rect_.bottom)
					loc.argValue = Location.bottom;
				else
					loc.argValue = Location.inside;
			}
			break;

			case top: {
				while (i.argValue <= highI && path.get(i.argValue).y <= rect_.top)
					i.argValue++;
				if (i.argValue > highI)
					break;
				if (path.get(i.argValue).y >= rect_.bottom)
					loc.argValue = Location.bottom;
				else if (path.get(i.argValue).x <= rect_.left)
					loc.argValue = Location.left;
				else if (path.get(i.argValue).x >= rect_.right)
					loc.argValue = Location.right;
				else
					loc.argValue = Location.inside;
			}
			break;

			case right: {
				while (i.argValue <= highI && path.get(i.argValue).x >= rect_.right)
					i.argValue++;
				if (i.argValue > highI)
					break;
				if (path.get(i.argValue).x <= rect_.left)
					loc.argValue = Location.left;
				else if (path.get(i.argValue).y <= rect_.top)
					loc.argValue = Location.top;
				else if (path.get(i.argValue).y >= rect_.bottom)
					loc.argValue = Location.bottom;
				else
					loc.argValue = Location.inside;
			}
			break;

			case bottom: {
				while (i.argValue <= highI && path.get(i.argValue).y >= rect_.bottom)
					i.argValue++;
				if (i.argValue > highI)
					break;
				if (path.get(i.argValue).y <= rect_.top)
					loc.argValue = Location.top;
				else if (path.get(i.argValue).x <= rect_.left)
					loc.argValue = Location.left;
				else if (path.get(i.argValue).x >= rect_.right)
					loc.argValue = Location.right;
				else
					loc.argValue = Location.inside;
			}
			break;

			case inside: {
				while (i.argValue <= highI) {
					if (path.get(i.argValue).x < rect_.left)
						loc.argValue = Location.left;
					else if (path.get(i.argValue).x > rect_.right)
						loc.argValue = Location.right;
					else if (path.get(i.argValue).y > rect_.bottom)
						loc.argValue = Location.bottom;
					else if (path.get(i.argValue).y < rect_.top)
						loc.argValue = Location.top;
					else {
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
		if (path.size() < 3 || rect_.IsEmpty())
			return new Path64();

		result_.clear();
		startLocs_.clear();
		RefObject<Integer> i = new RefObject<>(0);
		int highI = path.size() - 1;
		firstCross_ = Location.inside;
		RefObject<Location> crossingLoc = new RefObject<>(Location.inside);
		RefObject<Location> prev = new RefObject<>(null);
		RefObject<Location> loc = new RefObject<>(null);
		if (!GetLocation(rect_, path.get(highI), loc)) {
			prev.argValue = loc.argValue;
			i.argValue = highI - 1;
			while (i.argValue >= 0 && !GetLocation(rect_, path.get(i.argValue), prev))
				i.argValue--;
			if (i.argValue < 0)
				return path;
			if (prev.argValue == Location.inside)
				loc.argValue = Location.inside;
			i.argValue = 0;
		}
		Location startingLoc = loc.argValue;

		///////////////////////////////////////////////////
		while (i.argValue <= highI) {
			prev.argValue = loc.argValue;
			Location prevCrossLoc = crossingLoc.argValue;
			GetNextLocation(path, loc, i, highI);
			if (i.argValue > highI)
				break;

			Point64 prevPt = (i.argValue == 0) ? path.get(highI) : path.get(i.argValue - 1);
			crossingLoc.argValue = loc.argValue;
			Point64 ip = new Point64();
			if (!GetIntersection(rectPath_, path.get(i.argValue), prevPt, crossingLoc, ip)) {
				// ie remaining outside (& crossingLoc still == loc)

				if (prevCrossLoc == Location.inside) {
					boolean isClockw = IsClockwise(prev.argValue, loc.argValue, prevPt, path.get(i.argValue), mp_);
					do {
						startLocs_.add(prev.argValue);
						prev.argValue = GetAdjacentLocation(prev.argValue, isClockw);
					} while (prev.argValue != loc.argValue);
					crossingLoc.argValue = prevCrossLoc; // still not crossed
				} else if (prev.argValue != Location.inside && prev.argValue != loc.argValue) {
					boolean isClockw = IsClockwise(prev.argValue, loc.argValue, prevPt, path.get(i.argValue), mp_);
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

			if (loc.argValue == Location.inside) // path must be entering rect
			{
				if (firstCross_ == Location.inside) {
					firstCross_ = crossingLoc.argValue;
					startLocs_.add(prev.argValue);
				} else if (prev.argValue != crossingLoc.argValue) {
					boolean isClockw = IsClockwise(prev.argValue, crossingLoc.argValue, prevPt, path.get(i.argValue), mp_);
					do {
						AddCorner(prev, isClockw);
					} while (prev.argValue != crossingLoc.argValue);
				}
			} else if (prev.argValue != Location.inside) {
				// passing right through rect. 'ip' here will be the second
				// intersect pt but we'll also need the first intersect pt (ip2)
				loc.argValue = prev.argValue;
				Point64 ip2 = new Point64();
				GetIntersection(rectPath_, prevPt, path.get(i.argValue), loc, ip2);
				if (prevCrossLoc != Location.inside)
					AddCorner(prevCrossLoc, loc.argValue);

				if (firstCross_ == Location.inside) {
					firstCross_ = loc.argValue;
					startLocs_.add(prev.argValue);
				}

				loc.argValue = crossingLoc.argValue;
				result_.add(ip2);
				if (ip.opEquals(ip2)) {
					// it's very likely that path[i] is on rect
					GetLocation(rect_, path.get(i.argValue), loc);
					AddCorner(crossingLoc.argValue, loc.argValue);
					crossingLoc.argValue = loc.argValue;
					continue;
				}
			} else // path must be exiting rect
			{
				loc.argValue = crossingLoc.argValue;
				if (firstCross_ == Location.inside)
					firstCross_ = crossingLoc.argValue;
			}

			result_.add(ip);
		} //while i <= highI
		///////////////////////////////////////////////////

		// path must be entering rect
		if (firstCross_ == Location.inside) {
			if (startingLoc == Location.inside)
				return path;
			Rect64 tmp_rect = GetBounds(path);
			if (tmp_rect.Contains(rect_) &&
					Path1ContainsPath2(path, rectPath_)
							!= PointInPolygonResult.IsOutside)
				return rectPath_;
			return new Path64();
		}

		if (loc.argValue != Location.inside &&
				(loc.argValue != firstCross_ || startLocs_.size() > 2)) {
			if (startLocs_.size() > 0) {
				prev.argValue = loc.argValue;
				for (Location loc2 : startLocs_) {
					if (prev.argValue == loc2)
						continue;
					AddCorner(prev, HeadingClockwise(prev.argValue, loc2));
					prev.argValue = loc2;
				}
				loc.argValue = prev.argValue;
			}
			if (loc.argValue != firstCross_)
				AddCorner(loc, HeadingClockwise(loc.argValue, firstCross_));
		}

		if (result_.size() < 3)
			return new Path64();

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
			} else
				result.set(k, pt);
		}

		if (k < 2)
			result.clear();
		else if (InternalClipper.CrossProduct(result.get(0), result.get(k - 1), result.get(k)) == 0)
			result.remove(result.size() - 1);
		return result;
	}

	private Paths64 ExecuteInternal(Paths64 paths) {
		Paths64 result = new Paths64(paths.size());
		for (Path64 path : paths) {
			if (rect_.Intersects(GetBounds(path)))
				result.add(ExecuteInternal(path));
		}
		return result;
	}
}
