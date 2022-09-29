package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import clipper2.ClipperFileIO.TestCase;
import clipper2.core.Paths64;
import clipper2.engine.Clipper64;

class TestPolygons {

	private static final Stream<Arguments> testCases() throws IOException {
		return ClipperFileIO.loadTestCases("Polygons.txt").stream().map(t -> Arguments.of(t, t.testNum(), t.clipType(), t.fillRule()));
	}

	@MethodSource("testCases")
	@ParameterizedTest(name = "{1}: {2} {3}")
	final void RunPolygonsTestCase(TestCase test, int testNum, Object o, Object o1) {
		Clipper64 c64 = new Clipper64();
		var solution = new Paths64();
		var solution_open = new Paths64();

		c64.AddSubject(test.subj());
		c64.AddOpenSubject(test.subj_open());
		c64.AddClip(test.clip());
		c64.Execute(test.clipType(), test.fillRule(), solution, solution_open);

		int measuredCount = solution.size();
		long measuredArea = (long) Clipper.Area(solution);
		int countDiff = test.count() > 0 ? Math.abs(test.count() - measuredCount) : 0;
		long areaDiff = test.area() > 0 ? Math.abs(test.area() - measuredArea) : 0;

		if (test.testNum() == 23 || test.testNum() == 46) { // NOTE case 46 added in java port
			assertTrue(countDiff <= 4);
		} else if (test.testNum() == 27) {
			assertTrue(countDiff <= 2);
		} else if (List.of(18, 32, 42, 43, 45, 87, 102, 103, 111, 118, 183).contains(test.testNum())) {
			assertTrue(countDiff <= 1);
		} else if (test.testNum() >= 120) {
			if (test.count() > 0) {
				assertTrue(countDiff / test.count() <= 0.02);
			}
		} 
		else if (test.count() > 0) {
			assertEquals(0, countDiff, String.format("Vertex count incorrect. Expected=%s; actual=%s", test.count(), measuredCount));
		}

		if (List.of(22, 23, 24).contains(test.testNum())) {
			assertTrue(areaDiff <= 8);
		} else if (test.area() > 0 && areaDiff > 100) {
			assertTrue(areaDiff / test.area() <= 0.02);
		}

	}
}
