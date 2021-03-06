package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

public class ACQ_DisjunctionConstraint extends ACQ_MetaConstraint {

	public ACQ_DisjunctionConstraint(ConstraintFactory constraintFactory, ACQ_IConstraint c1, ACQ_IConstraint c2) {

		super(constraintFactory, "disjunction", c1, c2);

	}

	public ACQ_DisjunctionConstraint(ConstraintSet set) {

		super("disjunction", set);

	}

	@Override
	public ACQ_IConstraint getNegation() {
		if (this.constraintSet.size() == 2) {
			ACQ_IConstraint c0 = this.constraintSet.get_Constraint(0).getNegation();
			ACQ_IConstraint c1 = this.constraintSet.get_Constraint(1).getNegation();
			return new ACQ_ConjunctionConstraint(this.constraintFactory, c0, c1);
		} else {
			
			  ConstraintSet set = constraintFactory.createSet(); for (int i = 0; i <
			  this.constraintSet.size(); i++) {
			  set.add(this.constraintSet.get_Constraint(i).getNegation()); } assert
			  set.size() > 1 : "Disjunction constraint cannot contain one constraint only";
			  return new ACQ_ConjunctionConstraint(set);
			 
		}
	}

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {

		BoolVar[] reifyArray = model.boolVarArray(constraintSet.size());

		int i = 0;
		for (ACQ_IConstraint c : constraintSet) {
			c.toReifiedChoco(model, reifyArray[i], intVars);
			i++;
		}

		return new Constraint[] { model.sum(reifyArray, ">", 0) };

	}

	@Override
	/****
	 * b <=> C1 and C2 : C1 <=> b1 C2 <=> b2 b=b1*b2
	 */
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		BoolVar[] reifyArray = model.boolVarArray(constraintSet.size());

		int i = 0;
		for (ACQ_IConstraint c : constraintSet) {
			c.toReifiedChoco(model, reifyArray[i], intVars);
			i++;
		}

		model.max(b, reifyArray).post();
		// model.arithm(reifyArray[0], "*", reifyArray[1], "=", b).post();

	}

	@Override
	public boolean check(ACQ_Query query) {
		for (ACQ_IConstraint c : constraintSet) {
			int value[] = c.getProjection(query);
			if (((ACQ_Constraint) c).check(value))
				return true;
		}
		return false;
	}

	@Override
	public boolean check(int... value) {

		for (ACQ_IConstraint c : constraintSet)

			if (((ACQ_Constraint) c).check(value))
				return true;

		return false;
	}

	public int getNbCsts() {

		return constraintSet.size();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.constraintSet.size();
		for (int i = 0; i < this.constraintSet.size(); i++) {
			ACQ_IConstraint acqconstr = this.constraintSet.get_Constraint(i);
			result = prime * result + acqconstr.hashCode();
		}
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
		ACQ_DisjunctionConstraint other = (ACQ_DisjunctionConstraint) obj;

		if (this.constraintSet.size() != other.constraintSet.size())
			return false;
		for (int i = 0; i < this.constraintSet.size(); i++) {
			if (!this.constraintSet.get_Constraint(i).equals(other.constraintSet.get_Constraint(i)))
				return false;
		}

		if (!Arrays.equals(this.getVariables(), other.getVariables()))
			return false;
		return true;
	}

	boolean contains(ACQ_IConstraint constr) {
		for (ACQ_IConstraint subconstr : constraintSet) {
			if (constr.equals(subconstr))
				return true;
		}
		return false;
	}

}
