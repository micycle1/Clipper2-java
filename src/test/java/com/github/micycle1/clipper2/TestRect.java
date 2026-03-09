package com.github.micycle1.clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.micycle1.clipper2.core.Rect64;

class TestRect {

	private static void assertRectEquals(Rect64 expected, Rect64 actual) {
		assertEquals(expected.left, actual.left);
		assertEquals(expected.top, actual.top);
		assertEquals(expected.right, actual.right);
		assertEquals(expected.bottom, actual.bottom);
	}

	@Test
	void testRectOpAdd() {
		{
			Rect64 lhs = new Rect64(false);
			Rect64 rhs = new Rect64(-1, -1, 10, 10);
			Rect64 sum = Rect64.opAdd(lhs, rhs);
			assertRectEquals(rhs, sum);
			sum = Rect64.opAdd(rhs, lhs);
			assertRectEquals(rhs, sum);
		}
		{
			Rect64 lhs = new Rect64(false);
			Rect64 rhs = new Rect64(1, 1, 10, 10);
			Rect64 sum = Rect64.opAdd(lhs, rhs);
			assertRectEquals(rhs, sum);
			sum = Rect64.opAdd(rhs, lhs);
			assertRectEquals(rhs, sum);
		}
		{
			Rect64 lhs = new Rect64(0, 0, 1, 1);
			Rect64 rhs = new Rect64(-1, -1, 0, 0);
			Rect64 expected = new Rect64(-1, -1, 1, 1);
			Rect64 sum = Rect64.opAdd(lhs, rhs);
			assertRectEquals(expected, sum);
			sum = Rect64.opAdd(rhs, lhs);
			assertRectEquals(expected, sum);
		}
		{
			Rect64 lhs = new Rect64(-10, -10, -1, -1);
			Rect64 rhs = new Rect64(1, 1, 10, 10);
			Rect64 expected = new Rect64(-10, -10, 10, 10);
			Rect64 sum = Rect64.opAdd(lhs, rhs);
			assertRectEquals(expected, sum);
			sum = Rect64.opAdd(rhs, lhs);
			assertRectEquals(expected, sum);
		}
	}
}
