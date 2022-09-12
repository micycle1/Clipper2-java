package clipper2;

import java.util.ArrayList;
import java.util.List;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.PathType;
import clipper2.core.Point64;
import clipper2.core.PointD;

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

	public final void AddPathD(List<PointD> path, PathType polytype) {
		AddPathD(path, polytype, false);
	}

	public final void AddPathD(List<PointD> path, PathType polytype, boolean isOpen) {
		super.AddPath(Clipper.ScalePath64(path, _scale), polytype, isOpen);
	}

	public final void AddPathsD(List<List<PointD>> paths, PathType polytype) {
		AddPathsD(paths, polytype, false);
	}

	public final void AddPathsD(List<List<PointD>> paths, PathType polytype, boolean isOpen) {
		super.AddPaths(Clipper.ScalePaths64(paths, _scale), polytype, isOpen);
	}

	public final void AddSubjectD(List<PointD> path) {
		AddPathD(path, PathType.Subject);
	}

	public final void AddOpenSubjectD(List<PointD> path) {
		AddPathD(path, PathType.Subject, true);
	}

	public final void AddClipD(List<PointD> path) {
		AddPathD(path, PathType.Clip);
	}

	public final void AddSubjectsD(List<List<PointD>> paths) {
		AddPathsD(paths, PathType.Subject);
	}

	public final void AddOpenSubjectsD(List<List<PointD>> paths) {
		AddPathsD(paths, PathType.Subject, true);
	}

	public final void AddClipsD(List<List<PointD>> paths) {
		AddPathsD(paths, PathType.Clip);
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, List<List<PointD>> solutionClosed, List<List<PointD>> solutionOpen) {
		List<List<Point64>> solClosed64 = new ArrayList<>(), solOpen64 = new ArrayList<>();

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

		for (List<Point64> path : solClosed64) {
			solutionClosed.add(Clipper.ScalePathD(path, _invScale));
		}
		for (List<Point64> path : solOpen64) {
			solutionOpen.add(Clipper.ScalePathD(path, _invScale));
		}

		return true;
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, List<List<PointD>> solutionClosed) {
		return Execute(clipType, fillRule, solutionClosed, new ArrayList<>());
	}

}