package clipper2;

import clipper2.core.Rect64;
import clipper2.core.RectD;

public class Clipper {

	public static final Rect64 MaxInvalidRect64 = new Rect64(Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
	public static final RectD MaxInvalidRectD = new RectD(Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

}
