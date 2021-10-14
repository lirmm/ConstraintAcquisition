
package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

/**
 * Constraint between n variables. For example : X - Y + Z =0 or 2X+Y-Z > 2
 * 
 */
public class ScalarArithmetic extends ScalarConstraint {

	private final Operator op1;

	// required visibility to allow exportation
	protected final int cste;
	private final int[] vars;
	private final int[] coeff;
	private final String negation;

	/**
	 * Constructor for a constraint between three variables.
	 * 
	 * @param name  Name of this constraint
	 * @param var   Array of Variables of the constraint
	 * @param coeff Coefficient of the Variables of the constraint
	 * @param op1   Operator 1 of this constraint
	 * @param cste  constant of this constraint
	 * 
	 * @example X + Y -Z < 0
	 */
	public ScalarArithmetic(String name, int[] var, int[] coeif, Operator op, int cste) {
		super(name, var, coeif, cste);
		this.op1 = op;
		this.cste = cste;
		this.coeff = coeif;
		this.vars = var;
		this.negation = "UNKNOWN";
	}

	public ScalarArithmetic(String name, int[] var, int[] coeif, Operator op, int cste, String negation) {
		super(name, var, coeif, cste);
		this.op1 = op;
		this.cste = cste;
		this.coeff = coeif;
		this.vars = var;
		this.negation = negation;
	}

	/**
	 * Checks if this constraint has three operators
	 * 
	 * @return true if this constraint has three operators
	 */
	private boolean hasOperation() {
		return op1 != Operator.NONE;
	}

	/**
	 * Returns a new ScalarArithmetic constraint which is the negation of this
	 * constraint For instance, a constraint with "=" as operator will return a new
	 * constraint with the same variables but with "!=" as operator
	 * 
	 * @return A new ScalarArithmetic constraint, negation of this constraint
	 */
	@Override
	public ScalarArithmetic getNegation() {

		if (getName().equals("DistDiff") || getName().equals("DistEqual")|| getName().equals("DistGreater")|| getName().equals("DistLess")|| getName().equals("DistGreaterEqual")|| getName().equals("DistLessEqual"))
			return new ScalarArithmetic(getNegationName(), this.vars, this.coeff, Operator.getOpposite(op1), cste,
					getName());

		return new ScalarArithmetic("not_" + getName(), this.vars, this.coeff, Operator.getOpposite(op1), cste);

	}

	public String getNegationName() {
		return this.negation;
	}

	/**
	 * Add this constraint to the specified model (a choco solver model in this
	 * case)
	 * 
	 * @param model   Model to add this constraint to
	 * @param intVars Variables of the model involved in this constraint
	 * 
	 */
	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		IntVar[] temp = new IntVar[vars.length];
		IntVar[] V = model.retrieveIntVars(true);

		/*for (int i = 0; i < temp.length; i++) {
			System.out.print(vars[i]);

		}
		System.out.println("---------------------");
		for (IntVar v : intVars) {
			System.out.print(v.getId()-1);
			

		}
		System.out.println("---------------------");*/
		for (int i = 0; i < temp.length; i++)
			for (int j = 0; j < intVars.length; j++)
				if (vars[i] == (intVars[j].getId()-1))
					temp[i] = intVars[j];
		Constraint c = model.scalar(temp, coeff, op1.toString(), cste);
		return new Constraint[] {c  };

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((op1 == null) ? 0 : op1.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScalarArithmetic other = (ScalarArithmetic) obj;
		if (op1 != other.op1)
			return false;
		
		if (this.getVariables().length != other.getVariables().length)
			return false;
		
		for (int i = 0; i < this.getVariables().length; i++) {
			if (this.getVariables()[i] != other.getVariables()[i])
				return false;
		}
		
		for (int i = 0; i < coeff.length; i++) {
			if (coeff[i] != other.coeff[i]) {
				return false;
			}
			
		}
		return true;
	}

	/**
	 * Add this constraint to the specified model (a choco solver model in this
	 * case)
	 * 
	 * @param model   Model to add this constraint to
	 * @param intVars Variables of the model involved in this constraint
	 * 
	 */
	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		IntVar[] temp = new IntVar[vars.length];

		for (int i = 0; i < this.getVariables().length; i++) {

			temp[i] = intVars[this.getVariables()[i]];

		}

		model.scalar(temp, coeff, op1.toString(), cste).reifyWith(b);

	}

	/**
	 * Checks this constraint for a specified set of values
	 * 
	 * @param val1 sum of variable values multiplied by their coefficents which is
	 *             the left side of this constraint
	 * @param cste Value of the second variable of this constraint
	 * @return true if this constraint is satisfied for the specified set of values
	 */
	@Override
	protected boolean check(int[] vars, int[] coeff) {
		int val1 = 0;

		for (int i = 0; i < vars.length; i++) {

			val1 = val1 + (vars[i] * coeff[i]);
		}

		switch (op1) {
		case EQ:
			return val1 == cste;
		case NEQ:
			return val1 != cste;
		case GT:
			return val1 > cste;
		case GE:
			return val1 >= cste;
		case LT:
			return val1 < cste;
		case LE:
			return val1 <= cste;
		default:
			assert false;
		}

		return false;
	}

	@Override
	public int[] getProjection(ACQ_Query query) {

		int index = 0;

		int[] vars = this.getVariables();

		int[] values = new int[vars.length];
		for (int numvar : vars)
			values[index++] = query.getValue(numvar);
		return values;
	}

	

	/**
	 * Checks if the specified operator is an operation operator
	 * 
	 * @param operator Operator to verify
	 * @return true if the specified operator is Operator.PL or Operator.MN
	 */
	private static boolean isArithmOperation(Operator operator) {
		return operator.equals(Operator.PL) || operator.equals(Operator.MN);
	}

	@Override
	public String getNegName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}

}
