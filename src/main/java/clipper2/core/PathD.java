package clipper2.core;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class PathD extends ArrayList<PointD> {
	
	public PathD() {
		super();
	}
	
	public PathD(int n) {
		super(n);
	}
	
	public PathD(List<PointD> path) {
		super(path);
	}

}
