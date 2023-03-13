package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import clipper2.ClipperFileIO.TestCase;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.engine.Clipper64;
import clipper2.engine.PointInPolygonResult;
import clipper2.engine.PolyPath64;
import clipper2.engine.PolyPathBase;
import clipper2.engine.PolyTree64;
import tangible.RefObject;

class TestPolytree {

	private static final Stream<Arguments> testCases() throws IOException {
		return ClipperFileIO.loadTestCases("PolytreeHoleOwner2.txt").stream()
				.map(t -> Arguments.of(t, t.caption(), t.clipType(), t.fillRule()));
	}

	@MethodSource("testCases")
	@ParameterizedTest(name = "{1} {2} {3}")
	final void RunPolytreeTestCase(TestCase test, String caption, Object o, Object o1) {
		PolyTree64 solutionTree = new PolyTree64();
		Paths64 solution_open = new Paths64();
		Clipper64 clipper = new Clipper64();

		Paths64 subject = test.subj();
		Paths64 subjectOpen = test.subj_open();
		Paths64 clip = test.clip();

		List<Point64> pointsOfInterestOutside = Arrays.asList(new Point64(21887, 10420), new Point64(21726, 10825),
				new Point64(21662, 10845), new Point64(21617, 10890));

		for (Point64 pt : pointsOfInterestOutside) {
			for (Path64 path : subject) {
				assertEquals(PointInPolygonResult.IsOutside, Clipper.PointInPolygon(pt, path),
						"outside point of interest found inside subject");
			}
		}

		List<Point64> pointsOfInterestInside = Arrays.asList(new Point64(21887, 10430), new Point64(21843, 10520),
				new Point64(21810, 10686), new Point64(21900, 10461));

		for (Point64 pt : pointsOfInterestInside) {
			int poi_inside_counter = 0;
			for (Path64 path : subject) {
				if (Clipper.PointInPolygon(pt, path) == PointInPolygonResult.IsInside) {
					poi_inside_counter++;
				}
			}
			assertEquals(1, poi_inside_counter, String.format("poi_inside_counter - expected 1 but got %1$s", poi_inside_counter));
		}

		clipper.AddSubject(subject);
		clipper.AddOpenSubject(subjectOpen);
		clipper.AddClip(clip);
		clipper.Execute(test.clipType(), test.fillRule(), solutionTree, solution_open);

		Paths64 solutionPaths = Clipper.PolyTreeToPaths64(solutionTree);
		double a1 = Clipper.Area(solutionPaths), a2 = solutionTree.Area();

		assertTrue(a1 > 330000, String.format("solution has wrong area - value expected: 331,052; value returned; %1$s ", a1));

		assertTrue(Math.abs(a1 - a2) < 0.0001,
				String.format("solution tree has wrong area - value expected: %1$s; value returned; %2$s ", a1, a2));

		assertTrue(CheckPolytreeFullyContainsChildren(solutionTree), "The polytree doesn't properly contain its children");

		for (Point64 pt : pointsOfInterestOutside) {
			assertFalse(PolytreeContainsPoint(solutionTree, pt), "The polytree indicates it contains a point that it should not contain");
		}

		for (Point64 pt : pointsOfInterestInside) {
			assertTrue(PolytreeContainsPoint(solutionTree, pt),
					"The polytree indicates it does not contain a point that it should contain");
		}
	}

	private static boolean CheckPolytreeFullyContainsChildren(PolyTree64 polytree) {
		for (PolyPathBase p : polytree) {
			PolyPath64 child = (PolyPath64) p;
			if (child.getCount() > 0 && !PolyPathFullyContainsChildren(child)) {
				return false;
			}
		}
		return true;
	}

	private static boolean PolyPathFullyContainsChildren(PolyPath64 pp) {
		for (PolyPathBase c : pp) {
			PolyPath64 child = (PolyPath64) c;
			for (Point64 pt : child.getPolygon()) {
				if (Clipper.PointInPolygon(pt, pp.getPolygon()) == PointInPolygonResult.IsOutside) {
					return false;
				}
			}
			if (child.getCount() > 0 && !PolyPathFullyContainsChildren(child)) {
				return false;
			}
		}
		return true;
	}

	private static boolean PolytreeContainsPoint(PolyTree64 pp, Point64 pt) {
		int counter = 0;
		for (int i = 0; i < pp.getCount(); i++) {
			PolyPath64 child = pp.get(i);
			tangible.RefObject<Integer> tempRef_counter = new RefObject<>(counter);
			PolyPathContainsPoint(child, pt, tempRef_counter);
			counter = tempRef_counter.argValue;
		}
		assertTrue(counter >= 0, "Polytree has too many holes");
		return counter != 0;
	}

	private static void PolyPathContainsPoint(PolyPath64 pp, Point64 pt, RefObject<Integer> counter) {
		if (Clipper.PointInPolygon(pt, pp.getPolygon()) != PointInPolygonResult.IsOutside) {
			if (pp.getIsHole()) {
				counter.argValue--;
			} else {
				counter.argValue++;
			}
		}
		for (int i = 0; i < pp.getCount(); i++) {
			PolyPath64 child = pp.get(i);
			PolyPathContainsPoint(child, pt, counter);
		}
	}

}
