/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.tools.NameService;

/**
 *
 * @author agutierr
 */
public class UnaryArithmetic extends UnaryConstraint {
	final private Operator op;
	final private int cste;
	public UnaryArithmetic(String name, int var, Operator op, int cste){
		super(name,var);
		this.op=op;
		this.cste=cste;
	}
	@Override
	protected boolean check(int value) {
		switch (op) {
		case EQ:
			return value == cste;
		case NEQ:
			return value != cste;
		case GT:
			return value > cste;
		case GE:
			return value >= cste;
		case LT:
			return value < cste;
		case LE:
			return value <= cste;
		}
		return false;

	}
	@Override
	public UnaryArithmetic getNegation(){
		return new UnaryArithmetic(getNegName(),getScope().getFirst(),Operator.getOpposite(op),cste);
	}

	@Override
	public Constraint[] getChocoConstraints(Model model,IntVar... intVars) {
		IntVar l=null;
		for(IntVar v : intVars) {
			if(v.getName().equals(NameService.getVarName(this.getVariables()[0])))
					l= v;
		}
		
		return new Constraint[]{model.arithm(l, op.toString(), cste)};
		/*
		try {
			return new Constraint[]{model.arithm(intVars[this.getVariables()[0]], op.toString(), cste)};
		}
		catch (Exception e) {
			return new Constraint[0];
		}*/
	}
	@Override
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		int left=0;
		if(intVars.length>=this.getVariables().length)
		{
			left= this.getVariables()[0];
		}
		IntVar l=null;
		for(IntVar v : intVars) {
			if(v.getName().equals(NameService.getVarName(left)))
					l= v;
		}
		
		model.arithm(l, op.toString(), cste).reifyWith(b);

	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cste;
		result = prime * result + ((op == null) ? 0 : op.hashCode());
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
		UnaryArithmetic other = (UnaryArithmetic) obj;
		if (cste != other.cste)
			return false;
		if (op != other.op)
			return false;
		if(!Arrays.equals(this.getVariables(), other.getVariables()))
			return false;
		return true;
	}
	@Override
	public String getNegName() {
		// TODO Auto-generated method stub
		String newname = this.getName();
		if(this.getName().startsWith("Not")) {
			newname = newname.substring(3);
		}
		else {
			newname = "Not" + newname;
		}
		
		return newname;
	}
	
	@Override
	public boolean check(ACQ_Query query) {
		int value[] = this.getProjection(query);
		return check(value);
	}
	
}
