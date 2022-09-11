package clipper2.core;

/**
 * By far the most widely used filling rules for polygons are EvenOdd and
 * NonZero, sometimes called Alternate and Winding respectively.
 * <p>
 * https://en.wikipedia.org/wiki/Nonzero-rule
 */
public enum FillRule {

	EvenOdd, NonZero, Positive, Negative;

}