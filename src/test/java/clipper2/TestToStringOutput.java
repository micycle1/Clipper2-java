package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import clipper2.core.Path64;
import clipper2.core.PathD;
import clipper2.core.Paths64;
import clipper2.core.PathsD;
import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.engine.PolyPathBase;
import clipper2.engine.PolyTree64;

class TestToStringOutput {

	@Test
	void path64ToStringMatchesExistingFormat() {
		Path64 path = new Path64(new Point64(1, 2), new Point64(3, 4));
		assertEquals("(1,2) , (3,4) ", path.toString());
	}

	@Test
	void pathDToStringMatchesExistingFormat() {
		PathD path = new PathD(2);
		path.add(new PointD(1.5, 2.5));
		path.add(new PointD(3.5, 4.5));
		assertEquals("(1.500000,2.500000) , (3.500000,4.500000) ", path.toString());
	}

	@Test
	void paths64ToStringMatchesExistingFormat() {
		Paths64 paths = new Paths64(new Path64(new Point64(1, 2)));
		assertEquals("(1,2) \n", paths.toString());
	}

	@Test
	void pathsDToStringMatchesExistingFormat() {
		PathsD paths = new PathsD(1);
		PathD path = new PathD(1);
		path.add(new PointD(1.5, 2.5));
		paths.add(path);
		assertEquals("(1.500000,2.500000) \n", paths.toString());
	}

	@Test
	void polyPathBaseToStringMatchesExistingFormat() {
		PolyTree64 tree = new PolyTree64();
		PolyPathBase polygon = tree.AddChild(new Path64());
		polygon.AddChild(new Path64());
		assertEquals("Polytree with 1 polygon.\n  +- polygon (0) contains 1 hole.\n\n", tree.toString());
	}
}
