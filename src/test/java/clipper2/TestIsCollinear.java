package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.InternalClipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.engine.Clipper64;

class TestIsCollinear {

	private static void assertMulHi(Method mulMethod, Field hiField, String aHex, String bHex, String expectedHiHex) throws Exception {
		long a = Long.parseUnsignedLong(aHex, 16);
		long b = Long.parseUnsignedLong(bHex, 16);
		long expectedHi = Long.parseUnsignedLong(expectedHiHex, 16);
		Object result = mulMethod.invoke(null, a, b);
		long hi = hiField.getLong(result);
		assertEquals(expectedHi, hi);
	}

	@Test
	void testHiCalculation() throws Exception {
		Method mulMethod = InternalClipper.class.getDeclaredMethod("multiplyUInt64", long.class, long.class);
		mulMethod.setAccessible(true);
		Class<?> resultClass = Class.forName("clipper2.core.InternalClipper$UInt128Struct");
		Field hiField = resultClass.getDeclaredField("hi64");
		hiField.setAccessible(true);

		assertMulHi(mulMethod, hiField, "51eaed81157de061", "3a271fb2745b6fe9", "129bbebdfae0464e");
		assertMulHi(mulMethod, hiField, "3a271fb2745b6fe9", "51eaed81157de061", "129bbebdfae0464e");
		assertMulHi(mulMethod, hiField, "c2055706a62883fa", "26c78bc79c2322cc", "1d640701d192519b");
		assertMulHi(mulMethod, hiField, "26c78bc79c2322cc", "c2055706a62883fa", "1d640701d192519b");
		assertMulHi(mulMethod, hiField, "874ddae32094b0de", "9b1559a06fdf83e0", "51f76c49563e5bfe");
		assertMulHi(mulMethod, hiField, "9b1559a06fdf83e0", "874ddae32094b0de", "51f76c49563e5bfe");
		assertMulHi(mulMethod, hiField, "81fb3ad3636ca900", "239c000a982a8da4", "12148e28207b83a3");
		assertMulHi(mulMethod, hiField, "239c000a982a8da4", "81fb3ad3636ca900", "12148e28207b83a3");
		assertMulHi(mulMethod, hiField, "4be0b4c5d2725c44", "990cd6db34a04c30", "2d5d1a4183fd6165");
		assertMulHi(mulMethod, hiField, "990cd6db34a04c30", "4be0b4c5d2725c44", "2d5d1a4183fd6165");
		assertMulHi(mulMethod, hiField, "978ec0c0433c01f6", "2df03d097966b536", "1b3251d91fe272a5");
		assertMulHi(mulMethod, hiField, "2df03d097966b536", "978ec0c0433c01f6", "1b3251d91fe272a5");
		assertMulHi(mulMethod, hiField, "49c5cbbcfd716344", "c489e3b34b007ad3", "38a32c74c8c191a4");
		assertMulHi(mulMethod, hiField, "c489e3b34b007ad3", "49c5cbbcfd716344", "38a32c74c8c191a4");
		assertMulHi(mulMethod, hiField, "d3361cdbeed655d5", "1240da41e324953a", "0f0f4fa11e7e8f2a");
		assertMulHi(mulMethod, hiField, "1240da41e324953a", "d3361cdbeed655d5", "0f0f4fa11e7e8f2a");
		assertMulHi(mulMethod, hiField, "51b854f8e71b0ae0", "6f8d438aae530af5", "239c04ee3c8cc248");
		assertMulHi(mulMethod, hiField, "6f8d438aae530af5", "51b854f8e71b0ae0", "239c04ee3c8cc248");
		assertMulHi(mulMethod, hiField, "bbecf7dbc6147480", "bb0f73d0f82e2236", "895170f4e9a216a7");
		assertMulHi(mulMethod, hiField, "bb0f73d0f82e2236", "bbecf7dbc6147480", "895170f4e9a216a7");
	}

	@Test
	void testIsCollinear() {
		// A large integer not representable exactly by double.
		final long i = 9007199254740993L;
		Point64 pt1 = new Point64(0, 0);
		Point64 sharedPt = new Point64(i, i * 10);
		Point64 pt2 = new Point64(i * 10, i * 100);
		assertTrue(InternalClipper.IsCollinear(pt1, sharedPt, pt2));
	}

	@Test
	void testIsCollinear2() { // see #831
		final long i = 0x4000000000000L;
		Path64 subject = new Path64(new Point64(-i, -i), new Point64(i, -i), new Point64(-i, i), new Point64(i, i));
		Clipper64 clipper = new Clipper64();
		clipper.AddSubject(new Paths64(subject));
		Paths64 solution = new Paths64();
		clipper.Execute(ClipType.Union, FillRule.EvenOdd, solution);
		assertEquals(2, solution.size());
	}
}
