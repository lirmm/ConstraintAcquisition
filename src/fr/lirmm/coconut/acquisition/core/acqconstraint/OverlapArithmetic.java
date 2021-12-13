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
 *	Constraint between two variables. It can either be a straight comparison between these two
 *  For example : X >= Y or X != Y
 *  Or it can be an operation followed by a comparison using a constant
 *  For example : X + 5 <= Y or X - Y = 19
 *	
 * @author agutierr
 */
public class OverlapArithmetic extends OverlapConstraint {

	private final Operator op1;

	
	protected final int cste=0;

	private String negation;

	private boolean inverse;
	public OverlapArithmetic(String name, ACQ_TemporalVariable var1, Operator op, ACQ_TemporalVariable var2,String negation,boolean inverse) {

		super(name, var1, var2);
		this.op1 = op;
		this.negation= negation;
		this.inverse=inverse;

	}




	@Override
	public OverlapArithmetic getNegation() {

		String negationName= this.getNegName(); 
		if(negationName.equals("UNKNOWN"))
			negationName="not_" + getName();

			return new OverlapArithmetic(negationName, tempvariables[0], Operator.getOpposite(op1), tempvariables[1],this.getName(),inverse);
		

	}

	public String getNegName() {
		// TODO Auto-generated method stub
		return negation;
	}

	/**
	 * get the constraint to the specified model (a choco solver model in this case)
	 * 
	 * @param model Model to add this constraint to
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

			part1 = model.arithm(starty,  op1.toString(), endx);
			part2 = model.arithm(startx, op1.toString(), starty);
			cst = model.and(part1, part2,part3,part4);
		
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
		OverlapArithmetic other = (OverlapArithmetic) obj;
		if (cste != other.cste)
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

			part1 = model.arithm(starty,  op1.toString(), endx);
			part2 = model.arithm(startx, op1.toString(), starty);
			cst = model.and(part1, part2,part3,part4);
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
	
		switch (op) {
		case EQ:
			return (value1==value2 );
		case NEQ:
			return (value1!=value2 );
		case GT:
			return (value1>value2 );
		case GE:
			return (value1>=value2 );
		case LT:
			return (value1<value2);
		case LE:
			return (value1<=value2);

		}
		return false;
	}


	@Override
	public boolean check(ACQ_Query query) {
		ACQ_TemporalVariable value[] = this.getProjectionTempVariables(query);
		
		if(query.inverse) {
			boolean answer1=check(value[1].getStartValue(),value[0].getStartValue(),Operator.getOpposite(op1));
			boolean answer2=check(value[0].getStartValue(),value[1].getEndValue(),Operator.getOpposite(op1));
			boolean answer3=check(value[1].getStartValue(),value[1].getEndValue(),Operator.GE);
			boolean answer4=check(value[0].getStartValue(),value[0].getEndValue(),Operator.GE);

			return (answer1 && answer2 && answer3 && answer4);
		}else {
		
		boolean answer1=check(value[0].getStartValue(),value[1].getStartValue(),op1);
		boolean answer2=check(value[1].getStartValue(),value[0].getEndValue(),op1);
		boolean answer3=check(value[0].getStartValue(),value[0].getEndValue(),Operator.LT);
		boolean answer4=check(value[1].getStartValue(),value[1].getEndValue(),Operator.LT);

		return (answer1 && answer2 && answer3 && answer4);
		}
		

	
	}
	@Override
	public boolean isInverse() {
		// TODO Auto-generated method stub
		return inverse;
	}

	@Override
	public OverlapArithmetic getInverse() {
		switch(name) {
		
		case "OverlapsXY":
			return new OverlapArithmetic("IsOverlappedXY", tempvariables[1], op1, tempvariables[0],this.getName(),true);
		case "IsOverlappedXY":
			return new OverlapArithmetic("OverlapsXY", tempvariables[0],op1, tempvariables[1],this.getName(),false);
		
		}
		return null;
	}

	@Override
	public String toString() {
		return name+" ["+tempvariables[0].getStart()/2+","+tempvariables[1].getStart()/2+"]";
	}

}
