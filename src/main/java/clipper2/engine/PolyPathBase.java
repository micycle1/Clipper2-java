package clipper2.engine;

import java.util.ArrayList;
import java.util.List;

import clipper2.Nullable;
import clipper2.core.Path64;

public abstract class PolyPathBase implements Iterable<PolyPathBase> {

	@Nullable
	PolyPathBase parent;
	List<PolyPathBase> children = new ArrayList<>();

	PolyPathBase(@Nullable PolyPathBase parent) {
		this.parent = parent;
	}

	PolyPathBase() {
		this(null);
	}

	@Override
	public final NodeIterator iterator() {
		return new NodeIterator(children);
	}

	private int getLevel() {
		int result = 0;
		@Nullable
		PolyPathBase pp = parent;
		while (pp != null) {
			++result;
			pp = pp.parent;
		}
		return result;
	}

	/**
	 * Indicates whether the Polygon property represents a hole or the outer bounds
	 * of a polygon.
	 */
	public final boolean getIsHole() {
		int lvl = getLevel();
		return lvl != 0 && (lvl & 1) == 0;
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

	private String toStringInternal(int idx, int level) {
		int count = children.size();
		String result = "", padding = "", plural = "s";
		if (children.size() == 1) {
			plural = "";
		}
		// Create padding by concatenating spaces
		for (int i = 0; i < level * 2; i++) {
			padding += " ";
		}

		if ((level & 1) == 0) {
			result += String.format("%s+- hole (%d) contains %d nested polygon%s.\n", padding, idx, children.size(), plural);
		} else {
			result += String.format("%s+- polygon (%d) contains %d hole%s.\n", padding, idx, children.size(), plural);
		}
		for (int i = 0; i < count; i++) {
			if (children.get(i).getCount() > 0) {
				result += children.get(i).toStringInternal(i, level + 1);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		int count = children.size();
		if (getLevel() > 0) {
			return ""; // only accept tree root
		}
		String plural = "s";
		if (children.size() == 1) {
			plural = "";
		}
		String result = String.format("Polytree with %d polygon%s.\n", children.size(), plural);
		for (int i = 0; i < count; i++) {
			if (children.get(i).getCount() > 0) {
				result += children.get(i).toStringInternal(i, 1);
			}
		}
		return result + '\n';
	}

}