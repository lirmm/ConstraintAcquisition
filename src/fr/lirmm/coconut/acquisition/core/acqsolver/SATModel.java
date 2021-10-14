package fr.lirmm.coconut.acquisition.core.acqsolver;

import fr.lirmm.coconut.acquisition.core.acqconstraint.Unit;

abstract public class SATModel {
	
	public abstract Boolean get(Unit unit);
	
	public abstract String toString();
	
}
