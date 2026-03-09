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
			LowestPathInfo lowInfo = GetLowestPathInfo(inPaths);
			lowestPathIdx = lowInfo.idx;

			// the lowermost path must be an outer path, so if its orientation is negative,
			// then flag that the whole group is 'reversed' (will negate delta etc.)
			// as this is much more efficient than reversing every path.
			pathsReversed = (lowestPathIdx >= 0) && lowInfo.isNegArea;
		} else {
			lowestPathIdx = -1;
			pathsReversed = false;
		}
	}

	private static final class LowestPathInfo {
		int idx = -1;
		boolean isNegArea = false;
	}

	private static LowestPathInfo GetLowestPathInfo(Paths64 paths) {
		LowestPathInfo result = new LowestPathInfo();
		Point64 botPt = new Point64(Long.MAX_VALUE, Long.MIN_VALUE);
		for (int i = 0; i < paths.size(); i++) {
			double area = Double.MAX_VALUE;
			for (Point64 pt : paths.get(i)) {
				if (pt.y < botPt.y || (pt.y == botPt.y && pt.x >= botPt.x)) {
					continue;
				}
				if (area == Double.MAX_VALUE) {
					area = Clipper.Area(paths.get(i));
					if (area == 0) {
						break; // invalid closed path
					}
					result.isNegArea = area < 0;
				}
				result.idx = i;
				botPt.x = pt.x;
				botPt.y = pt.y;
			}
		}
		return result;
	}

}
