package clipper2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.offset.ClipperOffset;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;

public class TestOffsets {

	@Test
	void TestOffsets2() { // see #448 & #456
		double scale = 10, delta = 10 * scale, arc_tol = 0.25 * scale;

		Paths64 subject = new Paths64();
		Paths64 solution = new Paths64();
		ClipperOffset c = new ClipperOffset();
		subject.add(Clipper.MakePath(new long[] { 50, 50, 100, 50, 100, 150, 50, 150, 0, 100 }));

		subject = Clipper.ScalePaths(subject, scale);

		c.AddPaths(subject, JoinType.Round, EndType.Polygon);
		c.setArcTolerance(arc_tol);
		c.Execute(delta, solution);

		double min_dist = delta * 2;
		double max_dist = 0;

		for (Point64 subjPt : subject.get(0)) {
			Point64 prevPt = solution.get(0).get(solution.get(0).size() - 1);
			for (Point64 pt : solution.get(0)) {
				Point64 mp = midPoint(prevPt, pt);
				double d = distance(mp, subjPt);
				if (d < delta * 2) {
					if (d < min_dist)
						min_dist = d;
					if (d > max_dist)
						max_dist = d;
				}
				prevPt = pt;
			}
		}

		assertTrue(min_dist + 1 >= delta - arc_tol); // +1 for rounding errors
		assertTrue(solution.get(0).size() <= 21);
	}

	@Test
	void TestOffsets3() { // see #424
		Paths64 subjects = new Paths64(List.of(Clipper.MakePath(new long[] { 1525311078, 1352369439, 1526632284, 1366692987, 1519397110, 1367437476, 1520246456,
				1380177674, 1520613458, 1385913385, 1517383844, 1386238444, 1517771817, 1392099983, 1518233190, 1398758441, 1518421934, 1401883197, 1518694564,
				1406612275, 1520267428, 1430289121, 1520770744, 1438027612, 1521148232, 1443438264, 1521441833, 1448964260, 1521683005, 1452518932, 1521819320,
				1454374912, 1527943004, 1454154711, 1527649403, 1448523858, 1535901696, 1447989084, 1535524209, 1442788147, 1538953052, 1442463089, 1541553521,
				1442242888, 1541459149, 1438855987, 1538764308, 1439076188, 1538575565, 1436832236, 1538764308, 1436832236, 1536509870, 1405374956, 1550497874,
				1404347351, 1550214758, 1402428457, 1543818445, 1402868859, 1543734559, 1402124370, 1540672717, 1402344571, 1540473487, 1399995761, 1524996506,
				1400981422, 1524807762, 1398223667, 1530092585, 1397898609, 1531675935, 1397783265, 1531392819, 1394920653, 1529809469, 1395025510, 1529348096,
				1388880855, 1531099218, 1388660654, 1530826588, 1385158410, 1532955197, 1384938209, 1532661596, 1379003269, 1532472852, 1376235028, 1531277476,
				1376350372, 1530050642, 1361806623, 1599487345, 1352704983, 1602758902, 1378489467, 1618990858, 1376350372, 1615058698, 1344085688, 1603230761,
				1345700495, 1598648484, 1346329641, 1598931599, 1348667965, 1596698132, 1348993024, 1595775386, 1342722540 })));

		Paths64 solution = Clipper.InflatePaths(subjects, -209715, JoinType.Miter, EndType.Polygon);
		assertTrue(solution.get(0).size() - subjects.get(0).size() <= 1);
	}

	@Test
	void TestOffsets4() { // see #482
		Paths64 paths = new Paths64(List.of(Clipper.MakePath(new long[] { 0, 0, 20000, 200, 40000, 0, 40000, 50000, 0, 50000, 0, 0 })));
		Paths64 solution = Clipper.InflatePaths(paths, -5000, JoinType.Square, EndType.Polygon);
		assertEquals(5, solution.get(0).size());

		paths = new Paths64(List.of(Clipper.MakePath(new long[] { 0, 0, 20000, 400, 40000, 0, 40000, 50000, 0, 50000, 0, 0 })));
		solution = Clipper.InflatePaths(paths, -5000, JoinType.Square, EndType.Polygon);
		assertEquals(5, solution.get(0).size());

		paths = new Paths64(List.of(Clipper.MakePath(new long[] { 0, 0, 20000, 400, 40000, 0, 40000, 50000, 0, 50000, 0, 0 })));
		solution = Clipper.InflatePaths(paths, -5000, JoinType.Round, EndType.Polygon, 2, 100);
		assertTrue(solution.get(0).size() > 5);

		paths = new Paths64(List.of(Clipper.MakePath(new long[] { 0, 0, 20000, 1500, 40000, 0, 40000, 50000, 0, 50000, 0, 0 })));
		solution = Clipper.InflatePaths(paths, -5000, JoinType.Round, EndType.Polygon, 2, 100);
		assertTrue(solution.get(0).size() > 5);
	}

	@Test
	void TestOffsets6() {
		Path64 squarePath = Clipper.MakePath(new long[] { 620, 620, -620, 620, -620, -620, 620, -620 });

		Path64 complexPath = Clipper.MakePath(new long[] { 20, -277, 42, -275, 59, -272, 80, -266, 97, -261, 114, -254, 135, -243, 149, -235, 167, -222, 182,
				-211, 197, -197, 212, -181, 223, -167, 234, -150, 244, -133, 253, -116, 260, -99, 267, -78, 272, -61, 275, -40, 278, -18, 276, -39, 272, -61,
				267, -79, 260, -99, 253, -116, 245, -133, 235, -150, 223, -167, 212, -181, 197, -197, 182, -211, 168, -222, 152, -233, 135, -243, 114, -254, 97,
				-261, 80, -267, 59, -272, 42, -275, 20, -278 });

		Paths64 subjects = new Paths64(List.of(squarePath, complexPath));

		final double offset = -50;
		ClipperOffset offseter = new ClipperOffset();

		offseter.AddPaths(subjects, JoinType.Round, EndType.Polygon);
		Paths64 solution = new Paths64();
		offseter.Execute(offset, solution);

		assertEquals(2, solution.size());

		double area = Clipper.Area(solution.get(1));
		assertTrue(area < -47500);
	}

	@Test
	void TestOffsets7() { // (#593 & #715)
		Paths64 solution;
		Paths64 subject = new Paths64(List.of(Clipper.MakePath(new long[] { 0, 0, 100, 0, 100, 100, 0, 100 })));

		solution = Clipper.InflatePaths(subject, -50, JoinType.Miter, EndType.Polygon);
		assertEquals(0, solution.size());

		subject.add(Clipper.MakePath(new long[] { 40, 60, 60, 60, 60, 40, 40, 40 }));
		solution = Clipper.InflatePaths(subject, 10, JoinType.Miter, EndType.Polygon);
		assertEquals(1, solution.size());

		Collections.reverse(subject.get(0));
		Collections.reverse(subject.get(1));
		solution = Clipper.InflatePaths(subject, 10, JoinType.Miter, EndType.Polygon);
		assertEquals(1, solution.size());

		subject = new Paths64(List.of(subject.get(0)));
		solution = Clipper.InflatePaths(subject, -50, JoinType.Miter, EndType.Polygon);
		assertEquals(0, solution.size());
	}

	@Test
	void TestOffsets8() { // (#724)
		Paths64 subject = new Paths64(List.of(Clipper.MakePath(new long[] { 91759700, -49711991, 83886095, -50331657, -872415388, -50331657, -880288993,
				-49711991, -887968725, -47868251, -895265482, -44845834, -901999593, -40719165, -908005244, -35589856, -913134553, -29584205, -917261224,
				-22850094, -920283639, -15553337, -922127379, -7873605, -922747045, 0, -922747045, 1434498600, -922160557, 1442159790, -920414763, 1449642437,
				-917550346, 1456772156, -913634061, 1463382794, -908757180, 1469320287, -903033355, 1474446264, -896595982, 1478641262, -889595081, 1481807519,
				-882193810, 1483871245, -876133965, 1484596521, -876145751, 1484713389, -875781839, 1485061090, -874690056, 1485191762, -874447580, 1485237014,
				-874341490, 1485264094, -874171960, 1485309394, -873612294, 1485570372, -873201878, 1485980788, -872941042, 1486540152, -872893274, 1486720070,
				-872835064, 1487162210, -872834788, 1487185500, -872769052, 1487406000, -872297948, 1487583168, -871995958, 1487180514, -871995958, 1486914040,
				-871908872, 1486364208, -871671308, 1485897962, -871301302, 1485527956, -870835066, 1485290396, -870285226, 1485203310, -868659019, 1485203310,
				-868548443, 1485188472, -868239649, 1484791011, -868239527, 1484783879, -838860950, 1484783879, -830987345, 1484164215, -823307613, 1482320475,
				-816010856, 1479298059, -809276745, 1475171390, -803271094, 1470042081, -752939437, 1419710424, -747810128, 1413704773, -743683459, 1406970662,
				-740661042, 1399673904, -738817302, 1391994173, -738197636, 1384120567, -738197636, 1244148246, -738622462, 1237622613, -739889768, 1231207140,
				-802710260, 995094494, -802599822, 995052810, -802411513, 994586048, -802820028, 993050638, -802879992, 992592029, -802827240, 992175479,
				-802662144, 991759637, -802578556, 991608039, -802511951, 991496499, -801973473, 990661435, -801899365, 990554757, -801842657, 990478841,
				-801770997, 990326371, -801946911, 989917545, -801636397, 989501855, -801546099, 989389271, -800888669, 988625013, -800790843, 988518907,
				-800082405, 987801675, -799977513, 987702547, -799221423, 987035738, -799109961, 986944060, -798309801, 986330832, -798192297, 986247036,
				-797351857, 985690294, -797228867, 985614778, -796352124, 985117160, -796224232, 985050280, -795315342, 984614140, -795183152, 984556216,
				-794246418, 984183618, -794110558, 984134924, -793150414, 983827634, -793011528, 983788398, -792032522, 983547874, -791891266, 983518284,
				-790898035, 983345662, -790755079, 983325856, -789752329, 983221956, -789608349, 983212030, -787698545, 983146276, -787626385, 983145034,
				-536871008, 983145034, -528997403, 982525368, -521317671, 980681627, -514020914, 977659211, -507286803, 973532542, -501281152, 968403233,
				-496151843, 962397582, -492025174, 955663471, -489002757, 948366714, -487159017, 940686982, -486539351, 932813377, -486539351, 667455555,
				-486537885, 667377141, -486460249, 665302309, -486448529, 665145917, -486325921, 664057737, -486302547, 663902657, -486098961, 662826683,
				-486064063, 662673784, -485780639, 661616030, -485734413, 661466168, -485372735, 660432552, -485315439, 660286564, -484877531, 659282866,
				-484809485, 659141568, -484297795, 658173402, -484219379, 658037584, -483636768, 657110363, -483548422, 656980785, -482898150, 656099697,
				-482800368, 655977081, -482086070, 655147053, -481979398, 655032087, -481205068, 654257759, -481090104, 654151087, -480260074, 653436789,
				-480137460, 653339007, -479256372, 652688735, -479126794, 652600389, -478199574, 652017779, -478063753, 651939363, -477095589, 651427672,
				-476954289, 651359626, -475950593, 650921718, -475804605, 650864422, -474770989, 650502744, -474621127, 650456518, -473563373, 650173094,
				-473410475, 650138196, -472334498, 649934610, -472179420, 649911236, -471091240, 649788626, -470934848, 649776906, -468860016, 649699272,
				-468781602, 649697806, -385876037, 649697806, -378002432, 649078140, -370322700, 647234400, -363025943, 644211983, -356291832, 640085314,
				-350286181, 634956006, -345156872, 628950354, -341030203, 622216243, -338007786, 614919486, -336164046, 607239755, -335544380, 599366149,
				-335544380, 571247184, -335426942, 571236100, -335124952, 570833446, -335124952, 569200164, -335037864, 568650330, -334800300, 568184084,
				-334430294, 567814078, -333964058, 567576517, -333414218, 567489431, -331787995, 567489431, -331677419, 567474593, -331368625, 567077133,
				-331368503, 567070001, -142068459, 567070001, -136247086, 566711605, -136220070, 566848475, -135783414, 567098791, -135024220, 567004957,
				-134451560, 566929159, -134217752, 566913755, -133983942, 566929159, -133411282, 567004957, -132665482, 567097135, -132530294, 567091859,
				-132196038, 566715561, -132195672, 566711157, -126367045, 567070001, -33554438, 567070001, -27048611, 566647761, -20651940, 565388127,
				-14471751, 563312231, -8611738, 560454902, 36793963, 534548454, 43059832, 530319881, 48621743, 525200596, 53354240, 519306071, 57150572,
				512769270, 59925109, 505737634, 61615265, 498369779, 62182919, 490831896, 62182919, 474237629, 62300359, 474226543, 62602349, 473823889,
				62602349, 472190590, 62689435, 471640752, 62926995, 471174516, 63297005, 470804506, 63763241, 470566946, 64313081, 470479860, 65939308,
				470479860, 66049884, 470465022, 66358678, 470067562, 66358800, 470060430, 134217752, 470060430, 134217752, 0, 133598086, -7873605, 131754346,
				-15553337, 128731929, -22850094, 124605260, -29584205, 119475951, -35589856, 113470300, -40719165, 106736189, -44845834, 99439432, -47868251,
				91759700, -49711991
		})));

		double offset = -50329979.277800001;
		double arc_tol = 5000;

		Paths64 solution = Clipper.InflatePaths(subject, offset, JoinType.Round, EndType.Polygon, 2, arc_tol);
		OffsetQual oq = getOffsetQuality(subject.get(0), solution.get(0), offset);
		double smallestDist = distance(oq.smallestInSub, oq.smallestInSol);
		double largestDist = distance(oq.largestInSub, oq.largestInSol);
		final double rounding_tolerance = 1.0;
		offset = Math.abs(offset);

		assertTrue(offset - smallestDist - rounding_tolerance <= arc_tol);
		assertTrue(largestDist - offset - rounding_tolerance <= arc_tol);
	}

	@Test
	void TestOffsets9() { // (#733)
		// solution orientations should match subject orientations UNLESS
		// reverse_solution is set true in ClipperOffset's constructor

		// start subject's orientation positive ...
		Paths64 subject = new Paths64(Clipper.MakePath(new long[] { 100, 100, 200, 100, 200, 400, 100, 400 }));
		Paths64 solution = Clipper.InflatePaths(subject, 50, JoinType.Miter, EndType.Polygon);
		assertEquals(1, solution.size());
		assertTrue(Clipper.IsPositive(solution.get(0)));

		// reversing subject's orientation should not affect delta direction
		// (ie where positive deltas inflate).
		Collections.reverse(subject.get(0));
		solution = Clipper.InflatePaths(subject, 50, JoinType.Miter, EndType.Polygon);
		assertEquals(1, solution.size());
		assertTrue(Math.abs(Clipper.Area(solution.get(0))) > Math.abs(Clipper.Area(subject.get(0))));
		assertFalse(Clipper.IsPositive(solution.get(0)));

		ClipperOffset co = new ClipperOffset(2, 0, false, true); // last param. reverses solution
		co.AddPaths(subject, JoinType.Miter, EndType.Polygon);
		co.Execute(50, solution);
		assertEquals(1, solution.size());
		assertTrue(Math.abs(Clipper.Area(solution.get(0))) > Math.abs(Clipper.Area(subject.get(0))));
		assertTrue(Clipper.IsPositive(solution.get(0)));

		// add a hole (ie has reverse orientation to outer path)
		subject.add(Clipper.MakePath(new long[] { 130, 130, 170, 130, 170, 370, 130, 370 }));
		solution = Clipper.InflatePaths(subject, 30, JoinType.Miter, EndType.Polygon);
		assertEquals(1, solution.size());
		assertFalse(Clipper.IsPositive(solution.get(0)));

		co.Clear(); // should still reverse solution orientation
		co.AddPaths(subject, JoinType.Miter, EndType.Polygon);
		co.Execute(30, solution);
		assertEquals(1, solution.size());
		assertTrue(Math.abs(Clipper.Area(solution.get(0))) > Math.abs(Clipper.Area(subject.get(0))));
		assertTrue(Clipper.IsPositive(solution.get(0)));

		solution = Clipper.InflatePaths(subject, -15, JoinType.Miter, EndType.Polygon);
		assertEquals(0, solution.size());
	}

	private static Point64 midPoint(Point64 p1, Point64 p2) {
		Point64 result = new Point64();
		result.setX((p1.x + p2.x) / 2);
		result.setY((p1.y + p2.y) / 2);
		return result;
	}

	private static double distance(Point64 pt1, Point64 pt2) {
		long dx = pt1.x - pt2.x;
		long dy = pt1.y - pt2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	static class OffsetQual {
		PointD smallestInSub;
		PointD smallestInSol;
		PointD largestInSub;
		PointD largestInSol;
	}

	private static OffsetQual getOffsetQuality(Path64 subject, Path64 solution, double delta) {
		if (subject.size() == 0 || solution.size() == 0)
			return new OffsetQual();

		double desiredDistSqr = delta * delta;
		double smallestSqr = desiredDistSqr;
		double largestSqr = desiredDistSqr;
		OffsetQual oq = new OffsetQual();

		final int subVertexCount = 4; // 1 .. 100 :)
		final double subVertexFrac = 1.0 / subVertexCount;
		Point64 solPrev = solution.get(solution.size() - 1);

		for (Point64 solPt0 : solution) {
			for (int i = 0; i < subVertexCount; ++i) {
				// divide each edge in solution into series of sub-vertices (solPt)
				PointD solPt = new PointD(solPrev.x + (solPt0.x - solPrev.x) * subVertexFrac * i, solPrev.y + (solPt0.y - solPrev.y) * subVertexFrac * i);

				// now find the closest point in subject to each of these solPt
				PointD closestToSolPt = new PointD(0, 0);
				double closestDistSqr = Double.POSITIVE_INFINITY;
				Point64 subPrev = subject.get(subject.size() - 1);

				for (Point64 subPt : subject) {
					PointD closestPt = getClosestPointOnSegment(solPt, subPt, subPrev);
					subPrev = subPt;
					double sqrDist = distanceSqr(closestPt, solPt);
					if (sqrDist < closestDistSqr) {
						closestDistSqr = sqrDist;
						closestToSolPt = closestPt;
					}
				}

				// see how this distance compares with every other solPt
				if (closestDistSqr < smallestSqr) {
					smallestSqr = closestDistSqr;
					oq.smallestInSub = closestToSolPt;
					oq.smallestInSol = solPt;
				}
				if (closestDistSqr > largestSqr) {
					largestSqr = closestDistSqr;
					oq.largestInSub = closestToSolPt;
					oq.largestInSol = solPt;
				}
			}
			solPrev = solPt0;
		}
		return oq;
	}

	private static PointD getClosestPointOnSegment(PointD offPt, Point64 seg1, Point64 seg2) {
		// Handle case where segment is actually a point
		if (seg1.x == seg2.x && seg1.y == seg2.y) {
			return new PointD(seg1.x, seg1.y);
		}

		double dx = seg2.x - seg1.x;
		double dy = seg2.y - seg1.y;

		double q = ((offPt.x - seg1.x) * dx + (offPt.y - seg1.y) * dy) / (dx * dx + dy * dy);

		// Clamp q between 0 and 1
		q = Math.max(0, Math.min(1, q));

		return new PointD(seg1.x + q * dx, seg1.y + q * dy);
	}
	private static double distanceSqr(PointD pt1, PointD pt2) {
		double dx = pt1.x - pt2.x;
		double dy = pt1.y - pt2.y;
		return dx * dx + dy * dy;
	}

	private static double distance(PointD pt1, PointD pt2) {
		return Math.sqrt(distanceSqr(pt1, pt2));
	}

}
