package fr.lirmm.coconut.acquisition.core.algorithms;

import java.util.concurrent.CopyOnWriteArraySet;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_ConjunctionConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.learner.Query_type;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_PACQ_Manager;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_QueryMessage;
import fr.lirmm.coconut.acquisition.core.parallel.ObservedProtfolioManager;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.tools.FileManager;

public class ACQ_PACQ extends ACQ_QUACQ {

	private ACQ_PACQ_Manager coop;
	private String thread_id;
	private ObservedProtfolioManager observedportfolio;

	public ACQ_PACQ(ACQ_ConstraintSolver solver, ACQ_Bias bias, ACQ_Learner learner, ACQ_PACQ_Manager coop,
			ObservedProtfolioManager observedportfolio, ACQ_Heuristic heuristic,
			CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox) {

		super(solver, bias, learner, heuristic);
		this.coop = coop;
		this.observedportfolio = observedportfolio;

	}

	/**
	 * **************************************************************** findC
	 * (classic findC of IJCAI13 adapted for multithreading)
	 *
	 * @date 24-03-20
	 *         *****************************************************************
	 */
	protected ACQ_IConstraint findC_one(ACQ_Scope scope, ACQ_Query e) {

		// ACQ_Network learned_network_y = new ACQ_Network(learned_network, scope);
		ACQ_Network learned_network_y = coop.getLearned_network().getProjection(scope);

		ACQ_Bias bias_y = new ACQ_Bias(coop.bias.getExactProjection(scope));

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

			int temp = candidates.size();

			ACQ_Query partial_findC_query = query_gen(learned_network_y,
					new ACQ_Network(constraintFactory, scope, candidates), scope, Query_type.findc1, ACQ_Heuristic.SOL);
			assert (candidates.size() == temp);
			if (partial_findC_query.isEmpty()) {
				coop.Reduce(candidates);

				return candidates.iterator().next();

			}

			temp_kappa = bias_y.getKappa(partial_findC_query);

			if (temp_kappa.isEmpty()) {
				throw new RuntimeException("Collapse state");
			}

			boolean non_asked_query = coop.ask_query(partial_findC_query);
			if(log_queries) FileManager.printFile(this.getThread_id()+" :: "+partial_findC_query, "queries");

			if (non_asked_query)
				learner.non_asked_query(partial_findC_query);

			if (!partial_findC_query.isClassified()) {
				boolean b = learner.ask(partial_findC_query);
				partial_findC_query.classify(b);
				coop.send(new ACQ_QueryMessage(Thread.currentThread().getName(), partial_findC_query));
			}

			if (partial_findC_query.isPositive()) {
				// NL: kappa of partial_findC_query
				coop.Reduce(temp_kappa);

				candidates.removeAll(temp_kappa);
			} else {
				candidates.retainAll(temp_kappa);
			}
		}

	}

	/**
	 * **************************************************************** findC (new
	 * findC presented in AIJ on non-normalized CSPs (new implementation) (adapted
	 * to MT)
	 *
	 * @date 24-03-20
	 *         *****************************************************************
	 */

	protected ACQ_IConstraint findC_two(ACQ_Scope scope, ACQ_Query e) {

		ACQ_Network learned_network_y = coop.getLearned_network().getProjection(scope);

		// if(scope.size()>3)
		// System.out.println("test");

		ACQ_Bias bias_y = new ACQ_Bias(coop.bias.getExactProjection(scope));

		ConstraintSet temp_kappa;

		assert scope.size() >= 1;
//		System.out.println(bias_y.getKappa(e));
		ConstraintSet all_candidates = join(constraintFactory.createSet(bias_y.getConstraints()), bias_y.getKappa(e)); // NL:
																														// join
																														// operator
																														// between
																														// delta
																														// and
																														// kappa_delta
		// System.out.print("all condidates::"+ all_candidates+"\n");

		while (true) {

			ACQ_Query partial_findC_query = query_gen(learned_network_y,
					new ACQ_Network(constraintFactory, scope, all_candidates), scope, Query_type.findc2, ACQ_Heuristic.SOL);
			
			if (partial_findC_query.isEmpty()) {

				ACQ_IConstraint cst = pick(all_candidates);
				if (cst != null) {
					// NL: to check if all_candidates can be removed from B?
					 coop.Reduce(bias_y.getConstraints());
					// coop.Reduce(cst);
				}

				return cst;
			}

			temp_kappa = all_candidates.getKappa(partial_findC_query); // NL: level or all candidates?

			boolean non_asked_query = coop.ask_query(partial_findC_query);
			if(log_queries) 
				FileManager.printFile(this.getThread_id()+" :: "+partial_findC_query, "queries");
			if (non_asked_query)
				learner.non_asked_query(partial_findC_query);

			if (!partial_findC_query.isClassified()) {
				boolean b = learner.ask(partial_findC_query);

				partial_findC_query.classify(b);

				coop.send(new ACQ_QueryMessage(Thread.currentThread().getName(), partial_findC_query));

			}

			if (partial_findC_query.isPositive()) {

				all_candidates.removeAll(temp_kappa);
				coop.Reduce(temp_kappa);

			} else {

				ACQ_Scope S = findScope(partial_findC_query, scope, new ACQ_Scope());

				if (scope.containsAll(S) && scope.size() < S.size()) {

					ACQ_IConstraint cst = findC(S, partial_findC_query, normalizedCSP);

					if (cst == null)
						throw new RuntimeException("Collapse state");

					else {
						coop.getLearned_network().add(cst, false);
						coop.Reduce(cst);
						coop.Reduce(cst.getNegation());
					}

				} else

					all_candidates = join(all_candidates, temp_kappa);

			}
		}

	}

	/**
	 * ************************************************************** FindScope
	 * procedure (adapted to MT)
	 *
	 * @param negative_example : a complete negative example
	 * @param X                : problem variables (and/or) foreground variables
	 * @param Bgd              : background variables
	 * @param mutex            : boolean mutex
	 * @return variable scope
	 *
	 * @update 24/03/20
	 * **************************************************************
	 */

	protected ACQ_Scope findScope_two(ACQ_Query negative_example, ACQ_Scope X, ACQ_Scope Bgd) {

		if (Bgd.size() >= coop.bias.computeMinArity()) { // TESTME if minArity has the good value !!

			ACQ_Query query_bgd = new ACQ_Query(Bgd, Bgd.getProjection(negative_example)); // projection e|Bgd
			
			ConstraintSet temp_kappa = coop.bias.getKappa(query_bgd);
			if (!temp_kappa.isEmpty()) {

				boolean non_asked_query = coop.ask_query(query_bgd);
				if(log_queries) 
					FileManager.printFile(this.getThread_id()+" :: "+query_bgd, "queries");
				if (non_asked_query)
					learner.non_asked_query(query_bgd);

				if (!query_bgd.isClassified()) {
					learner.ask(query_bgd);
					coop.send(new ACQ_QueryMessage(Thread.currentThread().getName(), query_bgd));

				}

				if (query_bgd.isNegative())
					return ACQ_Scope.EMPTY; // NL: return emptyset

				else
					coop.Reduce(temp_kappa);

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

		ConstraintSet kappa_BXone = coop.bias.getKappa(query_BXone);

		ACQ_Query query_BX = new ACQ_Query(Bgd.union(X), Bgd.union(X).getProjection(negative_example)); // projection
																										// e|Bgd

		ConstraintSet kappa_BX = coop.bias.getKappa(query_BX);

		ACQ_Scope S1;

		if (kappa_BXone.equals(kappa_BX))
			S1 = ACQ_Scope.EMPTY;
		else
			S1 = findScope_two(negative_example, X2, Bgd.union(X1)); // NL: First recursive call of findScope

		ACQ_Query query_BSone = new ACQ_Query(Bgd.union(S1), Bgd.union(S1).getProjection(negative_example)); // projection
																												// e|Bgd

		ConstraintSet kappa_BSone = coop.bias.getKappa(query_BSone);

		ACQ_Scope S2;

		if (kappa_BSone.equals(kappa_BX))
			S2 = ACQ_Scope.EMPTY;

		else
			S2 = findScope_two(negative_example, X1, Bgd.union(S1)); // NL: Second recursive call of findScope

		return S1.union(S2);
	}

	protected ACQ_Scope findScope_one(ACQ_Query negative_example, ACQ_Scope X, ACQ_Scope Bgd, boolean mutex) {

		if (mutex && Bgd.size() >= coop.bias.computeMinArity()) { // TESTME if minArity has the good value !!

			ACQ_Query query_bgd = new ACQ_Query(Bgd, Bgd.getProjection(negative_example)); // projection e|Bgd
			ConstraintSet temp_kappa = coop.bias.getKappa(query_bgd);
			
			boolean non_asked_query = coop.ask_query(query_bgd);
			if(log_queries) 
				FileManager.printFile(this.getThread_id()+" :: "+query_bgd, "queries");
			if (non_asked_query)
				learner.non_asked_query(query_bgd);

			if (!query_bgd.isClassified()) {
				learner.ask(query_bgd);

				coop.send(new ACQ_QueryMessage(Thread.currentThread().getName(), query_bgd));
			}
			if (!query_bgd.isPositive()) // NL: negative
			{
				return ACQ_Scope.EMPTY; // NL: return emptyset
			} else {
				coop.Reduce(temp_kappa);

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

	@Override
	public boolean process() {
		return process(null);
	}

	public boolean process(Chrono chrono) {

		boolean collapse = false;
		boolean convergence = false;
		ACQ_IConstraint cst;
		ACQ_Query membership_query;
		ACQ_Bias bias_i;
		chrono.start("total_acq_time");
		do {

			bias_i = coop.getPartition(thread_id);

			if (with_collapse_state && !solver.solve(coop.getLearned_network()) && !solver.timeout_reached()) {
				collapse = true;
			}

			if (coop.getBias().getSize() <= 1) {
//				coop.Interrupt();
				return !collapse;
			}
			if (bias_i.getSize() > 0)
				membership_query = query_gen(coop.getLearned_network(), bias_i.network, coop.bias.getVars(),
						Query_type.MQ, heuristic);
			
			else
				membership_query = new ACQ_Query();
			
			if (heuristic.equals(ACQ_Heuristic.SOL) && solver.isTimeoutReached()) { // NL: in case of peeling process
				collapse = true;
				return !collapse;
			}

			if (membership_query.isEmpty()) {

				if (!solver.isTimeoutReached()) {
					if (solver.isPeeling_process()) {
						if (solver.get2remove() != null && solver.get2remove().getConstraints().size() != 0) {
							coop.Reduce(solver.get2remove().getConstraints());
							bias_i.reduce(solver.get2remove().getConstraints());
						}
				//		FileManager.printFile(Thread.currentThread().getName() + "::" + solver.get2remove(),
				//				"ToRemove");
						solver.setPeeling_process(false);
						solver.reset2remove();
					} else
						coop.Reduce(bias_i.getConstraints());

					if (bias_i.getSize() == 0) {
						System.out.println(thread_id + " :: Converged");
						convergence = true;
						return convergence;
					}
					
				} else {
		//			FileManager.printFile(Thread.currentThread().getName() + ":: \n " + bias_i.getNetwork()+"\n",
		//					"Bias_i");
		//			FileManager.printFile( Thread.currentThread().getName() + ":: \n " + getLearnedNetwork()+"\n",
		//					"learned_network");
					collapse = true;
					return !collapse;
				}
			}

			boolean answer = learner.ask(membership_query);
			if(log_queries)
				FileManager.printFile(this.getThread_id()+" :: "+membership_query, "queries");
			if (answer)

			{
				coop.Reduce(membership_query);

			}

			ACQ_Scope s = findScope(membership_query, coop.getBias().getVars(), new ACQ_Scope());

			coop.semaphore.acquireUninterruptibly();

			cst = findC(s, membership_query, normalizedCSP);

			coop.semaphore.release();

			if (cst == null) {
				observedportfolio.visited_scopes();
				continue;

			} else {
				if (cst instanceof ACQ_ConjunctionConstraint) {

					for (ACQ_IConstraint c : ((ACQ_ConjunctionConstraint) cst).constraintSet) {
						learned_network.add(cst, true);
						coop.getLearned_network().add(c, true);

					}
				} else {
					learned_network.add(cst, true);
					coop.getLearned_network().add(cst, true);

				}
				chrono.stop("total_acq_time");
				coop.setAcqTime();

			}

			if (verbose)
				System.out.println(
						Thread.currentThread().getName() + " :: learned_network::" + coop.getLearned_network().size()
								+ "\t::bias::" + coop.bias.network.size() + "\t::==> " + cst);

		} while (bias_i.getSize() != 0 || !coop.getBias().getConstraints().isEmpty());

		// NL diff cliques detection and reformulation using alldiff global constraints
		if (collapse && allDiff_detection
		// && (bias.getConstraints().size()*100/bias.getInitial_size()<bias_threshold)
		) {
			// learned_network.allDiffCliques();
			allDiff_detection = false;
			collapse = false;
		}

		return !collapse;

	}

	@Override
	public ACQ_Scope findScope(ACQ_Query negative_example, ACQ_Scope X, ACQ_Scope Bgd) {
		if (new_findscope)
			return findScope_two(negative_example, X, Bgd);

		return findScope_one(negative_example, X, Bgd, false);

	}

	@Override
	public ACQ_IConstraint findC(ACQ_Scope scope, ACQ_Query e, boolean normalizedCSP) {

		if (normalizedCSP)
			return findC_one(scope, e);
		else
			return findC_two(scope, e);

	}

	public String getThread_id() {
		return thread_id;
	}

	public void setThread_id(String thread_id) {
		this.thread_id = thread_id;
	}
	public void setLog_queries(boolean logqueries) {

		this.log_queries = logqueries;
	}
}
