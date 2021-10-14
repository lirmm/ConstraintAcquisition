package fr.lirmm.coconut.acquisition.core.acqsolver;

import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import fr.lirmm.coconut.acquisition.core.acqconstraint.Unit;

public class Z3SATModel extends SATModel {

	Model model;
	
	public Z3SATModel(Model model) {
		this.model = model;
	}

	protected Boolean isSet(Expr value) {
		try {
			value.isTrue();
			return true;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	@Override
	public Boolean get(Unit unit) {
		Expr value = model.evaluate(unit.getZ3Var(), false);
		
		assert value.isBool() : "value is supposed to be boolean but is not";
		
		if(isSet(value)) { // Value 
			//System.out.println(value.toString());
			return value.isTrue();
		}
		else {
			// Value of the variable is not set because it does not impacts satifisfiability. 
			// We can set the value we want
			//TODO Check if better if we set false we unconstrained (would reduce the size of Phi(I) )
			return true; 
		}
	}
	
	@Override
	public String toString() {
		String s = "(";
		for(FuncDecl fun : model.getFuncDecls()) {
			Expr value = model.evaluate(model.getConstInterp(fun), false);
			s += fun.getName() + "=" + value.toString() + ", ";
		}
		return s + ")";
	}


}
