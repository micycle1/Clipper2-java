package clipper2.offset;

import java.util.ArrayList;

import clipper2.Clipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;

class Group {

	Paths64 inPaths;
	JoinType joinType;
	EndType endType;
	boolean pathsReversed;
	int lowestPathIdx;

	Group(Paths64 paths, JoinType joinType) {
		this(paths, joinType, EndType.Polygon);
	}

	Group(Paths64 paths, JoinType joinType, EndType endType) {
		this.joinType = joinType;
		this.endType = endType;

		boolean isJoined = ((endType == EndType.Polygon) || (endType == EndType.Joined));
		inPaths = new Paths64(paths.size());

		for (Path64 path : paths) {
			inPaths.add(Clipper.StripDuplicates(path, isJoined));
		}

		if (endType == EndType.Polygon) {
			lowestPathIdx = GetLowestPathIdx(inPaths);
			pathsReversed = (lowestPathIdx >= 0) && (Clipper.Area(inPaths.get(lowestPathIdx)) < 0);
		} else {
			lowestPathIdx = -1;
			pathsReversed = false;
		}
	}

	private static int GetLowestPathIdx(Paths64 paths) {
		int result = -1;
		Point64 botPt = new Point64(Long.MAX_VALUE, Long.MIN_VALUE);
		for (int i = 0; i < paths.size(); i++) {
			for (Point64 pt : paths.get(i)) {
				if (pt.y < botPt.y || (pt.y == botPt.y && pt.x >= botPt.x)) {
					continue;
				}
				result = i;
				botPt = new Point64(pt);
			}
		}
		return result;
	}

}
