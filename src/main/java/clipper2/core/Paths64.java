package clipper2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class Paths64 extends ArrayList<Path64> {

	public Paths64() {
		super();
	}

	public Paths64(int n) {
		super(n);
	}

	public Paths64(List<Path64> paths) {
		super(paths);
	}

	public Paths64(Path64... paths) {
		super(Arrays.asList(paths));
	}

}
