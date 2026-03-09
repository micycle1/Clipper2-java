package com.github.micycle1.clipper2.engine;

import com.github.micycle1.clipper2.core.ClipType;
import com.github.micycle1.clipper2.core.FillRule;
import com.github.micycle1.clipper2.core.Paths64;

/**
 * The Clipper class performs boolean 'clipping'. This class is very similar to
 * ClipperD except that coordinates passed to Clipper64 objects are of type
 * <code>long</code> instead of type <code>double</code>.
 */
public class Clipper64 extends ClipperBase {

	/**
	 * Once subject and clip paths have been assigned (via
	 * <code>addSubject()</code>, <code>addOpenSubject()</code> and
	 * <code>addClip()</code> methods),
	 * <code>execute()</code> can then perform the specified clipping operation
	 * (intersection, union, difference or XOR).
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
	 * @param clipType the clipping operation to perform
	 * @param fillRule the fill rule used during clipping
	 * @param solutionClosed receives the closed solution paths
	 * @param solutionOpen receives any open solution paths
	 * @return {@code true} when clipping completed successfully
	 */
	public final boolean execute(ClipType clipType, FillRule fillRule, Paths64 solutionClosed, Paths64 solutionOpen) {
		solutionClosed.clear();
		solutionOpen.clear();
		try {
			ExecuteInternal(clipType, fillRule);
			BuildPaths(solutionClosed, solutionOpen);
		} catch (Exception e) {
			succeeded = false;
		}

		ClearSolutionOnly();
		return succeeded;
	}

	/**
	 * Executes the requested clipping operation and returns only closed solution
	 * paths.
	 * 
	 * @param clipType the clipping operation to perform
	 * @param fillRule the fill rule used during clipping
	 * @param solutionClosed receives the closed solution paths
	 * @return {@code true} when clipping completed successfully
	 */
	public final boolean execute(ClipType clipType, FillRule fillRule, Paths64 solutionClosed) {
		return execute(clipType, fillRule, solutionClosed, new Paths64());
	}

	/**
	 * Executes the requested clipping operation and writes the nested closed-path
	 * result to a {@link PolyTree64}.
	 * 
	 * @param clipType the clipping operation to perform
	 * @param fillRule the fill rule used during clipping
	 * @param polytree receives the closed solution hierarchy
	 * @param openPaths receives any open solution paths
	 * @return {@code true} when clipping completed successfully
	 */
	public final boolean execute(ClipType clipType, FillRule fillRule, PolyTree64 polytree, Paths64 openPaths) {
		polytree.clear();
		openPaths.clear();
		usingPolytree = true;
		try {
			ExecuteInternal(clipType, fillRule);
			BuildTree(polytree, openPaths);
		} catch (Exception e) {
			succeeded = false;
		}

		ClearSolutionOnly();
		return succeeded;
	}

	/**
	 * Executes the requested clipping operation and writes the nested closed-path
	 * result to a {@link PolyTree64}.
	 * 
	 * @param clipType the clipping operation to perform
	 * @param fillRule the fill rule used during clipping
	 * @param polytree receives the closed solution hierarchy
	 * @return {@code true} when clipping completed successfully
	 */
	public final boolean execute(ClipType clipType, FillRule fillRule, PolyTree64 polytree) {
		return execute(clipType, fillRule, polytree, new Paths64());
	}

	/**
	 * Loads preprocessed reusable path data into this clipper instance.
	 * 
	 * @param reuseableData cached path data to load
	 */
	@Override
	public void addReuseableData(ReuseableDataContainer64 reuseableData) {
		super.addReuseableData(reuseableData);
	}

}
