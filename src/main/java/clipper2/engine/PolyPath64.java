package clipper2.engine;

import clipper2.Clipper;
import clipper2.Nullable;
import clipper2.core.Path64;

public class PolyPath64 extends PolyPathBase {

	private Path64 Polygon;

	public final Path64 getPolygon() {
		return Polygon;
	}

	private void setPolygon(Path64 value) {
		Polygon = value;
	}

	public PolyPath64() {
		this(null);
	}

	public PolyPath64(@Nullable PolyPathBase parent) {
		super(parent);
	}

	@Override
	public PolyPathBase AddChild(Path64 p) {
		PolyPath64 newChild = new PolyPath64(this);
		newChild.setPolygon(p);
		children.add(newChild);
		return newChild;
	}

	public final PolyPath64 get(int index) {
		if (index < 0 || index >= children.size()) {
			throw new IllegalStateException();
		}
		return (PolyPath64) children.get(index);
	}

	public final double Area() {
		double result = getPolygon() == null ? 0 : Clipper.Area(getPolygon());
		for (var polyPathBase : children) {
			PolyPath64 child = (PolyPath64) polyPathBase;
			result += child.Area();
		}
		return result;
	}
}