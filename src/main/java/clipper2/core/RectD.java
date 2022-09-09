package clipper2.core;

//C# TO JAVA CONVERTER WARNING: Java does not allow user-defined value types. The behavior of this class may differ from the original:
//ORIGINAL LINE: public struct RectD
public final class RectD {
	public double left;
	public double top;
	public double right;
	public double bottom;

	public RectD() {
	}

	public RectD(double l, double t, double r, double b) {
		left = l;
		top = t;
		right = r;
		bottom = b;
	}

	public RectD(RectD rec) {
		left = rec.left;
		top = rec.top;
		right = rec.right;
		bottom = rec.bottom;
	}

	public double getWidth() {
		return right - left;
	}

	public void setWidth(double value) {
		right = left + value;
	}

	public double getHeight() {
		return bottom - top;
	}

	public void setHeight(double value) {
		bottom = top + value;
	}

	public boolean IsEmpty() {
		return bottom <= top || right <= left;
	}

	public PointD MidPoint() {
		return new PointD((left + right) / 2, (top + bottom) / 2);
	}

	public boolean PtIsInside(PointD pt) {
		return pt.x > left && pt.x < right && pt.y > top && pt.y < bottom;
	}

	@Override
	public RectD clone() {
		RectD varCopy = new RectD();

		varCopy.left = this.left;
		varCopy.top = this.top;
		varCopy.right = this.right;
		varCopy.bottom = this.bottom;

		return varCopy;
	}
}