package clipper2.engine;

// Joiner: structure used in merging "touching" solution polygons
public class Joiner {

	public int idx;
	public OutPt op1;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public OutPt? op2;
	public OutPt op2;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Joiner? next1;
	public Joiner next1;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Joiner? next2;
	public Joiner next2;
//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Joiner? nextH;
	public Joiner nextH;

//C# TO JAVA CONVERTER WARNING: Nullable reference types have no equivalent in Java:
//ORIGINAL LINE: public Joiner(OutPt op1, OutPt? op2, System.Nullable<Joiner> nextH)
	public Joiner(OutPt op1, OutPt op2, Joiner nextH) {
		this.idx = -1;
		this.nextH = nextH;
		this.op1 = op1;
		this.op2 = op2;
		next1 = op1.joiner;
		op1.joiner = this;

		if (op2 != null) {
			next2 = op2.joiner;
			op2.joiner = this;
		} else {
			next2 = null;
		}
	}
}