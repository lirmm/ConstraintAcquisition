package fr.lirmm.coconut.acquisition.core.learner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_ConjunctionConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;

/**
 * Class used to represent the bias in the constraint acquisition.
 */
public class ACQ_Bias {
	/**
	 * Network of the bias, including both variables and constraints
	 */
	public final ACQ_Network network;
	protected int initial_size;
	public boolean multiarity = false;

	/**
	 * Constructor of the bias from a network
	 * 
	 * @param network Network
	 */
	public ACQ_Bias(ACQ_Network network) {
		this.network = network;
		this.initial_size = network.size();
		this.multiarity = getBiasAritics();
	}

	/**
	 * Returns the size of the smallest scope of this bias
	 * 
	 * @return size of the smallest scope
	 */
	public int computeMinArity() {
		int minArity = Integer.MAX_VALUE;
		for (ACQ_IConstraint constraint : network.getConstraints()) {
			if (constraint.getArity() < minArity) {
				minArity = constraint.getArity();
			}
		}
		return minArity;
	}

	/**
	 * Returns the size of the largest scope of this bias
	 * 
	 * @return size of the largest scope
	 */
	public int computeMaxArity() {
		int maxArity = Integer.MIN_VALUE;
		for (ACQ_IConstraint constraint : network.getConstraints()) {
			if (constraint.getArity() > maxArity) {
				maxArity = constraint.getArity();
			}
		}
		return maxArity;
	}

	public boolean getBiasAritics() {
		if (computeMaxArity() > computeMinArity())
			return true;
		else
			return false;
	}

	/**
	 * Returns the variables of this bias
	 * 
	 * @return variables of this bias
	 */
	public ACQ_Scope getVars() {
		return network.getVariables();
	}

	/**
	 * Returns the set of constraints of this bias
	 * 
	 * @return set of constraints of this bias
	 */
	public ConstraintSet getConstraints() {
		return network.getConstraints();
	}

	/**
	 * Returns the set of constraints violated by query_bgd
	 * 
	 * @param query_bgd Positive or negative example
	 * @return set of constraints violated by query_bgd
	 */
	public ConstraintSet getKappa(ACQ_Query query_bgd) {
		return network.getConstraints().getKappa(query_bgd);
	}

	/**
	 * <p>
	 * Remove a set of constraints contained into this bias
	 * </p>
	 * <p>
	 * Take a set of constraints as parameter
	 * </p>
	 * 
	 * @param temp_kappa set of constraints to remove from this bias
	 */
	public void reduce(ConstraintSet temp_kappa) {
		network.removeAll(temp_kappa);
	}

	public void reduce(ACQ_IConstraint cst) {
		if (cst instanceof ACQ_ConjunctionConstraint) {
			for (ACQ_IConstraint c : ((ACQ_ConjunctionConstraint) cst).constraintSet) {
				network.remove(c);

			}
		} else {
			network.remove(cst);
		}
	}

	/**
	 * <p>
	 * Remove a set of constraints contained into this bias
	 * </p>
	 * <p>
	 * Take as parameter an example (positive or negative)
	 * </p>
	 * <p>
	 * Calculate the kappa of e and remove it from this bias
	 * </p>
	 * 
	 * @param e Positive or negative example
	 */
	public void reduce(ACQ_Query e) {

		reduce(getKappa(e));
	}

	/**
	 * <p>
	 * Returns a subnet of this bias. This subnet has the specified scope as scope
	 * and the set of constraints whose scope is included in varSet
	 * </p>
	 * 
	 * @param varSet ACQ_Scope
	 * @return a constraint network, subnet of this bias
	 */
	public ACQ_Network getProjection(final ACQ_Scope varSet) {
		// calcule l'ensemble des contraintes du biais dont les variables sont contenues
		// dans varSet
		final ConstraintSet projectedCSet = network.getFactory().createSet();
		network.forEach((x) -> {
			if (varSet.containsAll(x.getScope())) {
				projectedCSet.add(x);
			}
		});
		return new ACQ_Network(network.getFactory(), varSet, projectedCSet);
	}

	/**
	 * <p>
	 * Returns a subnet of this bias. This subset has the specified scope as scope
	 * and the set of constraints whose scope is exactly varSet
	 * </p>
	 * 
	 * @param varSet ACQ_Scope
	 * @return a constraint network, subnet of this bias
	 */
	public ACQ_Network getExactProjection(final ACQ_Scope varSet) {
		// calcule l'ensemble des contraintes du biais dont les variables sont contenues
		// dans varSet
		final ConstraintSet projectedCSet = network.getFactory().createSet();
		network.forEach((x) -> {
			if (varSet.equals(x.getScope())) {
				projectedCSet.add(x);
			}
		});
		return new ACQ_Network(network.getFactory(), varSet, projectedCSet);
	}

	public ACQ_Network getProjection_Scopes(final List<ACQ_Scope> varSet) {
		// calcule l'ensemble des contraintes du biais dont les variables sont contenues
		// dans varSet
		ACQ_Scope varSet2 = new ACQ_Scope(varSet);
		final ConstraintSet projectedCSet = network.getFactory().createSet();
		for (ACQ_Scope varSet1 : varSet) {
			network.forEach((x) -> {
				if (varSet1.containsAll(x.getScope())) {
					projectedCSet.add(x);
				}
			});
		}
		return new ACQ_Network(network.getFactory(), varSet2, projectedCSet);

	}

	public int getInitial_size() {
		return initial_size;
	}

	public int getSize() {
		return network.getConstraints().size();
	}

	public void empty() {
		network.removeAll();
		// System.out.println("empty");
		// System.out.println("bias="+network.size());
//		this.network= new ACQ_Network(network.getFactory(),this.getVars());
	}

	public ACQ_Network getNetwork() {
		return this.network;
	}

	public String toString() {

		return this.getNetwork().toString();
	}

	public Set<String> getLanguage() {
		Set<String> Language = new HashSet();
		for (ACQ_IConstraint cst : this.getConstraints())
			Language.add(cst.getName());
		return Language;
	}
}
