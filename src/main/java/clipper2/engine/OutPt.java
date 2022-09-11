package clipper2.engine;

import clipper2.Nullable;
import clipper2.core.Point64;

/**
 * Vertex data structure for clipping solutions
 */
public class OutPt {

	public Point64 pt;
	@Nullable
	public OutPt next;
	public OutPt prev;
	public OutRec outrec;
	@Nullable
	public Joiner joiner;

	public OutPt(Point64 pt, OutRec outrec) {
		this.pt = pt;
		this.outrec = outrec;
		next = this;
		prev = this;
		joiner = null;
	}

}