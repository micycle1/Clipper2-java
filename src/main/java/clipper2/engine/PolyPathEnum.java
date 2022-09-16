package clipper2.engine;

import java.util.Iterator;
import java.util.List;

class PolyPathEnum implements Iterator<PolyPathBase> {

	List<PolyPathBase> ppbList;
	int position = -1;

	PolyPathEnum(List<PolyPathBase> childs) {
		ppbList = childs;
	}

	@Override
	public final boolean hasNext() {
		return (position < ppbList.size());
	}

	@Override
	public PolyPathBase next() {
		position++;
		if (position < 0 || position >= ppbList.size()) {
			throw new IllegalStateException();
		}
		return ppbList.get(position);
	}

}