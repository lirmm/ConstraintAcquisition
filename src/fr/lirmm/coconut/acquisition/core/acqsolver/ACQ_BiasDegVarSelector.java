package fr.lirmm.coconut.acquisition.core.acqsolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorContradiction;
import org.chocosolver.solver.search.loop.monitors.IMonitorRestart;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.objects.IntMap;

import gnu.trove.list.array.TIntArrayList;

public class ACQ_BiasDegVarSelector extends AbstractStrategy<IntVar>  {

	
	private Variable[] chocoVars ;
	private ArrayList<Constraint> constraints;
	
	/**
	 * The way value is selected for a given variable
	 */
	private IntValueSelector valueSelector;

	


	/**
	 * <b>Bias most Dominating Variable</b> selector.
	 * 
	 * @param model , B_i
	 */
	public ACQ_BiasDegVarSelector(IntVar[] variables, IntValueSelector valueSelector ) {
		super(variables);
		Model model = variables[0].getModel();
		this.valueSelector = valueSelector;
		this.chocoVars=variables;
		this.constraints=getConstraints(model);

	

	}

	


	@Override
	public Decision<IntVar> getDecision() {
		IntVar best = null;
		int Max_score= Integer.MIN_VALUE;
		int id = 0 ;
		 for (IntVar v : vars) {
			 if(!v.isInstantiated()) {
			 int current_score =weight(v);

			 if(current_score>Max_score) {
				 Max_score=current_score;
				 best = v;
			}
			 }
			}
		
		return computeDecision(best);
	}

	private int weight(IntVar v) {
		int w = 1;
		for (Constraint constraint : constraints) {
				if(VarInConstraint(constraint, v))
			        w++;
			
		}
		return w;
	}

	@Override
	public Decision<IntVar> computeDecision(IntVar variable) {
		if (variable == null || variable.isInstantiated()) {
			return null;
		}
		int currentVal = valueSelector.selectValue(variable);
		return variable.getModel().getSolver().getDecisionPath().makeIntDecision(variable,
				DecisionOperatorFactory.makeIntEq(), currentVal);
	}

	
	public ArrayList<Constraint> getConstraints(Model model) {
		ArrayList<Constraint> Csts =new ArrayList<>();
		for(Constraint c : model.getCstrs()) {
			if(c.getName().contains("REIFICATIONCONSTRAINT")) {
					Csts.add(c);
			}

		}
		return Csts;
	}
	public boolean VarInConstraint(Constraint constraint,Variable variable) {

				for(Variable v1 :constraint.getPropagator(0).getVars()) 
					if(v1.getName().equals(variable.getName()))
						return true;
		
		return false;
	}
}