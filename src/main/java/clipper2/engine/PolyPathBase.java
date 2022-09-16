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
	public final PolyPathEnum iterator() {
		return new PolyPathEnum(children);
	}

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

	public final int getCount() {
		return children.size();
	}

	public abstract PolyPathBase AddChild(Path64 p);

	public final void Clear() {
		children.clear();
	}
}