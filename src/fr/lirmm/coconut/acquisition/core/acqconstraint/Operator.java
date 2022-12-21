package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.HashMap;

public enum Operator {

	NONE(), EQ(), LT(), GT(), NEQ(), LE(), GE(), PL(), MN(), Dist();

	private static HashMap<String, Operator> operators = new HashMap<>();

	static {
		operators.put("@", Operator.NONE);
		operators.put("=", Operator.EQ);
		operators.put(">", Operator.GT);
		operators.put(">=", Operator.GE);
		operators.put("<", Operator.LT);
		operators.put("<=", Operator.LE);
		operators.put("!=", Operator.NEQ);
		operators.put("+", Operator.PL);
		operators.put("-", Operator.MN);
		operators.put("abs", Operator.Dist);
	}

	public static Operator get(String name) {
		return operators.get(name);
	}

	@Override
	public String toString() {
		switch (this) {
		case LT:
			return "<";
		case GT:
			return ">";
		case LE:
			return "<=";
		case GE:
			return ">=";
		case NEQ:
			return "!=";
		case EQ:
			return "=";
		case PL:
			return "+";
		case MN:
			return "-";
		case Dist:
			return "dist";

		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Flips the direction of an inequality
	 * 
	 * @param operator op to flip
	 */
	public static String getFlip(String operator) {
		switch (get(operator)) {
		case LT:
			return ">";
		case GT:
			return "<";
		case LE:
			return ">=";
		case GE:
			return "<=";
		default:
			return operator;
		}
	}

	public static Operator getOpposite(Operator operator) {
		switch (operator) {
		case LT:
			return GE;
		case GT:
			return LE;
		case LE:
			return GT;
		case GE:
			return LT;
		case NEQ:
			return EQ;
		case EQ:
			return NEQ;
		case PL:
			return PL; // NL: neutral negation on PL and MN
		case MN:
			return MN;
		default:
			throw new UnsupportedOperationException();
		}
	}
	
	public static Operator getRightDirection(Operator operator) {
		switch (operator) {
		case LT:
			return GT;
		case GT:
			return LT;
		case LE:
			return GE;
		case GE:
			return LE;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static Operator getOperator(String s) {
		System.out.println(s);
		switch (s) {
		case ("EqualXY"):
			return Operator.EQ;
		case ("DiffXY"):
			return Operator.NEQ;
		case ("GreaterOrEqualXY"):
			return Operator.GE;
		case ("GreaterXY"):
			return Operator.GT;
		case ("LessOrEqualXY"):
			return Operator.LE;
		case ("LessXY"):
			return Operator.LT;
		case ("EqualX"):
			return Operator.EQ;
		case ("DiffX"):
			return Operator.NEQ;
		case ("GreaterOrEqualX"):
			return Operator.GE;
		case ("GreaterX"):
			return Operator.GT;
		case ("LessOrEqualX"):
			return Operator.LE;
		case ("LessX"):
			return Operator.LT;
		default:
			return getOperatorUnary(s);
		}
	}

	public static Operator getOperatorUnary(String s) {
		if (s.matches("^DiffX.*"))
			return Operator.NEQ;
		else if (s.matches("^EqualX.*"))
			return Operator.EQ;
		else if (s.matches("^GreaterOrEqualX.*"))
			return Operator.GE;
		else if (s.matches("^GreaterX.*"))
			return Operator.GT;
		else if (s.matches("^LessOrEqualX.*"))
			return Operator.LE;
		else if (s.matches("^LessX.*"))
			return Operator.LT;
		throw new UnsupportedOperationException();
	
	
	}
	
}
