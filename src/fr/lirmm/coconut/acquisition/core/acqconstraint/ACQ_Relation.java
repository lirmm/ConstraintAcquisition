package fr.lirmm.coconut.acquisition.core.acqconstraint;

public enum ACQ_Relation {

	EqualXY(Operator.EQ, 2, true, true, false, true, true, false),
	DifferentXY(Operator.NEQ, 2, false, true, false, false, false, false),
	GreaterXY(Operator.GT, 2, false, false, true, false, true, false),
	LessXY(Operator.LT, 2, false, false, true, false, true, false),
	GreaterEqualXY(Operator.GE, 2, true, false, false, true, true, false),
	LessEqualXY(Operator.LE, 2, true, false, false, true, true, false),
	EqualX_(Operator.EQ, 1, true, true, false, true, true, false),
	DifferentX_(Operator.NEQ, 1, false, true, false, false, false, false),
	GreaterX_(Operator.GT, 1, false, false, true, false, true, false),
	LessX_(Operator.LT, 1, false, false, true, false, true, false),
	GreaterEqualX_(Operator.GE, 1, true, false, false, true, true, false),
	LessEqualX_(Operator.LE, 1, true, false, false, true, true, false),
	InDiag1(Operator.EQ, 2, true, true, false, true, true, false),
	OutDiag1(Operator.NEQ, 2, false, true, false, false, false, false),
	InDiag2(Operator.EQ, 2, true, true, false, true, true, false),
	OutDiag2(Operator.NEQ, 2, false, true, false, false, false, false),
	AT_Equal(Operator.EQ, 2, true, true, false, true, true, false),
	AT_Diff(Operator.NEQ, 2, false, true, false, false, false, false),
	AT_GT(Operator.GT, 2, false, false, true, false, true, false),
	AT_LT(Operator.LT, 2, false, false, true, false, true, false),
	AT_GE(Operator.GE, 2, true, false, false, true, true, false),
	AT_LE(Operator.LE, 2, true, false, false, true, true, false),
	DistEqXY(Operator.EQ, 2, true, true, false, true, true, false),
	DistDiffXY(Operator.NEQ, 2, false, true, false, false, false, false),
	DistEqualXYZ(Operator.EQ, 3, true, true, false, true, true, false),
	DistDiffXYZ(Operator.NEQ, 3, false, true, false, false, false, false),
	DistGreaterXYZ(Operator.GT, 3, false, false, true, false, true, false),
	DistLessXYZ(Operator.LT, 3, false, false, true, false, true, false),
	DistGreaterEqualXYZ(Operator.GE, 3, true, false, false, true, true, false),
	DistLessEqualXYZ(Operator.LE, 3, true, false, false, true, true, false),
	DistEqualXYZT(Operator.EQ, 4, true, true, false, true, true, false),
	DistDiffXYZT(Operator.NEQ, 4, false, true, false, false, false, false),
	DistGreaterXYZT(Operator.GT, 4, false, false, true, false, true, false),
	DistLessXYZT(Operator.LT, 4, false, false, true, false, true, false),
	DistGreaterEqualXYZT(Operator.GE, 4, true, false, false, true, true, false),
	DistLessEqualXYZT(Operator.LE, 4, true, false, false, true, true, false),
	PEqualXY(Operator.EQ, 2, true, true, false, true, true, true),
	PDiffXY(Operator.NEQ, 2, false, true, false, false, false, true),
	PGreaterXY(Operator.GT, 2, false, false, true, false, true, true),
	PLessXY(Operator.LT, 2, false, false, true, false, true, true),
	PGreaterEqualXY(Operator.GE, 2, true, false, false, true, true, true),
	PLessEqualXY(Operator.LE, 2, true, false, false, true, true, true)

	;

	final Operator op;
	final int arity;
	final boolean reflexive;
	final boolean symmetric;
	final boolean asymmetric;
	final boolean antisymmetric;
	final boolean transitive;
	final boolean prec;

	ACQ_Relation(Operator op, int arity, boolean R, boolean S, boolean As, boolean Anti, boolean Tran, boolean prec) {
		this.op = op;
		this.arity = arity;
		this.reflexive = R;
		this.symmetric = S;
		this.asymmetric = As;
		this.antisymmetric = Anti;
		this.transitive = Tran;
		this.prec = prec;

	}

	public int getArity() {

		return arity;
	}

	public boolean IsPrecedence() {

		return prec;
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

	public Operator getOperator() {
		// TODO Auto-generated method stub
		return op;
	}

	public ACQ_Relation getNegation() {
		switch (this) {
		case EqualXY:
			return DifferentXY;
		case DifferentXY:
			return EqualXY;
		case GreaterXY:
			return LessEqualXY;
		case LessEqualXY:
			return GreaterXY;
		case GreaterEqualXY:
			return LessXY;
		case LessXY:
			return GreaterEqualXY;
		case EqualX_:
			return DifferentX_;
		case DifferentX_:
			return EqualX_;
		case GreaterX_:
			return LessEqualX_;
		case LessEqualX_:
			return GreaterX_;
		case GreaterEqualX_:
			return LessX_;
		case LessX_:
			return GreaterEqualX_;
		case AT_GT:
			return AT_LT;
		case AT_LE:
			return AT_GE;
		case AT_GE:
			return AT_LE;
		case AT_LT:
			return AT_GT;
		case DistGreaterXYZ:
			return DistLessXYZ;
		case DistLessEqualXYZ:
			return DistGreaterEqualXYZ;
		case DistLessXYZ:
			return DistGreaterXYZ;
		case DistGreaterEqualXYZ:
			return DistLessEqualXYZ;
		case DistGreaterXYZT:
			return DistLessXYZT;
		case DistLessEqualXYZT:
			return DistGreaterEqualXYZT;
		case DistLessXYZT:
			return DistGreaterXYZT;
		case DistGreaterEqualXYZT:
			return DistLessEqualXYZT;
		case PLessEqualXY:
			return PGreaterEqualXY;
		case PGreaterXY:
			return PLessXY;
		case PGreaterEqualXY:
			return PLessEqualXY;
		case PLessXY:
			return PGreaterXY;
		default:
			throw new UnsupportedOperationException();
		}
	}
	
	public ACQ_Relation getRightDirection() {			//NL: if < then >...  // precondition: not symetric relation
		switch (this) {
		case GreaterXY:
			return LessXY;
		case LessEqualXY:
			return GreaterEqualXY;
		case GreaterEqualXY:
			return LessEqualXY;
		case LessXY:
			return GreaterXY;
		case GreaterX_:
			return LessX_;
		case LessEqualX_:
			return GreaterEqualX_;
		case GreaterEqualX_:
			return LessEqualX_;
		case LessX_:
			return GreaterX_;
		case InDiag1:
			return OutDiag1;
		case OutDiag1:
			return InDiag1;
		case InDiag2:
			return OutDiag2;
		case OutDiag2:
			return InDiag2;
		case AT_Equal:
			return AT_Diff;
		case AT_Diff:
			return AT_Equal;
		case AT_GT:
			return AT_LE;
		case AT_LE:
			return AT_GT;
		case AT_GE:
			return AT_LT;
		case AT_LT:
			return AT_GE;
		case DistEqXY:
			return DistDiffXY;
		case DistDiffXY:
			return DistEqXY;
		case DistEqualXYZ:
			return DistDiffXYZ;
		case DistDiffXYZ:
			return DistEqualXYZ;
		case DistGreaterXYZ:
			return DistLessEqualXYZ;
		case DistLessEqualXYZ:
			return DistGreaterXYZ;
		case DistLessXYZ:
			return DistGreaterEqualXYZ;
		case DistGreaterEqualXYZ:
			return DistLessXYZ;
		case DistEqualXYZT:
			return DistDiffXYZT;
		case DistDiffXYZT:
			return DistEqualXYZT;
		case DistGreaterXYZT:
			return DistLessEqualXYZT;
		case DistLessEqualXYZT:
			return DistGreaterXYZT;
		case DistLessXYZT:
			return DistGreaterEqualXYZT;
		case DistGreaterEqualXYZT:
			return DistLessXYZT;
		case PLessEqualXY:
			return PGreaterXY;
		case PGreaterXY:
			return PLessEqualXY;
		case PGreaterEqualXY:
			return PLessXY;
		case PLessXY:
			return PGreaterEqualXY;
		case PEqualXY:
			return PDiffXY;
		case PDiffXY:
			return PEqualXY;

		default:
			throw new UnsupportedOperationException();
		}
	}

}