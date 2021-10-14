package fr.lirmm.coconut.acquisition.core.parallel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.algorithms.ACQ_PACQ;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.learner.ObservedLearner;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.tools.Collective_Stats;
import fr.lirmm.coconut.acquisition.core.tools.StatManager;
import fr.lirmm.coconut.acquisition.core.tools.TimeManager;
import fr.lirmm.coconut.acquisition.core.workspace.IExperience;


public class ACQ_PACQ_Runnable extends Thread {
	IExperience expe;
	public ACQ_Bias bias;
	public ACQ_Learner learner;
	public ACQ_PACQ_Manager coop;
	public Collective_Stats stats;
	public CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox;
	public ConcurrentHashMap<ACQ_Scope, CopyOnWriteArraySet<String>> scopes;
	protected int id;
	StatManager statManager;

	public ACQ_PACQ_Runnable(int id, IExperience expe, ACQ_Bias bias, ACQ_Learner learner,
			ACQ_PACQ_Manager coop, CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox, Collective_Stats stats) {
		this.id = id;
		this.expe = expe;
		this.learner = learner;
		this.bias = bias;
		this.coop = coop;
		this.queries_mailbox = queries_mailbox;
		this.stats = stats;
		this.statManager = new StatManager(bias.getVars().size());
	}

	public boolean executeExperience() {

		ObservedLearner observedLearner = new ObservedLearner(this.learner);
		ObservedProtfolioManager observedPortfolio = new ObservedProtfolioManager(this.coop.queries_mailbox);
		// observe learner for query stats
		PropertyChangeListener queryListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				switch (evt.getPropertyName()) {
				case "ASK":
					Boolean ret = (Boolean) evt.getOldValue();
					ACQ_Query query = (ACQ_Query) evt.getNewValue();
					statManager.update(query);
					break;
				case "NON_ASKED_QUERY":
					ACQ_Query query_ = (ACQ_Query) evt.getNewValue();
					statManager.update_non_asked_query(query_);
					break;

				case "VISITED_SCOPES":
					statManager.update_visited_scopes();
					break;
				}
			}
		};
		observedLearner.addPropertyChangeListener(queryListener);
		observedPortfolio.addPropertyChangeListener(queryListener);
		/*
		 * prepare solver
		 *
		 */

		ACQ_Heuristic heuristic = expe.getHeuristic();
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setLimit(expe.getTimeout());
		// observe solver for time measurement
		final TimeManager timeManager = new TimeManager();
		Chrono chrono = new Chrono(expe.getClass().getName());
		solver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					timeManager.add((Float) evt.getNewValue());
				} else if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
				}
			}
		});
		/*
		 * Instantiate Acquisition algorithm
		 */

		ACQ_PACQ acquisition = new ACQ_PACQ(solver, bias, observedLearner, coop, observedPortfolio, heuristic,
				queries_mailbox);
		// Param
		acquisition.setNormalizedCSP(expe.isNormalizedCSP());
		acquisition.setShuffleSplit(expe.isShuffleSplit());
		acquisition.setAllDiffDetection(expe.isAllDiffDetection());
		acquisition.setThread_id(Thread.currentThread().getName());
		acquisition.setVerbose(expe.isVerbose());
		acquisition.setLog_queries(expe.isLog_queries());

		/*
		 * Run
		 */
		chrono.start("total");
		boolean result = acquisition.process(chrono);
		chrono.stop("total");
		stats.saveChronos(id, chrono);
		stats.saveWallTime(id, coop.getThreadBean().getCurrentThreadCpuTime());
		stats.saveAcqTime(id, coop.getAcqTime());
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		stats.saveBias(id, coop.getPartition(Thread.currentThread().getName()));
		stats.saveLearnedNetwork_i(id, acquisition.getLearnedNetwork());
		stats.saveLearnedNetwork(id, coop.getLearned_network());
		stats.saveResults(id, result);
		return result;

	}

	@Override
	public void run() {
		executeExperience();
	}

}
