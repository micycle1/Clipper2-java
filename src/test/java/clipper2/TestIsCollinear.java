package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.InternalClipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.engine.Clipper64;

class TestIsCollinear {

	@Test
	void testIsCollinear() {
		// A large integer not representable exactly by double.
		final long i = 9007199254740993L;
		Point64 pt1 = new Point64(0, 0);
		Point64 sharedPt = new Point64(i, i * 10);
		Point64 pt2 = new Point64(i * 10, i * 100);
		assertTrue(InternalClipper.IsCollinear(pt1, sharedPt, pt2));
	}

	@Test
	void testIsCollinear2() { // see #831
		final long i = 0x4000000000000L;
		Path64 subject = new Path64(new Point64(-i, -i), new Point64(i, -i), new Point64(-i, i), new Point64(i, i));
		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(new Paths64(subject));
		Paths64 solution = new Paths64();
		clipper.Execute(ClipType.Union, FillRule.EvenOdd, solution);
		assertEquals(2, solution.size());
	}
}
