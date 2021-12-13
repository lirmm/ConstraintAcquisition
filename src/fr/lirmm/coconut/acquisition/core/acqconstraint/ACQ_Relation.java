package fr.lirmm.coconut.acquisition.core.acqconstraint;

public enum ACQ_Relation {
	EqualXY(Operator.EQ,2,true, true, false, true, true,false),
	DifferentXY(Operator.NEQ,2, false, true, false, false, false,false),
	GreaterXY(Operator.GT,2,false, false, true, false, true,false),
	LessXY(Operator.LT,2,false, false, true, false, true,false),
	GreaterEqualXY(Operator.GE,2,true, false, false, true, true,false),
	LessEqualXY(Operator.LE,2, true, false, false, true, true,false),
	EqualX_(Operator.EQ,1,true, true, false, true, true,false),
	DifferentX_(Operator.NEQ,1, false, true, false, false, false,false),
	GreaterX_(Operator.GT,1,false, false, true, false, true,false),
	LessX_(Operator.LT,1,false, false, true, false, true,false),
	GreaterEqualX_(Operator.GE,1,true, false, false, true, true,false),
	LessEqualX_(Operator.LE,1, true, false, false, true, true,false),
	InDiag1(Operator.EQ,2,true, true, false, true, true,false),
	OutDiag1(Operator.NEQ,2, false, true, false, false, false,false),
	InDiag2(Operator.EQ,2,true, true, false, true, true,false),
	OutDiag2(Operator.NEQ,2, false, true, false, false, false,false),
	AT_Equal(Operator.EQ,2,true, true, false, true, true,false),
	AT_Diff(Operator.NEQ,2, false, true, false, false, false,false),
	AT_GT(Operator.GT,2,false, false, true, false, true,false),
	AT_LT(Operator.LT,2,false, false, true, false, true,false),
	AT_GE(Operator.GE,2,true, false, false, true, true,false),
	AT_LE(Operator.LE,2, true, false, false, true, true,false),
	DistEqXY(Operator.EQ,2,true, true, false, true, true,false),
	DistDiffXY(Operator.NEQ,2, false, true, false, false, false,false),
	DistEqualXYZ(Operator.EQ,3,true, true, false, true, true,false),
	DistDiffXYZ(Operator.NEQ,3, false, true, false, false, false,false),
	DistGreaterXYZ(Operator.GT,3,false, false, true, false, true,false),
	DistLessXYZ(Operator.LT,3,false, false, true, false, true,false),
	DistGreaterEqualXYZ(Operator.GE,3,true, false, false, true, true,false),
	DistLessEqualXYZ(Operator.LE,3, true, false, false, true, true,false),
	DistEqualXYZT(Operator.EQ,4,true, true, false, true, true,false),
	DistDiffXYZT(Operator.NEQ,4, false, true, false, false, false,false),
	DistGreaterXYZT(Operator.GT,4,false, false, true, false, true,false),
	DistLessXYZT(Operator.LT,4,false, false, true, false, true,false),
	DistGreaterEqualXYZT(Operator.GE,4,true, false, false, true, true,false),
	DistLessEqualXYZT(Operator.LE,4, true, false, false, true, true,false),
	MeetsXY(Operator.EQ,2,false, false, false, false, false,true),
	NotMeetXY(Operator.NEQ,2, false, false, false, false, false,true),
	IsMetXY(Operator.EQ,2,false, false, false, false, false,true),
	IsNotMetXY(Operator.NEQ,2, false, false, false, false, false,true),
	PFinishXY(Operator.EQ,2,true, true, false, true, true,true),
	PNotFinishXY(Operator.NEQ,2, false, true, false, false, false,true),
	PGreaterXY(Operator.GT,2,false, false, true, false, true,true),
	PLessXY(Operator.LT,2,false, false, true, false, true,true),
	PGreaterEqualXY(Operator.GE,2,true, false, false, true, true,true),
	PrecedesXY(Operator.LT,2, true, false, false, true, true,true),
	IsPrecededXY(Operator.LT,2, true, false, false, true, true,true),
	NotPrecedesXY(Operator.GE,2, true, false, false, true, true,true),
	IsNotPrecededXY(Operator.GE,2, true, false, false, true, true,true),
	OverlapsXY(Operator.NONE,2, true, false, true, true, false,true),
	NotOverlapsXY(Operator.NONE,2, true, false, true, true, false,true),
	IsOverlappedXY(Operator.NONE,2, false, false, false, false, false,true),
	IsNotOverlappedXY(Operator.NONE,2, false, false, false, false, false,true),
	DuringXY(Operator.NONE,2, true, false, true, false, true,true),
	NotDuringXY(Operator.NONE,2, true, false, true, false, true,true),
	ContainsXY(Operator.NONE,2, false, false, false, false, false,true),
	NotContainsXY(Operator.NONE,2, false, false, false, false, false,true),
	ExactXY(Operator.NONE,2, true, true, false, true, true,true),
	NotExactXY(Operator.NONE,2, true, true, false, true, true,true),
	FinishXY(Operator.NONE,2, false, true, false, false, false,true),
	NotFinishXY(Operator.NONE,2, false, true, false, false, false,true),
	IsNotFinishedXY(Operator.NONE,2, false, true, false, false, false,true),
	IsFinishedXY(Operator.NONE,2, false, true, false, false, false,true),
	StartsXY(Operator.NONE,2, false, true, false, false, false,true),
	NotStartsXY(Operator.NONE,2, false, true, false, false, false,true),
	IsStartedXY(Operator.NONE,2, false, true, false, false, false,true),
	IsNotStartedXY(Operator.NONE,2, false, true, false, false, false,true),
	
	
	DisconnectedXY(Operator.NONE,2, false, true, false, false, false,true),
	ExternallyConnectedXY(Operator.NONE,2, false, true, false, false, false,true),
	TangentialProperPartXY(Operator.NONE,2, false, true, false, false, false,true),
	TangentialProperPartInverseXY(Operator.NONE,2, false, true, false, false, false,true),
	PartiallyOverlappingXY(Operator.NONE,2, false, true, false, false, false,true),
	NonTangentialProperPartXY(Operator.NONE,2, false, true, false, false, false,true),
	NonTangentialProperPartInverseXY(Operator.NONE,2, false, true, false, false, false,true),
	REqualXY(Operator.NONE,2, false, true, false, false, false,true);

	final Operator op;
	final int arity;
	final boolean reflexive;
	final boolean symmetric;
	final boolean asymmetric;
	final boolean antisymmetric;
	final boolean transitive;
	final boolean allen;

	ACQ_Relation(Operator op, int arity, boolean R, boolean S, boolean As, boolean Anti, boolean Tran,boolean prec) {
		this.op = op;
		this.arity= arity;
		this.reflexive = R;
		this.symmetric = S;
		this.asymmetric = As;
		this.antisymmetric = Anti;
		this.transitive = Tran;
		this.allen = prec;

	}

	public int getArity() {
		
		return arity;
	}
	public boolean IsAllen() {
		
		return allen;
	}
	public boolean isReflexive() {
		return reflexive;
	}

	public boolean isSymmetric() {
		return symmetric;
	}

	public boolean isAsymmetric() {
		return asymmetric;
	}

	public boolean isAntisymmetric() {
		return antisymmetric;
	}

	public boolean isTransitive() {
		return transitive;
	}
	public boolean IsPrecedence() {
		return allen;
	}
	
	public Operator getOperator() {
		// TODO Auto-generated method stub
		return op;
	}
	
	public  ACQ_Relation getNegation() {
		switch (this){
			case EqualXY:return DifferentXY;
			case DifferentXY:return EqualXY;
			case GreaterXY: return LessEqualXY;
			case LessEqualXY: return GreaterXY;
			case GreaterEqualXY: return LessXY;
			case LessXY: return GreaterEqualXY;
			case EqualX_: return DifferentX_;
			case DifferentX_: return EqualX_;
			case GreaterX_: return LessEqualX_;
			case LessEqualX_: return GreaterX_;
			case GreaterEqualX_: return LessX_;
			case LessX_: return GreaterEqualX_;
			case InDiag1: return OutDiag1;
			case OutDiag1: return InDiag1;
			case InDiag2: return OutDiag2;
			case OutDiag2: return InDiag2;
			case AT_Equal: return AT_Diff;
			case AT_Diff: return AT_Equal;
			case AT_GT: return AT_LE;
			case AT_LE: return AT_GT;
			case AT_GE: return AT_LT;
			case AT_LT: return AT_GE;
			case DistEqXY: return DistDiffXY;
			case DistDiffXY: return DistEqXY;
			case DistEqualXYZ: return DistDiffXYZ;
			case DistDiffXYZ: return DistEqualXYZ;
			case DistGreaterXYZ: return DistLessEqualXYZ;
			case DistLessEqualXYZ: return DistGreaterXYZ;
			case DistLessXYZ: return DistGreaterEqualXYZ;
			case DistGreaterEqualXYZ: return DistLessXYZ;
			case DistEqualXYZT: return DistDiffXYZT;
			case DistDiffXYZT: return DistEqualXYZT;
			case DistGreaterXYZT: return DistLessEqualXYZT;
			case DistLessEqualXYZT: return DistGreaterXYZT;
			case DistLessXYZT: return DistGreaterEqualXYZT;
			case DistGreaterEqualXYZT: return DistLessXYZT;
			case PrecedesXY: return NotPrecedesXY;
			case IsPrecededXY: return IsNotPrecededXY;
			case NotPrecedesXY : return PrecedesXY;
			case IsNotPrecededXY: return IsPrecededXY;
			case MeetsXY: return NotMeetXY;
			case NotMeetXY: return MeetsXY;
			case IsMetXY: return IsNotMetXY;
			case IsNotMetXY: return IsMetXY;
			case OverlapsXY: return NotOverlapsXY;
			case IsOverlappedXY: return IsNotOverlappedXY;
			case  NotOverlapsXY: return OverlapsXY;
			case  IsNotOverlappedXY: return IsOverlappedXY;
			case IsStartedXY: return IsNotStartedXY;
			case IsNotStartedXY: return IsStartedXY;
			case DuringXY: return NotDuringXY;
			case NotDuringXY: return DuringXY;
			case ContainsXY: return NotContainsXY;
			case NotContainsXY: return ContainsXY;
			case FinishXY: return NotFinishXY;
			case NotFinishXY: return FinishXY;
			case ExactXY: return NotExactXY;
			case NotExactXY: return ExactXY;
			
			case DisconnectedXY: return NotDuringXY;
			case ExternallyConnectedXY: return DuringXY;
			case TangentialProperPartXY: return NotContainsXY;
			case TangentialProperPartInverseXY: return ContainsXY;
			case PartiallyOverlappingXY: return NotFinishXY;
			case NonTangentialProperPartXY: return FinishXY;
			case NonTangentialProperPartInverseXY: return NotExactXY;
			case REqualXY: return NotExactXY;

			default:throw new UnsupportedOperationException();
		}
	}


}