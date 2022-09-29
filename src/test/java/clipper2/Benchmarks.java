package clipper2;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.engine.Clipper64;

/**
 * Benchmark class is located within test folder in order to be found by
 * jmh-maven-plugin.
 */
public class Benchmarks {
	
	private static final int DisplayWidth = 800;
	private static final int DisplayHeight = 600;

	@State(Scope.Benchmark)
	public static class BenchmarkState {

		Paths64 subj;
		Paths64 clip;
		Paths64 solution;

		@Param({ "1000", "2000", "3000", "4000" })
		public int edgeCount;

		@Setup(Level.Invocation)
		public void setup() {
			Random rand = new Random();

			subj = new Paths64();
			clip = new Paths64();
			solution = new Paths64();

			subj.add(MakeRandomPath(DisplayWidth, DisplayHeight, edgeCount, rand));
			clip.add(MakeRandomPath(DisplayWidth, DisplayHeight, edgeCount, rand));
		}

	}

	@Benchmark
	@OutputTimeUnit(TimeUnit.SECONDS)
	public void Intersection_N(BenchmarkState state) {
		Clipper64 c = new Clipper64();
		c.AddSubject(state.subj);
		c.AddClip(state.clip);
		c.Execute(ClipType.Intersection, FillRule.NonZero, state.solution);
	}

	private static Point64 MakeRandomPt(int maxWidth, int maxHeight, Random rand) {
		long x = rand.nextLong(maxWidth);
		long y = rand.nextLong(maxHeight);
		return new Point64(x, y);
	}

	private static Path64 MakeRandomPath(int width, int height, int count, Random rand) {
		Path64 result = new Path64(count);
		for (int i = 0; i < count; ++i)
			result.add(MakeRandomPt(width, height, rand));
		return result;
	}

}
