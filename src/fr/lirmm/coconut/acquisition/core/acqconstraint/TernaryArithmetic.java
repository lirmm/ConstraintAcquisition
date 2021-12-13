/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.core.acqconstraint;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

/**
 *	Constraint between three variables. 
 *  For example : X + Y <= Z or X - Y + Z = 19
 *	
 * @author NADJIB
 */
public class TernaryArithmetic extends TernaryConstraint {


	private final Operator op1, op2, op3;

	// required visibility to allow exportation
	protected final int cste;

	/**
	 * Constructor for a constraint between three variables. 
	 * 
	 * @param name Name of this constraint
	 * @param var1 Variable of this constraint
	 * @param op1 Operator 1 of this constraint
	 * @param var2 Variable of this constraint
	 * @param op2 Operator 2 of this constraint
	 * @param var3 Variable of this constraint
	 * 
	 * @example X + Y < Z
	 */
	public TernaryArithmetic(String name, int var1, Operator op1, int var2, Operator op2, int var3,int cste) {
		super(name, var1, var2, var3);
		this.op1 = op1;
		this.op2 = op2;
		this.cste=cste;
		this.op3 = Operator.NONE;
	}


	/**
	 * Checks if this constraint has three operators
	 * 
	 * @return true if this constraint has three operators
	 */
	private boolean hasOperation() {
		return op3 != Operator.NONE;
	}

	/**
	 * Returns a new BinaryArithmetic constraint which is the negation of this constraint
	 * By instance, a constraint with "=" as operator will return a new constraint with the 
	 * same variables but with "!=" as operator 
	 * 
	 * @return A new BinaryArithmetic constraint, negation of this constraint
	 */
	@Override
	public TernaryArithmetic getNegation() {

		return new TernaryArithmetic("not_" + getName(), getScope().getFirst(), 
				Operator.getOpposite(op1), getScope().getSecond(), Operator.getOpposite(op2), getScope().getThird(),cste);

	}

	/**
	 * Add this constraint to the specified model (a choco solver model in this case)
	 * 
	 * @param model Model to add this constraint to
	 * @param intVars Variables of the model involved in this constraint
	 * 
	 */
	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {

		return new Constraint[]{model.arithm(intVars[this.getVariables()[0]], op1.toString(), intVars[this.getVariables()[1]], 
				op2.toString(), intVars[this.getVariables()[2]])};


	}




	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((op1 == null) ? 0 : op1.hashCode());
		result = prime * result + ((op2 == null) ? 0 : op2.hashCode());
		result = prime * result + ((op3 == null) ? 0 : op3.hashCode());
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
		TernaryArithmetic other = (TernaryArithmetic) obj;
		if (op1 != other.op1)
			return false;
		if (op2 != other.op2)
			return false;
		if (op3 != other.op3)
			return false;
		if (this.getVariables()[0] != other.getVariables()[0])
			return false;
		if (this.getVariables()[1] != other.getVariables()[1])
			return false;
		if (this.getVariables()[2] != other.getVariables()[2])
			return false;
		return true;
	}




	/**
	 * Add this constraint to the specified model (a choco solver model in this case)
	 * 
	 * @param model Model to add this constraint to
	 * @param intVars Variables of the model involved in this constraint
	 * 
	 */
	@Override
	public void toReifiedChoco(Model model, BoolVar b,IntVar... intVars) {

		int first=0, second=1, third=2;
		if(intVars.length>this.getVariables().length)
		{
			first= this.getVariables()[0];
			second=this.getVariables()[1];
			third=this.getVariables()[2];

		}
		model.arithm(intVars[first], op1.toString(), intVars[second], 
				op2.toString(), intVars[third]).reifyWith(b);


	}
	/**
	 * Checks this constraint for a specified set of values
	 * 
	 * @param value1 Value of the first variable of this constraint
	 * @param value2 Value of the second variable of this constraint
	 * @return true if this constraint is satisfied for the specified set of values
	 */
	@Override
	protected boolean check(int value1, int value2, int value3) {

		int val1, val2, val3;
		Operator op,_op,_op1 ;
		if(op1 == Operator.NONE) {
			val1 = value1;
			val2 = value2;
			op = op2;
			_op = op3;
			if (isArithmOperation(_op)) {

				if (_op == Operator.PL) {
					val2 = value2 + value3;
				} else {
					val2 = value2 - value3;
				}
				val1=value1*cste;
			} 
			
				switch (op) {
				case EQ:
					return val1 == val2;
				case NEQ:
					return val1 != val2;
				case GT:
					return val1 > val2;
				case GE:
					return val1 >= val2;
				case LT:
					return val1 < val2;
				case LE:
					return val1 <= val2;
				}	
				
		}else if(op3 == Operator.NONE) {
			val1 = value1;
			val2 = value2;
			val3 = value3;
			op = op2;
		
		
			if (isArithmOperation(op1)) {

				if (op1 == Operator.PL) {
					val1 = value1 + value2;
				} else {
					val1 = value1 - value2;
				}
				val2=value3*cste;
			}
				
			switch (op) {
			case EQ:
				return val1 == val2;
			case NEQ:
				return val1 != val2;
			case GT:
				return val1 > val2;
			case GE:
				return val1 >= val2;
			case LT:
				return val1 < val2;
			case LE:
				return val1 <= val2;
			}
				
		}else {
			val1 = value1;
			val2 = value2;
			val3 = value3;
			op = op2;
			_op=op1;
			_op1=op3;
			
			if (isArithmOperation(_op) && isArithmOperation(_op1) ) {

				if (_op == Operator.PL && _op1 == Operator.PL) {
					val1 =value1+value2 + value3;
				} else if (_op == Operator.MN && _op1 == Operator.MN){
					val1 =value1 - value2 - value3;
				}
				else if (_op == Operator.PL && _op1 == Operator.MN){
					val1 =value1 + value2 - value3;
				}
				else if (_op == Operator.MN && _op1 == Operator.PL){
					val1 =value1 - value2 + value3;
				}
			} 
			
			switch (op) {
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
			}
		}	
		
		
		return false;
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
