package clipper2.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import clipper2.Clipper;
import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.InternalClipper;
import clipper2.core.Path64;
import clipper2.core.PathType;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.core.Rect64;
import tangible.OutObject;
import tangible.RefObject;

/**
 * Subject and Clip paths are passed to a Clipper object via AddSubject,
 * AddOpenSubject and AddClip methods. Clipping operations are then initiated by
 * calling Execute. And Execute can be called multiple times (ie with different
 * ClipTypes & FillRules) without having to reload these paths.
 */
public class ClipperBase {

	private ClipType cliptype;
	private FillRule fillrule;
	private Active actives = null;
	private Active sel = null;
	private Joiner horzJoiners = null;
	private List<LocalMinima> minimaList;
	private List<IntersectNode> intersectList;
	private List<Vertex> vertexList;
	private List<OutRec> outrecList;
	private List<Joiner> joinerList;
	private TreeSet<Long> scanlineList;
	private int currentLocMin;
	private long currentBotY;
	private boolean isSortedMinimaList;
	private boolean hasOpenPaths;
	public boolean usingPolytree;
	public boolean succeeded;
	private boolean preserveCollinear;
	private boolean reverseSolution;

	public ClipperBase() {
		minimaList = new ArrayList<>();
		intersectList = new ArrayList<>();
		vertexList = new ArrayList<>();
		outrecList = new ArrayList<>();
		joinerList = new ArrayList<>();
		scanlineList = new TreeSet<>();
		setPreserveCollinear(true);
	}

	public final boolean getPreserveCollinear() {
		return preserveCollinear;
	}

	public final void setPreserveCollinear(boolean value) {
		preserveCollinear = value;
	}

	public final boolean getReverseSolution() {
		return reverseSolution;
	}

	public final void setReverseSolution(boolean value) {
		reverseSolution = value;
	}

	private static boolean IsOdd(int val) {
		return ((val & 1) != 0);
	}

	private static boolean IsHotEdge(Active ae) {
		return ae.outrec != null;
	}

	private static boolean IsOpen(Active ae) {
		return ae.localMin.isOpen;
	}

	private static boolean IsOpenEnd(Active ae) {
		return ae.localMin.isOpen && IsOpenEnd(ae.vertexTop);
	}

	private static boolean IsOpenEnd(Vertex v) {
		return (v.flags.getValue() & (VertexFlags.OpenStart.getValue() | VertexFlags.OpenEnd.getValue())) != VertexFlags.None.getValue();
	}

	private static Active GetPrevHotEdge(Active ae) {
		Active prev = ae.prevInAEL;
		while (prev != null && (IsOpen(prev) || !IsHotEdge(prev))) {
			prev = prev.prevInAEL;
		}
		return prev;
	}

	private static boolean IsFront(Active ae) {
		return (ae == ae.outrec.frontEdge);
	}

	private static double GetDx(Point64 pt1, Point64 pt2) {
		/*-
		 *  Dx:                             0(90deg)                                    *
		 *                                  |                                           *
		 *               +inf (180deg) <--- o --. -inf (0deg)                           *
		 *******************************************************************************/
		double dy = pt2.Y - pt1.Y;
		if (dy != 0) {
			return (pt2.X - pt1.X) / dy;
		}
		if (pt2.X > pt1.X) {
			return Double.NEGATIVE_INFINITY;
		}
		return Double.POSITIVE_INFINITY;
	}

	private static long TopX(Active ae, long currentY) {
		if ((currentY == ae.top.Y) || (ae.top.X == ae.bot.X)) {
			return ae.top.X;
		}
		if (currentY == ae.bot.Y) {
			return ae.bot.X;
		}
		return ae.bot.X + (long) Math.rint(ae.dx * (currentY - ae.bot.Y));
	}

	private static boolean IsHorizontal(Active ae) {
		return (ae.top.Y == ae.bot.Y);
	}

	private static boolean IsHeadingRightHorz(Active ae) {
		return (Double.isInfinite(ae.dx));
	}

	private static boolean IsHeadingLeftHorz(Active ae) {
		return (Double.isInfinite(ae.dx));
	}

	private static void SwapActives(RefObject<Active> ae1, RefObject<Active> ae2) {
		Active temp = ae1.argValue;
		ae1.argValue = ae2.argValue;
		ae2.argValue = temp;
	}

	private static PathType GetPolyType(Active ae) {
		return ae.localMin.polytype;
	}

	private static boolean IsSamePolyType(Active ae1, Active ae2) {
		return ae1.localMin.polytype == ae2.localMin.polytype;
	}

	private static Point64 GetIntersectPoint(Active ae1, Active ae2) {
		double b1, b2;
		if (InternalClipper.IsAlmostZero(ae1.dx - ae2.dx)) {
			return ae1.top;
		}

		if (InternalClipper.IsAlmostZero(ae1.dx)) {
			if (IsHorizontal(ae2)) {
				return new Point64(ae1.bot.X, ae2.bot.Y);
			}
			b2 = ae2.bot.Y - (ae2.bot.X / ae2.dx);
			return new Point64(ae1.bot.X, (long) Math.rint(ae1.bot.X / ae2.dx + b2));
		}

		if (InternalClipper.IsAlmostZero(ae2.dx)) {
			if (IsHorizontal(ae1)) {
				return new Point64(ae2.bot.X, ae1.bot.Y);
			}
			b1 = ae1.bot.Y - (ae1.bot.X / ae1.dx);
			return new Point64(ae2.bot.X, (long) Math.rint(ae2.bot.X / ae1.dx + b1));
		}
		b1 = ae1.bot.X - ae1.bot.Y * ae1.dx;
		b2 = ae2.bot.X - ae2.bot.Y * ae2.dx;
		double q = (b2 - b1) / (ae1.dx - ae2.dx);
		return (Math.abs(ae1.dx) < Math.abs(ae2.dx)) ? new Point64((long) Math.rint(ae1.dx * q + b1), (long) Math.rint(q))
				: new Point64((long) Math.rint(ae2.dx * q + b2), (long) Math.rint(q));
	}

	private static void SetDx(Active ae) {
		ae.dx = GetDx(ae.bot, ae.top);
	}

	private static Vertex NextVertex(Active ae) {
		if (ae.windDx > 0) {
			return ae.vertexTop.next;
		}
		return ae.vertexTop.prev;
	}

	private Vertex PrevPrevVertex(Active ae) {
		if (ae.windDx > 0) {
			return ae.vertexTop.prev.prev;
		}
		return ae.vertexTop.next.next;
	}

	private static boolean IsMaxima(Vertex vertex) {
		return ((vertex.flags.getValue() & VertexFlags.LocalMax.getValue()) != VertexFlags.None.getValue());
	}

	private static boolean IsMaxima(Active ae) {
		return IsMaxima(ae.vertexTop);
	}

	private Active GetMaximaPair(Active ae) {
		Active ae2 = null;
		ae2 = ae.nextInAEL;
		while (ae2 != null) {
			if (ae2.vertexTop == ae.vertexTop) {
				return ae2; // Found!
			}
			ae2 = ae2.nextInAEL;
		}
		return null;
	}

	private static Vertex GetCurrYMaximaVertex(Active ae) {
		Vertex result = ae.vertexTop;
		if (ae.windDx > 0) {
			while (result.next.pt.Y == result.pt.Y) {
				result = result.next;
			}
		} else {
			while (result.prev.pt.Y == result.pt.Y) {
				result = result.prev;
			}
		}
		if (!IsMaxima(result)) {
			result = null; // not a maxima
		}
		return result;
	}

	private static Active GetHorzMaximaPair(Active horz, Vertex maxVert) {
		// we can't be sure whether the MaximaPair is on the left or right, so ...
		Active result = horz.prevInAEL;
		while (result != null && result.curX >= maxVert.pt.X) {
			if (result.vertexTop == maxVert) {
				return result; // Found!
			}
			result = result.prevInAEL;
		}
		result = horz.nextInAEL;
		while (result != null && TopX(result, horz.top.Y) <= maxVert.pt.X) {
			if (result.vertexTop == maxVert) {
				return result; // Found!
			}
			result = result.nextInAEL;
		}
		return null;
	}

	private static void SetSides(OutRec outrec, Active startEdge, Active endEdge) {
		outrec.frontEdge = startEdge;
		outrec.backEdge = endEdge;
	}

	private static void SwapOutrecs(Active ae1, Active ae2) {
		OutRec or1 = ae1.outrec; // at least one edge has
		OutRec or2 = ae2.outrec; // an assigned outrec
		if (or1 == or2) {
			Active ae = or1.frontEdge;
			or1.frontEdge = or1.backEdge;
			or1.backEdge = ae;
			return;
		}

		if (or1 != null) {
			if (ae1 == or1.frontEdge) {
				or1.frontEdge = ae2;
			} else {
				or1.backEdge = ae2;
			}
		}

		if (or2 != null) {
			if (ae2 == or2.frontEdge) {
				or2.frontEdge = ae1;
			} else {
				or2.backEdge = ae1;
			}
		}

		ae1.outrec = or2;
		ae2.outrec = or1;
	}

	private static double Area(OutPt op) {
		// https://en.wikipedia.org/wiki/Shoelaceformula
		double area = 0.0;
		OutPt op2 = op;
		do {
			area += (double) (op2.prev.pt.Y + op2.pt.Y) * (op2.prev.pt.X - op2.pt.X);
			op2 = op2.next;
		} while (op2 != op);
		return area * 0.5;
	}

	private static double AreaTriangle(Point64 pt1, Point64 pt2, Point64 pt3) {
		return (double) (pt3.Y + pt1.Y) * (pt3.X - pt1.X) + (double) (pt1.Y + pt2.Y) * (pt1.X - pt2.X)
				+ (double) (pt2.Y + pt3.Y) * (pt2.X - pt3.X);
	}

	private static OutRec GetRealOutRec(OutRec outRec) {
		while ((outRec != null) && (outRec.pts == null)) {
			outRec = outRec.owner;
		}
		return outRec;
	}

	private static void UncoupleOutRec(Active ae) {
		OutRec outrec = ae.outrec;
		if (outrec == null) {
			return;
		}
		outrec.frontEdge.outrec = null;
		outrec.backEdge.outrec = null;
		outrec.frontEdge = null;
		outrec.backEdge = null;
	}

	private boolean OutrecIsAscending(Active hotEdge) {
		return (hotEdge == hotEdge.outrec.frontEdge);
	}

	private static void SwapFrontBackSides(OutRec outrec) {
		// while this proc. is needed for open paths
		// it's almost never needed for closed paths
		Active ae2 = outrec.frontEdge;
		outrec.frontEdge = outrec.backEdge;
		outrec.backEdge = ae2;
		outrec.pts = outrec.pts.next;
	}

	private static boolean EdgesAdjacentInAEL(IntersectNode inode) {
		return (inode.edge1.nextInAEL == inode.edge2) || (inode.edge1.prevInAEL == inode.edge2);
	}

	protected final void ClearSolution() {
		while (actives != null) {
			DeleteFromAEL(actives);
		}
		scanlineList.clear();
		DisposeIntersectNodes();
		joinerList.clear();
		horzJoiners = null;
		outrecList.clear();
	}

	public final void Clear() {
		ClearSolution();
		minimaList.clear();
		vertexList.clear();
		currentLocMin = 0;
		isSortedMinimaList = false;
		hasOpenPaths = false;
	}

	protected final void Reset() {
		if (!isSortedMinimaList) {
			minimaList.sort((locMin1, locMin2) -> Long.compare(locMin2.vertex.pt.Y, locMin1.vertex.pt.Y));
			isSortedMinimaList = true;
		}

		for (int i = minimaList.size() - 1; i >= 0; i--) {
			scanlineList.add(minimaList.get(i).vertex.pt.Y);
		}

		currentBotY = 0;
		currentLocMin = 0;
		actives = null;
		sel = null;
		succeeded = true;
	}

	private void InsertScanline(long y) {
		if (!scanlineList.contains(y)) {
			scanlineList.add(y);
		}
	}

	private boolean PopScanline(OutObject<Long> y) {
		if (scanlineList.isEmpty()) {
			y.argValue = 0L;
			return false;
		}

		y.argValue = scanlineList.pollLast();
		while (!scanlineList.isEmpty() && scanlineList.last().equals(y.argValue)) {
			scanlineList.pollLast();
		}
		return true;
	}

	private boolean HasLocMinAtY(long y) {
		return (currentLocMin < minimaList.size() && minimaList.get(currentLocMin).vertex.pt.Y == y);
	}

	private LocalMinima PopLocalMinima() {
		return minimaList.get(currentLocMin++);
	}

	private void AddLocMin(Vertex vert, PathType polytype, boolean isOpen) {
		// make sure the vertex is added only once ...
		if ((vert.flags.getValue() & VertexFlags.LocalMin.getValue()) != VertexFlags.None.getValue()) {
			return;
		}
		vert.flags = VertexFlags.forValue(vert.flags.getValue() | VertexFlags.LocalMin.getValue());

		LocalMinima lm = new LocalMinima(vert, polytype, isOpen);
		minimaList.add(lm);
	}

	protected final void AddPathsToVertexList(Paths64 paths, PathType polytype, boolean isOpen) {
		for (Path64 path : paths) {
			Vertex v0 = null, prevv = null, currv;
			for (Point64 pt : path) {
				if (v0 == null) {
					v0 = new Vertex(pt, VertexFlags.None, null);
					vertexList.add(v0);
					prevv = v0;
				} else if (prevv.pt.opNotEquals(pt)) { // ie skips duplicates
					currv = new Vertex(pt, VertexFlags.None, prevv);
					vertexList.add(currv);
					prevv.next = currv;
					prevv = currv;
				}
			}
			if (prevv == null || prevv.prev == null) {
				continue;
			}
			if (!isOpen && v0.pt.opEquals(prevv.pt)) {
				prevv = prevv.prev;
			}
			prevv.next = v0;
			v0.prev = prevv;
			if (!isOpen && prevv == prevv.next) {
				continue;
			}

			// OK, we have a valid path
			boolean goingup, goingup0;
			if (isOpen) {
				currv = v0.next;
				while (v0 != currv && currv.pt.Y == v0.pt.Y) {
					currv = currv.next;
				}
				goingup = currv.pt.Y <= v0.pt.Y;
				if (goingup) {
					v0.flags = VertexFlags.OpenStart;
					AddLocMin(v0, polytype, true);
				} else {
					v0.flags = VertexFlags.forValue(VertexFlags.OpenStart.getValue() | VertexFlags.LocalMax.getValue());
				}
			} else { // closed path
				prevv = v0.prev;
				while (!v0.equals(prevv) && prevv.pt.Y == v0.pt.Y) {
					prevv = prevv.prev;
				}
				if (v0.equals(prevv)) {
					continue; // only open paths can be completely flat
				}
				goingup = prevv.pt.Y > v0.pt.Y;
			}

			goingup0 = goingup;
			prevv = v0;
			currv = v0.next;
			while (!v0.equals(currv)) {
				if (currv.pt.Y > prevv.pt.Y && goingup) {
					prevv.flags = VertexFlags.forValue(prevv.flags.getValue() | VertexFlags.LocalMax.getValue());
					goingup = false;
				} else if (currv.pt.Y < prevv.pt.Y && !goingup) {
					goingup = true;
					AddLocMin(prevv, polytype, isOpen);
				}
				prevv = currv;
				currv = currv.next;
			}

			if (isOpen) {
				prevv.flags = VertexFlags.forValue(prevv.flags.getValue() | VertexFlags.OpenEnd.getValue());
				if (goingup) {
					prevv.flags = VertexFlags.forValue(prevv.flags.getValue() | VertexFlags.LocalMax.getValue());
				} else {
					AddLocMin(prevv, polytype, isOpen);
				}
			} else if (goingup != goingup0) {
				if (goingup0) {
					AddLocMin(prevv, polytype, false);
				} else {
					prevv.flags = VertexFlags.forValue(prevv.flags.getValue() | VertexFlags.LocalMax.getValue());
				}
			}
		}
	}

	public final void AddSubject(Path64 path) {
		AddPath(path, PathType.Subject);
	}

	/**
	 * Adds one or more closed subject paths (polygons) to the Clipper object.
	 * 
	 * @param paths
	 */
	public final void AddSubject(Paths64 paths) {
		paths.forEach(path -> AddPath(path, PathType.Subject));
	}

	/**
	 * Adds one or more open subject paths (polylines) to the Clipper object.
	 * 
	 * @param path
	 */
	public final void AddOpenSubject(Path64 path) {
		AddPath(path, PathType.Subject, true);
	}

	public final void AddOpenSubject(Paths64 paths) {
		paths.forEach(path -> AddPath(path, PathType.Subject, true));
	}

	/**
	 * Adds one or more clip polygons to the Clipper object.
	 * 
	 * @param path
	 */
	public final void AddClip(Path64 path) {
		AddPath(path, PathType.Clip);
	}

	public final void AddClip(Paths64 paths) {
		paths.forEach(path -> AddPath(path, PathType.Clip));
	}

	public final void AddPath(Path64 path, PathType polytype) {
		AddPath(path, polytype, false);
	}

	public final void AddPath(Path64 path, PathType polytype, boolean isOpen) {
		Paths64 tmp = new Paths64();
		tmp.add(path);
		AddPaths(tmp, polytype, isOpen);
	}

	public final void AddPaths(Paths64 paths, PathType polytype) {
		AddPaths(paths, polytype, false);
	}

	public final void AddPaths(Paths64 paths, PathType polytype, boolean isOpen) {
		if (isOpen) {
			hasOpenPaths = true;
		}
		isSortedMinimaList = false;
		AddPathsToVertexList(paths, polytype, isOpen);
	}

	private boolean IsContributingClosed(Active ae) {
		switch (fillrule) {
			case Positive :
				if (ae.windCount != 1) {
					return false;
				}
				break;
			case Negative :
				if (ae.windCount != -1) {
					return false;
				}
				break;
			case NonZero :
				if (Math.abs(ae.windCount) != 1) {
					return false;
				}
				break;
		}

		switch (cliptype) {
			case Intersection :
				return switch (fillrule) {
					case Positive -> ae.windCount2 > 0;
					case Negative -> ae.windCount2 < 0;
					default -> ae.windCount2 != 0;
				};
			case Union :
				return switch (fillrule) {
					case Positive -> ae.windCount2 <= 0;
					case Negative -> ae.windCount2 >= 0;
					default -> ae.windCount2 == 0;
				};

			case Difference :
				boolean result = switch (fillrule) {
					case Positive -> ae.windCount2 <= 0;
					case Negative -> ae.windCount2 >= 0;
					default -> ae.windCount2 == 0;
				};
				return (GetPolyType(ae) == PathType.Subject) ? result : !result;

			case Xor :
				return true; // XOr is always contributing unless open

			default :
				return false;
		}
	}

	private boolean IsContributingOpen(Active ae) {
		boolean isInClip, isInSubj;
		switch (fillrule) {
			case Positive :
				isInSubj = ae.windCount > 0;
				isInClip = ae.windCount2 > 0;
				break;
			case Negative :
				isInSubj = ae.windCount < 0;
				isInClip = ae.windCount2 < 0;
				break;
			default :
				isInSubj = ae.windCount != 0;
				isInClip = ae.windCount2 != 0;
				break;
		}

		return switch (cliptype) {
			case Intersection -> isInClip;
			case Union -> !isInSubj && !isInClip;
			default -> !isInClip;
		};
	}

	private void SetWindCountForClosedPathEdge(Active ae) {
		/*
		 * Wind counts refer to polygon regions not edges, so here an edge's WindCnt
		 * indicates the higher of the wind counts for the two regions touching the
		 * edge. (nb: Adjacent regions can only ever have their wind counts differ by
		 * one. Also, open paths have no meaningful wind directions or counts.)
		 */

		Active ae2 = ae.prevInAEL;
		// find the nearest closed path edge of the same PolyType in AEL (heading left)
		PathType pt = GetPolyType(ae);
		while (ae2 != null && (GetPolyType(ae2) != pt || IsOpen(ae2))) {
			ae2 = ae2.prevInAEL;
		}

		if (ae2 == null) {
			ae.windCount = ae.windDx;
			ae2 = actives;
		} else if (fillrule == FillRule.EvenOdd) {
			ae.windCount = ae.windDx;
			ae.windCount2 = ae2.windCount2;
			ae2 = ae2.nextInAEL;
		} else {
			// NonZero, positive, or negative filling here ...
			// when e2's WindCnt is in the SAME direction as its WindDx,
			// then polygon will fill on the right of 'e2' (and 'e' will be inside)
			// nb: neither e2.WindCnt nor e2.WindDx should ever be 0.
			if (ae2.windCount * ae2.windDx < 0) {
				// opposite directions so 'ae' is outside 'ae2' ...
				if (Math.abs(ae2.windCount) > 1) {
					// outside prev poly but still inside another.
					if (ae2.windDx * ae.windDx < 0) {
						// reversing direction so use the same WC
						ae.windCount = ae2.windCount;
					} else {
						// otherwise keep 'reducing' the WC by 1 (i.e. towards 0) ...
						ae.windCount = ae2.windCount + ae.windDx;
					}
				} else {
					// now outside all polys of same polytype so set own WC ...
					ae.windCount = (IsOpen(ae) ? 1 : ae.windDx);
				}
			} else // 'ae' must be inside 'ae2'
			if (ae2.windDx * ae.windDx < 0) {
				// reversing direction so use the same WC
				ae.windCount = ae2.windCount;
			} else {
				// otherwise keep 'increasing' the WC by 1 (i.e. away from 0) ...
				ae.windCount = ae2.windCount + ae.windDx;
			}

			ae.windCount2 = ae2.windCount2;
			ae2 = ae2.nextInAEL; // i.e. get ready to calc WindCnt2
		}

		// update windCount2 ...
		if (fillrule == FillRule.EvenOdd) {
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) != pt && !IsOpen(ae2)) {
					ae.windCount2 = (ae.windCount2 == 0 ? 1 : 0);
				}
				ae2 = ae2.nextInAEL;
			}
		} else {
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) != pt && !IsOpen(ae2)) {
					ae.windCount2 += ae2.windDx;
				}
				ae2 = ae2.nextInAEL;
			}
		}
	}

	private void SetWindCountForOpenPathEdge(Active ae) {
		Active ae2 = actives;
		if (fillrule == FillRule.EvenOdd) {
			int cnt1 = 0, cnt2 = 0;
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) == PathType.Clip) {
					cnt2++;
				} else if (!IsOpen(ae2)) {
					cnt1++;
				}
				ae2 = ae2.nextInAEL;
			}

			ae.windCount = (IsOdd(cnt1) ? 1 : 0);
			ae.windCount2 = (IsOdd(cnt2) ? 1 : 0);
		} else {
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) == PathType.Clip) {
					ae.windCount2 += ae2.windDx;
				} else if (!IsOpen(ae2)) {
					ae.windCount += ae2.windDx;
				}
				ae2 = ae2.nextInAEL;
			}
		}
	}

	private boolean IsValidAelOrder(Active resident, Active newcomer) {
		if (newcomer.curX != resident.curX) {
			return newcomer.curX > resident.curX;
		}

		// get the turning direction a1.top, a2.bot, a2.top
		double d = InternalClipper.CrossProduct(resident.top, newcomer.bot, newcomer.top);
		if (d != 0) {
			return (d < 0);
		}

		// edges must be collinear to get here

		// for starting open paths, place them according to
		// the direction they're about to turn
		if (!IsMaxima(resident) && (resident.top.Y > newcomer.top.Y)) {
			return InternalClipper.CrossProduct(newcomer.bot, resident.top, NextVertex(resident).pt) <= 0;
		}

		if (!IsMaxima(newcomer) && (newcomer.top.Y > resident.top.Y)) {
			return InternalClipper.CrossProduct(newcomer.bot, newcomer.top, NextVertex(newcomer).pt) >= 0;
		}

		long y = newcomer.bot.Y;
		boolean newcomerIsLeft = newcomer.isLeftBound;

		if (resident.bot.Y != y || resident.localMin.vertex.pt.Y != y) {
			return newcomer.isLeftBound;
		}
		// resident must also have just been inserted
		if (resident.isLeftBound != newcomerIsLeft) {
			return newcomerIsLeft;
		}
		if (InternalClipper.CrossProduct(PrevPrevVertex(resident).pt, resident.bot, resident.top) == 0) {
			return true;
		}
		// compare turning direction of the alternate bound
		return (InternalClipper.CrossProduct(PrevPrevVertex(resident).pt, newcomer.bot, PrevPrevVertex(newcomer).pt) > 0) == newcomerIsLeft;
	}

	private void InsertLeftEdge(Active ae) {
		Active ae2;

		if (actives == null) {
			ae.prevInAEL = null;
			ae.nextInAEL = null;
			actives = ae;
		} else if (!IsValidAelOrder(actives, ae)) {
			ae.prevInAEL = null;
			ae.nextInAEL = actives;
			actives.prevInAEL = ae;
			actives = ae;
		} else {
			ae2 = actives;
			while (ae2.nextInAEL != null && IsValidAelOrder(ae2.nextInAEL, ae)) {
				ae2 = ae2.nextInAEL;
			}
			ae.nextInAEL = ae2.nextInAEL;
			if (ae2.nextInAEL != null) {
				ae2.nextInAEL.prevInAEL = ae;
			}
			ae.prevInAEL = ae2;
			ae2.nextInAEL = ae;
		}
	}

	private void InsertRightEdge(Active ae, Active ae2) {
		ae2.nextInAEL = ae.nextInAEL;
		if (ae.nextInAEL != null) {
			ae.nextInAEL.prevInAEL = ae2;
		}
		ae2.prevInAEL = ae;
		ae.nextInAEL = ae2;
	}

	private void InsertLocalMinimaIntoAEL(long botY) {
		LocalMinima localMinima;
		Active leftBound, rightBound = null;
		// Add any local minima (if any) at BotY ...
		// NB horizontal local minima edges should contain locMin.vertex.prev
		while (HasLocMinAtY(botY)) {
			localMinima = PopLocalMinima();
			if ((localMinima.vertex.flags.getValue() & VertexFlags.OpenStart.getValue()) != VertexFlags.None.getValue()) {
				leftBound = null;
			} else {
				leftBound = new Active();
				leftBound.bot = localMinima.vertex.pt;
				leftBound.curX = localMinima.vertex.pt.X;
				leftBound.windDx = -1;
				leftBound.vertexTop = localMinima.vertex.prev;
				leftBound.top = localMinima.vertex.prev.pt;
				leftBound.outrec = null;
				leftBound.localMin = localMinima;
				SetDx(leftBound);
			}

			if ((localMinima.vertex.flags.getValue() & VertexFlags.OpenEnd.getValue()) != VertexFlags.None.getValue()) {
				rightBound = null;
			} else {
				rightBound = new Active();
				rightBound.bot = localMinima.vertex.pt;
				rightBound.curX = localMinima.vertex.pt.X;
				rightBound.windDx = 1;
				rightBound.vertexTop = localMinima.vertex.next;
				rightBound.top = localMinima.vertex.next.pt;
				rightBound.outrec = null;
				rightBound.localMin = localMinima;
				SetDx(rightBound);
			}

			// Currently LeftB is just the descending bound and RightB is the ascending.
			// Now if the LeftB isn't on the left of RightB then we need swap them.
			if (leftBound != null && rightBound != null) {
				if (IsHorizontal(leftBound)) {
					if (IsHeadingRightHorz(leftBound)) {
						RefObject<Active> tempRefleftBound = new RefObject<>(leftBound);
						RefObject<Active> tempRefrightBound = new RefObject<>(rightBound);
						SwapActives(tempRefleftBound, tempRefrightBound);
						rightBound = tempRefrightBound.argValue;
						leftBound = tempRefleftBound.argValue;
					}
				} else if (IsHorizontal(rightBound)) {
					if (IsHeadingLeftHorz(rightBound)) {
						RefObject<Active> tempRefleftBound2 = new RefObject<>(leftBound);
						RefObject<Active> tempRefrightBound2 = new RefObject<>(rightBound);
						SwapActives(tempRefleftBound2, tempRefrightBound2);
						rightBound = tempRefrightBound2.argValue;
						leftBound = tempRefleftBound2.argValue;
					}
				} else if (leftBound.dx < rightBound.dx) {
					RefObject<Active> tempRefleftBound3 = new RefObject<>(leftBound);
					RefObject<Active> tempRefrightBound3 = new RefObject<>(rightBound);
					SwapActives(tempRefleftBound3, tempRefrightBound3);
					rightBound = tempRefrightBound3.argValue;
					leftBound = tempRefleftBound3.argValue;
				}
				// so when leftBound has windDx == 1, the polygon will be oriented
				// counter-clockwise in Cartesian coords (clockwise with inverted Y).
			} else if (leftBound == null) {
				leftBound = rightBound;
				rightBound = null;
			}

			boolean contributing;
			leftBound.isLeftBound = true;
			InsertLeftEdge(leftBound);

			if (IsOpen(leftBound)) {
				SetWindCountForOpenPathEdge(leftBound);
				contributing = IsContributingOpen(leftBound);
			} else {
				SetWindCountForClosedPathEdge(leftBound);
				contributing = IsContributingClosed(leftBound);
			}

			if (rightBound != null) {
				rightBound.windCount = leftBound.windCount;
				rightBound.windCount2 = leftBound.windCount2;
				InsertRightEdge(leftBound, rightBound); ///////

				if (contributing) {
					AddLocalMinPoly(leftBound, rightBound, leftBound.bot, true);
					if (!IsHorizontal(leftBound) && TestJoinWithPrev1(leftBound)) {
						OutPt op = AddOutPt(leftBound.prevInAEL, leftBound.bot);
						AddJoin(op, leftBound.outrec.pts);
					}
				}

				while (rightBound.nextInAEL != null && IsValidAelOrder(rightBound.nextInAEL, rightBound)) {
					IntersectEdges(rightBound, rightBound.nextInAEL, rightBound.bot);
					SwapPositionsInAEL(rightBound, rightBound.nextInAEL);
				}

				if (!IsHorizontal(rightBound) && TestJoinWithNext1(rightBound)) {
					OutPt op = AddOutPt(rightBound.nextInAEL, rightBound.bot);
					AddJoin(rightBound.outrec.pts, op);
				}

				if (IsHorizontal(rightBound)) {
					PushHorz(rightBound);
				} else {
					InsertScanline(rightBound.top.Y);
				}
			} else if (contributing) {
				StartOpenPath(leftBound, leftBound.bot);
			}

			if (IsHorizontal(leftBound)) {
				PushHorz(leftBound);
			} else {
				InsertScanline(leftBound.top.Y);
			}
		} // while (HasLocMinAtY())
	}

	private void PushHorz(Active ae) {
		ae.nextInSEL = sel;
		sel = ae;
	}

	private boolean PopHorz(OutObject<Active> ae) {
		ae.argValue = sel;
		if (sel == null) {
			return false;
		}
		sel = sel.nextInSEL;
		return true;
	}

	private boolean TestJoinWithPrev1(Active e) {
		// this is marginally quicker than TestJoinWithPrev2
		// but can only be used when e.PrevInAEL.currX is accurate
		return IsHotEdge(e) && !IsOpen(e) && (e.prevInAEL != null) && (e.prevInAEL.curX == e.curX) && IsHotEdge(e.prevInAEL)
				&& !IsOpen(e.prevInAEL) && (InternalClipper.CrossProduct(e.prevInAEL.top, e.bot, e.top) == 0);
	}

	private boolean TestJoinWithPrev2(Active e, Point64 currPt) {
		return IsHotEdge(e) && !IsOpen(e) && (e.prevInAEL != null) && !IsOpen(e.prevInAEL) && IsHotEdge(e.prevInAEL)
				&& (e.prevInAEL.top.Y < e.bot.Y) && (Math.abs(TopX(e.prevInAEL, currPt.Y) - currPt.X) < 2)
				&& (InternalClipper.CrossProduct(e.prevInAEL.top, currPt, e.top) == 0);
	}

	private boolean TestJoinWithNext1(Active e) {
		// this is marginally quicker than TestJoinWithNext2
		// but can only be used when e.NextInAEL.currX is accurate
		return IsHotEdge(e) && !IsOpen(e) && (e.nextInAEL != null) && (e.nextInAEL.curX == e.curX) && IsHotEdge(e.nextInAEL)
				&& !IsOpen(e.nextInAEL) && (InternalClipper.CrossProduct(e.nextInAEL.top, e.bot, e.top) == 0);
	}

	private boolean TestJoinWithNext2(Active e, Point64 currPt) {
		return IsHotEdge(e) && !IsOpen(e) && (e.nextInAEL != null) && !IsOpen(e.nextInAEL) && IsHotEdge(e.nextInAEL)
				&& (e.nextInAEL.top.Y < e.bot.Y) && (Math.abs(TopX(e.nextInAEL, currPt.Y) - currPt.X) < 2)
				&& (InternalClipper.CrossProduct(e.nextInAEL.top, currPt, e.top) == 0);
	}

	private OutPt AddLocalMinPoly(Active ae1, Active ae2, Point64 pt) {
		return AddLocalMinPoly(ae1, ae2, pt, false);
	}

	private OutPt AddLocalMinPoly(Active ae1, Active ae2, Point64 pt, boolean isNew) {
		OutRec outrec = new OutRec();
		outrecList.add(outrec);
		outrec.idx = outrecList.size() - 1;
		outrec.pts = null;
		outrec.polypath = null;
		ae1.outrec = outrec;
		ae2.outrec = outrec;

		// Setting the owner and inner/outer states (above) is an essential
		// precursor to setting edge 'sides' (ie left and right sides of output
		// polygons) and hence the orientation of output paths ...

		if (IsOpen(ae1)) {
			outrec.owner = null;
			outrec.isOpen = true;
			if (ae1.windDx > 0) {
				SetSides(outrec, ae1, ae2);
			} else {
				SetSides(outrec, ae2, ae1);
			}
		} else {
			outrec.isOpen = false;
			Active prevHotEdge = GetPrevHotEdge(ae1);
			// e.windDx is the winding direction of the **input** paths
			// and unrelated to the winding direction of output polygons.
			// Output orientation is determined by e.outrec.frontE which is
			// the ascending edge (see AddLocalMinPoly).
			if (prevHotEdge != null) {
				outrec.owner = prevHotEdge.outrec;
				if (OutrecIsAscending(prevHotEdge) == isNew) {
					SetSides(outrec, ae2, ae1);
				} else {
					SetSides(outrec, ae1, ae2);
				}
			} else {
				outrec.owner = null;
				if (isNew) {
					SetSides(outrec, ae1, ae2);
				} else {
					SetSides(outrec, ae2, ae1);
				}
			}
		}

		OutPt op = new OutPt(pt, outrec);
		outrec.pts = op;
		return op;
	}

	private OutPt AddLocalMaxPoly(Active ae1, Active ae2, Point64 pt) {
		if (IsFront(ae1) == IsFront(ae2)) {
			if (IsOpenEnd(ae1)) {
				SwapFrontBackSides(ae1.outrec);
			} else if (IsOpenEnd(ae2)) {
				SwapFrontBackSides(ae2.outrec);
			} else {
				succeeded = false;
				return null;
			}
		}

		OutPt result = AddOutPt(ae1, pt);
		if (ae1.outrec == ae2.outrec) {
			OutRec outrec = ae1.outrec;
			outrec.pts = result;
			UncoupleOutRec(ae1);
			if (!IsOpen(ae1)) {
				CleanCollinear(outrec);
			}
			result = outrec.pts;

			outrec.owner = GetRealOutRec(outrec.owner);
			if (usingPolytree && outrec.owner.frontEdge == null) { // NOTE strange c# syntax
				outrec.owner = GetRealOutRec(outrec.owner.owner);
			}
		}
		// and to preserve the winding orientation of outrec ...
		else if (IsOpen(ae1)) {
			if (ae1.windDx < 0) {
				JoinOutrecPaths(ae1, ae2);
			} else {
				JoinOutrecPaths(ae2, ae1);
			}
		} else if (ae1.outrec.idx < ae2.outrec.idx) {
			JoinOutrecPaths(ae1, ae2);
		} else {
			JoinOutrecPaths(ae2, ae1);
		}

		return result;
	}

	private void JoinOutrecPaths(Active ae1, Active ae2) {
		// join ae2 outrec path onto ae1 outrec path and then delete ae2 outrec path
		// pointers. (NB Only very rarely do the joining ends share the same coords.)
		OutPt p1Start = ae1.outrec.pts;
		OutPt p2Start = ae2.outrec.pts;
		OutPt p1End = p1Start.next;
		OutPt p2End = p2Start.next;
		if (IsFront(ae1)) {
			p2End.prev = p1Start;
			p1Start.next = p2End;
			p2Start.next = p1End;
			p1End.prev = p2Start;
			ae1.outrec.pts = p2Start;
			// nb: if IsOpen(e1) then e1 & e2 must be a 'maximaPair'
			ae1.outrec.frontEdge = ae2.outrec.frontEdge;
			if (ae1.outrec.frontEdge != null) {
				ae1.outrec.frontEdge.outrec = ae1.outrec;
			}
		} else {
			p1End.prev = p2Start;
			p2Start.next = p1End;
			p1Start.next = p2End;
			p2End.prev = p1Start;

			ae1.outrec.backEdge = ae2.outrec.backEdge;
			if (ae1.outrec.backEdge != null) {
				ae1.outrec.backEdge.outrec = ae1.outrec;
			}
		}

		// an owner must have a lower idx otherwise
		// it won't be a valid owner
		if (ae2.outrec.owner != null && ae2.outrec.owner.idx < ae1.outrec.idx) {
			if (ae1.outrec.owner == null || ae2.outrec.owner.idx < ae1.outrec.owner.idx) {
				ae1.outrec.owner = ae2.outrec.owner;
			}
		}

		// after joining, the ae2.OutRec must contains no vertices ...
		ae2.outrec.frontEdge = null;
		ae2.outrec.backEdge = null;
		ae2.outrec.pts = null;
		ae2.outrec.owner = ae1.outrec; // this may be redundant

		if (IsOpenEnd(ae1)) {
			ae2.outrec.pts = ae1.outrec.pts;
			ae1.outrec.pts = null;
		}

		// and ae1 and ae2 are maxima and are about to be dropped from the Actives list.
		ae1.outrec = null;
		ae2.outrec = null;
	}

	private OutPt AddOutPt(Active ae, Point64 pt) {
		OutPt newOp;

		// Outrec.OutPts: a circular doubly-linked-list of POutPt where ...
		// opFront[.Prev]* ~~~> opBack & opBack == opFront.Next
		OutRec outrec = ae.outrec;
		boolean toFront = IsFront(ae);
		OutPt opFront = outrec.pts;
		OutPt opBack = opFront.next;

		if (toFront && (pt.opEquals(opFront.pt))) {
			newOp = opFront;
		} else if (!toFront && (pt.opEquals(opBack.pt))) {
			newOp = opBack;
		} else {
			newOp = new OutPt(pt, outrec);
			opBack.prev = newOp;
			newOp.prev = opFront;
			newOp.next = opBack;
			opFront.next = newOp;
			if (toFront) {
				outrec.pts = newOp;
			}
		}
		return newOp;
	}

	private OutPt StartOpenPath(Active ae, Point64 pt) {
		OutRec outrec = new OutRec();
		outrecList.add(outrec);
		outrec.idx = outrecList.size() - 1;
		outrec.owner = null;
		outrec.isOpen = true;
		outrec.pts = null;
		outrec.polypath = null;
		if (ae.windDx > 0) {
			outrec.frontEdge = ae;
			outrec.backEdge = null;
		} else {
			outrec.frontEdge = null;
			outrec.backEdge = ae;
		}

		ae.outrec = outrec;
		OutPt op = new OutPt(pt, outrec);
		outrec.pts = op;
		return op;
	}

	private void UpdateEdgeIntoAEL(Active ae) {
		ae.bot = ae.top;
		ae.vertexTop = NextVertex(ae);
		ae.top = ae.vertexTop.pt;
		ae.curX = ae.bot.X;
		SetDx(ae);
		if (IsHorizontal(ae)) {
			return;
		}
		InsertScanline(ae.top.Y);
		if (TestJoinWithPrev1(ae)) {
			OutPt op1 = AddOutPt(ae.prevInAEL, ae.bot);
			OutPt op2 = AddOutPt(ae, ae.bot);
			AddJoin(op1, op2);
		}
	}

	private Active FindEdgeWithMatchingLocMin(Active e) {
		Active result = e.nextInAEL;
		while (result != null) {
			if (result.localMin.opEquals(e.localMin)) {
				return result;
			}
			if (!IsHorizontal(result) && e.bot.opNotEquals(result.bot)) {
				result = null;
			} else {
				result = result.nextInAEL;
			}
		}
		result = e.prevInAEL;
		while (result != null) {
			if (result.localMin.opEquals(e.localMin)) {
				return result;
			}
			if (!IsHorizontal(result) && e.bot.opNotEquals(result.bot)) {
				return null;
			}
			result = result.prevInAEL;
		}
		return result;
	}

	private OutPt IntersectEdges(Active ae1, Active ae2, Point64 pt) {
		OutPt resultOp = null;

		// MANAGE OPEN PATH INTERSECTIONS SEPARATELY ...
		if (hasOpenPaths && (IsOpen(ae1) || IsOpen(ae2))) {
			if (IsOpen(ae1) && IsOpen(ae2)) {
				return null;
			}
			// the following line avoids duplicating quite a bit of code
			if (IsOpen(ae2)) {
				RefObject<Active> tempRefae1 = new RefObject<>(ae1);
				RefObject<Active> tempRefae2 = new RefObject<>(ae2);
				SwapActives(tempRefae1, tempRefae2);
				ae2 = tempRefae2.argValue;
				ae1 = tempRefae1.argValue;
			}

			if (cliptype == ClipType.Union) {
				if (!IsHotEdge(ae2)) {
					return null;
				}
			} else if (ae2.localMin.polytype == PathType.Subject) {
				return null;
			}

			switch (fillrule) {
				case Positive :
					if (ae2.windCount != 1) {
						return null;
					}
					break;
				case Negative :
					if (ae2.windCount != -1) {
						return null;
					}
					break;
				default :
					if (Math.abs(ae2.windCount) != 1) {
						return null;
					}
					break;
			}

			// toggle contribution ...
			if (IsHotEdge(ae1)) {
				resultOp = AddOutPt(ae1, pt);
				if (IsFront(ae1)) {
					ae1.outrec.frontEdge = null;
				} else {
					ae1.outrec.backEdge = null;
				}
				ae1.outrec = null;
			}

			// horizontal edges can pass under open paths at a LocMins
			else if (pt.opEquals(ae1.localMin.vertex.pt) && !IsOpenEnd(ae1.localMin.vertex)) {
				// find the other side of the LocMin and
				// if it's 'hot' join up with it ...
				Active ae3 = FindEdgeWithMatchingLocMin(ae1);
				if (ae3 != null && IsHotEdge(ae3)) {
					ae1.outrec = ae3.outrec;
					if (ae1.windDx > 0) {
						SetSides(ae3.outrec, ae1, ae3);
					} else {
						SetSides(ae3.outrec, ae3, ae1);
					}
					return ae3.outrec.pts;
				}

				resultOp = StartOpenPath(ae1, pt);
			} else {
				resultOp = StartOpenPath(ae1, pt);
			}

			return resultOp;
		}

		// MANAGING CLOSED PATHS FROM HERE ON

		// UPDATE WINDING COUNTS...

		int oldE1WindCount, oldE2WindCount;
		if (ae1.localMin.polytype == ae2.localMin.polytype) {
			if (fillrule == FillRule.EvenOdd) {
				oldE1WindCount = ae1.windCount;
				ae1.windCount = ae2.windCount;
				ae2.windCount = oldE1WindCount;
			} else {
				if (ae1.windCount + ae2.windDx == 0) {
					ae1.windCount = -ae1.windCount;
				} else {
					ae1.windCount += ae2.windDx;
				}
				if (ae2.windCount - ae1.windDx == 0) {
					ae2.windCount = -ae2.windCount;
				} else {
					ae2.windCount -= ae1.windDx;
				}
			}
		} else {
			if (fillrule != FillRule.EvenOdd) {
				ae1.windCount2 += ae2.windDx;
			} else {
				ae1.windCount2 = (ae1.windCount2 == 0 ? 1 : 0);
			}
			if (fillrule != FillRule.EvenOdd) {
				ae2.windCount2 -= ae1.windDx;
			} else {
				ae2.windCount2 = (ae2.windCount2 == 0 ? 1 : 0);
			}
		}

		switch (fillrule) {
			case Positive :
				oldE1WindCount = ae1.windCount;
				oldE2WindCount = ae2.windCount;
				break;
			case Negative :
				oldE1WindCount = -ae1.windCount;
				oldE2WindCount = -ae2.windCount;
				break;
			default :
				oldE1WindCount = Math.abs(ae1.windCount);
				oldE2WindCount = Math.abs(ae2.windCount);
				break;
		}

		boolean e1WindCountIs0or1 = oldE1WindCount == 0 || oldE1WindCount == 1;
		boolean e2WindCountIs0or1 = oldE2WindCount == 0 || oldE2WindCount == 1;

		if ((!IsHotEdge(ae1) && !e1WindCountIs0or1) || (!IsHotEdge(ae2) && !e2WindCountIs0or1)) {
			return null;
		}

		// NOW PROCESS THE INTERSECTION ...

		// if both edges are 'hot' ...
		if (IsHotEdge(ae1) && IsHotEdge(ae2)) {
			if ((oldE1WindCount != 0 && oldE1WindCount != 1) || (oldE2WindCount != 0 && oldE2WindCount != 1)
					|| (ae1.localMin.polytype != ae2.localMin.polytype && cliptype != ClipType.Xor)) {
				resultOp = AddLocalMaxPoly(ae1, ae2, pt);
			} else if (IsFront(ae1) || (ae1.outrec == ae2.outrec)) {
				// this 'else if' condition isn't strictly needed but
				// it's sensible to split polygons that ony touch at
				// a common vertex (not at common edges).
				resultOp = AddLocalMaxPoly(ae1, ae2, pt);
				OutPt op2 = AddLocalMinPoly(ae1, ae2, pt);
				if (resultOp != null && resultOp.pt.opEquals(op2.pt) && !IsHorizontal(ae1) && !IsHorizontal(ae2)
						&& (InternalClipper.CrossProduct(ae1.bot, resultOp.pt, ae2.bot) == 0)) {
					AddJoin(resultOp, op2);
				}
			} else {
				// can't treat as maxima & minima
				resultOp = AddOutPt(ae1, pt);
				AddOutPt(ae2, pt);
				SwapOutrecs(ae1, ae2);
			}
		}

		// if one or other edge is 'hot' ...
		else if (IsHotEdge(ae1)) {
			resultOp = AddOutPt(ae1, pt);
			SwapOutrecs(ae1, ae2);
		} else if (IsHotEdge(ae2)) {
			resultOp = AddOutPt(ae2, pt);
			SwapOutrecs(ae1, ae2);
		}

		// neither edge is 'hot'
		else {
			long e1Wc2, e2Wc2;
			switch (fillrule) {
				case Positive :
					e1Wc2 = ae1.windCount2;
					e2Wc2 = ae2.windCount2;
					break;
				case Negative :
					e1Wc2 = -ae1.windCount2;
					e2Wc2 = -ae2.windCount2;
					break;
				default :
					e1Wc2 = Math.abs(ae1.windCount2);
					e2Wc2 = Math.abs(ae2.windCount2);
					break;
			}

			if (!IsSamePolyType(ae1, ae2)) {
				resultOp = AddLocalMinPoly(ae1, ae2, pt);
			} else if (oldE1WindCount == 1 && oldE2WindCount == 1) {
				resultOp = null;
				switch (cliptype) {
					case Union :
						if (e1Wc2 > 0 && e2Wc2 > 0) {
							return null;
						}
						resultOp = AddLocalMinPoly(ae1, ae2, pt);
						break;

					case Difference :
						if (((GetPolyType(ae1) == PathType.Clip) && (e1Wc2 > 0) && (e2Wc2 > 0))
								|| ((GetPolyType(ae1) == PathType.Subject) && (e1Wc2 <= 0) && (e2Wc2 <= 0))) {
							resultOp = AddLocalMinPoly(ae1, ae2, pt);
						}

						break;

					case Xor :
						resultOp = AddLocalMinPoly(ae1, ae2, pt);
						break;

					default : // ClipType.Intersection:
						if (e1Wc2 <= 0 || e2Wc2 <= 0) {
							return null;
						}
						resultOp = AddLocalMinPoly(ae1, ae2, pt);
						break;
				}
			}
		}

		return resultOp;
	}

	private void DeleteFromAEL(Active ae) {
		Active prev = ae.prevInAEL;
		Active next = ae.nextInAEL;
		if (prev == null && next == null && (!actives.equals(ae))) {
			return; // already deleted
		}
		if (prev != null) {
			prev.nextInAEL = next;
		} else {
			actives = next;
		}
		if (next != null) {
			next.prevInAEL = prev;
		}
		// delete &ae;
	}

	private void AdjustCurrXAndCopyToSEL(long topY) {
		Active ae = actives;
		sel = ae;
		while (ae != null) {
			ae.prevInSEL = ae.prevInAEL;
			ae.nextInSEL = ae.nextInAEL;
			ae.jump = ae.nextInSEL;
			ae.curX = TopX(ae, topY);
			// NB don't update ae.curr.Y yet (see AddNewIntersectNode)
			ae = ae.nextInAEL;
		}
	}

	protected final void ExecuteInternal(ClipType ct, FillRule fillRule) {
		if (ct == ClipType.None) {
			return;
		}
		fillrule = fillRule;
		cliptype = ct;
		Reset();
		long y;
		OutObject<Long> tempOuty = new OutObject<>();
		if (!PopScanline(tempOuty)) {
			return;
		} else {
			y = tempOuty.argValue;
		}
		while (succeeded) {
			InsertLocalMinimaIntoAEL(y);
			Active ae = null;
			OutObject<Active> tempOutae = new OutObject<>();
			while (PopHorz(tempOutae)) {
				ae = tempOutae.argValue;
				DoHorizontal(ae);
			}
			ae = tempOutae.argValue;
			ConvertHorzTrialsToJoins();
			currentBotY = y; // bottom of scanbeam
			OutObject<Long> tempOuty2 = new OutObject<>();
			if (!PopScanline(tempOuty2)) {
				y = tempOuty2.argValue;
				break; // y new top of scanbeam
			} else {
				y = tempOuty2.argValue;
			}
			DoIntersections(y);
			DoTopOfScanbeam(y);
			OutObject<Active> tempOutae2 = new OutObject<>();
			while (PopHorz(tempOutae2)) {
				ae = tempOutae2.argValue;
				DoHorizontal(ae);
			}
			ae = tempOutae2.argValue;
		}

		if (succeeded) {
			ProcessJoinList();
		}
	}

	private void DoIntersections(long topY) {
		if (BuildIntersectList(topY)) {
			ProcessIntersectList();
			DisposeIntersectNodes();
		}
	}

	private void DisposeIntersectNodes() {
		intersectList.clear();
	}

	private void AddNewIntersectNode(Active ae1, Active ae2, long topY) {
		Point64 pt = GetIntersectPoint(ae1, ae2);

		// rounding errors can occasionally place the calculated intersection
		// point either below or above the scanbeam, so check and correct ...
		if (pt.Y > currentBotY) {
			// ae.curr.y is still the bottom of scanbeam
			// use the more vertical of the 2 edges to derive pt.x ...
			if (Math.abs(ae1.dx) < Math.abs(ae2.dx)) {
				pt = new Point64(TopX(ae1, currentBotY), currentBotY);
			} else {
				pt = new Point64(TopX(ae2, currentBotY), currentBotY);
			}
		} else if (pt.Y < topY) {
			// topY is at the top of the scanbeam
			if (ae1.top.Y == topY) {
				pt = new Point64(ae1.top.X, topY);
			} else if (ae2.top.Y == topY) {
				pt = new Point64(ae2.top.X, topY);
			} else if (Math.abs(ae1.dx) < Math.abs(ae2.dx)) {
				pt = new Point64(ae1.curX, topY);
			} else {
				pt = new Point64(ae2.curX, topY);
			}
		}

		IntersectNode node = new IntersectNode(pt, ae1, ae2);
		intersectList.add(node);
	}

	private Active ExtractFromSEL(Active ae) {
		Active res = ae.nextInSEL;
		if (res != null) {
			res.prevInSEL = ae.prevInSEL;
		}
		ae.prevInSEL.nextInSEL = res;
		return res;
	}

	private void Insert1Before2InSEL(Active ae1, Active ae2) {
		ae1.prevInSEL = ae2.prevInSEL;
		if (ae1.prevInSEL != null) {
			ae1.prevInSEL.nextInSEL = ae1;
		}
		ae1.nextInSEL = ae2;
		ae2.prevInSEL = ae1;
	}

	private boolean BuildIntersectList(long topY) {
		if (actives == null || actives.nextInAEL == null) {
			return false;
		}

		// Calculate edge positions at the top of the current scanbeam, and from this
		// we will determine the intersections required to reach these new positions.
		AdjustCurrXAndCopyToSEL(topY);

		// Find all edge intersections in the current scanbeam using a stable merge
		// sort that ensures only adjacent edges are intersecting. Intersect info is
		// stored in FIntersectList ready to be processed in ProcessIntersectList.
		// Re merge sorts see https://stackoverflow.com/a/46319131/359538

		Active left = sel, right, lEnd, rEnd, currBase, prevBase, tmp;

		while (left.jump != null) {
			prevBase = null;
			while (left != null && left.jump != null) {
				currBase = left;
				right = left.jump;
				lEnd = right;
				rEnd = right.jump;
				left.jump = rEnd;
				while (left != lEnd && right != rEnd) {
					if (right.curX < left.curX) {
						tmp = right.prevInSEL;
						for (;;) {
							AddNewIntersectNode(tmp, right, topY);
							if (left.equals(tmp)) {
								break;
							}
							tmp = tmp.prevInSEL;
						}

						tmp = right;
						right = ExtractFromSEL(tmp);
						lEnd = right;
						Insert1Before2InSEL(tmp, left);
						if (left.equals(currBase)) {
							currBase = tmp;
							currBase.jump = rEnd;
							if (prevBase == null) {
								sel = currBase;
							} else {
								prevBase.jump = currBase;
							}
						}
					} else {
						left = left.nextInSEL;
					}
				}

				prevBase = currBase;
				left = rEnd;
			}
			left = sel;
		}

		return !intersectList.isEmpty();
	}

	private void ProcessIntersectList() {
		// We now have a list of intersections required so that edges will be
		// correctly positioned at the top of the scanbeam. However, it's important
		// that edge intersections are processed from the bottom up, but it's also
		// crucial that intersections only occur between adjacent edges.

		// First we do a quicksort so intersections proceed in a bottom up order ...
		intersectList.sort((a, b) -> {
			if (a.pt.Y == b.pt.Y) {
				return (a.pt.X < b.pt.X) ? -1 : 1;
			}
			return (a.pt.Y > b.pt.Y) ? -1 : 1;
		});

		// Now as we process these intersections, we must sometimes adjust the order
		// to ensure that intersecting edges are always adjacent ...
		for (int i = 0; i < intersectList.size(); ++i) {
			if (!EdgesAdjacentInAEL(intersectList.get(i))) {
				int j = i + 1;
				while (!EdgesAdjacentInAEL(intersectList.get(j))) {
					j++;
				}
				// swap
				Collections.swap(intersectList, i, j);
			}

			IntersectNode node = intersectList.get(i);
			IntersectEdges(node.edge1, node.edge2, node.pt);
			SwapPositionsInAEL(node.edge1, node.edge2);

			if (TestJoinWithPrev2(node.edge2, node.pt)) {
				OutPt op1 = AddOutPt(node.edge2.prevInAEL, node.pt);
				OutPt op2 = AddOutPt(node.edge2, node.pt);
				if (op1 != op2) {
					AddJoin(op1, op2);
				}
			} else if (TestJoinWithNext2(node.edge1, node.pt)) {
				OutPt op1 = AddOutPt(node.edge1, node.pt);
				OutPt op2 = AddOutPt(node.edge1.nextInAEL, node.pt);
				if (op1 != op2) {
					AddJoin(op1, op2);
				}
			}
		}
	}

	private void SwapPositionsInAEL(Active ae1, Active ae2) {
		// preconditon: ae1 must be immediately to the left of ae2
		Active next = ae2.nextInAEL;
		if (next != null) {
			next.prevInAEL = ae1;
		}
		Active prev = ae1.prevInAEL;
		if (prev != null) {
			prev.nextInAEL = ae2;
		}
		ae2.prevInAEL = prev;
		ae2.nextInAEL = ae1;
		ae1.prevInAEL = ae2;
		ae1.nextInAEL = next;
		if (ae2.prevInAEL == null) {
			actives = ae2;
		}
	}

	private boolean ResetHorzDirection(Active horz, Active maxPair, OutObject<Long> leftX, OutObject<Long> rightX) {
		if (horz.bot.X == horz.top.X) {
			// the horizontal edge is going nowhere ...
			leftX.argValue = horz.curX;
			rightX.argValue = horz.curX;
			Active ae = horz.nextInAEL;
			while (ae != null && !maxPair.equals(ae)) {
				ae = ae.nextInAEL;
			}
			return ae != null;
		}

		if (horz.curX < horz.top.X) {
			leftX.argValue = horz.curX;
			rightX.argValue = horz.top.X;
			return true;
		}
		leftX.argValue = horz.top.X;
		rightX.argValue = horz.curX;
		return false; // right to left
	}

	private boolean HorzIsSpike(Active horz) {
		Point64 nextPt = NextVertex(horz).pt;
		return (horz.bot.X < horz.top.X) != (horz.top.X < nextPt.X);
	}

	private void TrimHorz(Active horzEdge, boolean preserveCollinear) {
		boolean wasTrimmed = false;
		Point64 pt = NextVertex(horzEdge).pt;

		while (pt.Y == horzEdge.top.Y) {
			// always trim 180 deg. spikes (in closed paths)
			// but otherwise break if preserveCollinear = true
			if (preserveCollinear && (pt.X < horzEdge.top.X) != (horzEdge.bot.X < horzEdge.top.X)) {
				break;
			}

			horzEdge.vertexTop = NextVertex(horzEdge);
			horzEdge.top = pt;
			wasTrimmed = true;
			if (IsMaxima(horzEdge)) {
				break;
			}
			pt = NextVertex(horzEdge).pt;
		}
		if (wasTrimmed) {
			SetDx(horzEdge); // +/-infinity
		}
	}

	private void DoHorizontal(Active horz)
	/*-
	 * Notes: Horizontal edges (HEs) at scanline intersections (i.e. at the top or  *
	 * bottom of a scanbeam) are processed as if layered.The order in which HEs     *
	 * are processed doesn't matter. HEs intersect with the bottom vertices of      *
	 * other HEs[#] and with non-horizontal edges [*]. Once these intersections     *
	 * are completed, intermediate HEs are 'promoted' to the next edge in their     *
	 * bounds, and they in turn may be intersected[%] by other HEs.                 *
	 *                                                                              *
	 * eg: 3 horizontals at a scanline:    /   |                     /           /  *
	 *              |                     /    |     (HE3)o ========%========== o   *
	 *              o ======= o(HE2)     /     |         /         /                *
	 *          o ============#=========*======*========#=========o (HE1)           *
	 *         /              |        /       |       /                            *
	 *******************************************************************************/
	{
		Point64 pt;
		boolean horzIsOpen = IsOpen(horz);
		long Y = horz.bot.Y;

		Vertex vertexmax = null;
		Active maxPair = null;

		if (!horzIsOpen) {
			vertexmax = GetCurrYMaximaVertex(horz);
			if (vertexmax != null) {
				maxPair = GetHorzMaximaPair(horz, vertexmax);
				// remove 180 deg.spikes and also simplify
				// consecutive horizontals when preserveCollinear = true
				if (!vertexmax.equals(horz.vertexTop)) {
					TrimHorz(horz, getPreserveCollinear());
				}
			}
		}

		long leftX;
		OutObject<Long> tempOutleftX = new OutObject<>();
		long rightX;
		OutObject<Long> tempOutrightX = new OutObject<>();
		boolean isLeftToRight = ResetHorzDirection(horz, maxPair, tempOutleftX, tempOutrightX);
		rightX = tempOutrightX.argValue;
		leftX = tempOutleftX.argValue;

		if (IsHotEdge(horz)) {
			AddOutPt(horz, new Point64(horz.curX, Y));
		}

		OutPt op = null;
		for (;;) {
			if (horzIsOpen && IsMaxima(horz) && !IsOpenEnd(horz)) {
				vertexmax = GetCurrYMaximaVertex(horz);
				if (vertexmax != null) {
					maxPair = GetHorzMaximaPair(horz, vertexmax);
				}
			}

			// loops through consec. horizontal edges (if open)
			Active ae = null;
			if (isLeftToRight) {
				ae = horz.nextInAEL;
			} else {
				ae = horz.prevInAEL;
			}

			while (ae != null) {
				if (ae == maxPair) {
					if (IsHotEdge(horz)) {
						while (horz.vertexTop != ae.vertexTop) {
							AddOutPt(horz, horz.top);
							UpdateEdgeIntoAEL(horz);
						}
						op = AddLocalMaxPoly(horz, ae, horz.top);
						if (op != null && !IsOpen(horz) && op.pt.opEquals(horz.top)) {
							AddTrialHorzJoin(op);
						}
					}

					DeleteFromAEL(ae);
					DeleteFromAEL(horz);
					return;
				}

				// if horzEdge is a maxima, keep going until we reach
				// its maxima pair, otherwise check for break conditions
				if (vertexmax != horz.vertexTop || IsOpenEnd(horz)) {
					// otherwise stop when 'ae' is beyond the end of the horizontal line
					if ((isLeftToRight && ae.curX > rightX) || (!isLeftToRight && ae.curX < leftX)) {
						break;
					}

					if (ae.curX == horz.top.X && !IsHorizontal(ae)) {
						pt = NextVertex(horz).pt;
						if (isLeftToRight) {
							// with open paths we'll only break once past horz's end
							if (IsOpen(ae) && !IsSamePolyType(ae, horz) && !IsHotEdge(ae)) {
								if (TopX(ae, pt.Y) > pt.X) {
									break;
								}
							}
							// otherwise we'll only break when horz's outslope is greater than e's
							else if (TopX(ae, pt.Y) >= pt.X) {
								break;
							}
						} else // with open paths we'll only break once past horz's end
						if (IsOpen(ae) && !IsSamePolyType(ae, horz) && !IsHotEdge(ae)) {
							if (TopX(ae, pt.Y) < pt.X) {
								break;
							}
						}
						// otherwise we'll only break when horz's outslope is greater than e's
						else if (TopX(ae, pt.Y) <= pt.X) {
							break;
						}
					}
				}

				pt = new Point64(ae.curX, Y);

				if (isLeftToRight) {
					op = IntersectEdges(horz, ae, pt);
					SwapPositionsInAEL(horz, ae);

					if (IsHotEdge(horz) && op != null && !IsOpen(horz) && op.pt.opEquals(pt)) {
						AddTrialHorzJoin(op);
					}

					if (!IsHorizontal(ae) && TestJoinWithPrev1(ae)) {
						op = AddOutPt(ae.prevInAEL, pt);
						OutPt op2 = AddOutPt(ae, pt);
						AddJoin(op, op2);
					}

					horz.curX = ae.curX;
					ae = horz.nextInAEL;
				} else {
					op = IntersectEdges(ae, horz, pt);
					SwapPositionsInAEL(ae, horz);

					if (IsHotEdge(horz) && op != null && !IsOpen(horz) && op.pt.opEquals(pt)) {
						AddTrialHorzJoin(op);
					}

					if (!IsHorizontal(ae) && TestJoinWithNext1(ae)) {
						op = AddOutPt(ae, pt);
						OutPt op2 = AddOutPt(ae.nextInAEL, pt);
						AddJoin(op, op2);
					}

					horz.curX = ae.curX;
					ae = horz.prevInAEL;
				}
			} // we've reached the end of this horizontal

			// check if we've finished looping through consecutive horizontals
			if (horzIsOpen && IsOpenEnd(horz)) {
				if (IsHotEdge(horz)) {
					AddOutPt(horz, horz.top);
					if (IsFront(horz)) {
						horz.outrec.frontEdge = null;
					} else {
						horz.outrec.backEdge = null;
					}
				}
				horz.outrec = null;
				DeleteFromAEL(horz); // ie open at top
				return;
			}

			if (NextVertex(horz).pt.Y != horz.top.Y) {
				break;
			}

			// there must be a following (consecutive) horizontal
			if (IsHotEdge(horz)) {
				AddOutPt(horz, horz.top);
			}
			UpdateEdgeIntoAEL(horz);

			if (getPreserveCollinear() && HorzIsSpike(horz)) {
				TrimHorz(horz, true);
			}

			OutObject<Long> tempOutleftX2 = new OutObject<>();
			OutObject<Long> tempOutrightX2 = new OutObject<>();
			isLeftToRight = ResetHorzDirection(horz, maxPair, tempOutleftX2, tempOutrightX2);
			rightX = tempOutrightX2.argValue;
			leftX = tempOutleftX2.argValue;

		} // end for loop and end of (possible consecutive) horizontals

		if (IsHotEdge(horz)) {
			op = AddOutPt(horz, horz.top);
			if (!IsOpen(horz)) {
				AddTrialHorzJoin(op);
			}
		} else {
			op = null;
		}

		if ((horzIsOpen && !IsOpenEnd(horz)) || (!horzIsOpen && vertexmax != horz.vertexTop)) {
			UpdateEdgeIntoAEL(horz); // this is the end of an intermediate horiz.
			if (IsOpen(horz)) {
				return;
			}

			if (isLeftToRight && TestJoinWithNext1(horz)) {
				OutPt op2 = AddOutPt(horz.nextInAEL, horz.bot);
				AddJoin(op, op2);
			} else if (!isLeftToRight && TestJoinWithPrev1(horz)) {
				OutPt op2 = AddOutPt(horz.prevInAEL, horz.bot);
				AddJoin(op2, op);
			}
		} else if (IsHotEdge(horz)) {
			AddLocalMaxPoly(horz, maxPair, horz.top);
		} else {
			DeleteFromAEL(maxPair);
			DeleteFromAEL(horz);
		}
	}

	private void DoTopOfScanbeam(long y) {
		sel = null; // sel is reused to flag horizontals (see PushHorz below)
		Active ae = actives;
		while (ae != null) {
			// NB 'ae' will never be horizontal here
			if (ae.top.Y == y) {
				ae.curX = ae.top.X;
				if (IsMaxima(ae)) {
					ae = DoMaxima(ae); // TOP OF BOUND (MAXIMA)
					continue;
				}

				// INTERMEDIATE VERTEX ...
				if (IsHotEdge(ae)) {
					AddOutPt(ae, ae.top);
				}
				UpdateEdgeIntoAEL(ae);
				if (IsHorizontal(ae)) {
					PushHorz(ae); // horizontals are processed later
				}
			} else { // i.e. not the top of the edge
				ae.curX = TopX(ae, y);
			}

			ae = ae.nextInAEL;
		}
	}

	private Active DoMaxima(Active ae) {
		Active prevE = null;
		Active nextE, maxPair = null;
		prevE = ae.prevInAEL;
		nextE = ae.nextInAEL;

		if (IsOpenEnd(ae)) {
			if (IsHotEdge(ae)) {
				AddOutPt(ae, ae.top);
			}
			if (!IsHorizontal(ae)) {
				if (IsHotEdge(ae)) {
					if (IsFront(ae)) {
						ae.outrec.frontEdge = null;
					} else {
						ae.outrec.backEdge = null;
					}
					ae.outrec = null;
				}
				DeleteFromAEL(ae);
			}
			return nextE;
		}

		maxPair = GetMaximaPair(ae);
		if (maxPair == null) {
			return nextE; // eMaxPair is horizontal
		}

		// only non-horizontal maxima here.
		// process any edges between maxima pair ...
		while (!nextE.equals(maxPair)) {
			IntersectEdges(ae, nextE, ae.top);
			SwapPositionsInAEL(ae, nextE);
			nextE = ae.nextInAEL;
		}

		if (IsOpen(ae)) {
			if (IsHotEdge(ae)) {
				AddLocalMaxPoly(ae, maxPair, ae.top);
			}
			DeleteFromAEL(maxPair);
			DeleteFromAEL(ae);
			return (prevE != null ? prevE.nextInAEL : actives);
		}

		// here ae.nextInAel == ENext == EMaxPair ...
		if (IsHotEdge(ae)) {
			AddLocalMaxPoly(ae, maxPair, ae.top);
		}

		DeleteFromAEL(ae);
		DeleteFromAEL(maxPair);
		return (prevE != null ? prevE.nextInAEL : actives);
	}

	private static boolean IsValidPath(OutPt op) {
		return (op.next != op);
	}

	private static boolean AreReallyClose(Point64 pt1, Point64 pt2) {
		return (Math.abs(pt1.X - pt2.X) < 2) && (Math.abs(pt1.Y - pt2.Y) < 2);
	}

	private static boolean IsValidClosedPath(OutPt op) {
		return (op != null && !op.equals(op.next) && op.next != op.prev
				&& !(op.next.next == op.prev && (AreReallyClose(op.pt, op.next.pt) || AreReallyClose(op.pt, op.prev.pt))));
	}

	private static boolean ValueBetween(long val, long end1, long end2) {
		// NB accommodates axis aligned between where end1 == end2
		return ((val != end1) == (val != end2)) && ((val > end1) == (val < end2));
	}

	private static boolean ValueEqualOrBetween(long val, long end1, long end2) {
		return (val == end1) || (val == end2) || ((val > end1) == (val < end2));
	}

	private static boolean PointBetween(Point64 pt, Point64 corner1, Point64 corner2) {
		// NB points may not be collinear
		return ValueEqualOrBetween(pt.X, corner1.X, corner2.X) && ValueEqualOrBetween(pt.Y, corner1.Y, corner2.Y);
	}

	private static boolean CollinearSegsOverlap(Point64 seg1a, Point64 seg1b, Point64 seg2a, Point64 seg2b) {
		// precondition: seg1 and seg2 are collinear
		if (seg1a.X == seg1b.X) {
			if (seg2a.X != seg1a.X || seg2a.X != seg2b.X) {
				return false;
			}
		} else if (seg1a.X < seg1b.X) {
			if (seg2a.X < seg2b.X) {
				if (seg2a.X >= seg1b.X || seg2b.X <= seg1a.X) {
					return false;
				}
			} else if (seg2b.X >= seg1b.X || seg2a.X <= seg1a.X) {
				return false;
			}
		} else if (seg2a.X < seg2b.X) {
			if (seg2a.X >= seg1a.X || seg2b.X <= seg1b.X) {
				return false;
			}
		} else if (seg2b.X >= seg1a.X || seg2a.X <= seg1b.X) {
			return false;
		}

		if (seg1a.Y == seg1b.Y) {
			if (seg2a.Y != seg1a.Y || seg2a.Y != seg2b.Y) {
				return false;
			}
		} else if (seg1a.Y < seg1b.Y) {
			if (seg2a.Y < seg2b.Y) {
				if (seg2a.Y >= seg1b.Y || seg2b.Y <= seg1a.Y) {
					return false;
				}
			} else if (seg2b.Y >= seg1b.Y || seg2a.Y <= seg1a.Y) {
				return false;
			}
		} else if (seg2a.Y < seg2b.Y) {
			if (seg2a.Y >= seg1a.Y || seg2b.Y <= seg1b.Y) {
				return false;
			}
		} else if (seg2b.Y >= seg1a.Y || seg2a.Y <= seg1b.Y) {
			return false;
		}
		return true;
	}

	private static boolean HorzEdgesOverlap(long x1a, long x1b, long x2a, long x2b) {
		final long minOverlap = 2;
		if (x1a > x1b + minOverlap) {
			if (x2a > x2b + minOverlap) {
				return !((x1a <= x2b) || (x2a <= x1b));
			}
			return !((x1a <= x2a) || (x2b <= x1b));
		}

		if (x1b > x1a + minOverlap) {
			if (x2a > x2b + minOverlap) {
				return !((x1b <= x2b) || (x2a <= x1a));
			}
			return !((x1b <= x2a) || (x2b <= x1a));
		}
		return false;
	}

	private Joiner GetHorzTrialParent(OutPt op) {
		Joiner joiner = op.joiner;
		while (joiner != null) {
			if (joiner.op1 == op) {
				if (joiner.next1 != null && joiner.next1.idx < 0) {
					return joiner;
				}
				joiner = joiner.next1;
			} else {
				if (joiner.next2 != null && joiner.next2.idx < 0) {
					return joiner;
				}
				joiner = joiner.next1;
			}
		}
		return joiner;
	}

	private boolean OutPtInTrialHorzList(OutPt op) {
		return op.joiner != null && ((op.joiner.idx < 0) || GetHorzTrialParent(op) != null);
	}

	private boolean ValidateClosedPathEx(RefObject<OutPt> op) {
		if (IsValidClosedPath(op.argValue)) {
			return true;
		}
		if (op.argValue != null) {
			SafeDisposeOutPts(op);
		}
		return false;
	}

	private static OutPt InsertOp(Point64 pt, OutPt insertAfter) {
		OutPt result = new OutPt(pt, insertAfter.outrec);
		result.next = insertAfter.next;
		insertAfter.next.prev = result;
		insertAfter.next = result;
		result.prev = insertAfter;
		return result;
	}

	private static OutPt DisposeOutPt(OutPt op) {
		OutPt result = (op.next == op ? null : op.next);
		op.prev.next = op.next;
		op.next.prev = op.prev;
		// op == null;
		return result;
	}

	private void SafeDisposeOutPts(RefObject<OutPt> op) {
		OutRec outRec = GetRealOutRec(op.argValue.outrec);
		if (outRec.frontEdge != null) {
			outRec.frontEdge.outrec = null;
		}
		if (outRec.backEdge != null) {
			outRec.backEdge.outrec = null;
		}

		op.argValue.prev.next = null;
		OutPt op2 = op.argValue;
		while (op2 != null) {
			SafeDeleteOutPtJoiners(op2);
			op2 = op2.next;
		}
		outRec.pts = null;
	}

	private void SafeDeleteOutPtJoiners(OutPt op) {
		Joiner joiner = op.joiner;
		if (joiner == null) {
			return;
		}

		while (joiner != null) {
			if (joiner.idx < 0) {
				DeleteTrialHorzJoin(op);
			} else if (horzJoiners != null) {
				if (OutPtInTrialHorzList(joiner.op1)) {
					DeleteTrialHorzJoin(joiner.op1);
				}
				if (OutPtInTrialHorzList(joiner.op2)) {
					DeleteTrialHorzJoin(joiner.op2);
				}
				DeleteJoin(joiner);
			} else {
				DeleteJoin(joiner);
			}
			joiner = op.joiner;
		}
	}

	private void AddTrialHorzJoin(OutPt op) {
		// make sure 'op' isn't added more than once
		if (!op.outrec.isOpen && !OutPtInTrialHorzList(op)) {
			horzJoiners = new Joiner(op, null, horzJoiners);
		}

	}

	private static Joiner FindTrialJoinParent(RefObject<Joiner> joiner, OutPt op) {
		Joiner parent = joiner.argValue;
		while (parent != null) {
			if (op == parent.op1) {
				if (parent.next1 != null && parent.next1.idx < 0) {
					joiner.argValue = parent.next1;
					return parent;
				}
				parent = parent.next1;
			} else {
				if (parent.next2 != null && parent.next2.idx < 0) {
					joiner.argValue = parent.next2;
					return parent;
				}
				parent = parent.next2;
			}
		}
		return null;
	}

	private void DeleteTrialHorzJoin(OutPt op) {
		if (horzJoiners == null) {
			return;
		}

		Joiner joiner = op.joiner;
		Joiner parentH, parentOp = null;
		while (joiner != null) {
			if (joiner.idx < 0) {
				// first remove joiner from FHorzTrials
				if (horzJoiners.equals(joiner)) {
					horzJoiners = joiner.nextH;
				} else {
					parentH = horzJoiners;
					while (!joiner.equals(parentH.nextH)) {
						parentH = parentH.nextH;
					}
					parentH.nextH = joiner.nextH;
				}

				// now remove joiner from op's joiner list
				if (parentOp == null) {
					// joiner must be first one in list
					op.joiner = joiner.next1;
					// joiner == null;
					joiner = op.joiner;
				} else {
					// the trial joiner isn't first
					if (op == parentOp.op1) {
						parentOp.next1 = joiner.next1;
					} else {
						parentOp.next2 = joiner.next1;
					}
					// joiner = null;
					joiner = parentOp;
				}
			} else {
				// not a trial join so look further along the linked list
				RefObject<Joiner> tempRefjoiner = new RefObject<>(joiner);
				parentOp = FindTrialJoinParent(tempRefjoiner, op);
				joiner = tempRefjoiner.argValue;
				if (parentOp == null) {
					break;
				}
			}
			// loop in case there's more than one trial join
		}
	}

	private boolean GetHorzExtendedHorzSeg(RefObject<OutPt> op, OutObject<OutPt> op2) {
		OutRec outRec = GetRealOutRec(op.argValue.outrec);
		op2.argValue = op.argValue;
		if (outRec.frontEdge != null) {
			while (op.argValue.prev != outRec.pts && op.argValue.prev.pt.Y == op.argValue.pt.Y) {
				op.argValue = op.argValue.prev;
			}
			while (op2.argValue != outRec.pts && op2.argValue.next.pt.Y == op2.argValue.pt.Y) {
				op2.argValue = op2.argValue.next;
			}
			return op2.argValue != op.argValue;
		}

		while (op.argValue.prev != op2.argValue && op.argValue.prev.pt.Y == op.argValue.pt.Y) {
			op.argValue = op.argValue.prev;
		}
		while (op2.argValue.next != op.argValue && op2.argValue.next.pt.Y == op2.argValue.pt.Y) {
			op2.argValue = op2.argValue.next;
		}
		return op2.argValue != op.argValue && op2.argValue.next != op.argValue;
	}

	private void ConvertHorzTrialsToJoins() {
		while (horzJoiners != null) {
			Joiner joiner = horzJoiners;
			horzJoiners = horzJoiners.nextH;
			OutPt op1a = joiner.op1;
			if (joiner.equals(op1a.joiner)) {
				op1a.joiner = joiner.next1;
			} else {
				Joiner joinerParent = FindJoinParent(joiner, op1a);
				if (joinerParent.op1 == op1a) {
					joinerParent.next1 = joiner.next1;
				} else {
					joinerParent.next2 = joiner.next1;
				}
			}

			RefObject<OutPt> tempRefop1a = new RefObject<>(op1a);
			OutPt op1b;
			OutObject<OutPt> tempOutop1b = new OutObject<>();
			if (!GetHorzExtendedHorzSeg(tempRefop1a, tempOutop1b)) {
				op1b = tempOutop1b.argValue;
				op1a = tempRefop1a.argValue;
				if (op1a.outrec.frontEdge == null) {
					CleanCollinear(op1a.outrec);
				}
				continue;
			} else {
				op1b = tempOutop1b.argValue;
				op1a = tempRefop1a.argValue;
			}

			OutPt op2a;
			boolean joined = false;
			joiner = horzJoiners;
			while (joiner != null) {
				op2a = joiner.op1;
				RefObject<OutPt> tempRefop2a = new RefObject<>(op2a);
				OutPt op2b;
				OutObject<OutPt> tempOutop2b = new OutObject<>(); // NOTE SYNTAX
				if (GetHorzExtendedHorzSeg(tempRefop2a, tempOutop2b)
						&& HorzEdgesOverlap(op1a.pt.X, op1b.pt.X, op2a.pt.X, tempOutop2b.argValue.pt.X)) {
					op2b = tempOutop2b.argValue;
					op2a = tempRefop2a.argValue;
					// overlap found so promote to a 'real' join
					joined = true;
					if (op1a.pt.opEquals(op2b.pt)) {
						AddJoin(op1a, op2b);
					} else if (op1b.pt.opEquals(op2a.pt)) {
						AddJoin(op1b, op2a);
					} else if (op1a.pt.opEquals(op2a.pt)) {
						AddJoin(op1a, op2a);
					} else if (op1b.pt.opEquals(op2b.pt)) {
						AddJoin(op1b, op2b);
					} else if (ValueBetween(op1a.pt.X, op2a.pt.X, op2b.pt.X)) {
						AddJoin(op1a, InsertOp(op1a.pt, op2a));
					} else if (ValueBetween(op1b.pt.X, op2a.pt.X, op2b.pt.X)) {
						AddJoin(op1b, InsertOp(op1b.pt, op2a));
					} else if (ValueBetween(op2a.pt.X, op1a.pt.X, op1b.pt.X)) {
						AddJoin(op2a, InsertOp(op2a.pt, op1a));
					} else if (ValueBetween(op2b.pt.X, op1a.pt.X, op1b.pt.X)) {
						AddJoin(op2b, InsertOp(op2b.pt, op1a));
					}
					break;
				} else {
					op2b = tempOutop2b.argValue;
					op2a = tempRefop2a.argValue;
				}
				joiner = joiner.nextH;
			}
			if (!joined) {
				CleanCollinear(op1a.outrec);
			}
		}
	}

	private void AddJoin(OutPt op1, OutPt op2) {
		if ((op1.outrec == op2.outrec)
				&& ((op1 == op2) || ((op1.next == op2) && (op1 != op1.outrec.pts)) || ((op2.next == op1) && (op2 != op1.outrec.pts)))) {
			return;
		}

		Joiner joiner = new Joiner(op1, op2, null);
		joiner.idx = joinerList.size();
		joinerList.add(joiner);
	}

	private static Joiner FindJoinParent(Joiner joiner, OutPt op) {
		Joiner result = op.joiner;
		for (;;) {
			if (op == result.op1) {
				if (result.next1 == joiner) {
					return result;
				}
				result = result.next1;
			} else {
				if (result.next2 == joiner) {
					return result;
				}
				result = result.next2;
			}
		}
	}

	private void DeleteJoin(Joiner joiner) {
		// This method deletes a single join, and it doesn't check for or
		// delete trial horz. joins. For that, use the following method.
		OutPt op1 = joiner.op1, op2 = joiner.op2;

		Joiner parentJnr;
		if (op1.joiner != joiner) {
			parentJnr = FindJoinParent(joiner, op1);
			if (parentJnr.op1 == op1) {
				parentJnr.next1 = joiner.next1;
			} else {
				parentJnr.next2 = joiner.next1;
			}
		} else {
			op1.joiner = joiner.next1;
		}

		if (op2.joiner != joiner) {
			parentJnr = FindJoinParent(joiner, op2);
			if (parentJnr.op1 == op2) {
				parentJnr.next1 = joiner.next2;
			} else {
				parentJnr.next2 = joiner.next2;
			}
		} else {
			op2.joiner = joiner.next2;
		}

		joinerList.set(joiner.idx, null);
	}

	private void ProcessJoinList() {
		// NB can't use foreach here because list may
		// contain nulls which can't be enumerated
		for (Joiner j : joinerList) {
			if (j == null) {
				continue;
			}
			OutRec outrec = ProcessJoin(j);
			CleanCollinear(outrec);
		}
		joinerList.clear();
	}

	private static boolean CheckDisposeAdjacent(RefObject<OutPt> op, OutPt guard, OutRec outRec) {
		boolean result = false;
		while (op.argValue.prev != op.argValue) {
			if (op.argValue.pt.opEquals(op.argValue.prev.pt) && op.argValue != guard && op.argValue.prev.joiner != null
					&& op.argValue.joiner == null) {
				if (op.argValue == outRec.pts) {
					outRec.pts = op.argValue.prev;
				}
				op.argValue = DisposeOutPt(op.argValue);
				op.argValue = op.argValue.prev;
			} else {
				break;
			}
		}

		while (op.argValue.next != op.argValue) {
			if (op.argValue.pt.opEquals(op.argValue.next.pt) && op.argValue != guard && op.argValue.next.joiner != null
					&& op.argValue.joiner == null) {
				if (op.argValue == outRec.pts) {
					outRec.pts = op.argValue.prev;
				}
				op.argValue = DisposeOutPt(op.argValue);
				op.argValue = op.argValue.prev;
			} else {
				break;
			}
		}
		return result;
	}

	private static double DistanceFromLineSqrd(Point64 pt, Point64 linePt1, Point64 linePt2) {
		// perpendicular distance of point (x0,y0) = (a*x0 + b*y0 + C)/Sqrt(a*a + b*b)
		// where ax + by +c = 0 is the equation of the line
		// see https://en.wikipedia.org/wiki/Distancefromapointtoaline
		double a = (linePt1.Y - linePt2.Y);
		double b = (linePt2.X - linePt1.X);
		double c = a * linePt1.X + b * linePt1.Y;
		double q = a * pt.X + b * pt.Y - c;
		return (q * q) / (a * a + b * b);
	}

	private static double DistanceSqr(Point64 pt1, Point64 pt2) {
		return (double) (pt1.X - pt2.X) * (pt1.X - pt2.X) + (double) (pt1.Y - pt2.Y) * (pt1.Y - pt2.Y);
	}

	private OutRec ProcessJoin(Joiner j) {
		OutPt op1 = j.op1, op2 = j.op2;
		OutRec or1 = GetRealOutRec(op1.outrec);
		OutRec or2 = GetRealOutRec(op2.outrec);
		DeleteJoin(j);

		if (or2.pts == null) {
			return or1;
		}
		if (!IsValidClosedPath(op2)) {
			RefObject<OutPt> tempRefop2 = new RefObject<>(op2);
			SafeDisposeOutPts(tempRefop2);
			op2 = tempRefop2.argValue;
			return or1;
		}
		if ((or1.pts == null) || !IsValidClosedPath(op1)) {
			RefObject<OutPt> tempRefop1 = new RefObject<>(op1);
			SafeDisposeOutPts(tempRefop1);
			op1 = tempRefop1.argValue;
			return or2;
		}
		if (or1 == or2 && ((op1 == op2) || (op1.next == op2) || (op1.prev == op2))) {
			return or1;
		}

		RefObject<OutPt> tempRefop12 = new RefObject<>(op1);
		CheckDisposeAdjacent(tempRefop12, op2, or1);
		op1 = tempRefop12.argValue;
		RefObject<OutPt> tempRefop22 = new RefObject<>(op2);
		CheckDisposeAdjacent(tempRefop22, op1, or2);
		op2 = tempRefop22.argValue;
		if (op1.next == op2 || op2.next == op1) {
			return or1;
		}

		OutRec result = or1;
		for (;;) {
			if (!IsValidPath(op1) || !IsValidPath(op2) || (or1 == or2 && (op1.prev == op2 || op1.next == op2))) {
				return or1;
			}

			if (op1.prev.pt.opEquals(op2.next.pt) || ((InternalClipper.CrossProduct(op1.prev.pt, op1.pt, op2.next.pt) == 0)
					&& CollinearSegsOverlap(op1.prev.pt, op1.pt, op2.pt, op2.next.pt))) {
				if (or1 == or2) {
					// SPLIT REQUIRED
					// make sure op1.prev and op2.next match positions
					// by inserting an extra vertex if needed
					if (op1.prev.pt.opNotEquals(op2.next.pt)) {
						if (PointBetween(op1.prev.pt, op2.pt, op2.next.pt)) {
							op2.next = InsertOp(op1.prev.pt, op2);
						} else {
							op1.prev = InsertOp(op2.next.pt, op1.prev);
						}
					}

					// current to new
					// op1.p[opA] >>> op1 ... opA \ / op1
					// op2.n[opB] <<< op2 ... opB / \ op2
					OutPt opA = op1.prev, opB = op2.next;
					opA.next = opB;
					opB.prev = opA;
					op1.prev = op2;
					op2.next = op1;
					CompleteSplit(op1, opA, or1);
				} else {
					// JOIN, NOT SPLIT
					OutPt opA = op1.prev, opB = op2.next;
					opA.next = opB;
					opB.prev = opA;
					op1.prev = op2;
					op2.next = op1;

					// SafeDeleteOutPtJoiners(op2);
					// DisposeOutPt(op2);

					if (or1.idx < or2.idx) {
						or1.pts = op1;
						or2.pts = null;
						if (or1.owner != null && (or2.owner == null || or2.owner.idx < or1.owner.idx)) {
							or1.owner = or2.owner;
						}
						or2.owner = or1;
					} else {
						result = or2;
						or2.pts = op1;
						or1.pts = null;
						if (or2.owner != null && (or1.owner == null || or1.owner.idx < or2.owner.idx)) {
							or2.owner = or1.owner;
						}
						or1.owner = or2;
					}
				}
				break;
			}
			if (op1.next.pt.opEquals(op2.prev.pt) || ((InternalClipper.CrossProduct(op1.next.pt, op2.pt, op2.prev.pt) == 0)
					&& CollinearSegsOverlap(op1.next.pt, op1.pt, op2.pt, op2.prev.pt))) {
				if (or1 == or2) {
					// SPLIT REQUIRED
					// make sure op2.prev and op1.next match positions
					// by inserting an extra vertex if needed
					if (op2.prev.pt.opNotEquals(op1.next.pt)) {
						if (PointBetween(op2.prev.pt, op1.pt, op1.next.pt)) {
							op1.next = InsertOp(op2.prev.pt, op1);
						} else {
							op2.prev = InsertOp(op1.next.pt, op2.prev);
						}
					}

					// current to new
					// op2.p[opA] >>> op2 ... opA \ / op2
					// op1.n[opB] <<< op1 ... opB / \ op1
					OutPt opA = op2.prev, opB = op1.next;
					opA.next = opB;
					opB.prev = opA;
					op2.prev = op1;
					op1.next = op2;
					CompleteSplit(op1, opA, or1);
				} else {
					// JOIN, NOT SPLIT
					OutPt opA = op1.next, opB = op2.prev;
					opA.prev = opB;
					opB.next = opA;
					op1.next = op2;
					op2.prev = op1;

					// SafeDeleteOutPtJoiners(op2);
					// DisposeOutPt(op2);

					if (or1.idx < or2.idx) {
						or1.pts = op1;
						or2.pts = null;
						if (or1.owner != null && (or2.owner == null || or2.owner.idx < or1.owner.idx)) {
							or1.owner = or2.owner;
						}
						or2.owner = or1;
					} else {
						result = or2;
						or2.pts = op1;
						or1.pts = null;
						if (or2.owner != null && (or1.owner == null || or1.owner.idx < or2.owner.idx)) {
							or2.owner = or1.owner;
						}
						or1.owner = or2;
					}
				}
				break;
			}

			if (PointBetween(op1.next.pt, op2.pt, op2.prev.pt) && DistanceFromLineSqrd(op1.next.pt, op2.pt, op2.prev.pt) < 2.01) {
				InsertOp(op1.next.pt, op2.prev);
				continue;
			}
			if (PointBetween(op2.next.pt, op1.pt, op1.prev.pt) && DistanceFromLineSqrd(op2.next.pt, op1.pt, op1.prev.pt) < 2.01) {
				InsertOp(op2.next.pt, op1.prev);
				continue;
			}
			if (PointBetween(op1.prev.pt, op2.pt, op2.next.pt) && DistanceFromLineSqrd(op1.prev.pt, op2.pt, op2.next.pt) < 2.01) {
				InsertOp(op1.prev.pt, op2);
				continue;
			}
			if (PointBetween(op2.prev.pt, op1.pt, op1.next.pt) && DistanceFromLineSqrd(op2.prev.pt, op1.pt, op1.next.pt) < 2.01) {
				InsertOp(op2.prev.pt, op1);
				continue;
			}

			// something odd needs tidying up
			RefObject<OutPt> tempRefop13 = new RefObject<>(op1);
			if (CheckDisposeAdjacent(tempRefop13, op2, or1)) {
				op1 = tempRefop13.argValue;
				continue;
			} else {
				op1 = tempRefop13.argValue;
			}
			RefObject<OutPt> tempRefop23 = new RefObject<>(op2);
			if (CheckDisposeAdjacent(tempRefop23, op1, or1)) {
				op2 = tempRefop23.argValue;
				continue;
			} else {
				op2 = tempRefop23.argValue;
			}
			if (op1.prev.pt.opNotEquals(op2.next.pt) && (DistanceSqr(op1.prev.pt, op2.next.pt) < 2.01)) {
				op1.prev.pt = op2.next.pt;
				continue;
			}
			if (op1.next.pt.opNotEquals(op2.prev.pt) && (DistanceSqr(op1.next.pt, op2.prev.pt) < 2.01)) {
				op2.prev.pt = op1.next.pt;
				continue;
			}
			// OK, there doesn't seem to be a way to join after all
			// so just tidy up the polygons
			or1.pts = op1;
			if (or2 != or1) {
				or2.pts = op2;
				CleanCollinear(or2);
			}
			break;
		}
		return result;
	}

	private static void UpdateOutrecOwner(OutRec outrec) {
		OutPt opCurr = outrec.pts;
		for (;;) {
			opCurr.outrec = outrec;
			opCurr = opCurr.next;
			if (opCurr == outrec.pts) {
				return;
			}
		}
	}

	private void CompleteSplit(OutPt op1, OutPt op2, OutRec outrec) {
		double area1 = Area(op1);
		double area2 = Area(op2);
		boolean signschange = (area1 > 0) == (area2 < 0);

		// delete trivial splits (with zero or almost zero areas)
		if (area1 == 0 || (signschange && Math.abs(area1) < 2)) {
			RefObject<OutPt> tempRefObject = new RefObject<>(op1);
			SafeDisposeOutPts(tempRefObject);
			outrec.pts = op2;
		} else if (area2 == 0 || (signschange && Math.abs(area2) < 2)) {
			RefObject<OutPt> tempRefObject2 = new RefObject<>(op2);
			SafeDisposeOutPts(tempRefObject2);
			outrec.pts = op1;
		} else {
			OutRec newOr = new OutRec();
			newOr.idx = outrecList.size();
			outrecList.add(newOr);
			newOr.polypath = null;

			if (usingPolytree) {
				if (outrec.splits == null) {
					outrec.splits = new ArrayList<>();
				}
				outrec.splits.add(newOr);
			}

			if (Math.abs(area1) >= Math.abs(area2)) {
				outrec.pts = op1;
				newOr.pts = op2;
			} else {
				outrec.pts = op2;
				newOr.pts = op1;
			}

			if ((area1 > 0) == (area2 > 0)) {
				newOr.owner = outrec.owner;
			} else {
				newOr.owner = outrec;
			}

			UpdateOutrecOwner(newOr);
			CleanCollinear(newOr);
		}
	}

	private void CleanCollinear(OutRec outrec) {
		outrec = GetRealOutRec(outrec);
		// NOTE potentially buggy
		RefObject<OutPt> tempRefpts = new RefObject<>(outrec.pts);
		if (outrec == null || outrec.isOpen || outrec.frontEdge != null || !ValidateClosedPathEx(tempRefpts)) {
//			outrec.pts = tempRefpts.argValue;
			return;
		} else {
			outrec.pts = tempRefpts.argValue;
		}

		OutPt startOp = outrec.pts;
		OutPt op2 = startOp;
		for (;;) {
			if (op2.joiner != null) {
				return;
			}
			// NB if preserveCollinear == true, then only remove 180 deg. spikes
			if ((InternalClipper.CrossProduct(op2.prev.pt, op2.pt, op2.next.pt) == 0)
					&& ((op2.pt.opEquals(op2.prev.pt)) || (op2.pt.opEquals(op2.next.pt)) || !getPreserveCollinear()
							|| (InternalClipper.DotProduct(op2.prev.pt, op2.pt, op2.next.pt) < 0))) {
				if (op2.equals(outrec.pts)) {
					outrec.pts = op2.prev;
				}
				op2 = DisposeOutPt(op2);
				RefObject<OutPt> tempRefop2 = new RefObject<>(op2);
				if (!ValidateClosedPathEx(tempRefop2)) {
					op2 = tempRefop2.argValue;
					outrec.pts = null;
					return;
				} else {
					op2 = tempRefop2.argValue;
				}
				startOp = op2;
				continue;
			}
			op2 = op2.next;
			if (op2.equals(startOp)) {
				break;
			}
		}
		RefObject<OutPt> tempRefObject = new RefObject<>(outrec.pts);
		FixSelfIntersects(tempRefObject); // NOTE BUGGY
		if (outrec.pts != tempRefObject.argValue) {
			outrec.pts = tempRefObject.argValue;
		}
	}

	private OutPt DoSplitOp(RefObject<OutPt> outRecOp, OutPt splitOp) {
		OutPt prevOp = splitOp.prev, nextNextOp = splitOp.next.next;
		OutPt result = prevOp;
		PointD ipD = new PointD();
		InternalClipper.GetIntersectPoint(prevOp.pt, splitOp.pt, splitOp.next.pt, nextNextOp.pt, ipD);
		Point64 ip = new Point64(ipD);

		double area1 = Area(outRecOp.argValue);
		double area2 = AreaTriangle(ip, splitOp.pt, splitOp.next.pt);

		if (ip.opEquals(prevOp.pt) || ip.opEquals(nextNextOp.pt)) {
			nextNextOp.prev = prevOp;
			prevOp.next = nextNextOp;
		} else {
			OutPt newOp2 = new OutPt(ip, prevOp.outrec);
			newOp2.prev = prevOp;
			newOp2.next = nextNextOp;
			nextNextOp.prev = newOp2;
			prevOp.next = newOp2;
		}

		SafeDeleteOutPtJoiners(splitOp.next);
		SafeDeleteOutPtJoiners(splitOp);

		if ((Math.abs(area2) >= 1) && ((Math.abs(area2) > Math.abs(area1)) || ((area2 > 0) == (area1 > 0)))) {
			OutRec newOutRec = new OutRec();
			newOutRec.idx = outrecList.size();
			outrecList.add(newOutRec);
			newOutRec.owner = prevOp.outrec.owner;
			newOutRec.polypath = null;
			splitOp.outrec = newOutRec;
			splitOp.next.outrec = newOutRec;

			OutPt newOp = new OutPt(ip, newOutRec);
			newOp.prev = splitOp.next;
			newOp.next = splitOp;
			newOutRec.pts = newOp;
			splitOp.prev = newOp;
			splitOp.next.next = newOp;
		}
		return result;
	}

	private void FixSelfIntersects(RefObject<OutPt> op) { // TODO BUGGY -- op.pts not updated within method!
		if (!IsValidClosedPath(op.argValue)) {
			return;
		}
		OutPt op2 = op.argValue;
		for (;;) {
			// triangles can't self-intersect
			if (op2.prev == op2.next.next) {
				break;
			}
			if (InternalClipper.SegmentsIntersect(op2.prev.pt, op2.pt, op2.next.pt, op2.next.next.pt)) {
				if (op2 == op.argValue || op2.next == op.argValue) {
					op.argValue = op2.prev;
				}
				op2 = DoSplitOp(op, op2);
				op.argValue = op2;
				continue;
			}

			op2 = op2.next;

			if (op2 == op.argValue) {
				break;
			}
		}
	}

	public final boolean BuildPath(OutPt op, boolean reverse, boolean isOpen, Path64 path) {
		if (op.next == op || (!isOpen && op.next == op.prev)) {
			return false;
		}
		path.clear();

		Point64 lastPt;
		OutPt op2;
		if (reverse) {
			lastPt = op.pt;
			op2 = op.prev;
		} else {
			op = op.next;
			lastPt = op.pt;
			op2 = op.next;
		}
		path.add(lastPt);

		while (op2 != op) {
			if (op2.pt.opNotEquals(lastPt)) {
				lastPt = op2.pt;
				path.add(lastPt);
			}
			if (reverse) {
				op2 = op2.prev;
			} else {
				op2 = op2.next;
			}
		}
		return true;
	}

	protected final boolean BuildPaths(Paths64 solutionClosed, Paths64 solutionOpen) {
		solutionClosed.clear();
		solutionOpen.clear();

		for (OutRec outrec : outrecList) {
			if (outrec.pts == null) {
				continue;
			}

			Path64 path = new Path64();
			if (outrec.isOpen) {
				if (BuildPath(outrec.pts, getReverseSolution(), true, path)) {
					solutionOpen.add(path);
				}
			} else // closed paths should always return a Positive orientation
			// except when reverseSolution == true
			if (BuildPath(outrec.pts, getReverseSolution(), false, path)) {
				solutionClosed.add(path);
			}
		}
		return true;
	}

	private boolean Path1InsidePath2(OutRec or1, OutRec or2) {
		PointInPolygonResult result;
		OutPt op = or1.pts;
		do {
			result = InternalClipper.PointInPolygon(op.pt, or2.path);
			if (result != PointInPolygonResult.IsOn) {
				break;
			}
			op = op.next;
		} while (op != or1.pts);
		if (result == PointInPolygonResult.IsOn) {
			return Area(op) < Area(or2.pts);
		}
		return result == PointInPolygonResult.IsInside;
	}

	private Rect64 GetBounds(Path64 path) {
		if (path.isEmpty()) {
			return new Rect64();
		}
		Rect64 result = new Rect64(Long.MAX_VALUE, Long.MAX_VALUE, -Long.MAX_VALUE, -Long.MAX_VALUE);
		for (Point64 pt : path) {
			if (pt.X < result.left) {
				result.left = pt.X;
			}
			if (pt.X > result.right) {
				result.right = pt.X;
			}
			if (pt.Y < result.top) {
				result.top = pt.Y;
			}
			if (pt.Y > result.bottom) {
				result.bottom = pt.Y;
			}
		}
		return result;
	}

	private boolean DeepCheckOwner(OutRec outrec, OutRec owner) {
		if (owner.bounds.IsEmpty()) {
			owner.bounds = GetBounds(owner.path);
		}
		boolean isInsideOwnerBounds = owner.bounds.Contains(outrec.bounds);

		// while looking for the correct owner, check the owner's
		// splits **before** checking the owner itself because
		// splits can occur internally, and checking the owner
		// first would miss the inner split's true ownership
		if (owner.splits != null) {
			for (OutRec asplit : owner.splits) {
				OutRec split = GetRealOutRec(asplit);
				if (split == null || split.idx <= owner.idx || split.equals(outrec)) {
					continue;
				}
				if (split.splits != null && DeepCheckOwner(outrec, split)) {
					return true;
				}

				if (split.path.isEmpty()) {
					BuildPath(split.pts, getReverseSolution(), false, split.path);
				}
				if (split.bounds.IsEmpty()) {
					split.bounds = GetBounds(split.path);
				}

				if (split.bounds.Contains(outrec.bounds) && Path1InsidePath2(outrec, split)) {
					outrec.owner = split;
					return true;
				}
			}
		}

		// only continue when not inside recursion
		if (owner != outrec.owner) {
			return false;
		}

		for (;;) {
			if (isInsideOwnerBounds && Path1InsidePath2(outrec, outrec.owner)) {
				return true;
			}

			outrec.owner = outrec.owner.owner;
			if (outrec.owner == null) {
				return false;
			}
			isInsideOwnerBounds = outrec.owner.bounds.Contains(outrec.bounds);
		}
	}

	protected final boolean BuildTree(PolyPathBase polytree, Paths64 solutionOpen) {
		polytree.Clear();
		solutionOpen.clear();

		for (int i = 0; i < outrecList.size(); i++) {
			OutRec outrec = outrecList.get(i);
			if (outrec.pts == null) {
				continue;
			}

			if (outrec.isOpen) {
				Path64 openpath = new Path64();
				if (BuildPath(outrec.pts, getReverseSolution(), true, openpath)) {
					solutionOpen.add(openpath);
				}
				continue;
			}

			if (!BuildPath(outrec.pts, getReverseSolution(), false, outrec.path)) {
				continue;
			}
			if (outrec.bounds.IsEmpty()) {
				outrec.bounds = GetBounds(outrec.path);
			}
			outrec.owner = GetRealOutRec(outrec.owner);
			if (outrec.owner != null) {
				DeepCheckOwner(outrec, outrec.owner);
			}

			// swap order if outer/owner paths are preceeded by their inner paths
			if (outrec.owner != null && outrec.owner.idx > outrec.idx) {
				int j = outrec.owner.idx;
				outrec.owner.idx = i;
				outrec.idx = j;
				outrecList.set(i, outrecList.get(j));
				outrecList.set(j, outrec);
				outrec = outrecList.get(i);
				outrec.owner = GetRealOutRec(outrec.owner);
				BuildPath(outrec.pts, getReverseSolution(), false, outrec.path);
				if (outrec.bounds.IsEmpty()) {
					outrec.bounds = GetBounds(outrec.path);
				}
				if (outrec.owner != null) {
					DeepCheckOwner(outrec, outrec.owner);
				}
			}

			PolyPathBase ownerPP;
			if (outrec.owner != null && outrec.owner.polypath != null) {
				ownerPP = outrec.owner.polypath;
			} else {
				ownerPP = polytree;
			}
			outrec.polypath = ownerPP.AddChild(outrec.path);
		}
		return true;
	}

	public final Rect64 GetBounds() {
		Rect64 bounds = Clipper.MaxInvalidRect64;
		for (Vertex t : vertexList) {
			Vertex v = t;
			do {
				if (v.pt.X < bounds.left) {
					bounds.left = v.pt.X;
				}
				if (v.pt.X > bounds.right) {
					bounds.right = v.pt.X;
				}
				if (v.pt.Y < bounds.top) {
					bounds.top = v.pt.Y;
				}
				if (v.pt.Y > bounds.bottom) {
					bounds.bottom = v.pt.Y;
				}
				v = v.next;
			} while (v != t);
		}
		return bounds.IsEmpty() ? new Rect64(0, 0, 0, 0) : bounds;
	}

}
