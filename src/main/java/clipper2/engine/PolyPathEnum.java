package clipper2.engine;

import java.util.Iterator;
import java.util.List;

public class PolyPathEnum implements Iterator<PolyPathBase> {

	public List<PolyPathBase> _ppbList;
	private int position = -1;

	public PolyPathEnum(List<PolyPathBase> childs) {
		_ppbList = childs;
	}

	@Override
	public final boolean hasNext() {
		return (position < _ppbList.size());
	}

	@Override
	public PolyPathBase next() {
		position++;
		if (position < 0 || position >= _ppbList.size()) {
			throw new IllegalStateException();
		}
		return _ppbList.get(position);
	}

}