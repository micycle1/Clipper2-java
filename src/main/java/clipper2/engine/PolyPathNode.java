package clipper2.engine;

import clipper2.Nullable;
import clipper2.core.Path64;

import java.util.ArrayList;
import java.util.List;

public abstract class PolyPathNode implements Iterable<PolyPathNode> {

	@Nullable
	final PolyPathNode parent;
	List<PolyPathNode> children = new ArrayList<>();

	PolyPathNode(PolyPathNode parent) {
		this.parent = parent;
	}

	PolyPathNode() {
		this(null);
	}

	@Override
	public final NodeIterator iterator() {
		return new NodeIterator(children);
	}

	/**
	 * Indicates whether the Polygon property represents a hole or the outer bounds
	 * of a polygon.
	 */
	public final boolean getIsHole() {
		boolean result = true;
		PolyPathNode pp = parent;
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

	public abstract PolyPathNode AddChild(Path64 p);

	/**
	 * This method clears the Polygon and deletes any contained children.
	 */
	public final void Clear() {
		children.clear();
	}
}