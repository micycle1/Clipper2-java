package clipper2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.offset.ClipperOffset;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;

class TestOffsetOrientation {

	@Test
	void TestOffsettingOrientation1() {
		Paths64 subject = new Paths64(Clipper.MakePath(new int[] { 0, 0, 0, 5, 5, 5, 5, 0 }));

		Paths64 solution = Clipper.InflatePaths(subject, 1, JoinType.Round, EndType.Polygon);

		assertEquals(1, solution.size());
		// when offsetting, output orientation should match input
		assertTrue(Clipper.IsPositive(subject.get(0)) == Clipper.IsPositive(solution.get(0)));
	}

	@Test
	void TestOffsettingOrientation2() {
		Path64 s1 = Clipper.MakePath(new int[] { 20, 220, 280, 220, 280, 280, 20, 280 });
		Path64 s2 = Clipper.MakePath(new int[] { 0, 200, 0, 300, 300, 300, 300, 200 });
		Paths64 subject = new Paths64(List.of(s1, s2));

		ClipperOffset co = new ClipperOffset();
		co.setReverseSolution(true);
		co.AddPaths(subject, JoinType.Round, EndType.Polygon);

		Paths64 solution = new Paths64();
		co.Execute(5, solution);

		assertEquals(2, solution.size());
		/*
		 * When offsetting, output orientation should match input EXCEPT when
		 * ReverseSolution == true However, input path ORDER may not match output path
		 * order. For example, order will change whenever inner paths (holes) are
		 * defined before their container outer paths (as above). And when offsetting
		 * multiple outer paths, their order will likely change too. Due to the
		 * sweep-line algorithm used, paths with larger Y coordinates will likely be
		 * listed first.
		 */
		assertTrue(Clipper.IsPositive(subject.get(1)) != Clipper.IsPositive(solution.get(0)));

	}

}
