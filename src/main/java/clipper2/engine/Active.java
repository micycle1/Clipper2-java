package clipper2.engine;

import clipper2.Nullable;
import clipper2.core.Point64;

public class Active {

	public Point64 bot;
	public Point64 top;
	public long curX; // current (updated at every new scanline)
	public double dx;
	public int windDx; // 1 or -1 depending on winding direction
	public int windCount;
	public int windCount2; // winding count of the opposite polytype
	@Nullable
	public OutRec outrec;
	// AEL: 'active edge list' (Vatti's AET - active edge table)
	// a linked list of all edges (from left to right) that are present
	// (or 'active') within the current scanbeam (a horizontal 'beam' that
	// sweeps from bottom to top over the paths in the clipping operation).
	@Nullable
	public Active prevInAEL;
	@Nullable
	public Active nextInAEL;
	// SEL: 'sorted edge list' (Vatti's ST - sorted table)
	// linked list used when sorting edges into their new positions at the
	// top of scanbeams, but also (re)used to process horizontals.
	@Nullable
	public Active prevInSEL;
	@Nullable
	public Active nextInSEL;
	@Nullable
	public Active jump;
	@Nullable
	public Vertex vertexTop;
	public LocalMinima localMin = new LocalMinima(); // the bottom of an edge 'bound' (also Vatti)
	public boolean isLeftBound;
}