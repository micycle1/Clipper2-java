package clipper2.engine;

import java.util.ArrayList;
import java.util.List;

import clipper2.Nullable;
import clipper2.core.Path64;
import clipper2.core.Point64;
import clipper2.core.Rect64;

/**
 * Path data structure for clipping solutions.
 */
public class OutRec {

	public int idx;
	@Nullable
	public OutRec owner;
	@Nullable
	public List<OutRec> splits;
	@Nullable
	public Active frontEdge;
	@Nullable
	public Active backEdge;
	@Nullable
	public OutPt pts;
	@Nullable
	public PolyPathBase polypath;
	public Rect64 bounds;
	public Path64 path;
	public boolean isOpen;

	public OutRec() {
		bounds = new Rect64();
		path = new Path64();
	}

}