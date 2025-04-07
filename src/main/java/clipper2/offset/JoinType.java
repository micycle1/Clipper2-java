package clipper2.offset;

/**
 * The JoinType enumerator is only needed when offsetting (inflating/shrinking).
 * It isn't needed for polygon clipping.
 * <p>
 * When adding paths to a ClipperOffset object via the AddPaths method, the
 * joinType parameter may be any one of these types - Square, Round, Miter, or
 * Round.
 *
 */
public enum JoinType {
	/**
	 * Convex joins will be truncated using a 'squaring' edge. And the mid-points of
	 * these squaring edges will be exactly the offset distance away from their
	 * original (or starting) vertices.
	 */
	Square,
	/**
	 * Rounding is applied to all convex joins with the arc radius being the offset
	 * distance, and the original join vertex the arc center.
	 */
	Round,
	/**
	 * Edges are first offset a specified distance away from and parallel to their
	 * original (ie starting) edge positions. These offset edges are then extended
	 * to points where they intersect with adjacent edge offsets. However a limit
	 * must be imposed on how far mitered vertices can be from their original
	 * positions to avoid very convex joins producing unreasonably long and narrow
	 * spikes). To avoid unsightly spikes, joins will be 'squared' wherever
	 * distances between original vertices and their calculated offsets exceeds a
	 * specified value (expressed as a ratio relative to the offset distance).
	 */
	Miter,
	/**
	 * Bevelled joins are similar to 'squared' joins except that squaring won't
	 * occur at a fixed distance. While bevelled joins may not be as pretty as
	 * squared joins, bevelling is much easier (ie faster) than squaring. And
	 * perhaps this is why bevelling rather than squaring is preferred in numerous
	 * graphics display formats (including SVG and PDF document formats).
	 */
	Bevel;

}