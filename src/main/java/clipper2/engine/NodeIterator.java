package clipper2.engine;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class NodeIterator implements Iterator<PolyPathNode> {

	List<PolyPathNode> ppbList;
	int position = 0;

	NodeIterator(List<PolyPathNode> childs) {
		ppbList = childs;
	}

	@Override
	public final boolean hasNext() {
		return (position < ppbList.size());
	}

	@Override
	public PolyPathNode next() {
		if (position < 0 || position >= ppbList.size()) {
			throw new NoSuchElementException();
		}
		return ppbList.get(position++);
	}

}