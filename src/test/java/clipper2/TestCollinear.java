package clipper2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import clipper2.core.Path64;

class TestCollinear {

	@Test
	void TrimCollinearHandlesLargeIntegerCoordinates() {
		long n = 1_000_000_000_000_000L;
		Path64 path = Clipper.MakePath(new long[] {
				0, 0,
				3 * n, n,
				9 * n, 3 * n,
				9 * n, 0
		});

		Path64 trimmed = Clipper.TrimCollinear(path);
		assertEquals(3, trimmed.size());
	}
}
