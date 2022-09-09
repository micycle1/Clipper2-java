package clipper2.engine;

import java.util.ArrayList;

import clipper2.core.Point64;
import clipper2.core.Rect64;

// OutRec: path data structure for clipping solutions
public class OutRec {
	public int idx;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public OutRec? owner;
	public OutRec owner;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public List<OutRec>? splits;
	public ArrayList<OutRec> splits;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Active? frontEdge;
	public Active frontEdge;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Active? backEdge;
	public Active backEdge;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public OutPt? pts;
	public OutPt pts;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public PolyPathBase? polypath;
	public PolyPathBase polypath;
	public Rect64 bounds;
	public ArrayList<Point64> path;
	public boolean isOpen;

	public OutRec() {
		bounds = new Rect64();
		path = new ArrayList<Point64>();
	}
}