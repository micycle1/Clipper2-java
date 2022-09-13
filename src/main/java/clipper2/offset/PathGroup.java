package clipper2.offset;

import java.util.ArrayList;
import java.util.List;

import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;

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

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public PathGroup(Paths64 paths, JoinType joinType, EndType endType = EndType.Polygon)
	public PathGroup(Paths64 paths, JoinType joinType, EndType endType) {
		_inPaths = new Paths64(paths);
		_joinType = joinType;
		_endType = endType;
		_outPath = new Path64();
		_outPaths = new Paths64();
		_pathsReversed = false;
	}
}