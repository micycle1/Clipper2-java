package com.github.micycle1.clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.micycle1.clipper2.ClipperFileIO.TestCase;
import com.github.micycle1.clipper2.core.ClipType;
import com.github.micycle1.clipper2.core.FillRule;
import com.github.micycle1.clipper2.core.Path64;
import com.github.micycle1.clipper2.core.Paths64;
import com.github.micycle1.clipper2.core.Point64;
import com.github.micycle1.clipper2.engine.Clipper64;
import com.github.micycle1.clipper2.engine.PointInPolygonResult;
import com.github.micycle1.clipper2.engine.PolyPath64;
import com.github.micycle1.clipper2.engine.PolyPathBase;
import com.github.micycle1.clipper2.engine.PolyTree64;

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
			counter = PolyPathContainsPoint(child, pt, counter);
		}
		assertTrue(counter >= 0, "Polytree has too many holes");
		return counter != 0;
	}

	private static int PolyPathContainsPoint(PolyPath64 pp, Point64 pt, int counter) {
		if (Clipper.PointInPolygon(pt, pp.getPolygon()) != PointInPolygonResult.IsOutside) {
			if (pp.getIsHole()) {
				counter--;
			} else {
				counter++;
			}
		}
		for (int i = 0; i < pp.getCount(); i++) {
			PolyPath64 child = pp.get(i);
			counter = PolyPathContainsPoint(child, pt, counter);
		}
		return counter;
	}

	@Test
	void TestPolytree3() { // #942
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] { 1588700, -8717600, 1616200, -8474800, 1588700, -8474800 }));
		subject.add(Clipper.MakePath(new long[] { 13583800, -15601600, 13582800, -15508500, 13555300, -15508500, 13555500, -15182200, 13010900, -15185400 }));
		subject.add(Clipper.MakePath(new long[] { 956700, -3092300, 1152600, 3147400, 25600, 3151700 }));
		subject.add(Clipper.MakePath(new long[] { 22575900, -16604000, 31286800, -12171900, 31110200, 4882800, 30996200, 4826300, 30414400, 5447400, 30260000, 5391500,
				29662200, 5805400, 28844500, 5337900, 28435000, 5789300, 27721400, 5026400, 22876300, 5034300, 21977700, 4414900, 21148000, 4654700, 20917600, 4653400,
				19334300, 12411000, -2591700, 12177200, 53200, 3151100, -2564300, 12149800, 7819400, 4692400, 10116000, 5228600, 6975500, 3120100, 7379700, 3124700,
				11037900, 596200, 12257000, 2587800, 12257000, 596200, 15227300, 2352700, 18444400, 1112100, 19961100, 5549400, 20173200, 5078600, 20330000, 5079300,
				20970200, 4544300, 20989600, 4563700, 19465500, 1112100, 21611600, 4182100, 22925100, 1112200, 22952700, 1637200, 23059000, 1112200, 24908100, 4181200,
				27070100, 3800600, 27238000, 3800700, 28582200, 520300, 29367800, 1050100, 29291400, 179400, 29133700, 360700, 29056700, 312600, 29121900, 332500,
				29269900, 162300, 28941400, 213100, 27491300, -3041500, 27588700, -2997800, 22104900, -16142800, 13010900, -15603000, 13555500, -15182200,
				13555300, -15508500, 13582800, -15508500, 13583100, -15154700, 1588700, -8822800, 1588700, -8379900, 1588700, -8474800, 1616200, -8474800, 1003900,
				-630100, 1253300, -12284500, 12983400, -16239900 }));
		subject.add(Clipper.MakePath(new long[] { 198200, 12149800, 1010600, 12149800, 1011500, 11859600 }));
		subject.add(Clipper.MakePath(new long[] { 21996700, -7432000, 22096700, -7432000, 22096700, -7332000 }));

		PolyTree64 solutionTree = new PolyTree64();
		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(subject);
		clipper.Execute(ClipType.Union, FillRule.NonZero, solutionTree);

		assertTrue(solutionTree.getCount() == 1 && solutionTree.get(0).getCount() == 2 && solutionTree.get(0).get(1).getCount() == 1);
	}

	@Test
	void TestPolytree4() { // #957
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] { 77910, 46865, 78720, 46865, 78720, 48000, 77910, 48000, 77910, 46865 }));
		subject.add(Clipper.MakePath(new long[] { 82780, 53015, 93600, 53015, 93600, 54335, 82780, 54335, 82780, 53015 }));
		subject.add(Clipper.MakePath(new long[] { 82780, 48975, 84080, 48975, 84080, 53015, 82780, 53015, 82780, 48975 }));
		subject.add(Clipper.MakePath(new long[] { 77910, 48000, 84080, 48000, 84080, 48975, 77910, 48975, 77910, 48000 }));
		subject.add(Clipper.MakePath(new long[] { 89880, 40615, 90700, 40615, 90700, 46865, 89880, 46865, 89880, 40615 }));
		subject.add(Clipper.MakePath(new long[] { 92700, 54335, 93600, 54335, 93600, 61420, 92700, 61420, 92700, 54335 }));
		subject.add(Clipper.MakePath(new long[] { 78950, 47425, 84080, 47425, 84080, 47770, 78950, 47770, 78950, 47425 }));
		subject.add(Clipper.MakePath(new long[] { 82780, 61420, 93600, 61420, 93600, 62435, 82780, 62435, 82780, 61420 }));
		subject.add(Clipper.MakePath(new long[] { 101680, 63085, 100675, 63085, 100675, 47770, 100680, 47770, 100680, 40615, 101680, 40615, 101680, 63085 }));
		subject.add(Clipper.MakePath(new long[] { 76195, 39880, 89880, 39880, 89880, 41045, 76195, 41045, 76195, 39880 }));
		subject.add(Clipper.MakePath(new long[] { 85490, 56145, 90520, 56145, 90520, 59235, 85490, 59235, 85490, 56145 }));
		subject.add(Clipper.MakePath(new long[] { 89880, 39880, 101680, 39880, 101680, 40615, 89880, 40615, 89880, 39880 }));
		subject.add(Clipper.MakePath(new long[] { 89880, 46865, 100680, 46865, 100680, 47770, 89880, 47770, 89880, 46865 }));
		subject.add(Clipper.MakePath(new long[] { 82780, 54335, 83280, 54335, 83280, 61420, 82780, 61420, 82780, 54335 }));
		subject.add(Clipper.MakePath(new long[] { 76195, 41045, 76855, 41045, 76855, 62665, 76195, 62665, 76195, 41045 }));
		subject.add(Clipper.MakePath(new long[] { 76195, 62665, 100675, 62665, 100675, 63085, 76195, 63085, 76195, 62665 }));
		subject.add(Clipper.MakePath(new long[] { 82780, 41045, 84080, 41045, 84080, 47425, 82780, 47425, 82780, 41045 }));

		PolyTree64 solutionTree = new PolyTree64();
		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(subject);
		clipper.Execute(ClipType.Union, FillRule.NonZero, solutionTree);

		assertTrue(solutionTree.getCount() == 1 && solutionTree.get(0).getCount() == 2 && solutionTree.get(0).get(0).getCount() == 1);
	}

	@Test
	void TestPolytree5() { // #973
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] { 0, 0, 79530, 0, 79530, 940, 0, 940, 0, 0 }));
		subject.add(Clipper.MakePath(new long[] { 0, 33360, 79530, 33360, 79530, 34300, 0, 34300, 0, 33360 }));
		subject.add(Clipper.MakePath(new long[] { 78470, 940, 79530, 940, 79530, 33360, 78470, 33360, 78470, 940 }));
		subject.add(Clipper.MakePath(new long[] { 0, 940, 940, 940, 940, 33360, 0, 33360, 0, 940 }));
		subject.add(Clipper.MakePath(new long[] { 29290, 940, 30350, 940, 30350, 33360, 29290, 33360, 29290, 940 }));

		PolyTree64 solutionTree = new PolyTree64();
		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(subject);
		clipper.Execute(ClipType.Union, FillRule.NonZero, solutionTree);

		assertTrue(solutionTree.getCount() == 1 && solutionTree.get(0).getCount() == 2);
	}

	@Test
	void TestPolytreeUnion() {
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] { 0, 0, 0, 5, 5, 5, 5, 0 }));
		subject.add(Clipper.MakePath(new long[] { 1, 1, 1, 6, 6, 6, 6, 1 }));

		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(subject);

		PolyTree64 solution = new PolyTree64();
		Paths64 openPaths = new Paths64();
		if (Clipper.IsPositive(subject.get(0))) {
			clipper.Execute(ClipType.Union, FillRule.Positive, solution, openPaths);
		} else {
			clipper.setReverseSolution(true);
			clipper.Execute(ClipType.Union, FillRule.Negative, solution, openPaths);
		}

		assertEquals(0, openPaths.size());
		assertEquals(1, solution.getCount());
		assertEquals(8, solution.get(0).getPolygon().size());
		assertEquals(Clipper.IsPositive(subject.get(0)), Clipper.IsPositive(solution.get(0).getPolygon()));
	}

	@Test
	void TestPolytreeUnion2() { // #987
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] { 534, 1024, 534, -800, 1026, -800, 1026, 1024 }));
		subject.add(Clipper.MakePath(new long[] { 1, 1024, 8721, 1024, 8721, 1920, 1, 1920 }));
		subject.add(Clipper.MakePath(new long[] { 30, 1024, 30, -800, 70, -800, 70, 1024 }));
		subject.add(Clipper.MakePath(new long[] { 1, 1024, 1, -1024, 3841, -1024, 3841, 1024 }));
		subject.add(Clipper.MakePath(new long[] { 3900, -1024, 6145, -1024, 6145, 1024, 3900, 1024 }));
		subject.add(Clipper.MakePath(new long[] { 5884, 1024, 5662, 1024, 5662, -1024, 5884, -1024 }));
		subject.add(Clipper.MakePath(new long[] { 534, 1024, 200, 1024, 200, -800, 534, -800 }));
		subject.add(Clipper.MakePath(new long[] { 200, -800, 200, 1024, 70, 1024, 70, -800 }));
		subject.add(Clipper.MakePath(new long[] { 1200, 1920, 1313, 1920, 1313, -800, 1200, -800 }));
		subject.add(Clipper.MakePath(new long[] { 6045, -800, 6045, 1024, 5884, 1024, 5884, -800 }));

		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(subject);
		PolyTree64 solution = new PolyTree64();
		Paths64 openPaths = new Paths64();
		clipper.Execute(ClipType.Union, FillRule.EvenOdd, solution, openPaths);

		assertEquals(1, solution.getCount());
		assertEquals(1, solution.get(0).getCount());
	}

	@Test
	void TestPolytreeUnion3() {
		Paths64 subject = new Paths64();
		subject.add(Clipper.MakePath(new long[] {
				-120927680, 590077597,
				-120919386, 590077307,
				-120919432, 590077309,
				-120919451, 590077309,
				-120919455, 590077310,
				-120099297, 590048669,
				-120928004, 590077608,
				-120902794, 590076728,
				-120919444, 590077309,
				-120919450, 590077309,
				-120919842, 590077323,
				-120922852, 590077428,
				-120902452, 590076716,
				-120902455, 590076716,
				-120912590, 590077070,
				11914491, 249689797
		}));

		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(subject);
		PolyTree64 solution = new PolyTree64();
		clipper.Execute(ClipType.Union, FillRule.EvenOdd, solution);

		assertTrue(solution.getCount() >= 0);
	}

}
