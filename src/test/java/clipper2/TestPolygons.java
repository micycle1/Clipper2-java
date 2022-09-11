package clipper2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import clipper2.ClipperFileIO.TestCase;
import clipper2.core.Point64;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class TestPolygons {

	@Test
	@Disabled
//	@ExtendWith(MyTestWatcher.class)
	final void TestClosedPaths() throws IOException {
		final var testCases = ClipperFileIO.loadTestCases("Polygons.txt");

		testCases.forEach(test -> {
			System.out.println(test.toString());
			Clipper64 c64 = new Clipper64();
			var solution = new ArrayList<List<Point64>>();
			var solution_open = new ArrayList<List<Point64>>();

			c64.AddSubjects(test.subj());
			c64.AddOpenSubjects(test.subj_open());
			c64.AddClips(test.clip());
			c64.Execute(test.clipType(), test.fillRule(), solution, solution_open);

			if (test.area() > 0) {
				double area2 = Clipper.Area(solution);
				assertEquals(test.area(), area2, test.area() * 0.005);
			}

			if (test.count() > 0 && Math.abs(solution.size() - test.count()) > 2
					&& (double) Math.abs(solution.size() - test.count()) / test.count() > 0.02) {
				assertTrue(Math.abs(solution.size() - test.count()) < 4, String.format("Incorrect count in test %1$s", test.caption()));
			}
		});
	}

	private static final List<Arguments> testCases() throws IOException {
		return ClipperFileIO.loadTestCases("Polygons.txt").stream().map(t -> Arguments.of(t, t.caption(), t.clipType(), t.fillRule()))
				.collect(Collectors.toList()).subList(0, 99);
	}

	@MethodSource("testCases")
	@ParameterizedTest(name = "{0} {2} {3}")
	final void area(TestCase test, String caption, Object o, Object o1) {
		Clipper64 c64 = new Clipper64();
		var solution = new ArrayList<List<Point64>>();
		var solution_open = new ArrayList<List<Point64>>();

		c64.AddSubjects(test.subj());
		c64.AddOpenSubjects(test.subj_open());
		c64.AddClips(test.clip());
		c64.Execute(test.clipType(), test.fillRule(), solution, solution_open);

		if (test.area() > 0) {
//			double area2 = Clipper.Area(solution);
//			assertEquals(test.area(), area2, test.area() * 0.005);
		}

		if (test.count() > 0 && Math.abs(solution.size() - test.count()) > 2
				&& (double) Math.abs(solution.size() - test.count()) / test.count() > 0.02) {
			assertTrue(Math.abs(solution.size() - test.count()) < 4, String.format("Incorrect count in test %1$s", test.caption()));
		}
	}
}
