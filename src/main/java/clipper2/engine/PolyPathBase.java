package clipper2.engine;

import java.util.ArrayList;
import java.util.List;

import clipper2.core.Path64;

public abstract class PolyPathBase implements Iterable<PolyPathBase> {

	public PolyPathBase parent;
	public List<PolyPathBase> children = new ArrayList<>();

	PolyPathBase(PolyPathBase parent) {
		this.parent = parent;
	}

	PolyPathBase() {
		this(null);
	}

	@Override
	public final PolyPathIterator iterator() {
		return new PolyPathIterator(children);
	}

	/**
	 * Indicates whether the Polygon property represents a hole or the outer bounds
	 * of a polygon.
	 */
	public final boolean getIsHole() {
		return GetIsHole();
	}

	private boolean GetIsHole() {
		boolean result = true;
		PolyPathBase pp = parent;
		while (pp != null) {
			result = !result;
			pp = pp.parent;
		}

		return result;
	}

	/**
	 * Indicates the number of contained children.
	 */
	public final int getCount() {
		return children.size();
	}

	public abstract PolyPathBase AddChild(Path64 p);

	/**
	 * This method clears the Polygon and deletes any contained children.
	 */
	public final void Clear() {
		children.clear();
	}
}