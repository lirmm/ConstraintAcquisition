package fr.lirmm.coconut.acquisition.core.acqsolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainBest;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainImpact;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainLast;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMax;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMedian;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMiddle;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandom;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainRandomBound;
import org.chocosolver.solver.search.strategy.selectors.variables.ActivityBased;
import org.chocosolver.solver.search.strategy.selectors.variables.AntiFirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.Cyclic;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.GeneralizedMinDomVarSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.ImpactBased;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.selectors.variables.Largest;
import org.chocosolver.solver.search.strategy.selectors.variables.MaxRegret;
import org.chocosolver.solver.search.strategy.selectors.variables.Occurrence;
import org.chocosolver.solver.search.strategy.selectors.variables.Random;
import org.chocosolver.solver.search.strategy.selectors.variables.RandomVar;
import org.chocosolver.solver.search.strategy.selectors.variables.Smallest;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.PoolManager;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.tools.NameService;
import fr.lirmm.coconut.acquisition.core.workspace.ACQ_WS;

public class ACQ_ChocoSolver extends ACQ_ConstraintSolver {

	private String VLS;
	private String VRS;

	private Solver solver;

	/**
	 * Domain of variables (a revoir parce que les variables d'un meme solver
	 * peuvent avoir des domaines differents)
	 */
	private ACQ_IDomain domain;
	private ACQ_Scope vars;
	private boolean peeling_step = true;
	private boolean timeoutReached;
	private ACQ_Network constraint2remove;
	private List<ACQ_Scope> biasScopes;
	private boolean log_constraints = true;

	/**
	 * Constructor
	 * 
	 * @param domain Domain of variables
	 */

	public ACQ_ChocoSolver(ACQ_IDomain domain, String VRS, String VLS) {
		this.domain = domain;
		this.VRS = VRS;
		this.VLS = VLS;

	}

	/**
	 * Generate a query that satisfies networkA but violates at least one constraint
	 * of networkB
	 * 
	 * @param network_A Constraint network to satisfy
	 * @param network_B Constraint network to violate
	 * @return Query that satisfies networkA but violates at least one constraint of
	 *         networkB
	 */

	@Override
	public ACQ_Query solve_AnotB(ACQ_Network network_A, ACQ_Network network_B, boolean findc, ACQ_Heuristic heuristic) {

		ACQ_Query query;
		if (network_B.size() == 0 || (findc && network_B.size() == 1))
			return new ACQ_Query(); // NL: candidates set is singleton
		fireSolverEvent("BEG_solve_AnotB", false, true);

		Model model = new Model("solveAnotB");
		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();
		ACQ_ModelVariables chocoVars = buildModel(model, network, network_B, findc, heuristic);

		// FileManager.printFile(model, "model");
		solver = model.getSolver();

		if (this.timeout)
			solver.limitTime(this.getLimit());
		setSearch(solver, chocoVars.getVarArray(), VRS, VLS);
		// solver.setSearch(new DomOverWDeg(chocoVars.getVarArray(), 0, new
		// IntDomainRandom(0)));
		int[] tuple = new int[chocoVars.getVarArray().length];
		if (!findc && !heuristic.equals(ACQ_Heuristic.SOL))
			try {
				while (solver.solve()) {

					fireSolverEvent("TIMECOUNT_ANB", null, solver.getTimeCount());
					for (int i = 0; i < chocoVars.getVarArray().length; i++) {
						tuple[i] = chocoVars.getVarArray()[i].getValue();
					}
				}
			} catch (Exception e) {
				for (int i = 0; i < chocoVars.getVarArray().length; i++) {
					tuple[i] = chocoVars.getVarArray()[i].getValue();
				}
			}
		else {

			solver.solve();
			fireSolverEvent("TIMECOUNT_ANB", null, solver.getTimeCount());

		}
		// FileManager.printFile("solveAnotB::" + Thread.currentThread().getName() +
		// "::" + solver.getTimeCount(), "time");
		if (solver.getSolutionCount() == 0) {

			if (!findc && peeling_process && solver.isStopCriterionMet())
				return peeling_process(network, network_B);
			else {
				this.setTimeoutReached(solver.isStopCriterionMet());
				return new ACQ_Query();
			}
		}
		if (findc || heuristic.equals(ACQ_Heuristic.SOL))
			for (int i = 0; i < chocoVars.getVarArray().length; i++) {
				tuple[i] = chocoVars.getVarArray()[i].getValue();
			}

		query = new ACQ_Query(network_A.getVariables(), tuple);

		fireSolverEvent("END_solve_AnotB", true, false);
		return query;
	}

	@Override
	protected void setTimeoutReached(boolean timeoutReached) {

		this.timeoutReached = timeoutReached;
	}

	@Override
	public ACQ_Query peeling_process(ACQ_Network network_A, ACQ_Network network_B) {

		// FileManager.printFile(network_A, Thread.currentThread().getName());

		this.set2remove(network_B.constraintFactory, network_B.getVariables());
		fireSolverEvent("BEG_solve_peeling_process", false, true);

		this.setPeeling_process(true);

		Model model = new Model("solveAnotB");

		float time = this.getLimit() / 1000;

		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();

		IntVar[] chocoVars = buildModel(model, network, false);

		for (ACQ_IConstraint cst : network_B) {

			System.err.println("==> peeling process::" + cst + "::" + network_B.size());

			System.out.println("stop here!!");
			Constraint[] chocoConstraints = cst.getNegation().getChocoConstraints(model, chocoVars);

			model.post(chocoConstraints);
			solver = model.getSolver();
			solver.reset();
			solver.limitTime(this.getTimeBound() - this.getLimit()); // one second
			setSearch(solver, (IntVar[]) chocoVars, VRS, VLS);

			solver.solve();

			time += solver.getTimeCount();

			// solver.getMeasures().reset();

			if (solver.getSolutionCount() != 0) {
				// network_B.remove(cst);
				int[] tuple = new int[chocoVars.length];
				for (int i = 0; i < chocoVars.length; i++) {
					tuple[i] = chocoVars[i].getValue();
				}
				ACQ_Query query = new ACQ_Query(network_A.getVariables(), tuple);
				fireSolverEvent("END_solve_peeling_process", true, false);
				fireSolverEvent("TIMECOUNT_ANB", null, time);
				if (time > this.getTimeBound())
					this.setTimeoutReached(true);
				// FileManager.printFile("Thread :" + Thread.currentThread().getName() + "::" +
				// time, "peeling");
				return query;
			} else if (!solver.isStopCriterionMet()) {
				this.toRemove(cst);
			}
			// keep model and avoid to call buildModel() again
			model.unpost(chocoConstraints);
		}
		fireSolverEvent("TIMECOUNT_ANB", null, time);
		System.err.println("==> peeling process time::" + time);
		// FileManager.printFile("Peeling::" + Thread.currentThread().getName() + "::" +
		// time, "time");
		if (time > this.getTimeBound() || this.get2remove().size() == 0)
			this.setTimeoutReached(true);
		return new ACQ_Query();
	}

	private void toRemove(ACQ_IConstraint cst) {

		this.constraint2remove.add(cst, true);

	}

	public ACQ_Network get2remove() {

		return this.constraint2remove;

	}

	public void set2remove(ConstraintFactory constraintFactory, ACQ_Scope acq_Scope) {

		this.constraint2remove = new ACQ_Network(constraintFactory, acq_Scope);

	}

	@SuppressWarnings("unchecked")
	private void setSelectors(VarSelector vRS2, ValSelector vLS2, Model model, IntVar[] vars) {

		switch (vRS2) {
		case DomOverWDeg:
			// this.solver.setSearch(Search.domOverWDegSearch(vars), new IntDomainRandom(),
			// vars);
			this.solver.setSearch(Search.intVarSearch(
					(VariableSelector<IntVar>) new DomOverWDeg(vars, System.currentTimeMillis(),
							new IntDomainRandom(System.currentTimeMillis())),
					new IntDomainRandom(System.currentTimeMillis()), vars));
		case Random:
			this.solver.setSearch(Search.randomSearch(vars, System.currentTimeMillis()));
			// TODO: NL remaining strategies
		default:
			break;
		}

		switch (vLS2) {
		case IntDomainRandom:
			this.solver.setSearch();

		}

	}

	@Override
	public ACQ_Query solveA(ACQ_Network network_A) {

		fireSolverEvent("BEG_solveA", false, true);
		Model model = new Model("solveA");

		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();

		IntVar[] chocoVars = buildModel(model, network, false);

		solver = model.getSolver();
		// solver.reset();
		setSearch(solver, (IntVar[]) chocoVars, VRS, VLS);

		// solver.setSearch(new DomOverWDeg(chocoVars, 0, new IntDomainRandom(0)));

		// if (timeout)
		// solver.limitTime(this.getLimit());

		solver.solve();

		fireSolverEvent("TIMECOUNT_A", null, new Float(solver.getTimeCount()));

		if (solver.getSolutionCount() == 0) {
			System.err.println(solver.getContradictionException());
			return new ACQ_Query();
		}

		int[] tuple = new int[chocoVars.length];
		for (int i = 0; i < chocoVars.length; i++) {
			tuple[i] = chocoVars[i].getValue();
		}
		ACQ_Query query = new ACQ_Query(network.getVariables(), tuple);

		fireSolverEvent("END_solveA", true, false);
		return query;

	}

	/**
	 * Generate a query that satisfies networkA and at least one constraint of
	 * networkB
	 * 
	 * @param networkA  Constraint network to satisfy
	 * @param networkB  Constraint network where at least one constraint is
	 *                  satisfied
	 * @param heuristic
	 * @return Query that satisfies networkA and at least one constraint of networkB
	 */
	@Override
	public ACQ_Query solve_AnotAllB(ACQ_Network networkA, ACQ_Network networkB, ACQ_Heuristic heuristic) {

		fireSolverEvent("BEG_solve_AnotAllB", false, true);
		Model model = new Model("solveA");

		ACQ_Network network = new ACQ_Network(networkA.getFactory(), networkA.getVariables(),
				networkA.getConstraints());

		network.allDiffCliques();

		IntVar[] chocoVars = buildModel(model, network, false);
		solver = model.getSolver();
		setSearch(solver, (IntVar[]) chocoVars, VRS, VLS);

		// solver.setSearch(new DomOverWDeg(chocoVars, 0, new IntDomainRandom(0)));

		while (solver.solve()) {
			fireSolverEvent("TIMECOUNT_ANAB", null, new Float(solver.getTimeCount()));
			int[] tuple = new int[chocoVars.length];
			for (int i = 0; i < chocoVars.length; i++) {
				tuple[i] = chocoVars[i].getValue();
			}
			ACQ_Query query = new ACQ_Query(networkA.getVariables(), tuple);
			if (networkB.checkNotAllNeg(query)) {
				fireSolverEvent("END_solve_AnotAllB", true, false);
				return query;
			}
		}
		fireSolverEvent("END_solve_AnotAllB", true, false);
		return new ACQ_Query();
	}

	/**
	 * Build the solver model. Add from the specified network, the variables and the
	 * constraints to the specified model.
	 * 
	 * @param model   Target model to add variables and constraints
	 * @param network Constraint network to modelize
	 * @return An array of all the variables of the model
	 */
	public IntVar[] buildModel(Model model, ACQ_Network network, boolean on_network_vars) {
		// declare variables
		ArrayList<IntVar> intVars = new ArrayList<>();
		ArrayList<IntVar> intVars1 = new ArrayList<>();
	
	
		if (on_network_vars)
			network.getVariables().forEach(numvar1 -> {
				IntVar intVar1 = model.intVar(NameService.getVarName(numvar1), domain.getMin(numvar1),
						domain.getMax(numvar1));
				intVars1.add(intVar1);
			});

		else

			vars.forEach(numvar1 -> {
				IntVar intVar1 = model.intVar(NameService.getVarName(numvar1), domain.getMin(numvar1),
						domain.getMax(numvar1));
				intVars1.add(intVar1);
			});

		for (IntVar v : intVars1) {
			network.getVariables().forEach(numvar1 -> {
				if (NameService.getVarName(numvar1).equals(v.getName()))
					intVars.add(v);
			});
		}

		IntVar[] varArray1 = intVars1.toArray(new IntVar[intVars1.size()]);
	

		// add constraints in model
		for (ACQ_IConstraint c : network.getConstraints()) {
			model.post(c.getChocoConstraints(model, varArray1));
		}
		return varArray1;

	}

	/**
	 * Build a model (network_A and not network_B) using reified constraints based
	 * refutation. Assumption: network_A and network_B are expressed on the same set
	 * of variables.
	 * 
	 * @param model     Target model to add variables and constraints
	 * @param network_A Constraint network to add to *model*
	 * @param network_B Constraint network to add to *model* as refuted one using
	 *                  reified constraints
	 * @param heuristic
	 * @param findc
	 * @return varArray model variables @
	 */
	public ACQ_ModelVariables buildModel(Model model, ACQ_Network network_A, ACQ_Network network_B, boolean findc,
			ACQ_Heuristic heuristic) {

		ACQ_ModelVariables modelVariables = new ACQ_ModelVariables();
		// declare variables
		ArrayList<IntVar> intVars = new ArrayList<>();
		ArrayList<IntVar> intVars1 = new ArrayList<>();
		if (vars == null)
			System.err.println("vars null");
		for (int numvar1 : vars) {
			IntVar intVar1 = model.intVar(NameService.getVarName(numvar1), domain.getMin(numvar1),
					domain.getMax(numvar1));
			intVars1.add(intVar1);
		}
		;

		for (IntVar v : intVars1) {
			network_A.getVariables().forEach(numvar1 -> {
				if (NameService.getVarName(numvar1).equals(v.getName()))
					intVars.add(v);
			});

		}

		IntVar[] varArray = intVars.toArray(new IntVar[intVars.size()]);
		IntVar[] varArray1 = intVars1.toArray(new IntVar[intVars1.size()]);

		modelVariables.setVarArray(varArray);
		// add constraints in model
		for (ACQ_IConstraint c : network_A.getConstraints()) {

			model.post(c.getChocoConstraints(model, varArray1));
		}

		BoolVar[] reifyArray = model.boolVarArray(network_B.size());
		int i = 0;
		for (ACQ_IConstraint c : network_B.getConstraints()) {
			c.toReifiedChoco(model, reifyArray[i], varArray1);
			i++;
		}

		IntVar obj_var;
		if (findc)
			obj_var = model.intVar("obj_var", 1, reifyArray.length - 1);
		else if (reifyArray.length <= 1) {
			obj_var = model.intVar("obj_var", 0, 1);
			model.arithm(obj_var, "=", 0).post();
		} else
			obj_var = model.intVar("obj_var", 0, reifyArray.length - 1);

		modelVariables.setReifyArray(reifyArray);

		modelVariables.setObjVar(obj_var);

		if (!findc && heuristic.equals(ACQ_Heuristic.MAX))
			model.setObjective(Model.MINIMIZE, obj_var);

		if (!findc && heuristic.equals(ACQ_Heuristic.MIN))
			model.setObjective(Model.MAXIMIZE, obj_var);

		model.sum(reifyArray, "=", obj_var).post();

		return modelVariables;

	}

	/**
	 * Build a model (network_A and not network_B) using reified constraints based
	 * refutation. Assumption: network_A and network_B are expressed on the same set
	 * of variables.
	 * 
	 * @param model     Target model to add variables and constraints
	 * @param network_A Constraint network to add to *model*
	 * @param network_B Constraint network to add to *model* as refuted one using
	 *                  reified constraints
	 * @param heuristic
	 * @param findc
	 * @return varArray model variables @
	 */
	public ACQ_ModelVariables buildModel_Ls_notcs(Model model, ACQ_Network network_A, ACQ_IConstraint cs) {

		ACQ_ModelVariables modelVariables = new ACQ_ModelVariables();
		// declare variables
		ArrayList<IntVar> intVars = new ArrayList<>();
		ArrayList<IntVar> intVars1 = new ArrayList<>();
		if (vars == null)
			System.err.println("vars null");
		for (int numvar1 : vars) {
			IntVar intVar1 = model.intVar(NameService.getVarName(numvar1), domain.getMin(numvar1),
					domain.getMax(numvar1));
			intVars1.add(intVar1);
		}
		;

		for (IntVar v : intVars1) {
			intVars.add(v);

		}

		IntVar[] varArray = intVars.toArray(new IntVar[intVars.size()]);
		IntVar[] varArray1 = intVars1.toArray(new IntVar[intVars1.size()]);

		modelVariables.setVarArray(varArray);
		// add constraints in model
		for (ACQ_IConstraint c : network_A.getConstraints()) {

			model.post(c.getChocoConstraints(model, varArray1));
		}
		if (cs != null)
			model.post(cs.getChocoConstraints(model, varArray1));

		return modelVariables;

	}

	/**
	 * Ask this solver if there is any solution to the specified network
	 * 
	 * @param learned_network Constraint network to check
	 * @return true if there is at least one solution of the specified network
	 */
	@Override
	public boolean solve(ACQ_Network network_A) {
		fireSolverEvent("BEG_solve_network", false, true);

		Model model = new Model("solveA");

		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();

		IntVar[] chocoVars = buildModel(model, network, false);
		solver = model.getSolver();
		// setSearch(solver, chocoVars, VRS, VLS,network_A);
		solver.setSearch(new DomOverWDeg(chocoVars, 0, new IntDomainRandom(0)));
		if (timeout)
			solver.limitTime(this.getLimit());

		boolean b = solver.solve();

		fireSolverEvent("TIMECOUNT_N", null, new Float(solver.getTimeCount()));
		fireSolverEvent("END_solve_network", true, false);
		return b;
	}

	@Override
	public ACQ_Query solveQ(ACQ_Network network_A) {

		fireSolverEvent("BEG_solveQ", false, true);
		Model model = new Model("solveA");

		ACQ_Query query = new ACQ_Query();

		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();

		IntVar[] chocoVars = buildModel(model, network, false);
		solver = model.getSolver();
		setSearch(solver, chocoVars, VRS, VLS);

		// solver.setSearch(new DomOverWDeg(chocoVars, 0, new
		// IntDomainRandom(System.currentTimeMillis())));

		if (timeout)
			solver.limitTime(this.getLimit());

		solver.solve();

		fireSolverEvent("TIMECOUNT_Q", null, new Float(solver.getTimeCount()));

		if (solver.getSolutionCount() != 0) {
			int[] tuple = new int[chocoVars.length];
			for (int i = 0; i < chocoVars.length; i++) {
				tuple[i] = chocoVars[i].getValue();
			}
			query = new ACQ_Query(network.getVariables(), tuple);

		}
		fireSolverEvent("END_solveQ", true, false);

		return query;
	}

	public boolean debugSolve(ACQ_Network network) {

		Model model = new Model("solveA");

		buildModel(model, network, false);

		boolean isSolve = model.getSolver().solve();

		model.getSolver().printStatistics();

		return isSolve;
	}

	public ACQ_IDomain getDomain() {
		return this.domain;
	}

	@Override
	public void setVars(ACQ_Scope vars) {
		this.vars = new ACQ_Scope(vars);
	}

	@Override
	public ACQ_Query max_AnotB(ACQ_Network network_A, ACQ_Network network_B, ACQ_Heuristic heuristic) {

		int levels = network_B.getConstraints().get_levels();

		if (network_B.size() == 0 || network_B.size() == 1)
			return new ACQ_Query(); // NL: candidates set is singleton

		fireSolverEvent("BEG_solve_AnotB", false, true);

		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();

		for (int i = 1; i <= levels; i++) {

			Model model = new Model("maxAnotB_" + i);

			ConstraintSet network_level = network_B.getConstraints().getNextLevelCandidates(i);

			if (network_level.size() <= 1)
				continue;
			ACQ_ModelVariables chocoVars = buildModel(model, network,
					new ACQ_Network(network_B.getFactory(), network_A.getVariables(), network_level), true, heuristic);

			// FileManager.printFile(model, "model");
			solver = model.getSolver();

			if (this.timeout)

				solver.limitTime(this.getLimit());
			setSearch(solver, chocoVars.getVarArray(), VRS, VLS);

			// solver.setSearch(new DomOverWDeg(chocoVars.getVarArray(), 0, new
			// IntDomainRandom(0)));

			if (!heuristic.equals(ACQ_Heuristic.SOL))
				while (solver.solve()) {
					fireSolverEvent("TIMECOUNT_ANB", null, new Float(solver.getTimeCount()));
				}
			else {
				solver.solve();
				fireSolverEvent("TIMECOUNT_ANB", null, new Float(solver.getTimeCount()));
			}

			if (solver.getSolutionCount() != 0) {

				int[] tuple = new int[chocoVars.getVarArray().length];
				for (int j = 0; j < chocoVars.getVarArray().length; j++) {
					tuple[j] = chocoVars.getVarArray()[j].getValue();
				}
				ACQ_Query query = new ACQ_Query(network_A.getVariables(), tuple);

				// FileManager.printFile(solver.getTimeCount(), "time_sudoku");

				fireSolverEvent("END_solve_AnotB", true, false);
				return query;
			}
		}

		return solve_AnotB(network_A, network_B, true, heuristic);

	}

	protected ACQ_Query max_AnotB_old(ACQ_Network network_A, ACQ_Network network_B, ACQ_Heuristic heuristic) {

		if (network_B.size() == 0 || network_B.size() == 1)
			return new ACQ_Query(); // NL: candidates set is singleton

		fireSolverEvent("BEG_max_AnotB", false, true);

		Model model = new Model("maxAnotB");

		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();
		ACQ_ModelVariables chocoVars = buildMaxModel(model, network, network_B, heuristic);

		solver = model.getSolver();

		if (this.timeout)

			solver.limitTime(this.getLimit());
		setSearch(solver, chocoVars.getVarArray(), VRS, VLS);

//		solver.setSearch(new DomOverWDeg(chocoVars.getVarArray(), 0, new IntDomainRandom(0)));

		while (solver.solve()) {
			fireSolverEvent("TIMECOUNT_ANB", null, new Float(solver.getTimeCount()));
		}

		if (solver.getSolutionCount() == 0) {
			fireSolverEvent("END_solve_AnotB", true, false);
			return null;
		}

		int[] tuple = new int[chocoVars.getVarArray().length];
		for (int i = 0; i < chocoVars.getVarArray().length; i++) {
			tuple[i] = chocoVars.getVarArray()[i].getValue();
		}
		ACQ_Query query = new ACQ_Query(network_A.getVariables(), tuple);

		// FileManager.printFile(solver.getTimeCount(), "time_sudoku");

		fireSolverEvent("END_max_AnotB", true, false);
		return query;
	}

	private ACQ_ModelVariables buildMaxModel(Model model, ACQ_Network network_A, ACQ_Network network_B,
			ACQ_Heuristic heuristic) {

		ACQ_ModelVariables modelVariables = new ACQ_ModelVariables();
		// declare variables
		ArrayList<IntVar> intVars = new ArrayList<>();
		ArrayList<IntVar> intVars1 = new ArrayList<>();
		if (vars == null)
			System.err.println("vars null");
		for (int numvar1 : vars) {
			IntVar intVar1 = model.intVar(NameService.getVarName(numvar1), domain.getMin(numvar1),
					domain.getMax(numvar1));
			intVars1.add(intVar1);
		}
		;

		for (IntVar v : intVars1) {
			network_A.getVariables().forEach(numvar1 -> {
				if (NameService.getVarName(numvar1).equals(v.getName()))
					intVars.add(v);
			});

		}

		IntVar[] varArray = intVars.toArray(new IntVar[intVars.size()]);
		IntVar[] varArray1 = intVars1.toArray(new IntVar[intVars1.size()]);

		modelVariables.setVarArray(varArray);
		// add constraints in model
		for (ACQ_IConstraint c : network_A.getConstraints()) {

			model.post(c.getChocoConstraints(model, varArray1));
		}
		int levels = network_B.getConstraints().get_levels();

		IntVar[] objectives = new IntVar[levels];
		int[] coeffs = new int[levels];

		for (int i = 1; i <= levels; i++) {
			ConstraintSet constraints_level = network_B.getConstraints().getNextLevelCandidates(i);
			BoolVar[] reifyArray = model.boolVarArray(constraints_level.size());

			int j = 0;

			for (ACQ_IConstraint c : constraints_level) {

				c.toReifiedChoco(model, reifyArray[i - 1], varArray1);
				j++;
			}

			objectives[i - 1] = model.intVar("obj_var_" + i, 1, reifyArray.length - 1);

			modelVariables.setReifyArray(reifyArray);

			model.sum(reifyArray, "=", objectives[i - 1]).post();

			coeffs[i - 1] = ((levels + 1) - i) * network_B.size();

		}

		IntVar obj_var = model.intVar("obj_var", 1, network_B.size() * network_B.size());

		modelVariables.setObjVar(obj_var);

		// model.setObjective(Model.MAXIMIZE, obj_var);

		model.scalar(objectives, coeffs, "=", obj_var).post();

		return modelVariables;

	}

	@Override
	public ArrayList<ACQ_Query> allSolutions(ACQ_Network network_A) {

		ArrayList<ACQ_Query> solutions = new ArrayList<>();

		fireSolverEvent("BEG_solveAll", false, true);
		Model model = new Model("solveAll");

		ACQ_Network network = new ACQ_Network(network_A.getFactory(), network_A.getVariables(),
				network_A.getConstraints());

		network.allDiffCliques();

		IntVar[] chocoVars = buildModel(model, network, false);
		solver = model.getSolver();
		setSearch(solver, (IntVar[]) chocoVars, VRS, VLS);

		// solver.setSearch(new DomOverWDeg(chocoVars, 0, new
		// IntDomainRandom(System.currentTimeMillis())));

		if (timeout)
			solver.limitTime(this.getLimit());

		while (solver.solve()) {
			ACQ_Query query = new ACQ_Query();

			fireSolverEvent("TIMECOUNT_Q", null, new Float(solver.getTimeCount()));

			int[] tuple = new int[chocoVars.length];

			for (int i = 0; i < chocoVars.length; i++) {
				tuple[i] = chocoVars[i].getValue();
			}
			query = new ACQ_Query(network.getVariables(), tuple);

			fireSolverEvent("END_solveQ", true, false);
			solutions.add(query);

		}

		return solutions;

	}

	private void setSearch(Solver solver, IntVar[] vars, String vrs, String vls) {

		if (vrs.equals("FirstFail")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(
						Search.intVarSearch(new FirstFail(solver.getModel()), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new FirstFail(solver.getModel()),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("AntiFirstFail")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new AntiFirstFail(solver.getModel()), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(
						Search.intVarSearch(new AntiFirstFail(solver.getModel()), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new AntiFirstFail(solver.getModel()),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new AntiFirstFail(solver.getModel()), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(
						Search.intVarSearch(new AntiFirstFail(solver.getModel()), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(
						Search.intVarSearch(new AntiFirstFail(solver.getModel()), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new AntiFirstFail(solver.getModel()), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch(new AntiFirstFail(solver.getModel()),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new AntiFirstFail(solver.getModel()),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}

		if (vrs.equals("InputOrder")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(
						Search.intVarSearch(new InputOrder(solver.getModel()), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new InputOrder(solver.getModel()),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("ActivityBased")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ActivityBased(vars),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("Cyclic")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new Cyclic(), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new Cyclic(), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new Cyclic(), new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new Cyclic(), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new Cyclic(), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch(new Cyclic(), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new Cyclic(), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(
						Search.intVarSearch(new Cyclic(), new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(
						Search.intVarSearch(new Cyclic(), new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("DomOverWDeg")) {
			switch (vls) {
			case "IntDomainBest":

				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(), new IntDomainBest()));
				break;
			case "IntDomainImpact":
				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(), new IntDomainImpact()));
				break;

			case ("IntDomainLast"):
				solver.setSearch(
						new DomOverWDeg(vars, System.currentTimeMillis(), new IntDomainLast(null, null, null)));
				break;

			case ("IntDomainMax"):
				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(), new IntDomainMax()));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(), new IntDomainMedian()));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(), new IntDomainMiddle(true)));
				break;

			case ("IntDomainMin"):
				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(), new IntDomainMin()));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(),
						new IntDomainRandom(System.currentTimeMillis())));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(new DomOverWDeg(vars, System.currentTimeMillis(),
						new IntDomainRandomBound(System.currentTimeMillis())));
				break;

			}

		}

		if (vrs.equals("ImpactBased")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch((VariableSelector<IntVar>) new ImpactBased(vars, true),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("GeneralizedMinDomVarSelector")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(
						Search.intVarSearch(new GeneralizedMinDomVarSelector(), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new GeneralizedMinDomVarSelector(),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("Largest")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new Largest(), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new Largest(), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new Largest(), new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new Largest(), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new Largest(), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch(new Largest(), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new Largest(), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(
						Search.intVarSearch(new Largest(), new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(
						Search.intVarSearch(new Largest(), new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("Smallest")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new Smallest(), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new Smallest(), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new Smallest(), new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new Smallest(), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new Smallest(), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch(new Smallest(), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new Smallest(), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(
						Search.intVarSearch(new Smallest(), new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new Smallest(),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}

		if (vrs.equals("MaxRegret")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new MaxRegret(), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new MaxRegret(), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new MaxRegret(), new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new MaxRegret(), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new MaxRegret(), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch(new MaxRegret(), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new MaxRegret(), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(
						Search.intVarSearch(new MaxRegret(), new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new MaxRegret(),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}

		if (vrs.equals("Occurrence")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new Occurrence(), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new Occurrence(), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new Occurrence(), new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new Occurrence(), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new Occurrence(), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch(new Occurrence(), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new Occurrence(), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(
						Search.intVarSearch(new Occurrence(), new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new Occurrence(),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}

		if (vrs.equals("Random")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(
						Search.intVarSearch(new Random(System.currentTimeMillis()), new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(
						Search.intVarSearch(new Random(System.currentTimeMillis()), new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new Random(System.currentTimeMillis()),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(Search.intVarSearch(new Random(System.currentTimeMillis()), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(
						Search.intVarSearch(new Random(System.currentTimeMillis()), new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(
						Search.intVarSearch(new Random(System.currentTimeMillis()), new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(Search.intVarSearch(new Random(System.currentTimeMillis()), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch(new Random(System.currentTimeMillis()),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new Random(System.currentTimeMillis()),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}

		if (vrs.equals("RandomVar")) {
			switch (vls) {
			case "IntDomainBest":
				solver.setSearch(Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars),
						new IntDomainBest(), vars));
				break;

			case "IntDomainImpact":
				solver.setSearch(Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars),
						new IntDomainImpact(), vars));
				break;

			case ("IntDomainLast"):
				solver.setSearch(Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars),
						new IntDomainLast(null, null, null), vars));
				break;

			case ("IntDomainMax"):
				solver.setSearch(
						Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars), new IntDomainMax(), vars));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars),
						new IntDomainMedian(), vars));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars),
						new IntDomainMiddle(true), vars));
				break;

			case ("IntDomainMin"):
				solver.setSearch(
						Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars), new IntDomainMin(), vars));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars),
						new IntDomainRandom(System.currentTimeMillis()), vars));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(Search.intVarSearch(new RandomVar(System.currentTimeMillis(), vars),
						new IntDomainRandomBound(System.currentTimeMillis()), vars));
				break;

			}

		}
		if (vrs.equals("BiasDeg")) {
			switch (vls) {
			case "IntDomainBest":

				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainBest()));
				break;
			case "IntDomainImpact":
				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainImpact()));
				break;

			case ("IntDomainLast"):
				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainLast(null, null, null)));
				break;

			case ("IntDomainMax"):
				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainMax()));
				break;

			case ("IntDomainMedian"):
				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainMedian()));
				break;

			case ("IntDomainMiddle"):
				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainMiddle(true)));
				break;

			case ("IntDomainMin"):
				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainMin()));
				break;

			case ("IntDomainRandom"):
				solver.setSearch(new ACQ_BiasDegVarSelector(vars, new IntDomainRandom(System.currentTimeMillis())));
				break;

			case ("IntDomainRandomBound"):
				solver.setSearch(
						new ACQ_BiasDegVarSelector(vars, new IntDomainRandomBound(System.currentTimeMillis())));
				break;

			}
		}

	}

	@Override
	public boolean isTimeoutReached() {
		if (this.timeoutReached)
			System.err.println("++++++++++TIMEOUT++++++++++");
		return this.timeoutReached;
	}

	@Override
	public void reset2remove() {
		if (this.constraint2remove != null)
			this.constraint2remove.removeAll();
	}

	@Override
	public HashMap<Long, ACQ_Query> smart_solve(Model model, ACQ_Network L, ACQ_Scope scope, IntVar[] chocoVars,
			long time, ACQ_Heuristic heuristic) {

		ConstraintFactory constraintFactory = new ConstraintFactory();
		ConstraintSet constraints = constraintFactory.createSet();
		ACQ_Network empty = new ACQ_Network(constraintFactory, constraints);
		boolean propagateOnX = true;

		ACQ_Query query;
		HashMap<Long, ACQ_Query> pair = new HashMap<>();
		solver = model.getSolver();

		long spent_time = 0;

		solver.limitTime(time);
		setSearch(solver, chocoVars, VRS, VLS);

		int[] tuple = new int[chocoVars.length];

		fireSolverEvent("BEG_solve_AnotB", false, true);

		boolean stop = false;
		while (!stop && solver.solve()) {
			stop = true;
			// solver.solve();
			spent_time += (solver.getTimeCount() * 1000);
			// solver.limitTime(time - spent_time);

			fireSolverEvent("TIMECOUNT_ANB", null, solver.getTimeCount());
			for (int i = 0; i < chocoVars.length; i++) {
				tuple[i] = chocoVars[i].getValue();
			}
			if (!propagateOnX) {
				stop = false;
			} else {
				Model model1 = new Model("Verify");
				IntVar[] chocoVars1 = buildModel(model1, L, true);
				Iterator<Integer> it = scope.iterator();
				try {
					int j = 0;
					while (it.hasNext()) {
						int i = it.next().intValue();
						chocoVars1[i].instantiateTo(tuple[j], Cause.Null);
						j++;

					}
					model1.getSolver().propagate();
				} catch (ContradictionException e) {
					while (it.hasNext()) {
						int i = it.next().intValue();
						model.arithm(chocoVars1[i], "!=", tuple[i]);
						stop = false;
					}
				}
			}
		}

		if (solver.getSolutionCount() == 0) {
			pair.put((long) solver.getTimeCount(), new ACQ_Query());
			return pair;
		}

		query = new ACQ_Query(scope, tuple);
		pair.put(spent_time, query);
		fireSolverEvent("END_solve_AnotB", true, false);

		solver.limitTime(Long.MAX_VALUE);
		return pair;

	}

	@Override
	public HashMap<Long, ACQ_Query> smart_maxsolve(ACQ_Network L, ACQ_Network B, ACQ_IConstraint cs, ACQ_Query q,
			long time, ACQ_Criterion criterion, ACQ_Heuristic heuristic) {
		ACQ_Query query;

		Model model = new Model("Max model");
		B.add(cs.getNegation(), true);
		ACQ_ModelVariables chocoVars = buildModel(model, L, B, false, heuristic);
		IntVar[] variables = chocoVars.getVarArray();
		HashMap<Long, ACQ_Query> pair = new HashMap<>();

		ACQ_Scope scope = q.getScope();

		Solver solver = model.getSolver();
		solver.setSearch(new AbstractStrategy<IntVar>(variables) {
			// enables to recycle decision objects (good practice)
			PoolManager<IntDecision> pool = new PoolManager();

			@Override
			public Decision getDecision() {
				IntDecision d = pool.getE();
				if (d == null)
					d = new IntDecision(pool);
				IntVar next = null;
				for (IntVar v : variables) {
					if (scope.contains(v.getId())) {
						v.isInstantiatedTo(q.getValue(v.getId()));
					} else {
						if (!v.isInstantiated()) {
							next = v;
							break;
						}
					}
				}
				if (next == null) {
					return null;
				} else {
					// next decision is assigning nextVar to its lower bound
					d.set(next, next.getLB(), DecisionOperatorFactory.makeIntEq());
					return d;
				}
			}
		});
		if (this.timeout)
			solver.limitTime(time);
		switch (criterion) {
		case Kappa:
			setSearch(solver, chocoVars.getVarArray(), VarSelector.BiasDeg.toString(), VLS);
			break;
		case VarSize:
			setSearch(solver, chocoVars.getVarArray(), VarSelector.DomOverWDeg.toString(), VLS);

		}
		int[] tuple = new int[chocoVars.getVarArray().length];
		fireSolverEvent("BEG_solve_AnotB", false, true);

		while (solver.solve()) {
			fireSolverEvent("TIMECOUNT_ANB", null, solver.getTimeCount());
			for (int i = 0; i < chocoVars.getVarArray().length; i++) {
				tuple[i] = chocoVars.getVarArray()[i].getValue();
			}
			Model model1 = new Model("Verify");
			ACQ_ModelVariables chocoVars1 = buildModel_Ls_notcs(model1, L, null);
			Iterator<Integer> it = scope.iterator();
			try {
				int j = 0;
				while (it.hasNext()) {
					int i = it.next().intValue();
					chocoVars1.getVarArray()[i].instantiateTo(tuple[j], Cause.Null);
					j++;
				}

				model1.getSolver().propagate();
			} catch (ContradictionException e) {
				while (it.hasNext()) {
					int i = it.next().intValue();
					model.arithm(chocoVars.getVarArray()[i], "!=", tuple[i]);
				}
			}
		}

		if (solver.getSolutionCount() == 0) {
			pair.put((long) solver.getTimeCount(), new ACQ_Query());
			return pair;
		}

		query = new ACQ_Query(vars, tuple);
		pair.put((long) solver.getTimeCount(), query);
		fireSolverEvent("END_solve_AnotB", true, false);
		return pair;
	}

	@Override
	public ACQ_Query Generate_Query(ACQ_Network L, ACQ_Network B, ACQ_Criterion criterion, ACQ_Heuristic heuristic) {

		long time = 0;
		long cutoff = 1000;
		ACQ_WS utils = new ACQ_WS();
		if (this.biasScopes == null) { // for a lazy initialisation

			Set<ACQ_Scope> setScopes = new HashSet<>();

			for (ACQ_IConstraint cst : B)
				setScopes.add(cst.getScope());

			this.biasScopes = new ArrayList<>(setScopes);

			Collections.shuffle(biasScopes);

		}

		ACQ_Query last_query = null, query = null;

		ACQ_Scope scope = new ACQ_Scope();

		int scope_size = 0;
		for (int i =0 ;i<B.size();i++) {
			ACQ_IConstraint cs = utils.random(B);
			

			scope = scope.union(cs.getScope());
			scope_size = scope.size();

			ACQ_Network Ls = L.getProjection(scope);
			Ls.add(cs.getNegation(), true);

			Model model = new Model("Generate_Query");

			IntVar[] chocoVars = buildModel(model, Ls, true);
			
			if(chocoVars.length!= Ls.getVariables().size())
				{
				System.out.println("PROBLEM!!");
				System.exit(0);
				}

			HashMap<Long, ACQ_Query> pair_s = smart_solve(model, L, scope, chocoVars, cutoff - time, heuristic);

			query = pair_s.values().iterator().next();

			time = time + pair_s.keySet().iterator().next();

			if (query.isEmpty()) {
				B.remove(cs);
//				B.removeAll(B.getProjection(cs.getScope()).getConstraints());
//				L.add(cs, true); // NL: to check
//				if(log_constraints )
//					 FileManager.printFile(cs, "learnedNetwork");
	
				System.out.println("cs::" + cs + ":: query::" + query + "::time::" + time + "::bias::" + B.size());

				continue;
			}
			for (ACQ_Scope s : biasScopes) {

				scope = scope.union(s);

				if (scope_size == scope.size())
					continue;

				scope_size = scope.size();

				Ls = L.getProjection(scope);
				Ls.add(cs.getNegation(), true);

				Model modelbis = new Model("Generate_Query");
				chocoVars = buildModel(modelbis, Ls, true);

				pair_s = smart_solve(modelbis, L, scope, chocoVars, cutoff - time, heuristic);

				last_query = pair_s.values().iterator().next();

				time = time + pair_s.keySet().iterator().next();

				if (!last_query.isEmpty())
					query = last_query;
				else {
					if (time <= cutoff) {
						B.remove(cs);
				//		B.removeAll(B.getProjection(cs.getScope()).getConstraints());
				//		L.add(cs, true); // NL: to check

				//		if(log_constraints )
				//			 FileManager.printFile(cs, "learnedNetwork");

						System.out.println(
								"redundant one::" + cs + ":: bias size::" + B.size() + ":: network ::" + L.size());
						break;
					}
				}

				if (time > cutoff || query.getScope().size() == this.vars.size())
					return query;

			}
		}
		return query;

	}

}
