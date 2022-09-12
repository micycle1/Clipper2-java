package clipper2.engine;

import java.util.ArrayList;
import java.util.List;

import clipper2.core.Point64;

public abstract class PolyPathBase implements java.lang.Iterable<PolyPathBase> {

	public PolyPathBase _parent;
	public List<PolyPathBase> _childs = new ArrayList<>();

	public PolyPathBase(PolyPathBase parent) {
		_parent = parent;
	}

	public PolyPathBase() {
		this(null);
	}

	@Override
	public final PolyPathEnum iterator() {
		return new PolyPathEnum(_childs);
	}

	public final boolean getIsHole() {
		return GetIsHole();
	}

	private boolean GetIsHole() {
		boolean result = true;
		PolyPathBase pp = _parent;
		while (pp != null) {
			result = !result;
			pp = pp._parent;
		}

		return result;
	}

	public final int getCount() {
		return _childs.size();
	}

	public abstract PolyPathBase AddChild(List<Point64> p);

	public final void Clear() {
		_childs.clear();
	}
}