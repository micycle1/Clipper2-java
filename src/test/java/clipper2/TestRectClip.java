package clipper2;

import static org.junit.jupiter.api.Assertions.*;

import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Rect64;

import org.junit.jupiter.api.Test;

/**
 * RectClip tests. Ported from C++ version.
 */
class TestRectClip {

	@Test
	void testRectClip() {
		Paths64 sub = new Paths64();
		Paths64 clp = new Paths64();
		Paths64 sol; // Solution will be assigned by RectClip

		Rect64 rect = new Rect64(100, 100, 700, 500);
		clp.add(rect.AsPath());

		// Test case 1: Subject is identical to clip rect
		sub.add(Clipper.MakePath(new long[] { 100, 100, 700, 100, 700, 500, 100, 500 }));
		sol = Clipper.RectClip(rect, sub);
		// Use Math.abs because area can be negative depending on orientation
		assertEquals(Math.abs(Clipper.Area(sub)), Math.abs(Clipper.Area(sol)), "Test 1 failed");

		// Test case 2: Subject partially outside but covers same area within clip rect
		sub.clear();
		sub.add(Clipper.MakePath(new long[] { 110, 110, 700, 100, 700, 500, 100, 500 }));
		sol = Clipper.RectClip(rect, sub);
		// Area might differ slightly due to clipping precise shape, but conceptually
		// check against original subject
		// A better check might involve comparing vertices if area isn't exact?
		// Or check against the expected clipped shape area if known.
		// For now, let's keep the original logic's intent, assuming Area reflects the
		// clipped portion.
		assertEquals(Math.abs(Clipper.Area(sub)), Math.abs(Clipper.Area(sol)), "Test 2 failed"); // Might be brittle

		// Test case 3: Subject partially outside, clipped area should equal clip rect
		// area
		sub.clear();
		sub.add(Clipper.MakePath(new long[] { 90, 90, 700, 100, 700, 500, 100, 500 }));
		sol = Clipper.RectClip(rect, sub);
		assertEquals(Math.abs(Clipper.Area(clp)), Math.abs(Clipper.Area(sol)), "Test 3 failed");

		// Test case 4: Subject fully inside clip rect
		sub.clear();
		sub.add(Clipper.MakePath(new long[] { 110, 110, 690, 110, 690, 490, 110, 490 }));
		sol = Clipper.RectClip(rect, sub);
		assertEquals(Math.abs(Clipper.Area(sub)), Math.abs(Clipper.Area(sol)), "Test 4 failed");

		// Test case 5: Subject touches edge, should result in empty solution
		sub.clear();
		clp.clear(); // Clear previous clip path
		rect = new Rect64(390, 290, 410, 310);
		// No need to add rect.AsPath() to clp for RectClip, rect object is passed
		// directly
		sub.add(Clipper.MakePath(new long[] { 410, 290, 500, 290, 500, 310, 410, 310 }));
		sol = Clipper.RectClip(rect, sub);
		assertTrue(sol.isEmpty(), "Test 5 failed - should be empty");

		// Test case 6: Triangle outside rect
		sub.clear();
		sub.add(Clipper.MakePath(new long[] { 430, 290, 470, 330, 390, 330 }));
		sol = Clipper.RectClip(rect, sub);
		assertTrue(sol.isEmpty(), "Test 6 failed - should be empty");

		// Test case 7: Triangle outside rect
		sub.clear();
		sub.add(Clipper.MakePath(new long[] { 450, 290, 480, 330, 450, 330 }));
		sol = Clipper.RectClip(rect, sub);
		assertTrue(sol.isEmpty(), "Test 7 failed - should be empty");

		// Test case 8: Complex polygon clipped, check bounds of result
		sub.clear();
		sub.add(Clipper.MakePath(new long[] { 208, 66, 366, 112, 402, 303, 234, 332, 233, 262, 243, 140, 215, 126, 40, 172 }));
		rect = new Rect64(237, 164, 322, 248);
		sol = Clipper.RectClip(rect, sub);
		assertFalse(sol.isEmpty(), "Test 8 failed - should not be empty"); // Basic check
		Rect64 solBounds = Clipper.GetBounds(sol);
		// Check if the resulting bounds match the clipping rectangle bounds
		// Note: The clipped polygon might not *fill* the entire rect, but its bounds
		// should ideally be constrained *within* or *equal to* the clip rect if it
		// intersects fully.
		// The C++ test checks if the width/height *match* the clip rect width/height.
		// This implies the clipped result must touch all four sides of the clip rect.
		assertEquals(rect.getWidth(), solBounds.getWidth(), "Test 8 failed - Width mismatch");
		assertEquals(rect.getHeight(), solBounds.getHeight(), "Test 8 failed - Height mismatch");
	}

	@Test
	void testRectClip2() {
		Rect64 rect = new Rect64(54690, 0, 65628, 6000);
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] { 700000, 6000, 0, 6000, 0, 5925, 700000, 5925 }));

		Paths64 solution = Clipper.RectClip(rect, subject);

		assertNotNull(solution, "TestRectClip2 Solution should not be null");
		assertEquals(1, solution.size(), "TestRectClip2 Should have 1 path");
		assertEquals(4, solution.get(0).size(), "TestRectClip2 Path should have 4 points");
	}

	@Test
	void testRectClip3() {
		Rect64 r = new Rect64(-1800000000L, -137573171L, -1741475021L, 3355443L);
		Paths64 subject = new Paths64();
		Paths64 solution;

		subject.add(Clipper.MakePath(new long[] { -1800000000L, 10005000L, -1800000000L, -5000L, -1789994999L, -5000L, -1789994999L, 10005000L }));

		solution = Clipper.RectClip(r, subject);

		assertNotNull(solution, "TestRectClip3 Solution should not be null");
		assertEquals(1, solution.size(), "TestRectClip3 Should have 1 path");
		assertFalse(solution.get(0).isEmpty(), "TestRectClip3 Path should not be empty");
		Path64 expectedPath = Clipper.MakePath(new long[] { -1789994999L, 3355443L, -1800000000L, 3355443L, -1800000000L, -5000L, -1789994999L, -5000L });
		assertEquals(Math.abs(Clipper.Area(expectedPath)), Math.abs(Clipper.Area(solution.get(0))), "TestRectClip3 Area check");

	}

	private static Path64 clipper(Rect64 r, Paths64 subject) {
		return Clipper.Intersect(subject, new Paths64(r.AsPath()), FillRule.EvenOdd).get(0);
	}
}