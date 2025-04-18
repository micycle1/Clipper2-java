package clipper2.rectclip;

import java.util.ArrayList;
import java.util.List;

import clipper2.Clipper;
import clipper2.core.InternalClipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;
import clipper2.engine.PointInPolygonResult;
import tangible.RefObject;

/**
 * RectClip64 intersects subject polygons with the specified rectangular
 * clipping region. Polygons may be simple or complex (self-intersecting).
 * <p>
 * This function is extremely fast when compared to the Library's general
 * purpose Intersect clipper. Where Intersect has roughly O(n³) performance,
 * RectClip64 has O(n) performance.
 *
 * @since 1.0.6
 */
public class RectClip64 {

	// NOTE based on RectClip from Clipper2 v1.5.2

	protected enum Location {
		left, top, right, bottom, inside
	}

	protected final Rect64 rect_;
	protected final Point64 mp_;
	protected final Path64 rectPath_;
	protected Rect64 pathBounds_;
	protected List<OutPt2> results_;
	protected List<OutPt2>[] edges_;
	protected int currIdx_;

	public RectClip64(Rect64 rect) {
		currIdx_ = -1;
		rect_ = rect;
		mp_ = rect.MidPoint();
		rectPath_ = rect.AsPath();
		results_ = new ArrayList<>();
		edges_ = new ArrayList[8];
		for (int i = 0; i < 8; i++) {
			edges_[i] = new ArrayList<>();
		}
	}

	protected OutPt2 add(Point64 pt) {
		return add(pt, false);
	}

	protected OutPt2 add(Point64 pt, boolean startingNewPath) {
		int curr = results_.size();
		OutPt2 result;
		if (curr == 0 || startingNewPath) {
			result = new OutPt2(pt);
			results_.add(result);
			result.ownerIdx = curr;
			result.prev = result;
			result.next = result;
		} else {
			curr--;
			OutPt2 prevOp = results_.get(curr);
			if (prevOp.pt.equals(pt)) {
				return prevOp;
			}
			result = new OutPt2(pt);
			result.ownerIdx = curr;
			result.next = prevOp.next;
			prevOp.next.prev = result;
			prevOp.next = result;
			result.prev = prevOp;
			results_.set(curr, result);
		}
		return result;
	}

	private static boolean path1ContainsPath2(Path64 p1, Path64 p2) {
		int io = 0;
		for (Point64 pt : p2) {
			PointInPolygonResult pip = InternalClipper.PointInPolygon(pt, p1);
			switch (pip) {
				case IsInside :
					io--;
					break;
				case IsOutside :
					io++;
					break;
			}
			if (Math.abs(io) > 1) {
				break;
			}
		}
		return io <= 0;
	}

	private static boolean isClockwise(Location prev, Location curr, Point64 p1, Point64 p2, Point64 mid) {
		if (areOpposites(prev, curr)) {
			return InternalClipper.CrossProduct(p1, mid, p2) < 0;
		}
		return headingClockwise(prev, curr);
	}

	private static boolean areOpposites(Location a, Location b) {
		return Math.abs(a.ordinal() - b.ordinal()) == 2;
	}

	private static boolean headingClockwise(Location a, Location b) {
		return (a.ordinal() + 1) % 4 == b.ordinal();
	}

	private static Location getAdjacentLocation(Location loc, boolean cw) {
		int d = cw ? 1 : 3;
		return Location.values()[(loc.ordinal() + d) % 4];
	}

	private static OutPt2 unlinkOp(OutPt2 op) {
		if (op.next == op) {
			return null;
		}
		op.prev.next = op.next;
		op.next.prev = op.prev;
		return op.next;
	}

	private static OutPt2 unlinkOpBack(OutPt2 op) {
		if (op.next == op) {
			return null;
		}
		op.prev.next = op.next;
		op.next.prev = op.prev;
		return op.prev;
	}

	private static int getEdgesForPt(Point64 pt, Rect64 r) {
		int res = 0;
		if (pt.x == r.left) {
			res = 1;
		} else if (pt.x == r.right) {
			res = 4;
		}
		if (pt.y == r.top) {
			res += 2;
		} else if (pt.y == r.bottom) {
			res += 8;
		}
		return res;
	}

	private static boolean isHeadingClockwise(Point64 p1, Point64 p2, int idx) {
		switch (idx) {
			case 0 :
				return p2.y < p1.y;
			case 1 :
				return p2.x > p1.x;
			case 2 :
				return p2.y > p1.y;
			default :
				return p2.x < p1.x;
		}
	}

	private static boolean hasHorzOverlap(Point64 l1, Point64 r1, Point64 l2, Point64 r2) {
		return l1.x < r2.x && r1.x > l2.x;
	}

	private static boolean hasVertOverlap(Point64 t1, Point64 b1, Point64 t2, Point64 b2) {
		return t1.y < b2.y && b1.y > t2.y;
	}

	private static void addToEdge(List<OutPt2> edge, OutPt2 op) {
		if (op.edge != null) {
			return;
		}
		op.edge = edge;
		edge.add(op);
	}

	private static void uncoupleEdge(OutPt2 op) {
		if (op.edge == null) {
			return;
		}
		List<OutPt2> e = op.edge;
		for (int i = 0; i < e.size(); i++) {
			if (e.get(i) == op) {
				e.set(i, null);
				break;
			}
		}
		op.edge = null;
	}

	private static void setNewOwner(OutPt2 op, int idx) {
		op.ownerIdx = idx;
		OutPt2 o = op.next;
		while (o != op) {
			o.ownerIdx = idx;
			o = o.next;
		}
	}

	private void addCorner(Location prev, Location curr) {
		add(headingClockwise(prev, curr) ? rectPath_.get(prev.ordinal()) : rectPath_.get(curr.ordinal()));
	}

	private void addCorner(RefObject<Location> locRefObject, boolean cw) {
		if (cw) {
			add(rectPath_.get(locRefObject.argValue.ordinal()));
			locRefObject.argValue = getAdjacentLocation(locRefObject.argValue, true);
		} else {
			locRefObject.argValue = getAdjacentLocation(locRefObject.argValue, false);
			add(rectPath_.get(locRefObject.argValue.ordinal()));
		}
	}

	protected static boolean getLocation(Rect64 r, Point64 pt, RefObject<Location> locRefObject) {
		Location loc;
		if (pt.x == r.left && pt.y >= r.top && pt.y <= r.bottom) {
			locRefObject.argValue = Location.left;
			return false;
		}
		if (pt.x == r.right && pt.y >= r.top && pt.y <= r.bottom) {
			locRefObject.argValue = Location.right;
			return false;
		}
		if (pt.y == r.top && pt.x >= r.left && pt.x <= r.right) {
			locRefObject.argValue = Location.top;
			return false;
		}
		if (pt.y == r.bottom && pt.x >= r.left && pt.x <= r.right) {
			locRefObject.argValue = Location.bottom;
			return false;
		}
		if (pt.x < r.left) {
			loc = Location.left;
		} else if (pt.x > r.right) {
			loc = Location.right;
		} else if (pt.y < r.top) {
			loc = Location.top;
		} else if (pt.y > r.bottom) {
			loc = Location.bottom;
		} else {
			loc = Location.inside;
		}
		locRefObject.argValue = loc;
		return true;
	}

	private static boolean isHorizontal(Point64 a, Point64 b) {
		return a.y == b.y;
	}

	private static boolean getSegmentIntersection(Point64 p1, Point64 p2, Point64 p3, Point64 p4, Point64 ipRefObject) {
		double r1 = InternalClipper.CrossProduct(p1, p3, p4);
		double r2 = InternalClipper.CrossProduct(p2, p3, p4);
		if (r1 == 0) {
			ipRefObject.set(p1);
			if (r2 == 0) {
				return false;
			}
			if (p1.equals(p3) || p1.equals(p4)) {
				return true;
			}
			if (isHorizontal(p3, p4)) {
				return (p1.x > p3.x) == (p1.x < p4.x);
			}
			return (p1.y > p3.y) == (p1.y < p4.y);
		}
		if (r2 == 0) {
			ipRefObject.set(p2);
			if (p2.equals(p3) || p2.equals(p4)) {
				return true;
			}
			if (isHorizontal(p3, p4)) {
				return (p2.x > p3.x) == (p2.x < p4.x);
			}
			return (p2.y > p3.y) == (p2.y < p4.y);
		}
		if ((r1 > 0) == (r2 > 0)) {
			ipRefObject.set(new Point64(0, 0));
			return false;
		}
		double r3 = InternalClipper.CrossProduct(p3, p1, p2);
		double r4 = InternalClipper.CrossProduct(p4, p1, p2);
		if (r3 == 0) {
			ipRefObject.set(p3);
			if (p3.equals(p1) || p3.equals(p2)) {
				return true;
			}
			if (isHorizontal(p1, p2)) {
				return (p3.x > p1.x) == (p3.x < p2.x);
			}
			return (p3.y > p1.y) == (p3.y < p2.y);
		}
		if (r4 == 0) {
			ipRefObject.set(p4);
			if (p4.equals(p1) || p4.equals(p2)) {
				return true;
			}
			if (isHorizontal(p1, p2)) {
				return (p4.x > p1.x) == (p4.x < p2.x);
			}
			return (p4.y > p1.y) == (p4.y < p2.y);
		}
		if ((r3 > 0) == (r4 > 0)) {
			ipRefObject.set(new Point64(0, 0));
			return false;
		}
		return InternalClipper.GetIntersectPoint(p1, p2, p3, p4, ipRefObject);
	}

	protected static boolean getIntersection(Path64 rectPath, Point64 p, Point64 p2, RefObject<Location> locRefObject, Point64 ipRefObject) {
		ipRefObject.set(new Point64(0, 0));
		switch (locRefObject.argValue) {
			case left :
				if (getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(3), ipRefObject)) {
					return true;
				}
				if (p.y < rectPath.get(0).y && getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(1), ipRefObject)) {
					locRefObject.argValue = Location.top;
					return true;
				}
				if (!getSegmentIntersection(p, p2, rectPath.get(2), rectPath.get(3), ipRefObject)) {
					return false;
				}
				locRefObject.argValue = Location.bottom;
				return true;
			case right :
				if (getSegmentIntersection(p, p2, rectPath.get(1), rectPath.get(2), ipRefObject)) {
					return true;
				}
				if (p.y < rectPath.get(0).y && getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(1), ipRefObject)) {
					locRefObject.argValue = Location.top;
					return true;
				}
				if (!getSegmentIntersection(p, p2, rectPath.get(2), rectPath.get(3), ipRefObject)) {
					return false;
				}
				locRefObject.argValue = Location.bottom;
				return true;
			case top :
				if (getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(1), ipRefObject)) {
					return true;
				}
				if (p.x < rectPath.get(0).x && getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(3), ipRefObject)) {
					locRefObject.argValue = Location.left;
					return true;
				}
				if (p.x <= rectPath.get(1).x || !getSegmentIntersection(p, p2, rectPath.get(1), rectPath.get(2), ipRefObject)) {
					return false;
				}
				locRefObject.argValue = Location.right;
				return true;
			case bottom :
				if (getSegmentIntersection(p, p2, rectPath.get(2), rectPath.get(3), ipRefObject)) {
					return true;
				}
				if (p.x < rectPath.get(3).x && getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(3), ipRefObject)) {
					locRefObject.argValue = Location.left;
					return true;
				}
				if (p.x <= rectPath.get(2).x || !getSegmentIntersection(p, p2, rectPath.get(1), rectPath.get(2), ipRefObject)) {
					return false;
				}
				locRefObject.argValue = Location.right;
				return true;
			default :
				if (getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(3), ipRefObject)) {
					locRefObject.argValue = Location.left;
					return true;
				}
				if (getSegmentIntersection(p, p2, rectPath.get(0), rectPath.get(1), ipRefObject)) {
					locRefObject.argValue = Location.top;
					return true;
				}
				if (getSegmentIntersection(p, p2, rectPath.get(1), rectPath.get(2), ipRefObject)) {
					locRefObject.argValue = Location.right;
					return true;
				}
				if (!getSegmentIntersection(p, p2, rectPath.get(2), rectPath.get(3), ipRefObject)) {
					return false;
				}
				locRefObject.argValue = Location.bottom;
				return true;
		}
	}

	protected void getNextLocation(Path64 path, RefObject<Location> locRefObject, RefObject<Integer> iRefObject, int highI) {
		Location loc = locRefObject.argValue;
		int i = iRefObject.argValue;
		switch (loc) {
			case left :
				while (i <= highI && path.get(i).x <= rect_.left) {
					i++;
				}
				if (i <= highI) {
					if (path.get(i).x >= rect_.right) {
						loc = Location.right;
					} else if (path.get(i).y <= rect_.top) {
						loc = Location.top;
					} else if (path.get(i).y >= rect_.bottom) {
						loc = Location.bottom;
					} else {
						loc = Location.inside;
					}
				}
				break;
			case top :
				while (i <= highI && path.get(i).y <= rect_.top) {
					i++;
				}
				if (i <= highI) {
					if (path.get(i).y >= rect_.bottom) {
						loc = Location.bottom;
					} else if (path.get(i).x <= rect_.left) {
						loc = Location.left;
					} else if (path.get(i).x >= rect_.right) {
						loc = Location.right;
					} else {
						loc = Location.inside;
					}
				}
				break;
			case right :
				while (i <= highI && path.get(i).x >= rect_.right) {
					i++;
				}
				if (i <= highI) {
					if (path.get(i).x <= rect_.left) {
						loc = Location.left;
					} else if (path.get(i).y <= rect_.top) {
						loc = Location.top;
					} else if (path.get(i).y >= rect_.bottom) {
						loc = Location.bottom;
					} else {
						loc = Location.inside;
					}
				}
				break;
			case bottom :
				while (i <= highI && path.get(i).y >= rect_.bottom) {
					i++;
				}
				if (i <= highI) {
					if (path.get(i).y <= rect_.top) {
						loc = Location.top;
					} else if (path.get(i).x <= rect_.left) {
						loc = Location.left;
					} else if (path.get(i).x >= rect_.right) {
						loc = Location.right;
					} else {
						loc = Location.inside;
					}
				}
				break;
			case inside :
				while (i <= highI) {
					Point64 pt = path.get(i);
					if (pt.x < rect_.left) {
						loc = Location.left;
						break;
					} else if (pt.x > rect_.right) {
						loc = Location.right;
						break;
					} else if (pt.y > rect_.bottom) {
						loc = Location.bottom;
						break;
					} else if (pt.y < rect_.top) {
						loc = Location.top;
						break;
					} else {
						add(pt);
						i++;
						continue;
					}
				}
				break;
		}
		locRefObject.argValue = loc;
		iRefObject.argValue = i;
	}

	private static boolean startLocsAreClockwise(List<Location> locs) {
		int res = 0;
		for (int i = 1; i < locs.size(); i++) {
			int d = locs.get(i).ordinal() - locs.get(i - 1).ordinal();
			switch (d) {
				case -1 :
					res--;
					break;
				case 1 :
					res++;
					break;
				case -3 :
					res++;
					break;
				case 3 :
					res--;
					break;
			}
		}
		return res > 0;
	}

	protected void executeInternal(Path64 path) {
		if (path.size() < 3 || rect_.IsEmpty()) {
			return;
		}

		// ––– setup
		List<Location> startLocs = new ArrayList<>();
		Location firstCross = Location.inside;
		Location crossingLoc = Location.inside;
		Location prev = Location.inside;

		int highI = path.size() - 1;
		RefObject<Location> locRefObject = new RefObject<>();

		// find the location of the last point
		if (!getLocation(rect_, path.get(highI), locRefObject)) {
			prev = locRefObject.argValue;
			int j = highI - 1;
			RefObject<Location> prevRefObject = new RefObject<>(prev);
			while (j >= 0 && !getLocation(rect_, path.get(j), prevRefObject)) {
				j--;
			}
			if (j < 0) {
				// never touched the rect at all
				for (Point64 pt : path) {
					add(pt);
				}
				return;
			}
			prev = prevRefObject.argValue;
			if (prev == Location.inside) {
				locRefObject.argValue = Location.inside;
			}
		}

		// **capture the very first loc** for the tail‐end test
		Location startingLoc = locRefObject.argValue;

		// ––– main loop
		int i = 0;
		while (i <= highI) {
			prev = locRefObject.argValue;
			Location prevCrossLoc = crossingLoc;

			// advance i to the next index where the rect‐location changes
			RefObject<Integer> iRefObject = new RefObject<>(i);
			getNextLocation(path, locRefObject, iRefObject, highI);
			i = iRefObject.argValue;
			if (i > highI) {
				break;
			}

			// current segment runs from path[i-1] to path[i]
			Point64 prevPt = (i == 0) ? path.get(highI) : path.get(i - 1);
			crossingLoc = locRefObject.argValue;

			// see if that segment hits the rectangle boundary
			RefObject<Location> crossRefObject = new RefObject<>(crossingLoc);
			Point64 ipRefObject = new Point64();
			if (!getIntersection(rectPath_, path.get(i), prevPt, crossRefObject, ipRefObject)) {
				// still entirely outside
				crossingLoc = crossRefObject.argValue;
				if (prevCrossLoc == Location.inside) {
					boolean cw = isClockwise(prev, locRefObject.argValue, prevPt, path.get(i), mp_);
					do {
						startLocs.add(prev);
						prev = getAdjacentLocation(prev, cw);
					} while (prev != locRefObject.argValue);
					crossingLoc = prevCrossLoc;
				} else if (prev != Location.inside && prev != locRefObject.argValue) {
					boolean cw = isClockwise(prev, locRefObject.argValue, prevPt, path.get(i), mp_);
					RefObject<Location> pRefObject = new RefObject<>(prev);
					do {
						addCorner(pRefObject, cw);
						prev = pRefObject.argValue;
					} while (prev != locRefObject.argValue);
				}

				// **only place we increment i in the no‐intersection case**
				i++;
				continue;
			}

			// we *did* intersect
			crossingLoc = crossRefObject.argValue;
			Point64 ip = ipRefObject;

			if (locRefObject.argValue == Location.inside) {
				// entering rectangle
				if (firstCross == Location.inside) {
					firstCross = crossingLoc;
					startLocs.add(prev);
				} else if (prev != crossingLoc) {
					boolean cw = isClockwise(prev, crossingLoc, prevPt, path.get(i), mp_);
					RefObject<Location> pRefObject = new RefObject<>(prev);
					do {
						addCorner(pRefObject, cw);
						prev = pRefObject.argValue;
					} while (prev != crossingLoc);
				}
			} else if (prev != Location.inside) {
				// passing all the way through
				RefObject<Location> loc2RefObject = new RefObject<>(prev);
				Point64 ip2RefObject = new Point64();
				getIntersection(rectPath_, prevPt, path.get(i), loc2RefObject, ip2RefObject);
				Location newLoc = loc2RefObject.argValue;

				if (prevCrossLoc != Location.inside && prevCrossLoc != newLoc) {
					addCorner(prevCrossLoc, newLoc);
				}
				if (firstCross == Location.inside) {
					firstCross = newLoc;
					startLocs.add(prev);
				}
				locRefObject.argValue = crossingLoc;
				add(ip2RefObject);

				if (ip.equals(ip2RefObject)) {
					RefObject<Location> tmpRefObject = new RefObject<>(crossingLoc);
					RefObject<Location> onRectRefObject = new RefObject<>();
					getLocation(rect_, path.get(i), onRectRefObject);
					addCorner(tmpRefObject, headingClockwise(tmpRefObject.argValue, onRectRefObject.argValue));
					crossingLoc = tmpRefObject.argValue;

					i++;
					continue;
				}
			} else {
				// exiting rectangle
				locRefObject.argValue = crossingLoc;
				if (firstCross == Location.inside) {
					firstCross = crossingLoc;
				}
			}

			// add the intersection point
			add(ip);

			// no other explicit i++ here; getNextLocation will advance on the next loop
		}

		// ––– tail‐end logic (unchanged)
		if (firstCross == Location.inside) {
			// never intersected
			if (startingLoc == Location.inside || !pathBounds_.Contains(rect_) || !path1ContainsPath2(path, rectPath_)) {
				return;
			}

			boolean cw = startLocsAreClockwise(startLocs);
			for (int j = 0; j < 4; j++) {
				int k = cw ? j : 3 - j;
				add(rectPath_.get(k));
				addToEdge(edges_[k * 2], results_.get(0));
			}
		} else if (locRefObject.argValue != Location.inside && (locRefObject.argValue != firstCross || startLocs.size() > 2)) {
			if (!startLocs.isEmpty()) {
				prev = locRefObject.argValue;
				for (Location loc2 : startLocs) {
					if (prev == loc2) {
						continue;
					}
					boolean c = headingClockwise(prev, loc2);
					RefObject<Location> pRefObject = new RefObject<>(prev);
					addCorner(pRefObject, c);
					prev = pRefObject.argValue;
				}
				locRefObject.argValue = prev;
			}
			if (locRefObject.argValue != firstCross) {
				RefObject<Location> pRefObject = new RefObject<>(locRefObject.argValue);
				addCorner(pRefObject, headingClockwise(locRefObject.argValue, firstCross));
			}
		}
	}

	public Paths64 Execute(List<Path64> paths) {
		Paths64 res = new Paths64();
		if (rect_.IsEmpty()) {
			return res;
		}
		for (Path64 path : paths) {
			if (path.size() < 3) {
				continue;
			}
			pathBounds_ = Clipper.GetBounds(path);
			if (!rect_.Intersects(pathBounds_)) {
				continue;
			}
			if (rect_.Contains(pathBounds_)) {
				res.add(path);
				continue;
			}
			executeInternal(path);
			checkEdges();
			for (int i = 0; i < 4; i++) {
				tidyEdgePair(i, edges_[i * 2], edges_[i * 2 + 1]);
			}
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

	private void checkEdges() {
		for (int i = 0; i < results_.size(); i++) {
			OutPt2 op = results_.get(i);
			if (op == null) {
				continue;
			}
			OutPt2 o2 = op;
			do {
				if (InternalClipper.IsCollinear(o2.prev.pt, o2.pt, o2.next.pt)) {
					if (o2 == op) {
						o2 = unlinkOpBack(o2);
						if (o2 == null) {
							break;
						}
						op = o2.prev;
					} else {
						o2 = unlinkOpBack(o2);
						if (o2 == null) {
							break;
						}
					}
				} else {
					o2 = o2.next;
				}
			} while (o2 != op);
			if (o2 == null) {
				results_.set(i, null);
				continue;
			}
			results_.set(i, o2);
			int e1 = getEdgesForPt(op.prev.pt, rect_);
			o2 = op;
			do {
				int e2 = getEdgesForPt(o2.pt, rect_);
				if (e2 != 0 && o2.edge == null) {
					int comb = e1 & e2;
					for (int j = 0; j < 4; j++) {
						if ((comb & (1 << j)) == 0) {
							continue;
						}
						if (isHeadingClockwise(o2.prev.pt, o2.pt, j)) {
							addToEdge(edges_[j * 2], o2);
						} else {
							addToEdge(edges_[j * 2 + 1], o2);
						}
					}
				}
				e1 = e2;
				o2 = o2.next;
			} while (o2 != op);
		}
	}

	private void tidyEdgePair(int idx, List<OutPt2> cw, List<OutPt2> ccw) {
		if (ccw.isEmpty()) {
			return;
		}
		boolean isH = (idx == 1 || idx == 3);
		boolean cwL = (idx == 1 || idx == 2);
		int i = 0, j = 0;
		while (i < cw.size()) {
			OutPt2 p1 = cw.get(i);
			if (p1 == null || p1.next == p1.prev) {
				cw.set(i, null);
				j = 0;
				i++;
				continue;
			}
			while (j < ccw.size() && (ccw.get(j) == null || ccw.get(j).next == ccw.get(j).prev)) {
				j++;
			}
			if (j == ccw.size()) {
				i++;
				j = 0;
				continue;
			}
			OutPt2 p2, p1a, p2a;
			if (cwL) {
				p1 = cw.get(i).prev;
				p1a = cw.get(i);
				p2 = ccw.get(j);
				p2a = ccw.get(j).prev;
			} else {
				p1 = cw.get(i);
				p1a = cw.get(i).prev;
				p2 = ccw.get(j).prev;
				p2a = ccw.get(j);
			}
			if ((isH && !hasHorzOverlap(p1.pt, p1a.pt, p2.pt, p2a.pt)) || (!isH && !hasVertOverlap(p1.pt, p1a.pt, p2.pt, p2a.pt))) {
				j++;
				continue;
			}
			boolean rejoin = p1a.ownerIdx != p2.ownerIdx;
			if (rejoin) {
				results_.set(p2.ownerIdx, null);
				setNewOwner(p2, p1a.ownerIdx);
			}
			if (cwL) {
				p1.next = p2;
				p2.prev = p1;
				p1a.prev = p2a;
				p2a.next = p1a;
			} else {
				p1.prev = p2;
				p2.next = p1;
				p1a.next = p2a;
				p2a.prev = p1a;
			}
			if (!rejoin) {
				int ni = results_.size();
				results_.add(p1a);
				setNewOwner(p1a, ni);
			}
			OutPt2 o, o2;
			if (cwL) {
				o = p2;
				o2 = p1a;
			} else {
				o = p1;
				o2 = p2a;
			}
			results_.set(o.ownerIdx, o);
			results_.set(o2.ownerIdx, o2);
			boolean oL, o2L;
			if (isH) {
				oL = o.pt.x > o.prev.pt.x;
				o2L = o2.pt.x > o2.prev.pt.x;
			} else {
				oL = o.pt.y > o.prev.pt.y;
				o2L = o2.pt.y > o2.prev.pt.y;
			}
			if (o.next == o.prev || o.pt.equals(o.prev.pt)) {
				if (o2L == cwL) {
					cw.set(i, o2);
					ccw.set(j, null);
				} else {
					ccw.set(j, o2);
					cw.set(i, null);
				}
			} else if (o2.next == o2.prev || o2.pt.equals(o2.prev.pt)) {
				if (oL == cwL) {
					cw.set(i, o);
					ccw.set(j, null);
				} else {
					ccw.set(j, o);
					cw.set(i, null);
				}
			} else if (oL == o2L) {
				if (oL == cwL) {
					cw.set(i, o);
					uncoupleEdge(o2);
					addToEdge(cw, o2);
					ccw.set(j, null);
				} else {
					cw.set(i, null);
					ccw.set(j, o2);
					uncoupleEdge(o);
					addToEdge(ccw, o);
					j = 0;
				}
			} else {
				if (oL == cwL) {
					cw.set(i, o);
				} else {
					ccw.set(j, o);
				}
				if (o2L == cwL) {
					cw.set(i, o2);
				} else {
					ccw.set(j, o2);
				}
			}
		}
	}

	private static Path64 getPath(OutPt2 op) {
		Path64 res = new Path64();
		if (op == null || op.prev == op.next) {
			return res;
		}
		OutPt2 start = op.next;
		while (start != null && start != op) {
			if (InternalClipper.IsCollinear(start.prev.pt, start.pt, start.next.pt)) {
				op = start.prev;
				start = unlinkOp(start);
			} else {
				start = start.next;
			}
		}
		if (start == null) {
			return new Path64();
		}
		res.add(op.pt);
		OutPt2 p2 = op.next;
		while (p2 != op) {
			res.add(p2.pt);
			p2 = p2.next;
		}
		return res;
	}

	class OutPt2 {
		public OutPt2 next;
		public OutPt2 prev;
		public Point64 pt;
		public int ownerIdx;
		public List<OutPt2> edge;

		public OutPt2(Point64 pt) {
			this.pt = pt;
		}
	}
}
