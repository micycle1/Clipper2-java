package clipper2.engine;

import clipper2.core.Point64;

// Vertex: a pre-clipping data structure. It is used to separate polygons
// into ascending and descending 'bounds' (or sides) that start at local
// minima and ascend to a local maxima, before descending again.
public class Vertex {

	public Point64 pt = new Point64();
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Vertex? next;
	public Vertex next;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Vertex? prev;
	public Vertex prev;
	public VertexFlags flags;

//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Vertex(Point64 pt, VertexFlags flags, Vertex? prev)
	public Vertex(Point64 pt, VertexFlags flags, Vertex prev) {
		this.pt = pt.clone();
		this.flags = flags;
		next = null;
		this.prev = prev;
	}
}