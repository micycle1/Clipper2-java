package clipper2.engine;

import clipper2.core.Point64;

public class Active {
	
	public Point64 bot;
	public Point64 top;
	public long curX; // current (updated at every new scanline)
	public double dx;
	public int windDx; // 1 or -1 depending on winding direction
	public int windCount;
	public int windCount2; // winding count of the opposite polytype
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public OutRec? outrec;
	public OutRec outrec;

	// AEL: 'active edge list' (Vatti's AET - active edge table)
	// a linked list of all edges (from left to right) that are present
	// (or 'active') within the current scanbeam (a horizontal 'beam' that
	// sweeps from bottom to top over the paths in the clipping operation).
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Active? prevInAEL;
	public Active prevInAEL;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Active? nextInAEL;
	public Active nextInAEL;

	// SEL: 'sorted edge list' (Vatti's ST - sorted table)
	// linked list used when sorting edges into their new positions at the
	// top of scanbeams, but also (re)used to process horizontals.
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Active? prevInSEL;
	public Active prevInSEL;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Active? nextInSEL;
	public Active nextInSEL;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Active? jump;
	public Active jump;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Vertex? vertexTop;
	public Vertex vertexTop;
	public LocalMinima localMin = new LocalMinima(); // the bottom of an edge 'bound' (also Vatti)
	public boolean isLeftBound;
}