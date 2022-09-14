package clipper2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;

public class ClipperFileIO {

	record TestCase(String caption, ClipType clipType, FillRule fillRule, double area, double count, int GetIdx, Paths64 subj,
			Paths64 subj_open, Paths64 clip) {
	}

	public static List<TestCase> loadTestCases(String testFileName) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get("src/test/resources/%s".formatted(testFileName)));

		String caption = "";
		ClipType ct = ClipType.None;
		FillRule fillRule = FillRule.EvenOdd;
		double area = 0;
		double count = 0;
		int GetIdx = 0;
		var subj = new Paths64();
		var subj_open = new Paths64();
		var clip = new Paths64();

		List<TestCase> cases = new ArrayList<>();

		for (String s : lines) {
			if (s.isBlank() || s.length() == 0) {
				cases.add(new TestCase(caption, ct, fillRule, area, count, GetIdx, new Paths64(subj), new Paths64(subj_open),
						new Paths64(clip)));
				subj.clear();
				subj_open.clear();
				clip.clear();
				continue;
			}

			if (s.indexOf("CAPTION: ") == 0) {
				caption = s.substring(9);
				continue;
			}

			if (s.indexOf("CLIPTYPE: ") == 0) {
				if (s.indexOf("INTERSECTION") > 0) {
					ct = ClipType.Intersection;
				} else if (s.indexOf("UNION") > 0) {
					ct = ClipType.Union;
				} else if (s.indexOf("DIFFERENCE") > 0) {
					ct = ClipType.Difference;
				} else {
					ct = ClipType.Xor;
				}
				continue;
			}

			if (s.indexOf("FILLTYPE: ") == 0 || s.indexOf("FILLRULE: ") == 0) {
				if (s.indexOf("EVENODD") > 0) {
					fillRule = FillRule.EvenOdd;
				} else if (s.indexOf("POSITIVE") > 0) {
					fillRule = FillRule.Positive;
				} else if (s.indexOf("NEGATIVE") > 0) {
					fillRule = FillRule.Negative;
				} else {
					fillRule = FillRule.NonZero;
				}
				continue;
			}

			if (s.indexOf("SOL_AREA: ") == 0) {
				area = Long.parseLong(s.substring(10));
				continue;
			}

			if (s.indexOf("SOL_COUNT: ") == 0) {
				count = Integer.parseInt(s.substring(11));
				continue;
			}

			if (s.indexOf("SUBJECTS_OPEN") == 0) {
				GetIdx = 2;
				continue;
			} else if (s.indexOf("SUBJECTS") == 0) {
				GetIdx = 1;
				continue;
			} else if (s.indexOf("CLIPS") == 0) {
				GetIdx = 3;
				continue;
			} else {
//				continue;
			}

			Paths64 paths = PathFromStr(s); // 0 or 1 path
			if (paths == null || paths.isEmpty()) {
				if (GetIdx == 3) {
//					return result;
				}
				if (s.indexOf("SUBJECTS_OPEN") == 0) {
					GetIdx = 2;
				} else if (s.indexOf("CLIPS") == 0) {
					GetIdx = 3;
				} else {
//					return result;
				}
				continue;
			}
			if (GetIdx == 1 && !paths.get(0).isEmpty()) {
				subj.add(paths.get(0));
			} else if (GetIdx == 2) {
				subj_open.add(paths.get(0));
			} else {
				clip.add(paths.get(0));
			}

		}

		if (cases.isEmpty()) {
			cases.add(
					new TestCase(caption, ct, fillRule, area, count, GetIdx, new Paths64(subj), new Paths64(subj_open), new Paths64(clip)));
		}

		return cases;
	}

	public static Paths64 PathFromStr(String s) {
		if (s == null) {
			return null;
		}
		Path64 p = new Path64();
		Paths64 pp = new Paths64();

		for (var pair : s.split(" ")) {
			var xy = pair.split(",");
			long x = Long.parseLong(xy[0]);
			long y = Long.parseLong(xy[1]);
			p.add(new Point64(x, y));
		}
		if (p.size() > 2) {
			pp.add(p);
		}
		return pp;
	}

}
