package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import clipper2.ClipperFileIO.TestCase;
import clipper2.core.Paths64;
import clipper2.engine.Clipper64;

class TestLines {

	private static final Stream<Arguments> testCases() throws IOException {
		return ClipperFileIO.loadTestCases("Lines.txt").stream().map(t -> Arguments.of(t, t.caption(), t.clipType(), t.fillRule()));
	}

	@MethodSource("testCases")
	@ParameterizedTest(name = "{0} {2} {3}")
	final void RunLinesTestCase(TestCase test, String caption, Object o, Object o1) {
		Clipper64 c64 = new Clipper64();
		Paths64 solution = new Paths64();
		Paths64 solution_open = new Paths64();

		c64.AddSubject(test.subj());
		c64.AddOpenSubject(test.subj_open());
		c64.AddClip(test.clip());
		c64.Execute(test.clipType(), test.fillRule(), solution, solution_open);

		if (test.area() > 0) {
			double area2 = Clipper.Area(solution);
			assertEquals(test.area(), area2, test.area() * 0.005);
		}

		if (test.count() > 0 && Math.abs(solution.size() - test.count()) > 2
				&& Math.abs(solution.size() - test.count()) / test.count() > 0.03) {
			assertTrue(Math.abs(solution.size() - test.count()) <= 4,
					String.format("Vertex count incorrect. Difference=%s", (solution.size() - test.count())));
		}
	}
}
