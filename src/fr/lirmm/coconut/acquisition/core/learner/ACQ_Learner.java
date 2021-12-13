/***************************************************************
 * Learner class
 * 
 * Learner modelisation
 * 
 * @author LAZAAR
 * @date 29-11-16
 ***************************************************************/

package fr.lirmm.coconut.acquisition.core.learner;

import java.util.ArrayList;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;

/**
 * 
 * Class used to determine if whether or not a query is a valid solution or not
 *
 */
public class ACQ_Learner implements ILearner {
	/**
	 * Ask this learner if the tuple represented by the query e is a solution or not
	 * 
	 * @param e Example to classify as positive or negative
	 * @return true if the query e is positive
	 */

	private ACQ_Network targetNetwork;

	public ArrayList<ACQ_Query> memory = new ArrayList<>();

	boolean memory_enabled = true;

	public boolean isMemory_enabled() {
		return memory_enabled;
	}

	public void setMemory_enabled(boolean memory_enabled) {
		this.memory_enabled = memory_enabled;
	}

	public boolean ask(ACQ_Query e) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean ask(ACQ_IConstraint e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * 
	 * @param example
	 */
	public synchronized boolean ask_query(ACQ_Query example) {

		boolean asked_query = false;
		if (memory_enabled && !memory.isEmpty())
			for (ACQ_Query tmp : memory)
				if ((example.extend(tmp) && tmp.isNegative()) || tmp.extend(example) && tmp.isPositive()) {
					example.classify_as(tmp);
					asked_query = true;
					break;
				}
		if (!example.isClassified()) {
			ask(example);
			if (memory_enabled)
				add_memory(example);
		}
		return asked_query;

	}

	private synchronized void add_memory(ACQ_Query example) {
		memory.add(example);
	}

	public boolean equal(ACQ_Query a, ACQ_Query b) {
		for (int i : b.getScope()) {

			if (a.getValue(i) == b.getValue(i)) {
				return false;

			}
		}
		return true;
	}

	@Override
	public void non_asked_query(ACQ_Query query) {
		// TODO Auto-generated method stub

	}

	public ACQ_Network getTargetNetwork() {
		return targetNetwork;
	}

	public void setTargetNetwork(ACQ_Network targetNetwork) {
		this.targetNetwork = targetNetwork;
	}
	
	public void setTargetNetwork() {
		this.targetNetwork = buildTargetNetwork();
	}

	public ACQ_Network buildTargetNetwork() {
		return null;
	}

	


}