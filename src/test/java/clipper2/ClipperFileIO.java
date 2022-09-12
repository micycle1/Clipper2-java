package clipper2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.Point64;

public class ClipperFileIO {

	record TestCase(String caption, ClipType clipType, FillRule fillRule, double area, double count, int GetIdx, List<List<Point64>> subj,
			List<List<Point64>> subj_open, List<List<Point64>> clip) {
	}

	public static List<TestCase> loadTestCases(String testFileName) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get("src/test/resources/%s".formatted(testFileName)));

		String caption = "";
		ClipType ct = ClipType.None;
		FillRule fillRule = FillRule.EvenOdd;
		double area = 0;
		double count = 0;
		int GetIdx = 0;
		var subj = new ArrayList<List<Point64>>();
		var subj_open = new ArrayList<List<Point64>>();
		var clip = new ArrayList<List<Point64>>();

		List<TestCase> cases = new ArrayList<>();

		for (String s : lines) {
			if (s.isBlank()) {
				cases.add(new TestCase(caption, ct, fillRule, area, count, GetIdx, new ArrayList<>(subj), new ArrayList<>(subj_open),
						new ArrayList<>(clip)));
				subj.clear();
				subj_open.clear();
				clip.clear();
			}

			if (s.indexOf("CAPTION: ") == 0) {
				caption = s.substring(9);
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

			List<List<Point64>> paths = PathFromStr(s); // 0 or 1 path
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
			if (GetIdx == 1) {
				subj.add(paths.get(0));
			} else if (GetIdx == 2) {
				subj_open.add(paths.get(0));
			} else {
				clip.add(paths.get(0));
			}

		}

		return cases;
	}

	public static List<List<Point64>> PathFromStr(String s) {
		if (s == null) {
			return null;
		}
		List<Point64> p = new ArrayList<>();
		List<List<Point64>> pp = new ArrayList<>();
		int len = s.length(), i = 0, j;
		while (i < len) {
			boolean isNeg;
			while (s.charAt(i) < 33 && i < len) {
				i++;
			}
			if (i >= len) {
				break;
			}
			// get X ...
			isNeg = s.charAt(i) == 45;
			if (isNeg) {
				i++;
			}
			if (i >= len || s.charAt(i) < 48 || s.charAt(i) > 57) {
				break;
			}
			j = i + 1;
			while (j < len && s.charAt(j) > 47 && s.charAt(j) < 58) {
				j++;
			}
			long x = Long.parseLong(s.substring(i, j));
			if (isNeg) {
				x = -x;
			}
			// skip space or comma between X & Y ...
			i = j;
			while (i < len && (s.charAt(i) == 32 || s.charAt(i) == 44)) {
				i++;
			}
			// get Y ...
			if (i >= len) {
				break;
			}
			isNeg = s.charAt(i) == 45;
			if (isNeg) {
				i++;
			}
			if (i >= len || s.charAt(i) < 48 || s.charAt(i) > 57) {
				break;
			}
			j = i + 1;
			while (j < len && s.charAt(j) > 47 && s.charAt(j) < 58) {
				j++;
			}
			long y = Long.parseLong(s.substring(i, j));
			if (isNeg) {
				y = -y;
			}
			p.add(new Point64(x, y));
			// skip trailing space, comma ...
			i = j;
			int nlCnt = 0;
			while (i < len && (s.charAt(i) < 33 || s.charAt(i) == 44)) {
				if (i >= len) {
					break;
				}
				if (s.charAt(i) == 10) {
					nlCnt++;
					if (nlCnt == 2) {
						if (p.size() > 2) {
							pp.add(p);
						}
						p = new ArrayList<>();
					}
				}
				i++;
			}
		}
		if (p.size() > 2) {
			pp.add(p);
		}
		return pp;
	}

}
