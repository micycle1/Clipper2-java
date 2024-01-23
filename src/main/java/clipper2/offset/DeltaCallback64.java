package clipper2.offset;

import clipper2.core.Path64;
import clipper2.core.PathD;

/**
 * Functional interface for calculating a variable delta during polygon
 * offsetting.
 * <p>
 * Implementations of this interface define how to calculate the delta (the
 * amount of offset) to apply at each point in a polygon during an offset
 * operation. The offset can vary from point to point, allowing for variable
 * offsetting.
 */
@FunctionalInterface
public interface DeltaCallback64 {
	/**
	 * Calculates the delta (offset) for a given point in the polygon path.
	 * <p>
	 * This method is used during polygon offsetting operations to determine the
	 * amount by which each point of the polygon should be offset.
	 *
	 * @param path       The {@link Path64} object representing the original polygon
	 *                   path.
	 * @param path_norms The {@link PathD} object containing the normals of the
	 *                   path, which may be used to influence the delta calculation.
	 * @param currPt     The index of the current point in the path for which the
	 *                   delta is being calculated.
	 * @param prevPt     The index of the previous point in the path, which can be
	 *                   referenced to determine the delta based on adjacent
	 *                   segments.
	 * @return A {@code double} value representing the calculated delta for the
	 *         current point. This value will be used to offset the point in the
	 *         resulting polygon.
	 */
	double calculate(Path64 path, PathD path_norms, int currPt, int prevPt);
}
