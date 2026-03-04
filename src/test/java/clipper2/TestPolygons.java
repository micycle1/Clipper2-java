package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import clipper2.ClipperFileIO.TestCase;
import clipper2.core.ClipType;
import clipper2.core.FillRule;
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
		Paths64 solution = new Paths64();
		Paths64 solution_open = new Paths64();

		c64.AddSubject(test.subj());
		c64.AddOpenSubject(test.subj_open());
		c64.AddClip(test.clip());
		c64.Execute(test.clipType(), test.fillRule(), solution, solution_open);

		int measuredCount = solution.size();
		long measuredArea = (long) Clipper.Area(solution);
		int storedCount = test.count();
		long storedArea = test.area();
		int countDiff = storedCount > 0 ? Math.abs(storedCount - measuredCount) : 0;
		long areaDiff = storedArea > 0 ? Math.abs(storedArea - measuredArea) : 0;
		double areaDiffRatio = storedArea <= 0 ? 0 : (double) areaDiff / storedArea;

		// check polygon counts
		if (storedCount > 0) {
			if (Arrays.asList(140, 150, 165, 166, 168, 172, 173, 176, 177, 179).contains(testNum)) {
				assertTrue(countDiff <= 7, "Diff=" + countDiff);
			} else if (testNum == 126) {
				assertTrue(countDiff <= 3);
			} else if (Arrays.asList(16, 27, 121).contains(testNum)) {
				assertTrue(countDiff <= 2);
			} else if (testNum >= 120) {
				assertTrue(countDiff <= 6);
			} else if (Arrays.asList(23, 37, 43, 45, 87, 102, 111, 118, 119).contains(testNum)) {
				assertTrue(countDiff <= 1);
			} else {
				assertTrue(countDiff == 0);
			}
		}

		// check polygon areas
		if (storedArea > 0) {
			if (Arrays.asList(19, 22, 23, 24).contains(test.testNum())) {
				assertTrue(areaDiffRatio <= 0.5);
			} else if (testNum == 193) {
				assertTrue(areaDiffRatio <= 0.25);
			} else if (testNum == 63) {
				assertTrue(areaDiffRatio <= 0.1);
			} else if (testNum == 16) {
				assertTrue(areaDiffRatio <= 0.075);
			} else if (Arrays.asList(15, 26).contains(test.testNum())) {
				assertTrue(areaDiffRatio <= 0.05);
			} else if (Arrays.asList(52, 53, 54, 59, 60, 64, 117, 118, 119, 184).contains(test.testNum())) {
				assertTrue(areaDiffRatio <= 0.02);
			} else {
				assertTrue(areaDiffRatio <= 0.01);
			}
		}

	}

	@Test
	void TestCollinearOnMacOs() { // #777
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] { 0, -453054451, 0, -433253797, -455550000, 0 }));
		subject.add(Clipper.MakePath(new long[] { 0, -433253797, 0, 0, -455550000, 0 }));
		Clipper64 clipper = new Clipper64();
		clipper.setPreserveCollinear(false);
		clipper.AddSubject(subject);
		Paths64 solution = new Paths64();
		clipper.Execute(ClipType.Union, FillRule.NonZero, solution);
		assertEquals(1, solution.size());
		assertEquals(3, solution.get(0).size());
		assertEquals(Clipper.IsPositive(subject.get(0)), Clipper.IsPositive(solution.get(0)));
	}
}
