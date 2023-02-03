package clipper2.engine;

import clipper2.Clipper;
import clipper2.Nullable;
import clipper2.core.Path64;
import clipper2.core.PathD;

public class PolyPathD extends PolyPathNode {

	private PathD polygon;
	private double scale;

	PolyPathD() {
		this(null);
	}

	PolyPathD(@Nullable PolyPathNode parent) {
		super(parent);
	}

	@Override
	public PolyPathNode AddChild(Path64 p) {
		PolyPathD newChild = new PolyPathD(this);
		newChild.setScale(scale);
		newChild.setPolygon(Clipper.ScalePathD(p, scale));
		children.add(newChild);
		return newChild;
	}

	public final PolyPathD get(int index) {
		if (index < 0 || index >= children.size()) {
			throw new IllegalStateException();
		}
		return (PolyPathD) children.get(index);
	}

	public final double Area() {
		double result = getPolygon() == null ? 0 : Clipper.Area(getPolygon());
		for (PolyPathNode polyPathBase : children) {
			PolyPathD child = (PolyPathD) polyPathBase;
			result += child.Area();
		}
		return result;
	}

	public final PathD getPolygon() {
		return polygon;
	}

	private void setPolygon(PathD value) {
		polygon = value;
	}

	public double getScale() {
		return scale;
	}

	public final void setScale(double value) {
		scale = value;
	}
}