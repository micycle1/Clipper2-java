package clipper2.engine;

import java.util.*;

import clipper2.Clipper;
import clipper2.Nullable;
import clipper2.core.Point64;

public class PolyPath64 extends PolyPathBase {

	private List<Point64> Polygon;

	public final List<Point64> getPolygon() {
		return Polygon;
	}

	private void setPolygon(List<Point64> value) {
		Polygon = value;
	}

	public PolyPath64() {
		this(null);
	}

	public PolyPath64(@Nullable PolyPathBase parent) {
		super(parent);
	}

	@Override
	public PolyPathBase AddChild(List<Point64> p) {
		PolyPath64 newChild = new PolyPath64(this);
		newChild.setPolygon(p);
		_childs.add(newChild);
		return newChild;
	}

	public final PolyPath64 get(int index) {
		if (index < 0 || index >= _childs.size()) {
			throw new IllegalStateException();
		}
		return (PolyPath64) _childs.get(index);
	}

	public final double Area() {
		double result = getPolygon() == null ? 0 : Clipper.AreaPath(getPolygon());
		for (var polyPathBase : _childs) {
			PolyPath64 child = (PolyPath64) polyPathBase;
			result += child.Area();
		}
		return result;
	}
}