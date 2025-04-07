package clipper2.offset;

import static clipper2.core.InternalClipper.DEFAULT_ARC_TOLERANCE;
import static clipper2.core.InternalClipper.MAX_COORD;
import static clipper2.core.InternalClipper.MIN_COORD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import clipper2.Clipper;
import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.InternalClipper;
import clipper2.core.Path64;
import clipper2.core.PathD;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.core.Rect64;
import clipper2.engine.Clipper64;
import clipper2.engine.PolyTree64;
import tangible.OutObject;
import tangible.RefObject;

/**
 * Manages the process of offsetting (inflating/deflating) both open and closed
 * paths using different join types and end types.
 * <p>
 * Geometric <b>offsetting</b> refers to the process of creating <b>parallel
 * curves</b> that are offset a specified distance from their primary curves.
 * <p>
 * Library users will rarely need to access this class directly since it's
 * generally easier to use the {@link Clipper#InflatePaths(Paths64, double, JoinType, EndType)
 * InflatePaths()} function for polygon
 * offsetting.
 * <p>
 * <b>Notes:</b>
 * <ul>
 * <li>When offsetting <i>closed</i> paths (polygons), a positive offset delta
 * specifies how much outer polygon contours will expand and inner "hole"
 * contours will contract. The converse occurs with negative deltas.</li>
 * <li>You cannot offset <i>open</i> paths (polylines) with negative deltas
 * because it's not possible to contract/shrink open paths.</li>
 * <li>When offsetting, it's important not to confuse <b>EndType.Polygon</b>
 * with <b>EndType.Joined</b>. <b>EndType.Polygon</b> should be used when
 * offsetting polygons (closed paths). <b>EndType.Joined</b> should be used with
 * polylines (open paths).</li>
 * <li>Offsetting should <b>not</b> be performed on <b>intersecting closed
 * paths</b>, as doing so will almost always produce undesirable results.
 * Intersections must be removed before offsetting, which can be achieved
 * through a <b>Union</b> clipping operation.</li>
 * <li>It is OK to offset self-intersecting open paths (polylines), though the
 * intersecting (overlapping) regions will be flattened in the solution
 * polygon.</li>
 * <li>When offsetting closed paths (polygons), the <b>winding direction</b> of
 * paths in the solution will match that of the paths prior to offsetting.
 * Polygons with hole regions should comply with <b>NonZero filling</b>.</li>
 * <li>When offsetting open paths (polylines), the solutions will always have
 * <b>Positive orientation</b>.</li>
 * <li>Path <b>order</b> following offsetting very likely <i>won't</i> match
 * path order prior to offsetting.</li>
 * <li>While the ClipperOffset class itself won't accept paths with floating
 * point coordinates, the <b>InflatePaths</b> function will accept paths with
 * floating point coordinates.</li>
 * <li>Redundant segments should be removed before offsetting (see
 * {@link Clipper#SimplifyPaths(Paths64, double)
 * SimplifyPaths()}), and between offsetting operations too. These redundant
 * segments not only slow down offsetting, but they can cause unexpected
 * blemishes in offset solutions.</li>
 * </ul>
 */
public class ClipperOffset {

	private static double TOLERANCE = 1.0E-12;
	private static final String COORD_RANGE_ERROR = "Error: Coordinate range.";

	private final List<Group> groupList = new ArrayList<>();
	private final Path64 inPath = new Path64();
	private Path64 pathOut = new Path64();
	private final PathD normals = new PathD();
	private final Paths64 solution = new Paths64();
	private double groupDelta; // *0.5 for open paths; *-1.0 for negative areas
	private double delta;
	private double mitLimSqr;
	private double stepsPerRad;
	private double stepSin;
	private double stepCos;
	private JoinType joinType;
	private EndType endType;
	private double arcTolerance;
	private boolean mergeGroups;
	private double miterLimit;
	private boolean preserveCollinear;
	private boolean reverseSolution;
	private DeltaCallback64 deltaCallback;

	/**
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset(double miterLimit, double arcTolerance, boolean preserveCollinear) {
		this(miterLimit, arcTolerance, preserveCollinear, false);
	}

	/**
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset(double miterLimit, double arcTolerance) {
		this(miterLimit, arcTolerance, false, false);
	}

	/**
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset(double miterLimit) {
		this(miterLimit, 0.25, false, false);
	}

	/**
	 * Creates a ClipperOffset object, using default parameters.
	 *
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset() {
		this(2.0, 0.25, false, false);
	}

	/**
	 * Creates a ClipperOffset object, using the supplied parameters.
	 *
	 * @param miterLimit        This property sets the maximum distance in multiples
	 *                          of groupDelta that vertices can be offset from their
	 *                          original positions before squaring is applied.
	 *                          (Squaring truncates a miter by 'cutting it off' at 1
	 *                          × groupDelta distance from the original vertex.)
	 *                          <p>
	 *                          The default value for <code>miterLimit</code> is 2
	 *                          (i.e. twice groupDelta). This is also the smallest
	 *                          MiterLimit that's allowed. If mitering was
	 *                          unrestricted (ie without any squaring), then offsets
	 *                          at very acute angles would generate unacceptably
	 *                          long 'spikes'.
	 * @param arcTolerance      Since flattened paths can never perfectly represent
	 *                          arcs (see Trigonometry), this property specifies a
	 *                          maximum acceptable imperfection for rounded curves
	 *                          during offsetting.
	 *                          <p>
	 *                          It's important to make arcTolerance a sensible
	 *                          fraction of the offset groupDelta (arc radius).
	 *                          Large tolerances relative to the offset groupDelta
	 *                          will produce poor arc approximations but, just as
	 *                          importantly, very small tolerances will slow
	 *                          offsetting performance without noticeably improving
	 *                          curve quality.
	 *                          <p>
	 *                          arcTolerance is only relevant when offsetting with
	 *                          {@link JoinType#Round} and / or
	 *                          {@link EndType#Round} (see
	 *                          {{@link #AddPath(Path64, JoinType, EndType)
	 *                          AddPath()} and
	 *                          {@link #AddPaths(Paths64, JoinType, EndType)
	 *                          AddPaths()}. The default arcTolerance is 0.25.
	 * @param preserveCollinear When adjacent edges are collinear in closed path
	 *                          solutions, the common vertex can safely be removed
	 *                          to simplify the solution without altering path
	 *                          shape. However, because some users prefer to retain
	 *                          these common vertices, this feature is optional.
	 *                          Nevertheless, when adjacent edges in solutions are
	 *                          collinear and also create a 'spike' by overlapping,
	 *                          the vertex creating the spike will be removed
	 *                          irrespective of the PreserveCollinear setting. This
	 *                          property is false by default.
	 * @param reverseSolution   reverses the solution's orientation
	 */
	public ClipperOffset(double miterLimit, double arcTolerance, boolean preserveCollinear, boolean reverseSolution) {
		setMiterLimit(miterLimit);
		setArcTolerance(arcTolerance);
		setMergeGroups(true);
		setPreserveCollinear(preserveCollinear);
		setReverseSolution(reverseSolution);
	}

	public final void Clear() {
		groupList.clear();
	}

	public final void AddPath(Path64 path, JoinType joinType, EndType endType) {
		int cnt = path.size();
		if (cnt == 0) {
			return;
		}
		Paths64 pp = new Paths64(Arrays.asList(path));
		AddPaths(pp, joinType, endType);
	}

	public final void AddPaths(Paths64 paths, JoinType joinType, EndType endType) {
		int cnt = paths.size();
		if (cnt == 0) {
			return;
		}
		groupList.add(new Group(paths, joinType, endType));
	}

	private int CalcSolutionCapacity() {
		int result = 0;
		for (Group g : groupList) {
			result += (g.endType == EndType.Joined) ? g.inPaths.size() * 2 : g.inPaths.size();
		}
		return result;
	}

	private void ExecuteInternal(double delta) {
		solution.clear();
		if (groupList.isEmpty()) {
			return;
		}
		solution.ensureCapacity(CalcSolutionCapacity());

		// make sure the offset delta is significant
		if (Math.abs(delta) < 0.5) {
			for (Group group : groupList) {
				for (Path64 path : group.inPaths) {
					solution.add(path);
				}
			}
			return;
		}
		this.delta = delta;
		this.mitLimSqr = (miterLimit <= 1 ? 2.0 : 2.0 / Clipper.Sqr(miterLimit));

		for (Group group : groupList) {
			DoGroupOffset(group);
		}
	}

	boolean CheckPathsReversed() {
		boolean result = false;
		for (Group g : groupList) {
			if (g.endType == EndType.Polygon) {
				result = g.pathsReversed;
				break;
			}
		}

		return result;
	}

	public final void Execute(double delta, Paths64 solution) {
		solution.clear();
		ExecuteInternal(delta);
		if (groupList.isEmpty()) {
			return;
		}

		boolean pathsReversed = CheckPathsReversed();
		FillRule fillRule = pathsReversed ? FillRule.Negative : FillRule.Positive;

		// clean up self-intersections ...
		Clipper64 c = new Clipper64();
		c.setPreserveCollinear(preserveCollinear);
		// the solution should retain the orientation of the input
		c.setReverseSolution(reverseSolution != pathsReversed);
		c.AddSubject(this.solution);
		c.Execute(ClipType.Union, fillRule, solution);
	}

	public void Execute(DeltaCallback64 deltaCallback64, Paths64 solution) {
		deltaCallback = deltaCallback64;
		Execute(1.0, solution);
	}

	public void Execute(double delta, PolyTree64 solutionTree) {
		solutionTree.Clear();
		ExecuteInternal(delta);
		if (groupList.isEmpty()) {
			return;
		}

		boolean pathsReversed = CheckPathsReversed();
		FillRule fillRule = pathsReversed ? FillRule.Negative : FillRule.Positive;
		// clean up self-intersections ...
		Clipper64 c = new Clipper64();
		c.setPreserveCollinear(preserveCollinear);
		// the solution should normally retain the orientation of the input
		c.setReverseSolution(reverseSolution != pathsReversed);
		c.AddSubject(solution);
		c.Execute(ClipType.Union, fillRule, solutionTree);
	}

	public final double getArcTolerance() {
		return arcTolerance;
	}

	public final void setArcTolerance(double value) {
		arcTolerance = value;
	}

	public final boolean getMergeGroups() {
		return mergeGroups;
	}

	public final void setMergeGroups(boolean value) {
		mergeGroups = value;
	}

	public final double getMiterLimit() {
		return miterLimit;
	}

	public final void setMiterLimit(double value) {
		miterLimit = value;
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

	public final void setDeltaCallBack64(DeltaCallback64 callback) {
		deltaCallback = callback;
	}

	public final DeltaCallback64 getDeltaCallBack64() {
		return deltaCallback;
	}

	private static PointD GetUnitNormal(Point64 pt1, Point64 pt2) {
		double dx = (pt2.x - pt1.x);
		double dy = (pt2.y - pt1.y);
		if ((dx == 0) && (dy == 0)) {
			return new PointD();
		}

		double f = 1.0 / Math.sqrt(dx * dx + dy * dy);
		dx *= f;
		dy *= f;

		return new PointD(dy, -dx);
	}

	private static void GetBoundsAndLowestPolyIdx(Paths64 paths, OutObject<Integer> index, OutObject<Rect64> recRef) {
		final Rect64 rec = new Rect64(false); // ie invalid rect
		recRef.argValue = rec;
		long lpX = Long.MIN_VALUE;
		index.argValue = -1;
		for (int i = 0; i < paths.size(); i++) {
			for (Point64 pt : paths.get(i)) {
				if (pt.y >= rec.bottom) {
					if (pt.y > rec.bottom || pt.x < lpX) {
						index.argValue = i;
						lpX = pt.x;
						rec.bottom = pt.y;
					}
				} else if (pt.y < rec.top) {
					rec.top = pt.y;
				}
				if (pt.x > rec.right) {
					rec.right = pt.x;
				} else if (pt.x < rec.left) {
					rec.left = pt.y;
				}
			}
		}
	}

	private static PointD TranslatePoint(PointD pt, double dx, double dy) {
		return new PointD(pt.x + dx, pt.y + dy);
	}

	private static PointD ReflectPoint(PointD pt, PointD pivot) {
		return new PointD(pivot.x + (pivot.x - pt.x), pivot.y + (pivot.y - pt.y));
	}

	private static boolean AlmostZero(double value) {
		return AlmostZero(value, 0.001);
	}

	private static boolean AlmostZero(double value, double epsilon) {
		return Math.abs(value) < epsilon;
	}

	private static double Hypotenuse(double x, double y) {
		return Math.sqrt(x * x + y * y);
	}

	private static PointD NormalizeVector(PointD vec) {
		double h = Hypotenuse(vec.x, vec.y);
		if (AlmostZero(h)) {
			return new PointD(0, 0);
		}
		double inverseHypot = 1 / h;
		return new PointD(vec.x * inverseHypot, vec.y * inverseHypot);
	}

	private static PointD GetAvgUnitVector(PointD vec1, PointD vec2) {
		return NormalizeVector(new PointD(vec1.x + vec2.x, vec1.y + vec2.y));
	}

	private static PointD IntersectPoint(PointD pt1a, PointD pt1b, PointD pt2a, PointD pt2b) {
		if (InternalClipper.IsAlmostZero(pt1a.x - pt1b.x)) { // vertical
			if (InternalClipper.IsAlmostZero(pt2a.x - pt2b.x)) {
				return new PointD(0, 0);
			}
			double m2 = (pt2b.y - pt2a.y) / (pt2b.x - pt2a.x);
			double b2 = pt2a.y - m2 * pt2a.x;
			return new PointD(pt1a.x, m2 * pt1a.x + b2);
		}

		if (InternalClipper.IsAlmostZero(pt2a.x - pt2b.x)) { // vertical
			double m1 = (pt1b.y - pt1a.y) / (pt1b.x - pt1a.x);
			double b1 = pt1a.y - m1 * pt1a.x;
			return new PointD(pt2a.x, m1 * pt2a.x + b1);
		} else {
			double m1 = (pt1b.y - pt1a.y) / (pt1b.x - pt1a.x);
			double b1 = pt1a.y - m1 * pt1a.x;
			double m2 = (pt2b.y - pt2a.y) / (pt2b.x - pt2a.x);
			double b2 = pt2a.y - m2 * pt2a.x;
			if (InternalClipper.IsAlmostZero(m1 - m2)) {
				return new PointD(0, 0);
			}
			double x = (b2 - b1) / (m1 - m2);
			return new PointD(x, m1 * x + b1);
		}
	}

	private Point64 GetPerpendic(Point64 pt, PointD norm) {
		return new Point64(pt.x + norm.x * groupDelta, pt.y + norm.y * groupDelta);
	}

	private PointD GetPerpendicD(Point64 pt, PointD norm) {
		return new PointD(pt.x + norm.x * groupDelta, pt.y + norm.y * groupDelta);
	}

	private void DoBevel(Path64 path, int j, int k) {
		Point64 pt1, pt2;
		if (j == k) {
			double absDelta = Math.abs(groupDelta);
			pt1 = new Point64(path.get(j).x - absDelta * normals.get(j).x, path.get(j).y - absDelta * normals.get(j).y);
			pt2 = new Point64(path.get(j).x + absDelta * normals.get(j).x, path.get(j).y + absDelta * normals.get(j).y);
		} else {
			pt1 = new Point64(path.get(j).x + groupDelta * normals.get(k).x, path.get(j).y + groupDelta * normals.get(k).y);
			pt2 = new Point64(path.get(j).x + groupDelta * normals.get(j).x, path.get(j).y + groupDelta * normals.get(j).y);
		}
		pathOut.add(pt1);
		pathOut.add(pt2);
	}

	private void DoSquare(Path64 path, int j, int k) {
		PointD vec;
		if (j == k) {
			vec = new PointD(normals.get(j).y, -normals.get(j).x);
		} else {
			vec = GetAvgUnitVector(new PointD(-normals.get(k).y, normals.get(k).x), new PointD(normals.get(j).y, -normals.get(j).x));
		}
		double absDelta = Math.abs(groupDelta);
		// now offset the original vertex delta units along unit vector
		PointD ptQ = new PointD(path.get(j));
		ptQ = TranslatePoint(ptQ, absDelta * vec.x, absDelta * vec.y);

		// get perpendicular vertices
		PointD pt1 = TranslatePoint(ptQ, groupDelta * vec.y, groupDelta * -vec.x);
		PointD pt2 = TranslatePoint(ptQ, groupDelta * -vec.y, groupDelta * vec.x);
		// get 2 vertices along one edge offset
		PointD pt3 = GetPerpendicD(path.get(k), normals.get(k));

		if (j == k) {
			PointD pt4 = new PointD(pt3.x + vec.x * groupDelta, pt3.y + vec.y * groupDelta);
			PointD pt = IntersectPoint(pt1, pt2, pt3, pt4);
			// get the second intersect point through reflecion
			pathOut.add(new Point64(ReflectPoint(pt, ptQ)));
			pathOut.add(new Point64(pt));
		} else {
			PointD pt4 = GetPerpendicD(path.get(j), normals.get(k));
			PointD pt = IntersectPoint(pt1, pt2, pt3, pt4);
			pathOut.add(new Point64(pt));
			// get the second intersect point through reflecion
			pathOut.add(new Point64(ReflectPoint(pt, ptQ)));
		}
	}

	private void DoMiter(Group group, Path64 path, int j, int k, double cosA) {
		final double q = groupDelta / (cosA + 1);
		pathOut.add(new Point64(path.get(j).x + (normals.get(k).x + normals.get(j).x) * q, path.get(j).y + (normals.get(k).y + normals.get(j).y) * q));
	}

	private void DoRound(Path64 path, int j, int k, double angle) {
		if (deltaCallback != null) {
			// when deltaCallback is assigned, groupDelta won't be constant,
			// so we'll need to do the following calculations for *every* vertex.
			double absDelta = Math.abs(groupDelta);
			double arcTol = arcTolerance > TOLERANCE ? Math.min(absDelta, arcTolerance) : Math.log10(2 + absDelta) * DEFAULT_ARC_TOLERANCE;
			double stepsPer360 = Math.PI / Math.acos(1 - arcTol / absDelta);
			stepSin = Math.sin((2 * Math.PI) / stepsPer360);
			stepCos = Math.cos((2 * Math.PI) / stepsPer360);
			if (groupDelta < 0.0) {
				stepSin = -stepSin;
			}
			stepsPerRad = stepsPer360 / (2 * Math.PI);
		}

		final Point64 pt = path.get(j);
		PointD offsetVec = new PointD(normals.get(k).x * groupDelta, normals.get(k).y * groupDelta);
		if (j == k) {
			offsetVec.Negate();
		}
		pathOut.add(new Point64(pt.x + offsetVec.x, pt.y + offsetVec.y));
		int steps = (int) Math.ceil(stepsPerRad * Math.abs(angle)); // #448, #456
		for (int i = 1; i < steps; ++i) // ie 1 less than steps
		{
			offsetVec = new PointD(offsetVec.x * stepCos - stepSin * offsetVec.y, offsetVec.x * stepSin + offsetVec.y * stepCos);
			pathOut.add(new Point64(pt.x + offsetVec.x, pt.y + offsetVec.y));
		}
		pathOut.add(GetPerpendic(pt, normals.get(j)));
	}

	private void BuildNormals(Path64 path) {
		int cnt = path.size();
		normals.clear();

		for (int i = 0; i < cnt - 1; i++) {
			normals.add(GetUnitNormal(path.get(i), path.get(i + 1)));
		}
		normals.add(GetUnitNormal(path.get(cnt - 1), path.get(0)));
	}

	private void OffsetPoint(Group group, Path64 path, int j, RefObject<Integer> k) {
		// Let A = change in angle where edges join
		// A == 0: ie no change in angle (flat join)
		// A == PI: edges 'spike'
		// sin(A) < 0: right turning
		// cos(A) < 0: change in angle is more than 90 degree
		double sinA = InternalClipper.CrossProduct(normals.get(j), normals.get(k.argValue));
		double cosA = InternalClipper.DotProduct(normals.get(j), normals.get(k.argValue));
		if (sinA > 1.0) {
			sinA = 1.0;
		} else if (sinA < -1.0) {
			sinA = -1.0;
		}

		if (deltaCallback != null) {
			groupDelta = deltaCallback.calculate(path, normals, j, k.argValue);
			if (group.pathsReversed) {
				groupDelta = -groupDelta;
			}
		}
		if (Math.abs(groupDelta) < TOLERANCE) {
			pathOut.add(path.get(j));
			return;
		}

		if (cosA > -0.99 && (sinA * groupDelta < 0)) { // test for concavity first (#593)
			// is concave
			pathOut.add(GetPerpendic(path.get(j), normals.get(k.argValue)));
			// this extra point is the only (simple) way to ensure that
			// path reversals are fully cleaned with the trailing clipper
			pathOut.add(path.get(j)); // (#405)
			pathOut.add(GetPerpendic(path.get(j), normals.get(j)));
		} else if (cosA > 0.999 && joinType != JoinType.Round) {
			// almost straight - less than 2.5 degree (#424, #482, #526 & #724)
	        DoMiter(group, path, j, k.argValue, cosA);
		} else if (joinType == JoinType.Miter) {
			// miter unless the angle is sufficiently acute to exceed ML
			if (cosA > mitLimSqr - 1) {
				DoMiter(group, path, j, k.argValue, cosA);
			} else {
				DoSquare(path, j, k.argValue);
			}
		} else if (joinType == JoinType.Round) {
			DoRound(path, j, k.argValue, Math.atan2(sinA, cosA));
		} else if (joinType == JoinType.Bevel) {
			DoBevel(path, j, k.argValue);
		} else {
			DoSquare(path, j, k.argValue);
		}

		k.argValue = j;
	}

	private void OffsetPolygon(Group group, Path64 path) {
		pathOut = new Path64();
		int cnt = path.size();
		RefObject<Integer> prev = new RefObject<Integer>(cnt-1);
		for (int i = 0; i < cnt; i++) {
			OffsetPoint(group, path, i, prev);
		}
		solution.add(pathOut);
	}

	private void OffsetOpenJoined(Group group, Path64 path) {
		OffsetPolygon(group, path);
		path = Clipper.ReversePath(path);
		BuildNormals(path);
		OffsetPolygon(group, path);
	}

	private void OffsetOpenPath(Group group, Path64 path) {
		pathOut = new Path64();
		int highI = path.size() - 1;
		if (deltaCallback != null) {
			groupDelta = deltaCallback.calculate(path, normals, 0, 0);
		}
		// do the line start cap
		if (Math.abs(groupDelta) < TOLERANCE) {
			pathOut.add(path.get(0));
		} else {
			switch (endType) {
				case Butt :
					DoBevel(path, 0, 0);
					break;
				case Round :
					DoRound(path, 0, 0, Math.PI);
					break;
				default :
					DoSquare(path, 0, 0);
					break;
			}
		}
		// offset the left side going forward
		for (int i = 1, k = 0; i < highI; i++) {
			OffsetPoint(group, path, i, new RefObject<>(k)); // NOTE creating new ref object correct?
		}
		// reverse normals ...
		for (int i = highI; i > 0; i--) {
			normals.set(i, new PointD(-normals.get(i - 1).x, -normals.get(i - 1).y));
		}
		normals.set(0, normals.get(highI));
		if (deltaCallback != null) {
			groupDelta = deltaCallback.calculate(path, normals, highI, highI);
		}
		// do the line end cap
		if (Math.abs(groupDelta) < TOLERANCE) {
			pathOut.add(path.get(highI));
		} else {
			switch (endType) {
				case Butt :
					DoBevel(path, highI, highI);
					break;
				case Round :
					DoRound(path, highI, highI, Math.PI);
					break;
				default :
					DoSquare(path, highI, highI);
					break;
			}
		}
		// offset the left side going back
		for (int i = highI, k = 0; i > 0; i--) {
			OffsetPoint(group, path, i, new RefObject<>(k)); // NOTE creating new ref object correct?
		}
		solution.add(pathOut);
	}

	private void DoGroupOffset(Group group) {
		if (group.endType == EndType.Polygon) {
			if (group.lowestPathIdx < 0) {
				return;
			}
			// if (area == 0) return; // probably unhelpful (#430)
			groupDelta = (group.pathsReversed) ? -delta : delta;
		} else {
			groupDelta = Math.abs(delta); // * 0.5
		}

		double absDelta = Math.abs(groupDelta);
		if (!ValidateBounds(group.boundsList, absDelta)) {
			throw new RuntimeException(COORD_RANGE_ERROR);
		}

		joinType = group.joinType;
		endType = group.endType;

		if (group.joinType == JoinType.Round || group.endType == EndType.Round) {
			// calculate a sensible number of steps (for 360 deg for the given offset
			// arcTol - when fArcTolerance is undefined (0), the amount of
			// curve imprecision that's allowed is based on the size of the
			// offset (delta). Obviously very large offsets will almost always
			// require much less precision.
			double arcTol = arcTolerance > 0.01 ? arcTolerance : Math.log10(2 + absDelta) * InternalClipper.DEFAULT_ARC_TOLERANCE;
			double stepsPer360 = Math.PI / Math.acos(1 - arcTol / absDelta);
			stepSin = Math.sin((2 * Math.PI) / stepsPer360);
			stepCos = Math.cos((2 * Math.PI) / stepsPer360);
			if (groupDelta < 0.0) {
				stepSin = -stepSin;
			}
			stepsPerRad = stepsPer360 / (2 * Math.PI);
		}

		int i = 0;
		for (Path64 p : group.inPaths) {
			Rect64 pathBounds = group.boundsList.get(i++);
			if (!pathBounds.IsValid()) {
				continue;
			}
			int cnt = p.size();
			if ((cnt == 0) || ((cnt < 3) && (endType == EndType.Polygon))) {
				continue;
			}

			pathOut = new Path64();
			if (cnt == 1) {
				Point64 pt = p.get(0);

				// single vertex so build a circle or square ...
				if (group.endType == EndType.Round) {
					double r = absDelta;
					int steps = (int) Math.ceil(stepsPerRad * 2 * Math.PI);
					pathOut = Clipper.Ellipse(pt, r, r, steps);
				} else {
					int d = (int) Math.ceil(groupDelta);
					Rect64 r = new Rect64(pt.x - d, pt.y - d, pt.x + d, pt.y + d);
					pathOut = r.AsPath();
				}
				solution.add(pathOut);
				continue;
			} // end of offsetting a single (open path) point

			// when shrinking, then make sure the path can shrink that far (#593)
			if (groupDelta < 0 && Math.min(pathBounds.getWidth(), pathBounds.getHeight()) < -groupDelta * 2) {
				continue;
			}

			if (cnt == 2 && group.endType == EndType.Joined) {
				endType = (group.joinType == JoinType.Round) ? EndType.Round : EndType.Square;
			}

			BuildNormals(p);
			if (endType == EndType.Polygon) {
				OffsetPolygon(group, p);
			} else if (endType == EndType.Joined) {
				OffsetOpenJoined(group, p);
			} else {
				OffsetOpenPath(group, p);
			}
		}
	}

	private static boolean ValidateBounds(List<Rect64> boundsList, double delta) {
		int intDelta = (int) delta;
		for (Rect64 r : boundsList) {
			if (!r.IsValid()) {
				continue; // ignore invalid paths
			} else if (r.left < MIN_COORD + intDelta || r.right > MAX_COORD + intDelta || r.top < MIN_COORD + intDelta || r.bottom > MAX_COORD + intDelta) {
				return false;
			}
		}
		return true;
	}

}