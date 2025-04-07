package clipper2.offset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import clipper2.Clipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;

class Group {

	Paths64 inPaths;
	List<Rect64> boundsList;
	List<Boolean> isHoleList;
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

		// get bounds of each path --> boundsList
		boundsList = new ArrayList<>(inPaths.size());
		GetMultiBounds(inPaths, boundsList);

		if (endType == EndType.Polygon) {
			lowestPathIdx = GetLowestPathIdx(boundsList);
			isHoleList = new ArrayList<>(inPaths.size());

			for (Path64 path : inPaths) {
				isHoleList.add(Clipper.Area(path) < 0);
			}

			// the lowermost path must be an outer path, so if its orientation is negative,
			// then flag that the whole group is 'reversed' (will negate delta etc.)
			// as this is much more efficient than reversing every path.
			pathsReversed = (lowestPathIdx >= 0) && isHoleList.get(lowestPathIdx);
			if (pathsReversed) {
				for (int i = 0; i < isHoleList.size(); i++) {
					isHoleList.set(i, !isHoleList.get(i));
				}
			}
		} else {
			lowestPathIdx = -1;
			isHoleList = new ArrayList<>(Collections.nCopies(inPaths.size(), false));
			pathsReversed = false;
		}
	}

	private static void GetMultiBounds(Paths64 paths, List<Rect64> boundsList) {
		for (Path64 path : paths) {
			if (path.size() < 1) {
				boundsList.add(Clipper.InvalidRect64.clone());
				continue;
			}

			Point64 pt1 = path.get(0);
			Rect64 r = new Rect64(pt1.x, pt1.y, pt1.x, pt1.y);

			for (Point64 pt : path) {
				if (pt.y > r.bottom) {
					r.bottom = pt.y;
				} else if (pt.y < r.top) {
					r.top = pt.y;
				}
				if (pt.x > r.right) {
					r.right = pt.x;
				} else if (pt.x < r.left) {
					r.left = pt.x;
				}
			}

			boundsList.add(r);
		}
	}

	private static int GetLowestPathIdx(List<Rect64> boundsList) {
		int result = -1;
		Point64 botPt = new Point64(Long.MAX_VALUE, Long.MIN_VALUE);
		for (int i = 0; i < boundsList.size(); i++) {
			Rect64 r = boundsList.get(i);
			if (!r.IsValid()) {
				continue; // ignore invalid paths
			} else if (r.bottom > botPt.y || (r.bottom == botPt.y && r.left < botPt.x)) {
				botPt = new Point64(r.left, r.bottom);
				result = i;
			}
		}
		return result;
	}

}