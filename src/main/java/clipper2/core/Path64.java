package clipper2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class Path64 extends ArrayList<Point64> {
	
	public Path64() {
		super();
	}
	
	public Path64(int n) {
		super(n);
	}
	
	public Path64(List<Point64> path) {
		super(path);
	}
	
	public Path64(Point64... path) {
		super(Arrays.asList(path));
	}

}
