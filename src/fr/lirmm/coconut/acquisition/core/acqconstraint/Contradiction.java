package fr.lirmm.coconut.acquisition.core.acqconstraint;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

public class Contradiction {

	ACQ_Network net;
	ConstraintFactory factory;
	ACQ_Scope scope;

	public Contradiction(ConstraintFactory factory, ACQ_Scope scope) {
		this.net = new ACQ_Network(factory, scope);
		this.factory = factory;
		this.scope = scope;
	}

	public Contradiction(ACQ_Network net) {
		this.net = net;
		this.factory = net.getFactory();
		this.scope = net.getVariables();
	}

	public ACQ_Network toNetwork() {
		return net;
	}

	public ACQ_DisjunctionConstraint toDisjunction() {
		ACQ_IConstraint[] l = net.getArrayConstraints();
		assert l.length > 1 : "A constraint is by itself contradictory";
		ACQ_DisjunctionConstraint res = new ACQ_DisjunctionConstraint(factory, l[0].getNegation(), l[1].getNegation());
		for (int i = 2; i < l.length; i++) {
			res = new ACQ_DisjunctionConstraint(factory, res, l[i]);
		}
		return res;

	}

//	public void add(Unit unit) {
//		assert(unit.isNeg());
//	}

	public ACQ_Clause toClause(ConstraintMapping mapping) {
		ACQ_Clause result = new ACQ_Clause();
		for (ACQ_IConstraint c : net.getConstraints()) {
			Unit unit = mapping.get(c).clone();
			unit.setNeg();
			result.add(unit);
		}
		return result;
	}

	public Boolean isEmpty() {
		return net.isEmpty();
	}
}
