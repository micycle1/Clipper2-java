package clipper2.offset;

import clipper2.core.Path64;
import clipper2.core.Paths64;

public class PathGroup {

	public Paths64 inPaths;
	public Path64 outPath;
	public Paths64 outPaths;
	public JoinType joinType;
	public EndType endType;
	public boolean pathsReversed;

	public PathGroup(Paths64 paths, JoinType joinType) {
		this(paths, joinType, EndType.Polygon);
	}

	public PathGroup(Paths64 paths, JoinType joinType, EndType endType) {
		inPaths = new Paths64(paths);
		this.joinType = joinType;
		this.endType = endType;
		outPath = new Path64();
		outPaths = new Paths64();
		pathsReversed = false;
	}
}