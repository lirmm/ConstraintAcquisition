package fr.lirmm.coconut.acquisition.core.algorithms;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_ConjunctionConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Generation_Type;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Criterion;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.learner.Query_type;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.tools.FileManager;

public class ACQ_QUACQ {

	public final ACQ_Network learned_network;
	protected ACQ_ConstraintSolver solver;
	protected ACQ_Bias bias;
	protected final ACQ_Learner learner;
	protected ACQ_Heuristic heuristic;
	protected boolean normalizedCSP;
	protected boolean shuffle_split;
	protected boolean allDiff_detection;
	protected boolean new_findscope = false;
	protected ConstraintFactory constraintFactory;
	protected boolean with_collapse_state = false;
	protected boolean verbose = true;
	protected boolean log_queries = false;
	protected boolean log_constraints = true;

	public ACQ_QUACQ(ACQ_ConstraintSolver solver, ACQ_Bias bias, ACQ_Learner learner, ACQ_Heuristic heuristic) {

		// NL: config part
		this.heuristic = heuristic;
		this.solver = solver;
		this.bias = bias;
		this.constraintFactory = bias.network.getFactory();
		this.learned_network = new ACQ_Network(constraintFactory, bias.getVars());
		this.learner = learner;

	}

	public ACQ_Bias getBias() {
		return bias;
	}

	public ACQ_Network getLearnedNetwork() {
		return learned_network;
	}

	public ACQ_IConstraint findC(ACQ_Scope scope, ACQ_Query e, boolean normalizedCSP) {

		if (normalizedCSP)
			return findC_one(scope, e);
		else
			return findC_two(scope, e);

	}

	/**
	 * **************************************************************** findC
	 * (classic findC of IJCAI13)
	 *
	 * @date 03-10-17
	 * @update 17-11-18
	 ******************************************************************/

	protected ACQ_IConstraint findC_one(ACQ_Scope scope, ACQ_Query e) {

		// ACQ_Network learned_network_y = new ACQ_Network(learned_network, scope);
		ACQ_Network learned_network_y = learned_network.getProjection(scope);
		ACQ_Bias bias_y = new ACQ_Bias(bias.getExactProjection(scope));

		ConstraintSet temp_kappa;

		assert scope.size() >= 1;

		ConstraintSet candidates = constraintFactory.createSet(bias_y.getConstraints()); // NL: candidates = delta in
																							// IJCAI13

		assert (candidates.size() == bias_y.getConstraints().size());

		if (e != null)
			candidates.retainAll(bias_y.getKappa(e));

		while (true) {

			if (candidates.isEmpty()) {
				return null;
			}
			// TODO check if returning null or empty constraint (NL: 03-10-17)

			int temp = candidates.size();

			ACQ_Query partial_findC_query = query_gen(learned_network_y,
					new ACQ_Network(constraintFactory, scope, candidates), scope, Query_type.findc1, ACQ_Heuristic.SOL);

			assert (candidates.size() == temp);

			if (partial_findC_query.isEmpty()) {

				bias.reduce(candidates);

				return candidates.iterator().next();

			}

			temp_kappa = bias_y.getKappa(partial_findC_query);

			if (temp_kappa.isEmpty()) {
				throw new RuntimeException("Collapse state");
			}

			boolean non_asked_query = learner.ask_query(partial_findC_query);
			if (log_queries)
				FileManager.printFile(partial_findC_query, "queries");
			if (non_asked_query)
				learner.non_asked_query(partial_findC_query);

			if (partial_findC_query.isPositive()) {
				bias.reduce(temp_kappa); // NL: kappa of partial_findC_query
				bias_y.reduce(temp_kappa);

				candidates.removeAll(temp_kappa);
			} else {
				candidates.retainAll(temp_kappa);
			}
		}

	}

	/**
	 * **************************************************************** findC (new
	 * findC presented in AIJ on non-normalized CSPs (new implementation)
	 *
	 * @date 19-02-20
	 * @author LAZAAR
	 *         *****************************************************************
	 */

	protected ACQ_IConstraint findC_two(ACQ_Scope scope, ACQ_Query e) {

		ACQ_Network learned_network_y = learned_network.getProjection(scope);

		if (scope.size() > 2 && learned_network_y.size() == 0)
			System.out.println("stop!");

		ACQ_Bias bias_y = new ACQ_Bias(bias.getExactProjection(scope));

		ConstraintSet temp_kappa;

		assert scope.size() >= 1;

		ConstraintSet all_candidates = join(constraintFactory.createSet(bias_y.getConstraints()), bias_y.getKappa(e)); // NL:
																														// join
																														// kappa_delta
		// System.out.print("all condidates::"+ all_candidates+"\n");

		while (true) {

			ACQ_Query partial_findC_query = query_gen(learned_network_y,
					new ACQ_Network(constraintFactory, scope, all_candidates), scope, Query_type.findc2,
					ACQ_Heuristic.SOL);

			// System.err.println("=====>"+ partial_findC_query+"::"+all_candidates);

			if (partial_findC_query.isEmpty()) {

				ACQ_IConstraint cst = pick(all_candidates);
				if (cst != null) {
					// bias.reduce(cst); // NL: to check if all_candidates can be removed from B?
					bias.reduce(bias_y.getConstraints());
				}

				return cst;
			}

			temp_kappa = all_candidates.getKappa(partial_findC_query); // NL: level or all candidates?

			boolean non_asked_query = learner.ask_query(partial_findC_query);
			if (log_queries)
				FileManager.printFile(partial_findC_query, "queries");
			if (non_asked_query)
				learner.non_asked_query(partial_findC_query);

			if (partial_findC_query.isPositive()) {

				all_candidates.removeAll(temp_kappa);
				bias.reduce(temp_kappa);

			} else {

				ACQ_Scope S = findScope(partial_findC_query, scope, new ACQ_Scope());

				if (scope.containsAll(S) && scope.size() < S.size()) {

					ACQ_IConstraint cst = findC(S, partial_findC_query, normalizedCSP);

					if (cst == null)
						throw new RuntimeException("Collapse state");

					else {
						learned_network.add(cst, false);

						bias.reduce(constraintFactory.createSet(cst.getNegation()));

					}

				} else

					all_candidates = join(all_candidates, temp_kappa);

			}
		}

	}

	/**
	 * **************************************************************** findC (new
	 * findC presented in AIJ on non-normalized CSPs
	 *
	 * @date 30-05-19
	 * @author LAZAAR
	 *         *****************************************************************
	 */

	private ACQ_IConstraint findC_AIJ_old(ACQ_Scope scope, ACQ_Query e) {

		ACQ_Network learned_network_y = learned_network.getProjection(scope);

		ACQ_Bias bias_y = new ACQ_Bias(bias.getProjection(scope));

		ConstraintSet temp_kappa;

		assert scope.size() > 1;

		ConstraintSet all_candidates = join(constraintFactory.createSet(bias_y.getConstraints()), bias_y.getKappa(e)); // NL:
																														// join
																														// operator
																														// between
																														// delta
																														// and
																														// kappa_delta

		System.err.println(all_candidates);

		ConstraintSet level_candidates = all_candidates.getNextLevelCandidates(1); // NL: invariant::level_candidates
																					// subset of all_candidates
		System.err.println(level_candidates);

		int max_level = all_candidates.get_levels();

		int level = 1;

		/*--------------------------------------------------------*/
		// NL: if all_candidates is reduced to one constraint

		/*
		 * if(all_candidates.size()==1) { ACQ_Network TMP_network= new
		 * ACQ_Network(bias.getVars(),new
		 * ACQ_ConstraintSet(learned_network_y.getConstraints()));
		 * 
		 * TMP_network.addAll(all_candidates, true);
		 * 
		 * ACQ_Query TMP_query = query_gen(learned_network_y, new ACQ_Network(scope,
		 * level_candidates), scope, Query_type.MQ, Heuristic.SOL);
		 * 
		 * 
		 * learner.asked_query(TMP_query);
		 * 
		 * if (!TMP_query.isClassified()) {
		 * 
		 * boolean TMP_b = learner.ask(TMP_query);
		 * 
		 * TMP_query.classify(TMP_b);
		 * 
		 * learner.memory_up(TMP_query); }
		 * 
		 * if (TMP_query.isPositive()) {
		 * 
		 * throw new RuntimeException("Collapse state");
		 * 
		 * } else { bias.reduce(all_candidates); return
		 * all_candidates.iterator().next();
		 * 
		 * 
		 * } }
		 * 
		 * /*--------------------------------------------------------
		 */

		while (true) {

			ACQ_Query partial_findC_query = new ACQ_Query();

			while (partial_findC_query.isEmpty()) {

				partial_findC_query = new ACQ_Query();
				while (level_candidates.size() <= 1 && level <= max_level) {
					level++;
					level_candidates = all_candidates.getNextLevelCandidates(level);
				}

				/*
				 * if(level_candidates.size()==1) { ACQ_IConstraint cst =
				 * level_candidates.iterator().next(); bias.reduce(cst); //NL: to check if
				 * all_candidates can be removed from B?
				 * 
				 * return cst; }
				 */
				if (level > max_level) {

					ACQ_IConstraint cst = pick(all_candidates);
					bias.reduce(cst); // NL: to check if all_candidates can be removed from B?

					return cst;
				}

				// int temp = level_candidates.getLevel();

				partial_findC_query = query_gen(learned_network_y,
						new ACQ_Network(constraintFactory, scope, all_candidates), scope, Query_type.findc2,
						ACQ_Heuristic.SOL);

				System.err.println(all_candidates.getKappa(partial_findC_query));
				if (partial_findC_query.isEmpty()) {

					ACQ_Network network = new ACQ_Network(constraintFactory, bias.getVars(),
							learned_network_y.getConstraints());

					network.addAll(level_candidates, true);

					partial_findC_query = solver.solveA(network);
				}

			}

			temp_kappa = level_candidates.getKappa(partial_findC_query); // NL: level or all candidates?

			/*
			 * if (temp_kappa.isEmpty()) {
			 * 
			 * throw new RuntimeException("Collapse state");
			 * 
			 * }
			 */ // NL: 21-01-2020:: check if necessary to have collapse state here

			boolean non_asked_query = learner.ask_query(partial_findC_query);
			if (non_asked_query)
				learner.non_asked_query(partial_findC_query);

			if (partial_findC_query.isPositive()) {

				level_candidates.removeAll(temp_kappa);

			} else {

				ACQ_Scope S = findScope(partial_findC_query, scope, new ACQ_Scope());

				if (scope.containsAll(S) && scope.size() < S.size()) {

					ACQ_IConstraint cst = findC(S, partial_findC_query, normalizedCSP);

					if (cst == null)
						throw new RuntimeException("Collapse state");

					else {
						learned_network.add(cst, false);
						bias.reduce(constraintFactory.createSet(cst.getNegation()));
					}

				} else {

					all_candidates = join(all_candidates, all_candidates.getKappa(partial_findC_query));

					System.err.println(all_candidates);

					level_candidates = all_candidates.getNextLevelCandidates(level);

					System.err.println(level_candidates);

					max_level = all_candidates.get_levels();

				}

			}
		}

	}

	/************************************
	 * 
	 * @param all_candidates
	 * @return
	 */

	protected ACQ_IConstraint pick(ConstraintSet all_candidates) {

		int min = 0;
		ACQ_IConstraint result = null;

		for (ACQ_IConstraint cst : all_candidates) {

			if (!(cst instanceof ACQ_ConjunctionConstraint))

				return cst;
			else {
				min = ((ACQ_ConjunctionConstraint) cst).getNbCsts();
				result = cst;
			}
		}

		for (ACQ_IConstraint cst : all_candidates)

			if (cst instanceof ACQ_ConjunctionConstraint)
				if (min > ((ACQ_ConjunctionConstraint) cst).getNbCsts()) {
					min = ((ACQ_ConjunctionConstraint) cst).getNbCsts();
					result = cst;
				}

		return result;
	}

	/************************************************************
	 * Join Operator
	 * 
	 * set_one join set_two = { c1 and c2 | c1 in set_one and c2 in set_two and (c1
	 * and c2 is sat)}
	 * 
	 * @param set_one
	 * @param set_two
	 * @return set_three
	 ************************************************************/

	public ConstraintSet join(ConstraintSet set_one, ConstraintSet set_two) {

		ConstraintSet set_three = constraintFactory.createSet();

		for (ACQ_IConstraint c1 : set_one)
			for (ACQ_IConstraint c2 : set_two) {
				if (!c1.equals(c2) && solver.solve(new ACQ_Network(constraintFactory, this.bias.getVars(),
						constraintFactory.createSet(new ACQ_IConstraint[] { c1, c2 })))) {

					ACQ_ConjunctionConstraint candidate = new ACQ_ConjunctionConstraint(constraintFactory, c1, c2);
					if (!set_three.check(candidate)
							&& !set_three.contains(new ACQ_ConjunctionConstraint(constraintFactory, c2, c1)))
						set_three.add(candidate);
				}
				if (c1.equals(c2))
					set_three.add(c1);

			}

		// set_three.addAll(set_one);
		// set_three.constraints.addAll(set_two.constraints);

		return set_three;
	}

	public ACQ_Scope findScope(ACQ_Query negative_example, ACQ_Scope X, ACQ_Scope Bgd) {
		if (new_findscope)
			return findScope_two(negative_example, X, Bgd);

		return findScope_one(negative_example, X, Bgd, false);

	}

	/**
	 * ************************************************************** FindScope
	 * procedure
	 *
	 * @param negative_example : a complete negative example
	 * @param X                : problem variables (and/or) foreground variables
	 * @param Bgd              : background variables
	 * @param mutex            : boolean mutex
	 * @return variable scope
	 *
	 * @update 31/05/19
	 ****************************************************************/

	protected ACQ_Scope findScope_two(ACQ_Query negative_example, ACQ_Scope X, ACQ_Scope Bgd) {

		if (Bgd.size() >= bias.computeMinArity()) { // TESTME if minArity has the good value !!

			ACQ_Query query_bgd = new ACQ_Query(Bgd, Bgd.getProjection(negative_example)); // projection e|Bgd

			ConstraintSet temp_kappa = bias.getKappa(query_bgd);
			if (!temp_kappa.isEmpty()) {

				boolean non_asked_query = learner.ask_query(query_bgd);
				if (log_queries)
					FileManager.printFile(query_bgd, "queries");
				if (non_asked_query)
					learner.non_asked_query(query_bgd);
				if (query_bgd.isNegative())
					return ACQ_Scope.EMPTY; // NL: return emptyset

				else {
					bias.reduce(temp_kappa);

				}

			}
		}

		if (X.size() == 1)
			return X;

		// NL: different splitting manners can be defined here!

		ACQ_Scope[] splitedX;
		if (shuffle_split)
			splitedX = X.shuffleSplit();
		else
			splitedX = X.split();

		ACQ_Scope X1 = splitedX[0];
		ACQ_Scope X2 = splitedX[1];

		ACQ_Query query_BXone = new ACQ_Query(Bgd.union(X1), Bgd.union(X1).getProjection(negative_example)); // projection
																												// e|Bgd

		ConstraintSet kappa_BXone;

		kappa_BXone = bias.getKappa(query_BXone);

		ACQ_Query query_BX = new ACQ_Query(Bgd.union(X), Bgd.union(X).getProjection(negative_example)); // projection
																										// e|Bgd

		ConstraintSet kappa_BX = bias.getKappa(query_BX);

		ACQ_Scope S1;

		if (kappa_BXone.equals(kappa_BX))
			S1 = ACQ_Scope.EMPTY;
		else
			S1 = findScope_two(negative_example, X2, Bgd.union(X1)); // NL: First recursive call of findScope

		ACQ_Query query_BSone = new ACQ_Query(Bgd.union(S1), Bgd.union(S1).getProjection(negative_example)); // projection
																												// e|Bgd

		ConstraintSet kappa_BSone = bias.getKappa(query_BSone);

		ACQ_Scope S2;

		if (kappa_BSone.equals(kappa_BX))
			S2 = ACQ_Scope.EMPTY;

		else
			S2 = findScope_two(negative_example, X1, Bgd.union(S1)); // NL: Second recursive call of findScope

		return S1.union(S2);
	}

	protected ACQ_Scope findScope_one(ACQ_Query negative_example, ACQ_Scope X, ACQ_Scope Bgd, boolean mutex) {

		if (mutex && Bgd.size() >= bias.computeMinArity()) { // TESTME if minArity has the good value !!

			ACQ_Query query_bgd = new ACQ_Query(Bgd, Bgd.getProjection(negative_example)); // projection e|Bgd
			ConstraintSet temp_kappa = bias.getKappa(query_bgd);

			boolean non_asked_query = learner.ask_query(query_bgd);
			if (log_queries)
				FileManager.printFile(query_bgd, "queries");
			if (non_asked_query)
				learner.non_asked_query(query_bgd);

			if (!query_bgd.isPositive()) // NL: negative
			{
				return ACQ_Scope.EMPTY; // NL: return emptyset
			} else {
				bias.reduce(temp_kappa);
			}
		}

		if (X.size() == 1) {
			return X;
		}

		// NL: different splitting manners can be defined here!
		ACQ_Scope[] splitedX;

		if (shuffle_split)
			splitedX = X.shuffleSplit();
		else
			splitedX = X.split();

		ACQ_Scope X1 = splitedX[0];
		ACQ_Scope X2 = splitedX[1];

		ACQ_Scope S1 = findScope_one(negative_example, X2, Bgd.union(X1), true); // NL: First recursive call of
																					// findScope
		mutex = (S1 != ACQ_Scope.EMPTY);
		ACQ_Scope S2 = findScope_one(negative_example, X1, Bgd.union(S1), mutex); // NL: Second recursive call of
																					// findScope

		return S1.union(S2);
	}

	/**
	 * **************************************************************************
	 * query_gen
	 *
	 * @param network1
	 * @param network2
	 * @param scope
	 * @param type
	 * @return Query
	 * @date 03-10-2017
	 *
	 *       get a query of type "type" on scope "scope" s.t., network1 and not
	 *       network2
	 *       ***************************************************************************
	 */
	public ACQ_Query query_gen(ACQ_Network network1, ACQ_Network network2, ACQ_Scope scope, Query_type type,
			ACQ_Heuristic h) {

		switch (type) {
		case findc1:
			return solver.solve_AnotB(network1, network2, true, heuristic);
		case findc2:
			return solver.max_AnotB(network1, network2, heuristic);
		case MQ:
			return solver.solve_AnotB(network1, network2, false, heuristic);
		// return solver.peeling_process(network1, network2);
		// return generateExample(network1, network2, true);

		case findscope:
		default:
			return solver.solve_AnotB(network1, network2, false, heuristic);
		}
	}

	public boolean process() {
		return process(null);
	}

	public boolean process(Chrono chrono) {

		boolean convergence = false;
		boolean collapse = false;

		// assert(learned_network.size()==0);
		chrono.start("total_acq_time");

		while (!convergence && !collapse) {

			if (with_collapse_state && !solver.solve(learned_network) && !solver.timeout_reached()) {
				collapse = true;

			}

			if (bias.getConstraints().isEmpty())
				break;

			ACQ_Query membership_query = GenerateQuery(learned_network, bias.network, bias.getVars(),
					Generation_Type.CLASSIC, Query_type.MQ, heuristic, ACQ_Criterion.Kappa);
			// System.out.println(membership_query.getScope().size());

			if (heuristic.equals(ACQ_Heuristic.SOL) && solver.isTimeoutReached()) { // NL: in case of peeling process
				collapse = true;
			}

			if (membership_query.isEmpty()) {
				if (solver.isTimeoutReached()) {
					collapse = true;
				} else {
					convergence = true;
					bias.empty();
				}
			}

			else {
				boolean answer = learner.ask(membership_query);
				if (log_queries)
					FileManager.printFile(membership_query, "queries");
				if (answer)

				{

					bias.reduce(membership_query);

				} else {

					ACQ_IConstraint cst;

					if (bias.getKappa(membership_query).size() == 1) {
						cst = bias.getKappa(membership_query).iterator().next();
						bias.reduce(bias.getExactProjection(cst.getScope()).getConstraints());

					} else {
						ACQ_Scope s = findScope(membership_query, membership_query.getScope(), new ACQ_Scope()); // NL:
																													// modification
																													// bias.getVars()
																													// =>
																													// membership_query.getScope()
						cst = findC(s, membership_query, normalizedCSP);

						if (log_constraints && cst != null)
							FileManager.printFile(cst, "learnedNetwork");

					}
					if (cst == null) {
						collapse = true;
					} else {
						if (cst instanceof ACQ_ConjunctionConstraint) {
							for (ACQ_IConstraint c : ((ACQ_ConjunctionConstraint) cst).constraintSet) {
								learned_network.add(c, true);
								bias.reduce(c);
								bias.reduce(c.getNegation());
							}

						} else {
							learned_network.add(cst, true);
							bias.reduce(cst.getNegation());
						}

						if (verbose)
							System.out.println("learned_network::" + learned_network.size() + "\t::bias::"
									+ bias.network.size() + "\t::==> " + cst);

						chrono.stop("total_acq_time");

					}
				}
			}

			// NL diff cliques detection and reformulation using alldiff global constraints
			if (collapse && allDiff_detection
			// && (bias.getConstraints().size()*100/bias.getInitial_size()<bias_threshold)
			) {
				// learned_network.allDiffCliques();
				allDiff_detection = false;
				collapse = false;
			}

		}

		System.out.println("Solve..." + collapse);

		System.out.println(solver.solve(learned_network));

		return !collapse;
	}

	public boolean isNormalizedCSP() {
		return normalizedCSP;
	}

	public void setNormalizedCSP(boolean normalizedCSP) {
		this.normalizedCSP = normalizedCSP;
	}

	public boolean isShuffleSplit() {
		return shuffle_split;
	}

	public void setShuffleSplit(boolean shuffle) {
		this.shuffle_split = shuffle;
	}

	public boolean isAllDiffDetection() {
		return allDiff_detection;
	}

	public void setAllDiffDetection(boolean allDiff_detection) {
		this.allDiff_detection = allDiff_detection;
	}

	public void setVerbose(boolean verbose) {

		this.verbose = verbose;
	}

	public void setLog_queries(boolean logqueries) {

		this.log_queries = logqueries;
	}

	private ACQ_Query smart_query_gen(ACQ_Network network1, ACQ_Network network2, Query_type type,
			ACQ_Criterion criterion, ACQ_Heuristic h) {

		return solver.Generate_Query(network1, network2, criterion, h);

	}

	public ACQ_Query GenerateQuery(ACQ_Network network1, ACQ_Network network2, ACQ_Scope scope,
			Generation_Type genration, Query_type type, ACQ_Heuristic h, ACQ_Criterion criterion) {
		switch (genration) {
		case CLASSIC:
			return query_gen(network1, network2, scope, type, h);
		case SMART:
			return smart_query_gen(network1, network2, type, criterion, h);
		default:
			return query_gen(network1, network2, scope, type, h);

		}

	}
}
