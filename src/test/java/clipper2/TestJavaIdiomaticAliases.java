package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import clipper2.core.Point64;
import clipper2.core.PointD;
import clipper2.core.Rect64;
import clipper2.core.RectD;

class TestJavaIdiomaticAliases {

	@Test
	void rect64CamelCaseAliasesMatchPascalCaseMethods() {
		Rect64 rect = new Rect64(0, 0, 10, 10);
		Point64 point = new Point64(5, 5);
		Rect64 inner = new Rect64(1, 1, 9, 9);

		assertEquals(rect.AsPath(), rect.asPath());
		assertEquals(rect.IsEmpty(), rect.isEmpty());
		assertEquals(rect.IsValid(), rect.isValid());
		assertEquals(rect.MidPoint(), rect.midPoint());
		assertEquals(rect.Contains(point), rect.contains(point));
		assertEquals(rect.Contains(inner), rect.contains(inner));
		assertEquals(rect.Intersects(inner), rect.intersects(inner));
	}

	@Test
	void rectDCamelCaseAliasesMatchPascalCaseMethods() {
		RectD rect = new RectD(0, 0, 10, 10);
		PointD point = new PointD(5, 5);
		RectD inner = new RectD(1, 1, 9, 9);

		assertEquals(rect.AsPath(), rect.asPath());
		assertEquals(rect.IsEmpty(), rect.isEmpty());
		assertEquals(rect.MidPoint(), rect.midPoint());
		assertEquals(rect.Contains(point), rect.contains(point));
		assertEquals(rect.Contains(inner), rect.contains(inner));
		assertEquals(rect.Intersects(inner), rect.intersects(inner));
	}

	@Test
	void pointDNegateAliasMatchesExistingMethod() {
		PointD point = new PointD(3, -4);

		point.negate();

		assertTrue(point.equals(new PointD(-3, 4)));
	}
}
