package clipper2.offset;

import clipper2.core.Path64;
import clipper2.core.Paths64;

public class PathGroup {

	public Paths64 _inPaths;
	public Path64 _outPath;
	public Paths64 _outPaths;
	public JoinType _joinType;
	public EndType _endType;
	public boolean _pathsReversed;

	public PathGroup(Paths64 paths, JoinType joinType) {
		this(paths, joinType, EndType.Polygon);
	}

	public PathGroup(Paths64 paths, JoinType joinType, EndType endType) {
		_inPaths = new Paths64(paths);
		_joinType = joinType;
		_endType = endType;
		_outPath = new Path64();
		_outPaths = new Paths64();
		_pathsReversed = false;
	}
}