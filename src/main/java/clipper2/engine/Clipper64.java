package clipper2.engine;

import java.util.ArrayList;
import java.util.List;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.PathType;
import clipper2.core.Point64;

public class Clipper64 extends ClipperBase {

	public final void addPath(List<Point64> path, PathType polytype) {
		addPath(path, polytype, false);
	}

	public final void addPath(List<Point64> path, PathType polytype, boolean isOpen) {
		super.AddPath(path, polytype, isOpen);
	}

	public final void addPaths(List<List<Point64>> paths, PathType polytype) {
		addPaths(paths, polytype, false);
	}

	public final void addPaths(List<List<Point64>> paths, PathType polytype, boolean isOpen) {
		super.AddPaths(paths, polytype, isOpen);
	}

	public final void addSubject(List<List<Point64>> paths) {
		addPaths(paths, PathType.Subject);
	}

	public final void addOpenSubject(List<List<Point64>> paths) {
		addPaths(paths, PathType.Subject, true);
	}

	public final void addClip(List<List<Point64>> paths) {
		addPaths(paths, PathType.Clip);
	}

	/**
	 * Once subject and clip paths have been assigned (via {@link #addSubject(List)
	 * addSubject}, AddOpenSubject and AddClip methods), <code>Execute()</code> can
	 * then perform the specified clipping operation (intersection, union,
	 * difference or XOR).
	 * <p>
	 * The solution parameter can be either a Paths64 or a PolyTree64, though since
	 * the Paths64 structure is simpler and more easily populated (with clipping
	 * about 5% faster), it should generally be preferred.
	 * <p>
	 * While polygons in solutions should never intersect (either with other
	 * polygons or with themselves), they will frequently be nested such that outer
	 * polygons will contain inner 'hole' polygons with in turn may contain outer
	 * polygons (to any level of nesting). And given that PolyTree64 and PolyTreeD
	 * preserve these parent-child relationships, these two PolyTree classes will be
	 * very useful to some users.
	 * 
	 * @param clipType
	 * @param fillRule
	 * @param solutionClosed
	 * @param solutionOpen
	 * @return
	 */
	public final boolean Execute(ClipType clipType, FillRule fillRule, List<List<Point64>> solutionClosed,
			List<List<Point64>> solutionOpen) {
		solutionClosed.clear();
		solutionOpen.clear();
		ExecuteInternal(clipType, fillRule);
		BuildPaths(solutionClosed, solutionOpen);
		try { // NOTE avoid for now
		} catch (java.lang.Exception e) {
			_succeeded = false;
		}

		ClearSolution();
		return _succeeded;
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, List<List<Point64>> solutionClosed) {
		return Execute(clipType, fillRule, solutionClosed, new ArrayList<>());
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, PolyTree64 polytree, List<List<Point64>> openPaths) {
		polytree.Clear();
		openPaths.clear();
		_using_polytree = true;
		try {
			ExecuteInternal(clipType, fillRule);
			BuildTree(polytree, openPaths);
		} catch (java.lang.Exception e) {
			_succeeded = false;
		}

		ClearSolution();
		return _succeeded;
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, PolyTree64 polytree) {
		return Execute(clipType, fillRule, polytree, new ArrayList<>());
	}

}