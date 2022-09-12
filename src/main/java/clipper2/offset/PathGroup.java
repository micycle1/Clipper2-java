package clipper2.offset;

import java.util.*;

import clipper2.core.Point64;

  public class PathGroup {
	  
	public List<List<Point64>> _inPaths;
	public List<Point64> _outPath;
	public List<List<Point64>> _outPaths;
	public JoinType _joinType;
	public EndType _endType;
	public boolean _pathsReversed;


	public PathGroup(List<List<Point64>> paths, JoinType joinType) {
		this(paths, joinType, EndType.Polygon);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public PathGroup(List<List<Point64>> paths, JoinType joinType, EndType endType = EndType.Polygon)
	public PathGroup(List<List<Point64>> paths, JoinType joinType, EndType endType) {
	  _inPaths = new ArrayList<>(paths);
	  _joinType = joinType;
	  _endType = endType;
	  _outPath = new ArrayList<>();
	  _outPaths = new ArrayList<>();
	  _pathsReversed = false;
	}
  }