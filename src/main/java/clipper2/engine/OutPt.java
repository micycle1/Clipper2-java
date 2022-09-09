package clipper2.engine;

import clipper2.core.Point64;

// OutPt: vertex data structure for clipping solutions
public class OutPt {
	
	public Point64 pt;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public OutPt? next;
	public OutPt next;
	public OutPt prev;
	public OutRec outrec;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Joiner? joiner;
	public Joiner joiner;

	public OutPt(Point64 pt, OutRec outrec) {
		this.pt = pt;
		this.outrec = outrec;
		next = this;
		prev = this;
		joiner = null;
	}
}