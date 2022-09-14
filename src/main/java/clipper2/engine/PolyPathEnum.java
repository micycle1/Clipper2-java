package clipper2.engine;

import java.util.Iterator;
import java.util.List;

public class PolyPathEnum implements Iterator<PolyPathBase> {

	public List<PolyPathBase> ppbList;
	private int position = -1;

	public PolyPathEnum(List<PolyPathBase> childs) {
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