package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;


public class ACQ_ConjunctionConstraint extends ACQ_MetaConstraint{


	public ACQ_ConjunctionConstraint(ConstraintFactory constraintFactory,ACQ_IConstraint c1, ACQ_IConstraint c2) {

		super(constraintFactory,"conjunction", c1, c2);


	}

	public ACQ_ConjunctionConstraint(ConstraintSet set) {

		super("conjunction", set);


	}
	
	public ConstraintSet getConstraints() {
		return constraintSet;
	}
	
	@Override
	public ACQ_IConstraint getNegation() {
		if (this.constraintSet.size() == 2) {
			ACQ_IConstraint c0 = this.constraintSet.get_Constraint(0).getNegation();
			ACQ_IConstraint c1 = this.constraintSet.get_Constraint(1).getNegation();
			return new ACQ_DisjunctionConstraint(this.constraintFactory, c0, c1);
		}
		else {
			assert false : "TODO";
			return new ACQ_DisjunctionConstraint(this.constraintSet);
			/*ConstraintSet set = constraintFactory.createSet();
			for (int i = 0; i < this.constraintSet.size(); i++) {
				set.add(this.constraintSet.get_Constraint(i).getNegation());
			}
			assert set.size() > 1 : "Disjunction constraint cannot contain one constraint only";
			return new ACQ_DisjunctionConstraint(set);*/
		}
		//return new ACQ_DisjunctionConstraint(this.constraintSet);

	}

	@Override
	public Constraint[] getChocoConstraints(Model model, IntVar... intVars) {
		Constraint[] chocoConstraints=new Constraint[0];

		for(ACQ_IConstraint c: constraintSet)
			chocoConstraints=ArrayUtils.append(chocoConstraints,c.getChocoConstraints(model, intVars));
		return chocoConstraints;

	}

	@Override
	/****
	 * b <=> C1 and C2 :
	 * C1 <=> b1
	 * C2 <=> b2
	 * b=b1*b2
	 */
	public void toReifiedChoco(Model model, BoolVar b, IntVar... intVars) {
		BoolVar[] reifyArray = model.boolVarArray(constraintSet.size());

		int i=0;
		for(ACQ_IConstraint c: constraintSet)
		{
			c.toReifiedChoco(model, reifyArray[i], intVars);
			i++;
		}

		model.min(b, reifyArray).post();
		//	model.arithm(reifyArray[0], "*", reifyArray[1], "=", b).post();

	}
	
	@Override
	public boolean check(ACQ_Query query) {
		for(ACQ_IConstraint c: constraintSet) {
			int value[] = c.getProjection(query);
			if(!((ACQ_Constraint) c).check(value))
				return false;
		}
		return true;
	}

	@Override
	public boolean check(int... value) {


		for(ACQ_IConstraint c: constraintSet)

			if(!((ACQ_Constraint) c).check(ordred_value(c, value))) 
				return false;

		return true;
	}






	private int[] ordred_value(ACQ_IConstraint c, int... value) {

		if(value.length==0 || !(c instanceof ScalarConstraint))
			return value;


		int[] result= new int[value.length];
		int[] vars= c.getVariables();
		int[] orderedvars= triBulle(vars);

		for(int i=0; i<value.length; i++)
		{
			int j;
			for(j=0; j<vars.length; j++)
			{
			if(vars[i]==orderedvars[j])
				break;
			}
			result[i]= value[j];
		}

		return result;

	}




	public  int[] triBulle(int tab1[]) {

		int[] tab= new int[tab1.length];
		
		for(int i=0; i<tab1.length; i++) tab[i]=tab1[i];
		int tampon = 0;
		boolean permut;

		do {
			permut = false;
			for (int i = 0; i < tab.length - 1; i++) {
				if (tab[i] > tab[i + 1]) {
					tampon = tab[i];
					tab[i] = tab[i + 1];
					tab[i + 1] = tampon;
					permut = true;
				}
			}
		} while (permut);

		return tab;
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
		ACQ_ConjunctionConstraint other = (ACQ_ConjunctionConstraint) obj;
		
		if (this.constraintSet.size() != other.constraintSet.size())
			return false;
		for(int i = 0; i < this.constraintSet.size(); i++) {
			if (!this.constraintSet.get_Constraint(i).equals(other.constraintSet.get_Constraint(i)))
				return false;
		}
		
		if(!Arrays.equals(this.getVariables(), other.getVariables()))
			return false;
		return true;
	}

}
