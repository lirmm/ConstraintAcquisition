package fr.lirmm.coconut.acquisition.core.acqsolver;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

/**
 * Abstract class that defines the main functions used by the solver
 * 
 *
 */
abstract public class ACQ_ConstraintSolver {
	/**
	 * Used to evaluate the performance of this solver
	 */
	protected boolean timeout=true;
	protected Long limit=(long) 1000;			// one second
	protected Long timeBound=(long) 60000;			// one minute
	protected boolean peeling_process=false;

    private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);



	/**
	 * Checks if there is any solution of the specified constraint network
	 * 
	 * @param learned_network Constraint network to check
	 * @return true if there is at least one solution
	 */
	public abstract boolean solve(ACQ_Network learned_network);

	
	/**
	 * returns a query on the specified constraint network
	 * 
	 * @param learned_network Constraint network to solve
	 * @return query if there is at least one solution
	 */
	public abstract ACQ_Query solveQ(ACQ_Network learned_network);
	/**
	 * Generate a query that satisfies networkA but violates at least one constraint of networkB
	 * 
	 * @param networkA Constraint network to satisfy
	 * @param networkB Constraint network to violate
	 * @param heuristic
	 * @return Query that satisfies networkA but violates at least one constraint of networkB
	 */
	public abstract ACQ_Query solve_AnotB(ACQ_Network network1,
			ACQ_Network network2,boolean findc, ACQ_Heuristic heuristic);

	/**
	 * Function used to check if this solver exceed a definite time
	 * 
	 * @return true if the time is exceeded
	 */
	public boolean timeout_reached(){
		return this.timeout;
	}
	/**
	 * Generate a query that satisfies networkA and at least one constraint of networkB
	 * 
	 * @param networkA Constraint network to satisfy
	 * @param networkB Constraint network where at least one constraint is satisfied
	 * @param heuristic
	 * @return Query that satisfies networkA and at least one constraint of networkB
	 */
	public abstract ACQ_Query solve_AnotAllB(ACQ_Network networkA, 
			ACQ_Network networkB, ACQ_Heuristic heuristic);

	public ACQ_Query solveA(ACQ_Network network) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isTimeout() {
		return timeout;
	}

	public void setTimeout(boolean timeout) {
		this.timeout = timeout;
	}

	public Long getLimit() {
		return limit;
	}

	public Long getTimeBound() {
		return timeBound;
	}
	public void setLimit(Long limit) {
		this.limit = limit;
	}



	public boolean isPeeling_process() {
		return peeling_process;
	}

	public void setPeeling_process(boolean peeling_process) {
		this.peeling_process = peeling_process;
	}

	public abstract ACQ_Query peeling_process(ACQ_Network network_A, ACQ_Network network_B);


	public abstract void setVars(ACQ_Scope vars);

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

	public void fireSolverEvent(String name,Object oldValue,Object newValue){
		pcs.firePropertyChange(name, oldValue, newValue);
	}


	public abstract ACQ_Query max_AnotB(ACQ_Network network1, ACQ_Network network2, ACQ_Heuristic heuristic);
	public abstract HashMap<Long, ACQ_Query> smart_solve(Model model,ACQ_Network network,ACQ_Scope S ,IntVar[] vars,long time,ACQ_Heuristic heuristic);
	public abstract HashMap<Long, ACQ_Query> smart_maxsolve(ACQ_Network network1,ACQ_Network network2,ACQ_IConstraint not_cs,ACQ_Query qeury,long time, ACQ_Criterion criterion,ACQ_Heuristic heuristic);


	public abstract ACQ_Query Generate_Query(ACQ_Network network1, ACQ_Network network2,ACQ_Criterion criterion,ACQ_Heuristic heuristic);


	protected abstract ArrayList<ACQ_Query>  allSolutions(ACQ_Network learned_network);

	protected abstract void  setTimeoutReached(boolean timeoutReached);
	
	public abstract boolean  isTimeoutReached();


	public abstract ACQ_Network get2remove();


	public abstract void reset2remove();


	public abstract ACQ_IDomain getDomain();



}
