package clipper2.engine;

import clipper2.core.Point64;

// IntersectNode: a structure representing 2 intersecting edges.
// Intersections must be sorted so they are processed from the largest
// Y coordinates to the smallest while keeping edges adjacent.
public final class IntersectNode {

	public Point64 pt = new Point64();
	public Active edge1;
	public Active edge2;

	public IntersectNode() {
	}

	public IntersectNode(Point64 pt, Active edge1, Active edge2) {
		this.pt = pt.clone();
		this.edge1 = edge1;
		this.edge2 = edge2;
	}

	public IntersectNode clone() {
		IntersectNode varCopy = new IntersectNode();

		varCopy.pt = this.pt.clone();
		varCopy.edge1 = this.edge1;
		varCopy.edge2 = this.edge2;

		return varCopy;
	}
}