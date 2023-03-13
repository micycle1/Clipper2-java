package clipper2;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class ClipperFileIO {

	static class TestCase{
		private final String caption;
		private final ClipType clipType;
		private final FillRule fillRule;
		private final long area;
		private final int count;
		private final int GetIdx;
		private final Paths64 subj;
		private final Paths64 subj_open;
		private final Paths64 clip;
		private final int testNum;

		TestCase(
	String caption, ClipType clipType, FillRule fillRule, long area, int count, int GetIdx, Paths64 subj, Paths64 subj_open, Paths64 clip, int testNum) {
			this.caption = caption;
			this.clipType = clipType;
			this.fillRule = fillRule;
			this.area = area;
			this.count = count;
			this.GetIdx = GetIdx;
			this.subj = subj;
			this.subj_open = subj_open;
			this.clip = clip;
			this.testNum = testNum;
		}

		public String caption() {
			return caption;
		}

		public ClipType clipType() {
			return clipType;
		}

		public FillRule fillRule() {
			return fillRule;
		}

		public long area() {
			return area;
		}

		public int count() {
			return count;
		}

		public int GetIdx() {
			return GetIdx;
		}

		public Paths64 subj() {
			return subj;
		}

		public Paths64 subj_open() {
			return subj_open;
		}

		public Paths64 clip() {
			return clip;
		}

		public int testNum() {
			return testNum;
		}
	}

	static List<TestCase> loadTestCases(String testFileName) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get(String.format("src/test/resources/%s", testFileName)));
		lines = new ArrayList<>(lines);
		lines.add("");

		String caption = "";
		ClipType ct = ClipType.None;
		FillRule fillRule = FillRule.EvenOdd;
		long area = 0;
		int count = 0;
		int GetIdx = 0;
		Paths64 subj = new Paths64();
		Paths64 subj_open = new Paths64();
		Paths64 clip = new Paths64();

		List<TestCase> cases = new ArrayList<>();

		for (String s : lines) {
			if (s.matches("\\s*")) {
				if (GetIdx != 0) {
					cases.add(new TestCase(caption, ct, fillRule, area, count, GetIdx, new Paths64(subj), new Paths64(subj_open),
							new Paths64(clip), cases.size() + 1));
					subj.clear();
					subj_open.clear();
					clip.clear();
					GetIdx = 0;
				}
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

		return cases;
	}

	static Paths64 PathFromStr(String s) {
		if (s == null) return new Paths64();
		Path64 p = new Path64();
		Paths64 pp = new Paths64();
		int len = s.length(), i = 0, j;
		while (i < len)
		{
			boolean isNeg;
			while (s.charAt(i) < 33 && i < len) i++;
			if (i >= len) break;
			//get X ...
			isNeg = s.charAt(i) == 45;
			if (isNeg) i++;
			if (i >= len || s.charAt(i) < 48 || s.charAt(i) > 57) break;
			j = i + 1;
			while (j < len && s.charAt(j) > 47 && s.charAt(j) < 58) j++;
			Long x = LongTryParse(s.substring(i, j));
			if (x == null) break;
			if (isNeg) x = -x;
			//skip space or comma between X & Y ...
			i = j;
			while (i < len && (s.charAt(i) == 32 || s.charAt(i) == 44)) i++;
			//get Y ...
			if (i >= len) break;
			isNeg = s.charAt(i) == 45;
			if (isNeg) i++;
			if (i >= len || s.charAt(i) < 48 || s.charAt(i) > 57) break;
			j = i + 1;
			while (j < len && s.charAt(j) > 47 && s.charAt(j) < 58) j++;
			Long y = LongTryParse(s.substring(i, j));
			if (y == null) break;
			if (isNeg) y = -y;
			p.add(new Point64(x, y));
			//skip trailing space, comma ...
			i = j;
			int nlCnt = 0;
			while (i < len && (s.charAt(i) < 33 || s.charAt(i) == 44))
			{
				if (i >= len) break;
				if (s.charAt(i) == 10)
				{
					nlCnt++;
					if (nlCnt == 2)
					{
						if (p.size() > 0) pp.add(p);
						p = new Path64();
					}
				}
				i++;
			}
		}
		if (p.size() > 0) pp.add(p);
		return pp;
	}

	private static @Nullable Long LongTryParse(String s) {
		try {
			return Long.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
