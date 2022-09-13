package clipper2.engine;

import clipper2.Clipper;
import clipper2.Nullable;
import clipper2.core.Path64;
import clipper2.core.PathD;

public class PolyPathD extends PolyPathBase {

	private PathD Polygon;
	private double Scale;

	public final PathD getPolygon() {
		return Polygon;
	}

	private void setPolygon(PathD value) {
		Polygon = value;
	}
	
	public double getScale() {
		return Scale;
	}
	public final void setScale(double value) {
		Scale = value;
	}

	public PolyPathD() {
		this(null);
	}

	public PolyPathD(@Nullable PolyPathBase parent) {
		super(parent);
	}

	@Override
	public PolyPathBase AddChild(Path64 p) {
		PolyPathD newChild = new PolyPathD(this);
		newChild.setScale(Scale);
		newChild.setPolygon(Clipper.ScalePathD(p, Scale));
		_childs.add(newChild);
		return newChild;
	}

	public final PolyPathD get(int index) {
		if (index < 0 || index >= _childs.size()) {
			throw new IllegalStateException();
		}
		return (PolyPathD) _childs.get(index);
	}

	public final double Area() {
		double result = getPolygon() == null ? 0 : Clipper.Area(getPolygon());
		for (var polyPathBase : _childs) {
			PolyPathD child = (PolyPathD) polyPathBase;
			result += child.Area();
		}
		return result;
	}
}