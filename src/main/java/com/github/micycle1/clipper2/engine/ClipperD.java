package com.github.micycle1.clipper2.engine;

import com.github.micycle1.clipper2.Clipper;
import com.github.micycle1.clipper2.core.ClipType;
import com.github.micycle1.clipper2.core.FillRule;
import com.github.micycle1.clipper2.core.Path64;
import com.github.micycle1.clipper2.core.PathD;
import com.github.micycle1.clipper2.core.PathType;
import com.github.micycle1.clipper2.core.Paths64;
import com.github.micycle1.clipper2.core.PathsD;

/**
 * The ClipperD class performs boolean 'clipping'. This class is very similar to
 * Clipper64 except that coordinates passed to ClipperD objects are of type
 * <code>double</code> instead of type <code>long</code>.
 */
public class ClipperD extends ClipperBase {

	private static final String PRECISION_RANGE_ERROR = "Error: Precision is out of range.";
	private double scale;
	private double invScale;

	public ClipperD() {
		this(2);
	}

	/**
	 * @param roundingDecimalPrecision default = 2
	 */
	public ClipperD(int roundingDecimalPrecision) {
		if (roundingDecimalPrecision < -8 || roundingDecimalPrecision > 8) {
			throw new IllegalArgumentException(PRECISION_RANGE_ERROR);
		}
		scale = Math.pow(10, roundingDecimalPrecision);
		invScale = 1 / scale;
	}

	public void addPath(PathD path, PathType polytype) {
		addPath(path, polytype, false);
	}

	public void addPath(PathD path, PathType polytype, boolean isOpen) {
		super.addPath(Clipper.scalePath64(path, scale), polytype, isOpen);
	}

	public void addPaths(PathsD paths, PathType polytype) {
		addPaths(paths, polytype, false);
	}

	public void addPaths(PathsD paths, PathType polytype, boolean isOpen) {
		super.addPaths(Clipper.scalePaths64(paths, scale), polytype, isOpen);
	}

	public void addSubject(PathD path) {
		addPath(path, PathType.Subject);
	}

	public void addOpenSubject(PathD path) {
		addPath(path, PathType.Subject, true);
	}

	public void addClip(PathD path) {
		addPath(path, PathType.Clip);
	}

	public void addSubjects(PathsD paths) {
		addPaths(paths, PathType.Subject);
	}

	public void addOpenSubjects(PathsD paths) {
		addPaths(paths, PathType.Subject, true);
	}

	public void addClips(PathsD paths) {
		addPaths(paths, PathType.Clip);
	}

	public boolean execute(ClipType clipType, FillRule fillRule, PathsD solutionClosed, PathsD solutionOpen) {
		Paths64 solClosed64 = new Paths64(), solOpen64 = new Paths64();

		boolean success = true;
		solutionClosed.clear();
		solutionOpen.clear();
		try {
			ExecuteInternal(clipType, fillRule);
			BuildPaths(solClosed64, solOpen64);
		} catch (Exception e) {
			success = false;
		}

		ClearSolutionOnly();
		if (!success) {
			return false;
		}

		solutionClosed.ensureCapacity(solClosed64.size());
		for (Path64 path : solClosed64) {
			solutionClosed.add(Clipper.scalePathD(path, invScale));
		}
		solutionOpen.ensureCapacity(solOpen64.size());
		for (Path64 path : solOpen64) {
			solutionOpen.add(Clipper.scalePathD(path, invScale));
		}

		return true;
	}

	public boolean execute(ClipType clipType, FillRule fillRule, PathsD solutionClosed) {
		return execute(clipType, fillRule, solutionClosed, new PathsD());
	}

	public boolean execute(ClipType clipType, FillRule fillRule, PolyTreeD polytree, PathsD openPaths) {
		polytree.clear();
		polytree.setScale(invScale);
		openPaths.clear();
		Paths64 oPaths = new Paths64();
		boolean success = true;
		try {
			ExecuteInternal(clipType, fillRule);
			BuildTree(polytree, oPaths);
		} catch (Exception e) {
			success = false;
		}
		ClearSolutionOnly();
		if (!success) {
			return false;
		}
		if (oPaths.isEmpty()) {
			return true;
		}
		openPaths.ensureCapacity(oPaths.size());
		for (Path64 path : oPaths) {
			openPaths.add(Clipper.scalePathD(path, invScale));
		}
		return true;
	}

	public boolean execute(ClipType clipType, FillRule fillRule, PolyTreeD polytree) {
		return execute(clipType, fillRule, polytree, new PathsD());
	}

}
