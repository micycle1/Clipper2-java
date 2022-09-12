package clipper2.offset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import clipper2.Clipper;
import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.InternalClipper;
import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.engine.Clipper64;
import tangible.RefObject;

public class ClipperOffset {

	private final List<PathGroup> _pathGroups = new ArrayList<>();
	private final List<PointD> _normals = new ArrayList<>();
	private final List<List<Point64>> solution = new ArrayList<>();
	private double _delta, _abs_delta, _tmpLimit, _stepsPerRad;
	private JoinType _joinType = JoinType.values()[0];
	private double ArcTolerance;

	public final double getArcTolerance() {
		return ArcTolerance;
	}

	public final void setArcTolerance(double value) {
		ArcTolerance = value;
	}

	private boolean MergeGroups;

	public final boolean getMergeGroups() {
		return MergeGroups;
	}

	public final void setMergeGroups(boolean value) {
		MergeGroups = value;
	}

	private double MiterLimit;

	public final double getMiterLimit() {
		return MiterLimit;
	}

	public final void setMiterLimit(double value) {
		MiterLimit = value;
	}

	private boolean PreserveCollinear;

	public final boolean getPreserveCollinear() {
		return PreserveCollinear;
	}

	public final void setPreserveCollinear(boolean value) {
		PreserveCollinear = value;
	}

	private boolean ReverseSolution;

	public final boolean getReverseSolution() {
		return ReverseSolution;
	}

	public final void setReverseSolution(boolean value) {
		ReverseSolution = value;
	}

	private static final double TwoPi = Math.PI * 2;

	public ClipperOffset(double miterLimit, double arcTolerance, boolean preserveCollinear) {
		this(miterLimit, arcTolerance, preserveCollinear, false);
	}

	public ClipperOffset(double miterLimit, double arcTolerance) {
		this(miterLimit, arcTolerance, false, false);
	}

	public ClipperOffset(double miterLimit) {
		this(miterLimit, 0.0, false, false);
	}

	public ClipperOffset() {
		this(2.0, 0.0, false, false);
	}

	public ClipperOffset(double miterLimit, double arcTolerance, boolean preserveCollinear, boolean reverseSolution) {
		setMiterLimit(miterLimit);
		setArcTolerance(arcTolerance);
		setMergeGroups(true);
		setPreserveCollinear(preserveCollinear);
		setReverseSolution(reverseSolution);
	}

	public final void Clear() {
		_pathGroups.clear();
	}

	public final void AddPath(List<Point64> path, JoinType joinType, EndType endType) {
		int cnt = path.size();
		if (cnt == 0) {
			return;
		}
		List<List<Point64>> pp = new ArrayList<>(Arrays.asList(path));
		AddPaths(pp, joinType, endType);
	}

	public final void AddPaths(List<List<Point64>> paths, JoinType joinType, EndType endType) {
		int cnt = paths.size();
		if (cnt == 0) {
			return;
		}
		_pathGroups.add(new PathGroup(paths, joinType, endType));
	}

	public final List<List<Point64>> Execute(double delta) {
		solution.clear();
		if (Math.abs(delta) < 0.5) {
			for (PathGroup group : _pathGroups) {
				for (List<Point64> path : group._inPaths) {
					solution.add(path);
				}
			}
			return solution;
		}

		_tmpLimit = (getMiterLimit() <= 1 ? 2.0 : 2.0 / Clipper.Sqr(getMiterLimit()));

		for (PathGroup group : _pathGroups) {
			DoGroupOffset(group, delta);
		}

		if (getMergeGroups() && !_pathGroups.isEmpty()) {
			// clean up self-intersections ...
			Clipper64 c = new Clipper64();
			c.setPreserveCollinear(getPreserveCollinear());
			c.setReverseSolution(getReverseSolution() != _pathGroups.get(0)._pathsReversed);
			c.AddSubjects(solution);
			if (_pathGroups.get(0)._pathsReversed) {
				c.Execute(ClipType.Union, FillRule.Negative, solution);
			} else {
				c.Execute(ClipType.Union, FillRule.Positive, solution);
			}
		}
		return solution;
	}

	public static PointD GetUnitNormal(Point64 pt1, Point64 pt2) {
		double dx = (pt2.X - pt1.X);
		double dy = (pt2.Y - pt1.Y);
		if ((dx == 0) && (dy == 0)) {
			return new PointD();
		}

		double f = 1.0 / Math.sqrt(dx * dx + dy * dy);
		dx *= f;
		dy *= f;

		return new PointD(dy, -dx);
	}

	private int GetLowestPolygonIdx(List<List<Point64>> paths) {
		Point64 lp = new Point64(0, Long.MIN_VALUE);
		int result = -1;
		for (int i = 0; i < paths.size(); i++) {
			for (Point64 pt : paths.get(i)) {
				if (pt.Y < lp.Y || (pt.Y == lp.Y && pt.X >= lp.X)) {
					continue;
				}
				result = i;
				lp = pt;
			}
		}
		return result;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private PointD TranslatePoint(PointD pt, double dx, double dy)
	private PointD TranslatePoint(PointD pt, double dx, double dy) {
		return new PointD(pt.x + dx, pt.y + dy);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private PointD ReflectPoint(PointD pt, PointD pivot)
	private PointD ReflectPoint(PointD pt, PointD pivot) {
		return new PointD(pivot.x + (pivot.x - pt.x), pivot.y + (pivot.y - pt.y));
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private bool AlmostZero(double value, double epsilon = 0.001)

	private boolean AlmostZero(double value) {
		return AlmostZero(value, 0.001);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private bool AlmostZero(double value, double epsilon = 0.001)
	private boolean AlmostZero(double value, double epsilon) {
		return Math.abs(value) < epsilon;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private double Hypotenuse(double x, double y)
	private double Hypotenuse(double x, double y) {
		return Math.sqrt(x * x + y * y);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private PointD NormalizeVector(PointD vec)
	private PointD NormalizeVector(PointD vec) {
		double h = Hypotenuse(vec.x, vec.y);
		if (AlmostZero(h)) {
			return new PointD(0, 0);
		}
		double inverseHypot = 1 / h;
		return new PointD(vec.x * inverseHypot, vec.y * inverseHypot);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private PointD GetAvgUnitVector(PointD vec1, PointD vec2)
	private PointD GetAvgUnitVector(PointD vec1, PointD vec2) {
		return NormalizeVector(new PointD(vec1.x + vec2.x, vec1.y + vec2.y));
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private PointD IntersectPoint(PointD pt1a, PointD pt1b, PointD pt2a, PointD pt2b)
	private PointD IntersectPoint(PointD pt1a, PointD pt1b, PointD pt2a, PointD pt2b) {
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

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void DoSquare(PathGroup group, List<Point64> path, int j, int k)
	private void DoSquare(PathGroup group, List<Point64> path, int j, int k) {
		// square off at delta distance from original vertex
		PointD vec, pt, ptQ, pt1, pt2, pt3, pt4;

		// using the reciprocal of unit normals (as unit vectors)
		// get the average unit vector ...
		vec = GetAvgUnitVector(new PointD(-_normals.get(k).y, _normals.get(k).x), new PointD(_normals.get(j).y, -_normals.get(j).x));

		// now offset the original vertex delta units along unit vector
		ptQ = new PointD(path.get(j));
		ptQ = TranslatePoint(ptQ, _abs_delta * vec.x, _abs_delta * vec.y);

		// get perpendicular vertices
		pt1 = TranslatePoint(ptQ, _delta * vec.y, _delta * -vec.x);
		pt2 = TranslatePoint(ptQ, _delta * -vec.y, _delta * vec.x);
		// get 2 vertices along one edge offset
		pt3 = new PointD(path.get(k).X + _normals.get(k).x * _delta, path.get(k).Y + _normals.get(k).y * _delta);
		pt4 = new PointD(path.get(j).X + _normals.get(k).x * _delta, path.get(j).Y + _normals.get(k).y * _delta);

		// get the intersection point
		pt = IntersectPoint(pt1, pt2, pt3, pt4);
		group._outPath.add(new Point64(pt));
		// get the second intersect point through reflecion
		group._outPath.add(new Point64(ReflectPoint(pt, ptQ)));
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void DoMiter(PathGroup group, List<Point64> path, int j, int k, double cosA)
	private void DoMiter(PathGroup group, List<Point64> path, int j, int k, double cosA) {
		double q = _delta / (cosA + 1);
		group._outPath.add(new Point64(path.get(j).X + (_normals.get(k).x + _normals.get(j).x) * q,
				path.get(j).Y + (_normals.get(k).y + _normals.get(j).y) * q));
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void DoRound(PathGroup group, Point64 pt, PointD normal1, PointD normal2, double angle)
	private void DoRound(PathGroup group, Point64 pt, PointD normal1, PointD normal2, double angle) {
		// even though angle may be negative this is a convex join
		PointD pt2 = new PointD(normal2.x * _delta, normal2.y * _delta);
		int steps = (int) Math.rint(_stepsPerRad * Math.abs(angle) + 0.501);
		group._outPath.add(new Point64(pt.X + pt2.x, pt.Y + pt2.y));
		double stepSin = Math.sin(angle / steps);
		double stepCos = Math.cos(angle / steps);
		for (int i = 0; i < steps; i++) {
			pt2 = new PointD(pt2.x * stepCos - stepSin * pt2.y, pt2.x * stepSin + pt2.y * stepCos);
			group._outPath.add(new Point64(pt.X + pt2.x, pt.Y + pt2.y));
		}
		group._outPath.add(new Point64(pt.X + normal1.x * _delta, pt.Y + normal1.y * _delta));
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void BuildNormals(List<Point64> path)
	private void BuildNormals(List<Point64> path) {
		int cnt = path.size();
		_normals.clear();

		for (int i = 0; i < cnt - 1; i++) {
			_normals.add(GetUnitNormal(path.get(i), path.get(i + 1)));
		}
		_normals.add(GetUnitNormal(path.get(cnt - 1), path.get(0)));
	}

	private void OffsetPoint(PathGroup group, List<Point64> path, int j, RefObject<Integer> k) {
		// Let A = change in angle where edges join
		// A == 0: ie no change in angle (flat join)
		// A == PI: edges 'spike'
		// sin(A) < 0: right turning
		// cos(A) < 0: change in angle is more than 90 degree
		double sinA = InternalClipper.CrossProduct(_normals.get(j), _normals.get(k.argValue));
		double cosA = InternalClipper.DotProduct(_normals.get(j), _normals.get(k.argValue));
		if (sinA > 1.0) {
			sinA = 1.0;
		} else if (sinA < -1.0) {
			sinA = -1.0;
		}

		boolean almostNoAngle = (AlmostZero(sinA) && cosA > 0);
		if (almostNoAngle || (sinA * _delta < 0)) {
			Point64 p1 = new Point64(path.get(j).X + _normals.get(k.argValue).x * _delta,
					path.get(j).Y + _normals.get(k.argValue).y * _delta);
			Point64 p2 = new Point64(path.get(j).X + _normals.get(j).x * _delta, path.get(j).Y + _normals.get(j).y * _delta);
			group._outPath.add(p1);
			if (p1 != p2) {
				// when concave add an extra vertex to ensure neat clipping
				if (!almostNoAngle) {
					group._outPath.add(path.get(j));
				}
				group._outPath.add(p2);
			}
		} else if (_joinType == JoinType.Round) {
			DoRound(group, path.get(j), _normals.get(j), _normals.get(k.argValue), Math.atan2(sinA, cosA));
		}
		// else miter when the angle isn't too acute (and hence exceed ML)
		else if (_joinType == JoinType.Miter && cosA > _tmpLimit - 1) {
			DoMiter(group, path, j, k.argValue, cosA);
		}
		// else only square angles that deviate > 90 degrees
		else if (cosA < -0.001) {
			DoSquare(group, path, j, k.argValue);
		} else {
			// don't square shallow angles that are safe to miter
			DoMiter(group, path, j, k.argValue, cosA);
		}

		k.argValue = j;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void OffsetPolygon(PathGroup group, List<Point64> path)
	private void OffsetPolygon(PathGroup group, List<Point64> path) {
		group._outPath = new ArrayList<>();
		int cnt = path.size(), prev = cnt - 1;
		for (int i = 0; i < cnt; i++) {
			RefObject<Integer> tempRef_prev = new RefObject<>(prev);
			OffsetPoint(group, path, i, tempRef_prev);
			prev = tempRef_prev.argValue;
		}
		group._outPaths.add(group._outPath);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private void OffsetOpenJoined(PathGroup group, List<Point64> path)
	private void OffsetOpenJoined(PathGroup group, List<Point64> path) {
		OffsetPolygon(group, path);
		path = Clipper.ReversePath(path);
		BuildNormals(path);
		OffsetPolygon(group, path);
	}

	private void OffsetOpenPath(PathGroup group, List<Point64> path, EndType endType) {
		group._outPath = new ArrayList<>();
		int cnt = path.size() - 1, k = 0;
		for (int i = 1; i < cnt; i++) {
			RefObject<Integer> tempRef_k = new RefObject<>(k);
			OffsetPoint(group, path, i, tempRef_k);
			k = tempRef_k.argValue;
		}
		cnt++;

		_normals.set(cnt - 1, new PointD(-_normals.get(cnt - 2).x, -_normals.get(cnt - 2).y));

		switch (endType) {
			case Butt :
				group._outPath.add(new Point64(path.get(cnt - 1).X + _normals.get(cnt - 2).x * _delta,
						path.get(cnt - 1).Y + _normals.get(cnt - 2).y * _delta));
				group._outPath.add(new Point64(path.get(cnt - 1).X - _normals.get(cnt - 2).x * _delta,
						path.get(cnt - 1).Y - _normals.get(cnt - 2).y * _delta));
				break;
			case Round :
				DoRound(group, path.get(cnt - 1), _normals.get(cnt - 1), _normals.get(cnt - 2), Math.PI);
				break;
			default :
				DoSquare(group, path, cnt - 1, cnt - 2);
				break;
		}

		// reverse normals ...
		for (int i = cnt - 2; i > 0; i--) {
			_normals.set(i, new PointD(-_normals.get(i - 1).x, -_normals.get(i - 1).y));
		}
		_normals.set(0, new PointD(-_normals.get(1).x, -_normals.get(1).y));

		k = cnt - 1;
		for (int i = cnt - 2; i > 0; i--) {
			RefObject<Integer> tempRef_k2 = new RefObject<>(k);
			OffsetPoint(group, path, i, tempRef_k2);
			k = tempRef_k2.argValue;
		}

		// now cap the start ...
		switch (endType) {
			case Butt :
				group._outPath.add(new Point64(path.get(0).X + _normals.get(1).x * _delta, path.get(0).Y + _normals.get(1).y * _delta));
				group._outPath.add(new Point64(path.get(0).X - _normals.get(1).x * _delta, path.get(0).Y - _normals.get(1).y * _delta));
				break;
			case Round :
				DoRound(group, path.get(0), _normals.get(0), _normals.get(1), Math.PI);
				break;
			default :
				DoSquare(group, path, 0, 1);
				break;
		}

		group._outPaths.add(group._outPath);
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [MethodImpl(MethodImplOptions.AggressiveInlining)] private bool IsFullyOpenEndType(EndType et)
	private boolean IsFullyOpenEndType(EndType et) {
		return (et != EndType.Polygon) && (et != EndType.Joined);
	}

	private void DoGroupOffset(PathGroup group, double delta) {
		if (group._endType != EndType.Polygon) {
			delta = Math.abs(delta) / 2;
		}
		boolean isClosedPaths = !IsFullyOpenEndType(group._endType);

		if (isClosedPaths) {
			// the lowermost polygon must be an outer polygon. So we can use that as the
			// designated orientation for outer polygons (needed for tidy-up clipping)
			int lowestIdx = GetLowestPolygonIdx(group._inPaths);
			if (lowestIdx < 0) {
				return;
			}
			// nb: don't use the default orientation here ...
			double area = Clipper.AreaPath(group._inPaths.get(lowestIdx));
			if (area == 0) {
				return;
			}
			group._pathsReversed = (area < 0);
			if (group._pathsReversed) {
				delta = -delta;
			}
		} else {
			group._pathsReversed = false;
		}

		_delta = delta;
		_abs_delta = Math.abs(_delta);
		_joinType = group._joinType;

		// calculate a sensible number of steps (for 360 deg for the given offset
		if (group._joinType == JoinType.Round || group._endType == EndType.Round) {
			double arcTol = getArcTolerance() > 0.01 ? getArcTolerance() : Math.log10(2 + _abs_delta) * 0.25; // empirically derived
			// get steps per 180 degrees (see offset_triginometry2.svg)
			_stepsPerRad = Math.PI / Math.acos(1 - arcTol / _abs_delta) / TwoPi;
		}

		for (List<Point64> p : group._inPaths) {
			List<Point64> path = Clipper.StripDuplicates(p, isClosedPaths);
			int cnt = path.size();
			if (cnt == 0 || (cnt < 3 && !IsFullyOpenEndType(group._endType))) {
				continue;
			}

			if (cnt == 1) {
				group._outPath = new ArrayList<>();
				// single vertex so build a circle or square ...
				if (group._endType == EndType.Round) {
					DoRound(group, path.get(0), new PointD(1.0, 0.0), new PointD(-1.0, 0.0), TwoPi);
				} else {
					group._outPath.add(new Point64(path.get(0).X - _delta, path.get(0).Y - _delta));
					group._outPath.add(new Point64(path.get(0).X + _delta, path.get(0).Y - _delta));
					group._outPath.add(new Point64(path.get(0).X + _delta, path.get(0).Y + _delta));
					group._outPath.add(new Point64(path.get(0).X - _delta, path.get(0).Y + _delta));
				}
				group._outPaths.add(group._outPath);
			} else {
				BuildNormals(path);
				if (group._endType == EndType.Polygon) {
					OffsetPolygon(group, path);
				} else if (group._endType == EndType.Joined) {
					OffsetOpenJoined(group, path);
				} else {
					OffsetOpenPath(group, path, group._endType);
				}
			}
		}

		if (!getMergeGroups()) {
			// clean up self-intersections
			Clipper64 c = new Clipper64();
			c.setPreserveCollinear(getPreserveCollinear());
			c.setReverseSolution(getReverseSolution() != group._pathsReversed);
			c.AddSubjects(group._outPaths);
			if (group._pathsReversed) {
				c.Execute(ClipType.Union, FillRule.Negative, group._outPaths);
			} else {
				c.Execute(ClipType.Union, FillRule.Positive, group._outPaths);
			}
		}
		solution.addAll(group._outPaths);
		group._outPaths.clear();
	}
}