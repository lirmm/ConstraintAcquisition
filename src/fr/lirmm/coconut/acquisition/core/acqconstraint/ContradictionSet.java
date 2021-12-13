package fr.lirmm.coconut.acquisition.core.acqconstraint;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;

public class ContradictionSet {
	ACQ_CNF cnf;
	ConstraintFactory factory;
	ACQ_Scope scope;
	ConstraintMapping mapping;
	
	public ContradictionSet(ConstraintFactory factory, ACQ_Scope scope, ConstraintMapping mapping) {
		this.cnf = new ACQ_CNF();
		this.factory = factory;
		this.scope = scope;
		this.mapping = mapping;
	}
	
	public ACQ_CNF toCNF() {
		return cnf;
	}
	
	public int getSize() {
		return cnf.size();
	}
	
	public void unitPropagate(Unit unit, Chrono chrono) {
		cnf.unitPropagate(unit, chrono);
	}
	
	public void add(Contradiction contr) {
		cnf.add(contr.toClause(mapping));
	}
	
	public String toString() {
		return this.toCNF().toString();
	}
}
