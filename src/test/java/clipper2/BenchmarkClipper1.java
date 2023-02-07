package clipper2;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Setup;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import de.lighti.clipper.Clipper.ClipType;
import de.lighti.clipper.Clipper.PolyFillType;
import de.lighti.clipper.Clipper.PolyType;
import de.lighti.clipper.DefaultClipper;
import de.lighti.clipper.Path;
import de.lighti.clipper.Paths;
import de.lighti.clipper.Point.LongPoint;

/**
 * Benchmarks for Clipper 1. Class is located within test folder in order to be
 * found by jmh-maven-plugin.
 */
@Measurement(time = 3)
public class BenchmarkClipper1 {

	private static final int DisplayWidth = 800;
	private static final int DisplayHeight = 600;

	private static long seed = 0;

	@State(Scope.Benchmark)
	public static class BenchmarkState {

		Paths subj;
		Paths clip;
		Paths solution;

		@Param({ "1000", "2000", "4000" })
		public int edgeCount;

		@Setup(Level.Invocation) // recreate path for each edgeCount run
		public void setup() {
			Random rand = new Random(seed++);

			subj = new Paths();
			clip = new Paths();
			solution = new Paths();

			subj.add(MakeRandomPath(DisplayWidth, DisplayHeight, edgeCount, rand));
			clip.add(MakeRandomPath(DisplayWidth, DisplayHeight, edgeCount, rand));
		}

	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.SECONDS)
	public void Intersection(BenchmarkState state) {
		DefaultClipper c = new DefaultClipper();
		c.addPaths(state.subj, PolyType.SUBJECT, true);
		c.addPaths(state.clip, PolyType.CLIP, true);
		c.execute(ClipType.INTERSECTION, state.solution, PolyFillType.NON_ZERO, PolyFillType.NON_ZERO);
	}

	private static LongPoint MakeRandomPt(int maxWidth, int maxHeight, Random rand) {
		long x = rand.nextInt(maxWidth);
		long y = rand.nextInt(maxHeight);
		return new LongPoint(x, y);
	}

	private static Path MakeRandomPath(int width, int height, int count, Random rand) {
		Path result = new Path(count);
		for (int i = 0; i < count; ++i) {
			result.add(MakeRandomPt(width, height, rand));
		}
		return result;
	}

}
