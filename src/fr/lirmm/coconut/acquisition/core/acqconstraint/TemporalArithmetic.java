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
import fr.lirmm.coconut.acquisition.core.tools.NameService;

/**
 * Constraint between two variables. It can either be a straight comparison
 * between these two For example : X >= Y or X != Y Or it can be an operation
 * followed by a comparison using a constant For example : X + 5 <= Y or X - Y =
 * 19
 * 
 * @author agutierr
 */
public class TemporalArithmetic extends TemporalConstraint {
	/**
	 * Operator(s) used into this constraint. It must have at least one operator,
	 * the second is optional depending on the constraint we want to build.
	 */
	private final Operator op1;
	private final Operator op2;

	// required visibility to allow exportation
	/**
	 * Optional constant value to allow more comparison for the constraint
	 */
	protected final int cste = 0;
	protected boolean conjunction = false;

	private String negation;
	private boolean inverse;

	/**
	 * Constructor for a constraint between two variables.
	 * 
	 * @param name Name of this constraint
	 * @param var1 Variable of this constraint
	 * @param op   Operator of this constraint
	 * @param var2 Variable of this constraint
	 */
	public TemporalArithmetic(String name, ACQ_TemporalVariable var1, Operator op, ACQ_TemporalVariable var2,
			boolean conjunction, Operator op1, String negation,boolean inverse) {

		super(name, var1, var2);
		this.op1 = op;
		this.op2 = op1;
		this.conjunction = conjunction;
		this.negation = negation;
		this.inverse = inverse;


	}

	/**
	 * Returns a new TemporalArithmeticNegation constraint which is the negation of
	 * this constraint By instance, a constraint with "=" as operator will return a
	 * new constraint with the same variables but with "!=" as operator
	 * 
	 * @return A new TemporalArithmeticNegation constraint, negation of this
	 *         constraint
	 */
	@Override
	public TemporalArithmeticNegation getNegation() {

		String negationName = this.getNegName();
		if (negationName.equals("UNKNOWN"))
			negationName = "not_" + getName();

		return new TemporalArithmeticNegation(negationName, tempvariables[0], Operator.getOpposite(op1),
				tempvariables[1], this.conjunction, Operator.getOpposite(op2), this.getName(),inverse);

	}

	public String getNegName() {
		// TODO Auto-generated method stub
		return negation;
	}

	/**
	 * get the constraint to the specified model (a choco solver model in this case)
	 * 
	 * @param model   Model to add this constraint to
	 * @param intVars Variables of the model involved in this constraint
	 * 
	 */
	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		Constraint part1;
		Constraint part2;
		Constraint cst;
		IntVar startx = null;
		IntVar starty = null;
		IntVar endx = null;
		IntVar endy = null;
		for (IntVar v : intVars) {	
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[0].getStart())))
				startx = v;
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[0].getEnd())))
				endx = v;
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[1].getStart())))
				starty = v;
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[1].getEnd())))
				endy = v;
				
			
		}
		Constraint part3=model.arithm(startx, Operator.LT.toString(), endx);
		Constraint part4=model.arithm(starty, Operator.LT.toString(), endy);

		if (conjunction) {
			part1 = model.arithm(endx,  op2.toString(), endy);
			part2 = model.arithm(startx, op1.toString(), starty);
			
			cst = model.and(part1, part2,part3,part4);
		} else {
			cst = model.and(part3,part4,model.arithm(endx, op1.toString(), starty));
		}
		return new Constraint[] { cst };
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cste;
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
		TemporalArithmetic other = (TemporalArithmetic) obj;
		if (cste != other.cste)
			return false;
		if (op1 != other.op1)
			return false;
		if (this.getTemporalVariables()[0] != other.getTemporalVariables()[0])
			return false;
		if (this.getTemporalVariables()[1] != other.getTemporalVariables()[1])
			return false;

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
		int left = 0, right = 1;
		Constraint part1;
		Constraint part2;
		Constraint cst;
		IntVar startx = null;
		IntVar starty = null;
		IntVar endx = null;
		IntVar endy = null;
		for (IntVar v : intVars) {	
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[0].getStart())))
				startx = v;
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[0].getEnd())))
				endx = v;
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[1].getStart())))
				starty = v;
			if (v.getName().equals(NameService.getVarName(this.getTemporalVariables()[1].getEnd())))
				endy = v;
				
			
		}
		Constraint part3=model.arithm(startx, Operator.LT.toString(), endx);
		Constraint part4=model.arithm(starty, Operator.LT.toString(), endy);

		if (conjunction) {
			part1 = model.arithm(endx,  op2.toString(), endy);
			part2 = model.arithm(startx, op1.toString(), starty);
			
			cst = model.and(part1, part2,part3,part4);
		} else {
			cst = model.and(part3,part4,model.arithm(endx, op1.toString(), starty));
		}
		cst.reifyWith(b);

	}

	/**
	 * Checks this constraint for a specified set of values
	 * 
	 * @param value1 Value of the first variable of this constraint
	 * @param value2 Value of the second variable of this constraint
	 * @return true if this constraint is satisfied for the specified set of values
	 */
	@Override
	protected boolean check(int value1, int value2,Operator op) {
		
		boolean op_ = false;
		switch (op) {
		case EQ:
			op_ = (value1 == value2);
			break;
		case NEQ:
			op_ = value1 != value2;
			break;

		case GT:
			op_ = value1 > value2;
			break;

		case GE:
			op_ = value1 >= value2;
			break;

		case LT:
			op_ = value1 < value2;
			break;

		case LE:
			op_ = value1 <= value2;
			break;

		}
		
			return (op_);

	}
	@Override
	public boolean isInverse() {
		// TODO Auto-generated method stub
		return inverse;
	}

	@Override
	public boolean check(ACQ_Query query) {
		ACQ_TemporalVariable value[] = this.getProjectionTempVariables(query);

		if(query.inverse) {
			
			boolean answer1=check(value[1].getStartValue(),value[0].getStartValue(),op1);
			boolean answer2=check(value[1].getEndValue(),value[0].getEndValue(),op2);
			boolean answer3=check(value[1].getStartValue(),value[1].getEndValue(),Operator.LT);
			boolean answer4=check(value[0].getStartValue(),value[0].getEndValue(),Operator.LT);
			boolean answer5=check(value[1].getEndValue(),value[0].getStartValue(),op1);

			if(conjunction)
			return (!answer1 && !answer2 && !answer3 && !answer4);
			else
				return (!answer5 && !answer3 && !answer4);
			
		}else {
		boolean answer1=check(value[0].getStartValue(),value[1].getStartValue(),op1);
		boolean answer2=check(value[0].getEndValue(),value[1].getEndValue(),op2);
		boolean answer3=check(value[0].getStartValue(),value[0].getEndValue(),Operator.LT);
		boolean answer4=check(value[1].getStartValue(),value[1].getEndValue(),Operator.LT);
		boolean answer5=check(value[0].getEndValue(),value[1].getStartValue(),op1);

		if(conjunction)
		return (answer1 && answer2 && answer3 && answer4);
		else
			return (answer5 && answer3 && answer4);
		}
	}

	@Override
	public boolean check(int... value) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public TemporalArithmetic getInverse() {
		switch(name) {
		case "PrecedesXY":
			return new TemporalArithmetic("IsPrecededXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsPrecededXY":
			return new TemporalArithmetic("PrecedesXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);

		case "MeetsXY":
			return new TemporalArithmetic("IsMetXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsMetXY":
			return new TemporalArithmetic("MeetsXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
	
		case "StartsXY":
			return new TemporalArithmetic("IsStartedXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsStartedXY":
			return new TemporalArithmetic("StartsXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		case "DuringXY":
			return new TemporalArithmetic("ContainsXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "ContainsXY":
			return new TemporalArithmetic("DuringXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		case "FinishXY":
			return new TemporalArithmetic("IsFinishedXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsFinishedXY":
			return new TemporalArithmetic("FinishXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		case "ExactXY":
			return new TemporalArithmetic("ExactXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		
		}
		return null;
	}
	/*public String getInverse() {
		switch(name) {
		case "PrecedesXY":
			return new TemporalArithmetic("IsPrecededXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsPrecededXY":
			return new TemporalArithmetic("PrecedesXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);

		case "MeetsXY":
			return new TemporalArithmetic("IsMetXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsMetXY":
			return new TemporalArithmetic("MeetsXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
	
		case "StartsXY":
			return new TemporalArithmetic("IsStartedXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsStartedXY":
			return new TemporalArithmetic("StartsXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		case "DuringXY":
			return new TemporalArithmetic("ContainsXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "ContainsXY":
			return new TemporalArithmetic("DuringXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		case "FinishXY":
			return new TemporalArithmetic("IsFinishedXY", tempvariables[1], op1,
					tempvariables[0], this.conjunction,op2, this.getName(),true);
		case "IsFinishedXY":
			return new TemporalArithmetic("FinishXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		case "ExactXY":
			return new TemporalArithmetic("ExactXY", tempvariables[0], op1,
					tempvariables[1], this.conjunction,op2, this.getName(),false);
		
		}
		return null;
	}*/
	@Override
	public String toString() {
		return name+" ["+tempvariables[0].getStart()/2+","+tempvariables[1].getStart()/2+"]";
	}
	
}
