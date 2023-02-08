package clipper2.engine;

import clipper2.Clipper;
import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.PathD;
import clipper2.core.PathType;
import clipper2.core.Paths64;
import clipper2.core.PathsD;

/**
 * The ClipperD class performs boolean 'clipping'. This class is very similar to
 * Clipper64 except that coordinates passed to ClipperD objects are of type
 * <code>double</code> instead of type <code>long</code>.
 */
public class ClipperD extends ClipperBase {

	private double _scale;
	private double _invScale;

	public ClipperD() {
		this(2);
	}

	/**
	 *
	 * @param roundingDecimalPrecision default = 2
	 */
	public ClipperD(int roundingDecimalPrecision) {
		if (roundingDecimalPrecision < -8 || roundingDecimalPrecision > 8) {
			throw new IllegalArgumentException("Error - RoundingDecimalPrecision exceeds the allowed range.");
		}
		_scale = Math.pow(10, roundingDecimalPrecision);
		_invScale = 1 / _scale;
	}

	public final void AddPathD(PathD path, PathType polytype) {
		AddPathD(path, polytype, false);
	}

	public final void AddPathD(PathD path, PathType polytype, boolean isOpen) {
		super.AddPath(Clipper.ScalePath64(path, _scale), polytype, isOpen);
	}

	public final void AddPathsD(PathsD paths, PathType polytype) {
		AddPathsD(paths, polytype, false);
	}

	public final void AddPathsD(PathsD paths, PathType polytype, boolean isOpen) {
		super.AddPaths(Clipper.ScalePaths64(paths, _scale), polytype, isOpen);
	}

	public final void AddSubjectD(PathD path) {
		AddPathD(path, PathType.Subject);
	}

	public final void AddOpenSubjectD(PathD path) {
		AddPathD(path, PathType.Subject, true);
	}

	public final void AddClipD(PathD path) {
		AddPathD(path, PathType.Clip);
	}

	public final void AddSubjectsD(PathsD paths) {
		AddPathsD(paths, PathType.Subject);
	}

	public final void AddOpenSubjectsD(PathsD paths) {
		AddPathsD(paths, PathType.Subject, true);
	}

	public final void AddClipsD(PathsD paths) {
		AddPathsD(paths, PathType.Clip);
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, PathsD solutionClosed, PathsD solutionOpen) {
		Paths64 solClosed64 = new Paths64(), solOpen64 = new Paths64();

		boolean success = true;
		solutionClosed.clear();
		solutionOpen.clear();
		try {
			ExecuteInternal(clipType, fillRule);
			BuildPaths(solClosed64, solOpen64);
		} catch (java.lang.Exception e) {
			success = false;
		}

		ClearSolution();
		if (!success) {
			return false;
		}

		for (Path64 path : solClosed64) {
			solutionClosed.add(Clipper.ScalePathD(path, _invScale));
		}
		for (Path64 path : solOpen64) {
			solutionOpen.add(Clipper.ScalePathD(path, _invScale));
		}

		return true;
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, PathsD solutionClosed) {
		return Execute(clipType, fillRule, solutionClosed, new PathsD());
	}

}