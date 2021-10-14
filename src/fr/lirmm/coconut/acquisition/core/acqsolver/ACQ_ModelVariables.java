package fr.lirmm.coconut.acquisition.core.acqsolver;

import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

public class ACQ_ModelVariables {

	
	private IntVar[] varArray;			// integer model variables	
	
	private BoolVar[] boolArray;		// boolean model variables	
	
	private IntVar objVar;			// integer objective variable	

	private BoolVar[] reifyArray;		// reifying variables

	public IntVar[] getVarArray() {
		return varArray;
	}

	public void setVarArray(IntVar[] varArray) {
		this.varArray = varArray;
	}

	public BoolVar[] getBoolArray() {
		return boolArray;
	}

	public void setBoolArray(BoolVar[] boolArray) {
		this.boolArray = boolArray;
	}

	public IntVar getObjVar() {
		return objVar;
	}

	public void setObjVar(IntVar objVar) {
		this.objVar = objVar;
	}

	public BoolVar[] getReifyArray() {
		return reifyArray;
	}

	public void setReifyArray(BoolVar[] reifyArray) {
		this.reifyArray = reifyArray;
	}

	public Object getIntVars() {
		// TODO Auto-generated method stub
		return this.varArray;
	}
	
	
}
