package fr.lirmm.coconut.acquisition.core.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.chocosolver.util.tools.StringUtils;
import org.json.JSONObject;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_ConjunctionConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ContradictionSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.acqsolver.SATSolver;
import fr.lirmm.coconut.acquisition.core.algorithms.ACQ_CONACQv1;
import fr.lirmm.coconut.acquisition.core.algorithms.ACQ_CONACQv2;
import fr.lirmm.coconut.acquisition.core.algorithms.ACQ_QUACQ;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Algorithm;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ObservedLearner;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_PACQ_Manager;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_PACQ_Runnable;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_Partition;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_QueryMessage;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.tools.Collective_Stats;
import fr.lirmm.coconut.acquisition.core.tools.FileManager;
import fr.lirmm.coconut.acquisition.core.tools.StatManager;
import fr.lirmm.coconut.acquisition.core.tools.TimeManager;
import fr.lirmm.coconut.acquisition.core.tools.TimeUnit;
import fr.lirmm.coconut.acquisition.core.tools.convergenceTest.AcquisitionRate;

public class ACQ_WS {
	public static Collective_Stats stats = new Collective_Stats();
	public static int instance = 0;
	private static boolean printLearnedNetworkInLogFile = false;
	private static boolean printBiasInLogFile = false;
	 static ArrayList<String> users;
	public static Collective_Stats executeExperience(IExperience expe) {

		/*
		 * prepare bias
		 */
		int id = 0;
		ACQ_Bias bias = expe.createBias();
		/*
		 * prepare learner
		 */
		ACQ_Learner learner = expe.createLearner();
		ObservedLearner observedLearner = new ObservedLearner(learner);
		// observe learner for query stats
		final StatManager statManager = new StatManager(bias.getVars().size());
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

				}
			}
		};
		observedLearner.addPropertyChangeListener(queryListener);
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
			private boolean printTimeInLogFile = true;

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					float time = (Float) evt.getNewValue();
					timeManager.add((Float) time);
					if (printTimeInLogFile)
						FileManager.printFile(time, "chrono");
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
		ACQ_QUACQ acquisition = new ACQ_QUACQ(solver, bias, observedLearner, heuristic);
		// Param
		acquisition.setNormalizedCSP(expe.isNormalizedCSP());
		acquisition.setShuffleSplit(expe.isShuffleSplit());
		acquisition.setAllDiffDetection(expe.isAllDiffDetection());
		acquisition.setVerbose(expe.isVerbose());
		acquisition.setLog_queries(expe.isLog_queries());

		/*
		 * Run
		 */
		chrono.start("total");
		boolean result = acquisition.process(chrono);
		chrono.stop("total");
		stats.saveChronos(id, chrono);
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		stats.saveBias(id, acquisition.getBias());
		stats.saveLearnedNetwork(id, acquisition.getLearnedNetwork());
		stats.saveResults(id, result);
		stats.ComputeGlobalStats(0, observedLearner, bias, (DefaultExperience) expe, learner.memory.size());

		/*
		 * Print results
		 */
		System.out.println("Learned Network Size: " + acquisition.getLearnedNetwork().size());
		System.out.println("Initial Bias size: " + acquisition.getBias().getInitial_size());
		System.out.println("Final Bias size: " + acquisition.getBias().getSize());

		System.out.println(statManager + "\n" + timeManager.getResults());
		DecimalFormat df = new DecimalFormat("0.00E0");
		double totalTime = (double) chrono.getResult("total") / 1000.0;
		double total_acq_time = (double) chrono.getLast("total_acq_time") / 1000.0;

		System.out.println("------Execution times------");
		for (String serieName : chrono.getSerieNames()) {
			if (!serieName.contains("total")) {
				double serieTime = (double) chrono.getResult(serieName) / 1000.0;
				System.out.println(serieName + " : " + df.format(serieTime));
			}
		}
		System.out.println("Convergence time : " + df.format(totalTime));
		System.out.println("Acquisition time : " + df.format(total_acq_time));
		System.out.println("*************Learned Network CL example ******");
		ACQ_Query q = solver.solveA(acquisition.getLearnedNetwork());

		q.classify(learner.ask(q));
		System.out.println("query : " + q.toString());

		ACQ_Network target_network = expe.createTargetNetwork();
		int convergenceRate = 0;
		double RelativeAcquisitionRate = 0;
		double AbsoluteAcquisitionRate = 0;

		AcquisitionRate ar = new AcquisitionRate(acquisition.getLearnedNetwork(), target_network,
				solver.getDomain().getMin(0), solver.getDomain().getMax(0));
		try {
			convergenceRate = expe.convergenceRate(solver, target_network, acquisition.getLearnedNetwork());
			RelativeAcquisitionRate = ar.relativeAcquisitionRate;
			AbsoluteAcquisitionRate = ar.absoluteAcquisitionRate;

			System.out.println("convergenceRate: " + convergenceRate);
			System.out.println("RelativeAcquisitionRate: " + RelativeAcquisitionRate);
			System.out.println("AbsoluteAcquisitionRate: " + AbsoluteAcquisitionRate);

		} catch (Exception e) {
			System.out.print("Target network not implemented");
		}
		if (expe.isPuzzlePrint())
			puzzleprint(q, expe.isQueens());
		else
			System.out.println("Query :: " + Arrays.toString(q.values));
		System.out.println("Classification :: " + learner.ask(q));

		if (result)
			System.out.println("YES...Converged");
		else
			System.out.println("NO...Collapsed");
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();

		String input = dateFormat.format(date) + "\t" + 1 + "\t" + acquisition.getLearnedNetwork().size() + "\t"
				+ RelativeAcquisitionRate + "\t" + AbsoluteAcquisitionRate + "\t" + convergenceRate + "\t"
				+ +(statManager.getNbCompleteQuery() + statManager.getNbPartialQuery()) + "\t"
				+ ((statManager.getNbCompleteQuery() + statManager.getNbPartialQuery())
						/ acquisition.getLearnedNetwork().size())
				+ "\t" + statManager.getNbCompleteQuery() + "\t" + statManager.getQuerySize() + "\t"
				+ df.format(total_acq_time) + "\t" + df.format(totalTime) + "\t" + df.format(timeManager.getMax())
				+ "\t" + 0 + statManager.getNon_asked_query() + "\t" + 0 + "\t"
				+ acquisition.getBias().getInitial_size() + "\t" + acquisition.getBias().getSize() + "\t"
				+ expe.getVrs() + "\t" + expe.getHeuristic();

		// FileManager.printFile(bias,"bias");

		// System.out.println("query :: " + Arrays.toString(q.values));
		// System.out.println("Classification :: " + learner.ask(q));

		// System.out.println("Network :: " + learned_network);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));
		FileManager.printResults(acquisition.getLearnedNetwork(),
				expe.getName() + "_" + dtf.format(now) + ".LearnedNetwork");

		FileManager.printResults(input, expe.getName() + "_" + instance + "_" + dtf.format(now) + ".results");

		return stats;

	}


	public static Collective_Stats executeExperience(DefaultExperience expe, ACQ_Algorithm algo, int nbThread,
			ACQ_Partition partition) {
		if (algo.equals(ACQ_Algorithm.QUACQ))
			return executeExperience(expe);
		 
		else // PACQ
			
			return LAUNCH_PORTFOLIO(nbThread, partition, expe);
		

	}


	public static Collective_Stats LAUNCH_PORTFOLIO(int nb_threads, ACQ_Partition partition, DefaultExperience expe) {

		System.gc();

		CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox = new CopyOnWriteArraySet<>();

		Collective_Stats stats = new Collective_Stats();

		ExecutorService executor = Executors.newCachedThreadPool();

		ACQ_Bias bias = expe.createBias();

		ACQ_PACQ_Manager coop = new ACQ_PACQ_Manager(queries_mailbox);

		coop.setBias(bias);

		coop.setLearnedNetwork(bias.getVars());

		coop.setPartitionType(partition);

		coop.applyPartitioning(nb_threads);

		ACQ_Learner learner = expe.createLearner();

		for (int i = 0; i < nb_threads; i++) {
			ACQ_PACQ_Runnable p = new ACQ_PACQ_Runnable(i, expe, bias, learner, coop, queries_mailbox, stats);

			executor.execute(p);

		}

		executor.shutdown();

		while (!executor.isTerminated()) {

		}

		System.out.println(Thread.currentThread().getName() + ":: TERMINATED!");

		Printstats(stats, nb_threads, learner, bias, expe, coop.getPartitionType());

		return stats;
	}

	public static Collective_Stats LAUNCH_PACQDIST(int nb_threads, ACQ_Partition partition, DefaultExperience expe) {

		System.gc();

		CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox = new CopyOnWriteArraySet<>();

		Collective_Stats stats = new Collective_Stats();

		ExecutorService executor = Executors.newCachedThreadPool();

		ArrayList<ACQ_Bias> biases = expe.createDistBias();

		for (int i = 1; i < biases.size(); i++)
			biases.get(0).getNetwork().addAll(biases.get(i).getNetwork(), true);
		ACQ_Bias bias = biases.get(0);

		ACQ_PACQ_Manager coop = new ACQ_PACQ_Manager(queries_mailbox);

		coop.setBias(bias);

		coop.setLearnedNetwork(bias.getVars());

		coop.setPartitionType(partition);

		coop.applyPartitioning(nb_threads);

		ACQ_Learner learner = expe.createLearner();

		for (int i = 0; i < nb_threads; i++) {
			ACQ_PACQ_Runnable p = new ACQ_PACQ_Runnable(i, expe, bias, learner, coop, queries_mailbox, stats);

			executor.execute(p);

		}

		executor.shutdown();

		while (!executor.isTerminated()) {

		}

		System.out.println(Thread.currentThread().getName() + ":: TERMINATED!");

		Printstats(stats, nb_threads, learner, bias, expe, coop.getPartitionType());

		return stats;
	}


	public static int[] bitSet2Int(BitSet bs) {
		int[] result = new int[bs.cardinality()];
		int counter = 0;
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			result[counter++] = i;
		}
		return result;
	}

	public static int[] mergeWithoutDuplicates(int[] a, int[] b) {
		BitSet bs = new BitSet();
		for (int numvar : a)
			bs.set(numvar);
		for (int numvar : b)
			bs.set(numvar);
		return bitSet2Int(bs);
	}

	public static void Printstats(Collective_Stats stats, int nb_threads, ACQ_Learner learner, ACQ_Bias bias,
			DefaultExperience expe, ACQ_Partition partition) {
		double wallTime = 0.0;
		double acqTime = 0.0;
		int totalqueries = 0;
		int totalvisits = 0;
		int totalmqueries = 0;
		int totalquerysize = 0;
		int total_non_asked = 0;
		double tmax = Double.MIN_VALUE;
		for (int i = 0; i < stats.getChronos().size(); i++) {
			System.out.print("\n\n");
			System.out.println("================== Thread :: " + i + "========================");
			try {
				System.out.println(stats.getStatManagers().get(i) + "\n" + stats.getTimeManagers().get(i).getResults());
				totalqueries += stats.getStatManagers().get(i).getNbCompleteQuery()
						+ stats.getStatManagers().get(i).getNbPartialQuery();
				totalmqueries += stats.getStatManagers().get(i).getNbCompleteQuery();
				totalvisits += stats.getStatManagers().get(i).getVisited_scopes();
				totalquerysize += stats.getStatManagers().get(i).getQuerySize();
				total_non_asked += stats.getStatManagers().get(i).getNon_asked_query();
				if (stats.getTimeManagers().get(i).getMax() > tmax)
					tmax = stats.getTimeManagers().get(i).getMax();
				DecimalFormat df = new DecimalFormat("0.00E0");
				double totaltime_per_thread = 0;
				double totaltime_per_thread_cpu = 0;

				double acq_time = 0;

				totaltime_per_thread = (double) stats.getChronos().get(i).getResult("total") / 1000.0;
				totaltime_per_thread_cpu = (double) stats.getChronosCPU().get(i).getResult("total") / 1000.0;

				acq_time = (double) stats.getChronos().get(i).getLast("total_acq_time") / 1000.0;

				System.out.println("------Execution times-------");

				for (String serieName : stats.getChronos().get(i).getSerieNames()) {
					if (!serieName.contains("total")) {
						double serieTime = (double) stats.getChronos().get(i).getResult(serieName) / 1000.0;
						System.out.println(serieName + " : " + df.format(serieTime));
					}

				}
				System.out.println("Convergence time Th : " + df.format(totaltime_per_thread));
				System.out.println("Acquisition time Th : " + df.format(acq_time));
				double wall_time_per_thread = (stats.getWallTime(i) / 1_000_000_000.0);
				double acq_time_per_thread = (stats.getAcqTime(i) / 1_000_000_000.0);

				if (acqTime < acq_time_per_thread)
					acqTime = acq_time_per_thread;
				if (wallTime < wall_time_per_thread)
					wallTime = wall_time_per_thread;
				FileManager.printFile("wall time::" + df.format(wall_time_per_thread) + "wall time alain:: "
						+ df.format(totaltime_per_thread_cpu), "cpu_time_comparaison");
				System.err.println("wall time alain:: " + df.format(totaltime_per_thread_cpu));
				System.err.println("wall time::" + df.format(wall_time_per_thread));
				System.err.println("acq time::" + df.format(acq_time_per_thread));

				System.out.println("-------Learned Network & Bias Size--------");
				ACQ_Network learned_network = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());
				for (ACQ_IConstraint cst : stats.getCL_i().get(i).getConstraints()) {
					if (cst instanceof ACQ_ConjunctionConstraint) {
						for (ACQ_IConstraint c : ((ACQ_ConjunctionConstraint) cst).constraintSet) {
							learned_network.add(c, true);
						}
					} else if (cst instanceof ACQ_IConstraint) {
						learned_network.add(cst, true);
					}

				}
				double queries = (stats.getStatManagers().get(i).getNbCompleteQuery()
						+ stats.getStatManagers().get(i).getNbPartialQuery());
				double r = queries / (double) learned_network.size();
				if (stats.getResults().get(i)) {
				}
				System.out.println("Learned Network Size : " + learned_network.size());
				if (printLearnedNetworkInLogFile)
					FileManager.printFile(learned_network, "LearnedNetwork");
				System.out.println("R : " + Math.round(r));

				if (printBiasInLogFile)
					FileManager.printFile(bias, "bias");
				// System.out.println("Bias Initial Size : " +
				// stats.getBiases().get(i).getInitial_size());
				// System.out.println("Bias Final Size : " +
				// stats.getBiases().get(i).getSize());
				System.out.println("==========================================================");

			} catch (Exception e) {
			}
		}

		// ConstraintFactory constraintFactory=new ConstraintFactory();
		ACQ_Network learned_network = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());

		for (ACQ_Network n : stats.getCL().values()) {

			learned_network.addAll(n, false);
		}
		int Bias_init = 0;
		int Bias_final = 0;
		for (ACQ_Bias b : stats.getBiases().values()) {
			Bias_init += b.getInitial_size();
			Bias_final += b.getSize();
		}
		System.out.print("Complete CL :: \n\n");
		final ACQ_ConstraintSolver solver = expe.createSolver();
		DecimalFormat df = new DecimalFormat("0.00E0");
		solver.setVars(bias.getVars());
		solver.setTimeout(false);
		// ACQ_Query q = solver.solveA(learned_network);
		System.out.println("================== Total Stats========================");
		System.out.println("Learned Network Size : " + learned_network.size());
		System.out.println("Total Queries : " + totalqueries);
		System.out.println("Total MQ : " + totalmqueries);
		System.out.println("Total AVG Query size : " + totalquerysize / nb_threads);
		System.out.println("Query Load Balancing : " + stats.calculate_query_SD());
		System.out.println("min session : " + stats.get_query_min());
		System.out.println("max session : " + stats.get_query_max());
		System.out.println("-------------------------------------------------------");
		// System.out.println("Total Acquisition time : " + df.format(total_acq_time));
		// System.out.println("Total Execution time : " + df.format(totaltime));
		System.out.println("ACQ time : " + df.format(acqTime));
		System.out.println("WALL time : " + df.format(wallTime));
		System.out.println("Total T_Max : " + df.format(tmax));
		System.out.println("-------------------------------------------------------");
		System.out.println("Non Asked Queries : " + total_non_asked);
		System.out.println("Redundant Scopes Visits : " + totalvisits);
		System.out.println("-------------------------------------------------------");
		// System.out.println("AVG Execution time : " + df.format(totaltime /
		// nb_threads));
		// System.out.println("AVG Acquisition time : " + df.format(total_acq_time /
		// nb_threads));
		// System.out.println("Total T_Avg : " + df.format(total_tavg / nb_threads));
		// System.out.println("Total AVG R : " + (totalr / nb_threads));
		// System.out.println("Total Convergence: " + total_convergence);
		System.out.println("Total Biases Initial Size :" + Bias_init);
		System.out.println("Total Biases Final Size :" + Bias_final);

		int collapsed_threads = 0;

		for (Integer key : stats.results.keySet())
			if (stats.results.get(key) != null && !stats.results.get(key))
				collapsed_threads++;
		System.out.println("Threads Collapsed:: " + collapsed_threads);

		// System.out.println("query :: " + Arrays.toString(q.values));
		// System.out.println("Classification :: " + learner.ask(q));

		// System.out.println("Network :: " + learned_network);

		// FileManager.printFile(bias,"bias");

		// System.out.println("query :: " + Arrays.toString(q.values));
		// System.out.println("Classification :: " + learner.ask(q));

		// System.out.println("Network :: " + learned_network);
		int convergenceRate = 0;
		double RelativeAcquisitionRate = 0;
		double AbsoluteAcquisitionRate = 0;
		try {
		ACQ_Network target_network = expe.createTargetNetwork();
		AcquisitionRate ar = new AcquisitionRate(learned_network, target_network, solver.getDomain().getMin(0),
				solver.getDomain().getMax(0));
	
			convergenceRate = expe.convergenceRate(solver, target_network, learned_network);
			RelativeAcquisitionRate = ar.relativeAcquisitionRate;
			AbsoluteAcquisitionRate = ar.absoluteAcquisitionRate;

			System.out.println("convergenceRate: " + convergenceRate);
			System.out.println("RelativeAcquisitionRate: " + RelativeAcquisitionRate);
			System.out.println("AbsoluteAcquisitionRate: " + AbsoluteAcquisitionRate);

		} catch (Exception e) {
			System.out.print("Target network not implemented");
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();
		FileManager.printResults(learned_network, expe.getName() + dateFormat.format(date) + ".LearnedNetwork");

		String input = (dateFormat.format(date) + "\t" + nb_threads + "\t" + learned_network.size() + "\t"
				+ RelativeAcquisitionRate + "\t" + AbsoluteAcquisitionRate + "\t" + convergenceRate
				+ +learned_network.size() + "\t" + (totalqueries / nb_threads) + "\t"
				+ (totalqueries / learned_network.size()) + "\t" + (totalmqueries / nb_threads) + "\t"
				+ (totalquerysize / nb_threads) + "\t" + df.format(acqTime) + "\t" + df.format(wallTime) + "\t"
				+ df.format(tmax) + "\t" + 0 + "\t" + (total_non_asked / nb_threads) + "\t" + (totalvisits / nb_threads)
				+ "\t" + Bias_init + "\t" + Bias_final + "\t" + expe.getVrs() + "\t" + expe.getHeuristic() + "\t"
				+ partition + "\t" + collapsed_threads);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));
		FileManager.printResults(input, expe.getName() + instance + "_" + dtf.format(now) + ".results");

	}

	public static void PrintstatsDist(int agents, Collective_Stats stats, ACQ_Bias bias, DefaultExperience expe) {
		double wallTime = 0.0;
		double acqTime = 0.0;
		int totalqueries = 0;
		int totalvisits = 0;
		int totalmqueries = 0;
		int totalquerysize = 0;
		int total_non_asked = 0;
		double tmax = Double.MIN_VALUE;
		for (int i = 0; i < stats.getChronos().size(); i++) {
			System.out.print("\n\n");
			System.out.println("================== Thread :: " + i + "========================");
			try {
				System.out.println(stats.getStatManagers().get(i) + "\n" + stats.getTimeManagers().get(i).getResults());
				totalqueries += stats.getStatManagers().get(i).getNbCompleteQuery()
						+ stats.getStatManagers().get(i).getNbPartialQuery();
				totalmqueries += stats.getStatManagers().get(i).getNbCompleteQuery();
				totalvisits += stats.getStatManagers().get(i).getVisited_scopes();
				totalquerysize += stats.getStatManagers().get(i).getQuerySize();
				total_non_asked += stats.getStatManagers().get(i).getNon_asked_query();
				if (stats.getTimeManagers().get(i).getMax() > tmax)
					tmax = stats.getTimeManagers().get(i).getMax();
				DecimalFormat df = new DecimalFormat("0.00E0");
				double totaltime_per_thread = 0;
				double acq_time = 0;

				totaltime_per_thread = (double) stats.getChronos().get(i).getResult("total") / 1000.0;

				acq_time = (double) stats.getChronos().get(i).getLast("total_acq_time") / 1000.0;

				System.out.println("------Execution times-------");

				for (String serieName : stats.getChronos().get(i).getSerieNames()) {
					if (!serieName.contains("total")) {
						double serieTime = (double) stats.getChronos().get(i).getResult(serieName) / 1000.0;
						System.out.println(serieName + " : " + df.format(serieTime));
					}

				}

				System.out.println("Convergence time Th : " + df.format(totaltime_per_thread));
				System.out.println("Acquisition time Th : " + df.format(acq_time));
				System.out.println("CL ::" + stats.getCL().get(i).size());

				if (acqTime < acq_time)
					acqTime = acq_time;
				if (wallTime < totaltime_per_thread)
					wallTime = totaltime_per_thread;

				System.out.println("-------Learned Network & Bias Size--------");
				ACQ_Network learned_network = new ACQ_Network();
				for (ACQ_Network network : stats.getCL().values()) {
					learned_network.addAll(network.getConstraints(), true);
				}
				double queries = (stats.getStatManagers().get(i).getNbCompleteQuery()
						+ stats.getStatManagers().get(i).getNbPartialQuery());
				double r = queries / (double) learned_network.size();
				if (stats.getResults().get(i)) {
				}
				System.out.println("Learned Network Size : " + learned_network.size());
				System.out.println("R : " + Math.round(r));

				// System.out.println("Bias Initial Size : " +
				// stats.getBiases().get(i).getInitial_size());
				// System.out.println("Bias Final Size : " +
				// stats.getBiases().get(i).getSize());
				System.out.println("==========================================================");
			} catch (Exception e) {
			}
		}

		// ConstraintFactory constraintFactory=new ConstraintFactory();
		int learned_network = 0;
		for (ACQ_Network n : stats.getCL().values()) {
			learned_network += n.size();
		}
		int Bias_init = bias.getSize();
		int Bias_final = 0;
		for (ACQ_Bias b : stats.getBiases().values()) {
			Bias_final += b.getSize();
		}
		System.out.print("Complete CL :: \n\n");
		final ACQ_ConstraintSolver solver = expe.createSolver();
		DecimalFormat df = new DecimalFormat("0.00E0");
		solver.setVars(bias.getVars());
		solver.setTimeout(false);
		// ACQ_Query q = solver.solveA(learned_network);
		System.out.println("================== Total Stats========================");
		System.out.println("Learned Network Size : " + learned_network);
		System.out.println("Agents : " + agents);
		System.out.println("Total Queries : " + (totalqueries / agents));
		System.out.println("Total MQ : " + (totalmqueries / agents));
		System.out.println("Total AVG Query size : " + (totalquerysize / agents));
		System.out.println("-------------------------------------------------------");
		// System.out.println("Total Acquisition time : " + df.format(total_acq_time));
		// System.out.println("Total Execution time : " + df.format(totaltime));
		System.out.println("ACQ time : " + df.format(acqTime));
		System.out.println("WALL time : " + df.format(wallTime));
		System.out.println("Total T_Max : " + df.format(tmax));
		System.out.println("-------------------------------------------------------");
		System.out.println("Non Asked Queries : " + total_non_asked);
		System.out.println("Redundant Scopes Visits : " + totalvisits);
		System.out.println("-------------------------------------------------------");
		// System.out.println("AVG Execution time : " + df.format(totaltime /
		// nb_threads));
		// System.out.println("AVG Acquisition time : " + df.format(total_acq_time /
		// nb_threads));
		// System.out.println("Total T_Avg : " + df.format(total_tavg / nb_threads));
		// System.out.println("Total AVG R : " + (totalr / nb_threads));
		// System.out.println("Total Convergence: " + total_convergence);
		System.out.println("Total Biases Initial Size :" + Bias_init);
		System.out.println("Total Biases Final Size :" + Bias_final);

		for (Integer key : stats.results.keySet()) {

			if (stats.results.get(key) != null && !stats.results.get(key)) {

				System.out.println("Thread n° : " + key + " = Collapsed");
			}
			else {

				System.out.println("Thread n° : " + key + " = Converged");
			}
		}

		// System.out.println("query :: " + Arrays.toString(q.values));
		// System.out.println("Classification :: " + learner.ask(q));

		// System.out.println("Network :: " + learned_network);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/ddHH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();

		String input = (stats.results.size() + "\t" + learned_network + "\t" + (totalqueries / agents) + "\t"
				+ (totalqueries / learned_network) + "\t" + (totalmqueries / agents) + "\t" + (totalquerysize / agents)
				+ "\t" + df.format(acqTime) + "\t" + df.format(wallTime) + "\t" + df.format(tmax) + "\t"
				+ (total_non_asked / agents) + "\t" + (totalvisits / agents) + "\t" + Bias_init + "\t" + Bias_final
				+ "\t" + expe.getVrs());
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));

		FileManager.printResults(learned_network, expe.getName() + "_" + dtf.format(now) + ".LearnedNetwork");

		FileManager.printFile(bias, "bias");
		FileManager.printResults(input, expe.getName() + instance + "_" + dtf.format(now) + ".results");

	}

	public ACQ_IConstraint random(ACQ_Network coll) {
		int num = (int) (Math.random() * coll.size());
		for (ACQ_IConstraint t : coll)
			if (--num < 0)
				return t;
		throw new AssertionError();
	}

	public static void puzzleprint(ACQ_Query query, boolean queens) {
		int n;
		if (!queens) {
			n = (int) Math.sqrt(query.getScope().size());
			StringBuilder st = new StringBuilder();
			String line = "+";
			for (int i = 0; i < n; i++) {
				line += "----+";
			}
			line += "\n";
			st.append(line);
			for (int i = 0; i < n; i++) {
				st.append("|");
				for (int j = 0; j < n; j++) {
					st.append(StringUtils.pad((query.values[i * n + j]) + "", -3, " ")).append(" |");
				}
				st.append(MessageFormat.format("\n{0}", line));
			}
			st.append("\n\n\n");
			System.out.println(st.toString());
		} else {
			n = query.getScope().size();
			StringBuilder st = new StringBuilder();
			String line = "+";
			for (int i = 0; i < n; i++) {
				line += "----+";
			}
			line += "\n";
			st.append(line);
			for (int i = 0; i < n; i++) {
				st.append("|");
				for (int j = 0; j < n; j++) {
					if (j == query.values[i])
						st.append(StringUtils.pad(("Q") + "", -3, " ")).append(" |");
					else
						st.append(StringUtils.pad(("*") + "", -3, " ")).append(" |");

				}
				st.append(MessageFormat.format("\n{0}", line));
			}
			st.append("\n\n\n");
			System.out.println(st.toString());
		}
	}

	public static Collective_Stats executeConacqV2Experience(IExperience expe) {
		/*
		 * prepare bias
		 */
		int id = 0;
		ACQ_Bias bias = expe.createBias();

		/*
		 * prepare learner
		 */
		ACQ_Learner learner = expe.createLearner();
		ObservedLearner observedLearner = new ObservedLearner(learner);

		// observe learner for query stats
		final StatManager statManager = new StatManager(bias.getVars().size());
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

				}
			}
		};
		observedLearner.addPropertyChangeListener(queryListener);
		/*
		 * prepare solver
		 *
		 */

		// ACQ_Heuristic heuristic = expe.getHeuristic();
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setLimit(expe.getTimeout());

		TimeUnit unit = TimeUnit.S;

		// observe solver for time measurement
		final TimeManager timeManager = new TimeManager("CP");
		timeManager.setUnit(unit);
		Chrono chrono = new Chrono(expe.getClass().getName(), true);
		solver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					timeManager.add(secondToUnit((Float) evt.getNewValue(), unit));
				} else if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
				}
			}
		});

		final SATSolver satSolver = expe.createSATSolver();
		satSolver.setLimit(expe.getTimeout());

		// observe sat solver time measurement
		final TimeManager satTimeManager = new TimeManager("SAT");
		satTimeManager.setUnit(unit);
		satSolver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));

					if (evt.getPropertyName().startsWith("END_TIMECOUNT")) {
						satTimeManager.add(chrono.getLast(evt.getPropertyName().substring(4), unit));
					}
				}
			}
		});

		/*
		 * Instantiate Strategies
		 */
		ArrayList<ACQ_Network> strat = expe.createStrategy(bias);

		/*
		 * Instantiate Acquisition algorithm
		 */
		ACQ_CONACQv2 acquisition = new ACQ_CONACQv2(observedLearner, bias, satSolver, solver);
		if (expe.isVerbose())
			System.out.println(String.format("Bias size : %d", acquisition.getBias().getSize()));

		/*
		 * Instantiate Background knowledge
		 */
		ContradictionSet backknow = expe.createBackgroundKnowledge(bias, acquisition.mapping);
		if (expe.isVerbose() && backknow != null)
			System.out.println(String.format("BN size : %d", backknow.getSize()));
		acquisition.setBackgroundKnowledge(backknow);

		// Param
		acquisition.setVerbose(expe.isVerbose());
		acquisition.setLog_queries(expe.isLog_queries());
		acquisition.setStrat(strat);
		acquisition.setMaxRand(expe.getMaxRand());

		/*
		 * Run
		 */
		chrono.start("total");
		boolean result;
		try {
			result = acquisition.process(chrono, expe.getMaxQueries());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		chrono.stop("total");

		/*
		 * Print results
		 */

		if (expe.getJson()) {
			JSONObject obj = new JSONObject();
			obj.put("time_unit", unit.toString());
			obj.put("nb_queries", statManager.getNbCompleteQuery());
			obj.put("constrSolvTime", chrono.getResult("solveQ", unit) + chrono.getResult("solve_network", unit));
			obj.put("constrSolvNbCall", chrono.nbInstances("solveQ") + chrono.nbInstances("solve_network"));
			obj.put("satSolvTime", chrono.getResult("satsolve", unit));
			obj.put("satSolvNbCall", chrono.nbInstances("satsolve"));
			obj.put("convTime", chrono.getResult("total", unit));
			obj.put("network", acquisition.getLearnedNetwork().toString());
			obj.put("unitPropTime", chrono.getResult("unit_propagate", unit));
			obj.put("unitPropNbCall", chrono.nbInstances("unit_propagate"));
			obj.put("quickxTime", chrono.getResult("quick_explain", unit));
			obj.put("quickxNbCall", chrono.nbInstances("quick_explain"));
			obj.put("buildFormulaTime", chrono.getResult("build_formula", unit));
			obj.put("buildFormulaNbCall", chrono.nbInstances("build_formula"));
			obj.put("toNetworkTime", chrono.getResult("to_network", unit));
			obj.put("toNetworkNbCall", chrono.nbInstances("to_network"));
			obj.put("rate_bias_removed", acquisition.getPreprocessDiminution());
			System.out.println(obj.toString());
		} else {
			System.out.println((expe.isVerbose() ? "\n" : "") + statManager + "\n" + timeManager.getResults() + "\n"
					+ satTimeManager.getResults());
			DecimalFormat df = new DecimalFormat("0.00E0");
			double totalTime = (double) chrono.getResult("total", unit);
			double total_acq_time = (double) chrono.getLast("total_acq_time", unit);
			double mean_query_time = (double) chrono.getMean("gen_query", unit);

			System.out.println("------Execution times------");
			for (String serieName : chrono.getSerieNames()) {
				if (!serieName.contains("total") && !serieName.contains("TIMECOUNT")
						&& !serieName.contains("gen_query")) {
					double serieTime = (double) chrono.getResult(serieName, unit);
					System.out.println(serieName + " : " + df.format(serieTime) + unit + " (#call: "
							+ chrono.nbInstances(serieName) + ")");
				}
			}
			System.out.println("Preprocess removed " + acquisition.getPreprocessDiminution() + "% of the bias");
			System.out.println("Mean query generation time: " + df.format(mean_query_time) + unit);
			System.out.println("Convergence time : " + df.format(totalTime) + unit);
			System.out.println("Acquisition time : " + df.format(total_acq_time) + unit);
			System.out.println("\n*************Learned Network CL example ******");
			System.out.println(acquisition.getLearnedNetwork().toString());
			ACQ_Query q = solver.solveA(acquisition.getLearnedNetwork());
			System.out.println("query :: " + Arrays.toString(q.values));
			System.out.println("Classification :: " + learner.ask(q));
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now));
			int convergenceRate = 0;
			double RelativeAcquisitionRate = 0;
			double AbsoluteAcquisitionRate = 0;
			ACQ_Network target_network = expe.createTargetNetwork();
			AcquisitionRate ar = new AcquisitionRate(acquisition.getLearnedNetwork(), target_network,
					solver.getDomain().getMin(0), solver.getDomain().getMax(0));
			try {
				convergenceRate = expe.convergenceRate(solver, target_network, acquisition.getLearnedNetwork());
				RelativeAcquisitionRate = ar.relativeAcquisitionRate;
				AbsoluteAcquisitionRate = ar.absoluteAcquisitionRate;

				System.out.println("convergenceRate: " + convergenceRate);
				System.out.println("RelativeAcquisitionRate: " + RelativeAcquisitionRate);
				System.out.println("AbsoluteAcquisitionRate: " + AbsoluteAcquisitionRate);

			} catch (Exception e) {
				System.out.print("Target network not implemented");
			}
			String input = dtf.format(now) + "\t" + 1 + "\t" + acquisition.getLearnedNetwork().size() + "\t"
					+ RelativeAcquisitionRate + "\t" + AbsoluteAcquisitionRate + "\t" + convergenceRate + "\t"
					+ +(statManager.getNbCompleteQuery() + statManager.getNbPartialQuery()) + "\t"
					+ ((statManager.getNbCompleteQuery() + statManager.getNbPartialQuery())
							/ acquisition.getLearnedNetwork().size())
					+ "\t" + statManager.getNbCompleteQuery() + "\t" + statManager.getQuerySize() + "\t"
					+ df.format(total_acq_time) + "\t" + df.format(totalTime) + "\t" + df.format(timeManager.getMax())
					+ "\t" + 0 + statManager.getNon_asked_query() + "\t" + 0 + "\t"
					+ acquisition.getBias().getInitial_size() + "\t" + acquisition.getBias().getSize() + "\t"
					+ expe.getVrs() + "\t" + expe.getHeuristic();

			// FileManager.printFile(bias,"bias");

			// System.out.println("query :: " + Arrays.toString(q.values));
			// System.out.println("Classification :: " + learner.ask(q));

			// System.out.println("Network :: " + learned_network);

			FileManager.printResults(acquisition.getLearnedNetwork(),
					expe.getName() + "_" + dtf.format(now) + ".LearnedNetwork");

			FileManager.printResults(input, expe.getName() + "_" + instance + "_" + dtf.format(now) + ".results");

			if (result)
				System.out.println("YES...Converged");
			else
				System.out.println("NO...Collapsed");

		}
		return stats;
	}

	public static Collective_Stats executeConacqV1Experience(IExperience expe) {
		/*
		 * prepare bias
		 */
		int id = 0;
		ACQ_Bias bias = expe.createBias();

		/*
		 * prepare learner
		 */
		ACQ_Learner learner = expe.createLearner();
		ObservedLearner observedLearner = new ObservedLearner(learner);

		// observe learner for query stats
		final StatManager statManager = new StatManager(bias.getVars().size());
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

				}
			}
		};
		observedLearner.addPropertyChangeListener(queryListener);
		/*
		 * prepare solver
		 *
		 */

		// ACQ_Heuristic heuristic = expe.getHeuristic();
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setLimit(expe.getTimeout());

		TimeUnit unit = TimeUnit.S;

		// observe solver for time measurement
		final TimeManager timeManager = new TimeManager("CP");
		timeManager.setUnit(unit);
		Chrono chrono = new Chrono(expe.getClass().getName(), true);
		solver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					timeManager.add(secondToUnit((Float) evt.getNewValue(), unit));
				} else if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
				}
			}
		});

		final SATSolver satSolver = expe.createSATSolver();
		satSolver.setLimit(expe.getTimeout());

		// observe sat solver time measurement
		final TimeManager satTimeManager = new TimeManager("SAT");
		satTimeManager.setUnit(unit);
		satSolver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));

					if (evt.getPropertyName().startsWith("END_TIMECOUNT")) {
						satTimeManager.add(chrono.getLast(evt.getPropertyName().substring(4), unit));
					}
				}
			}
		});

		/*
		 * Instantiate Strategies
		 */
		ArrayList<ACQ_Network> strat = expe.createStrategy(bias);

		/*
		 * Instantiate Acquisition algorithm
		 */
		ACQ_CONACQv1 acquisition = new ACQ_CONACQv1(observedLearner, bias, satSolver, solver, expe.getDataFile());
		if (expe.isVerbose())
			System.out.println(String.format("Bias size : %d", acquisition.getBias().getSize()));
		/*
		 * Instantiate Background knowledge
		 */
		ContradictionSet backknow = expe.createBackgroundKnowledge(bias, acquisition.mapping);
		if (expe.isVerbose() && backknow != null)
			System.out.println(String.format("BN size : %d", backknow.getSize()));
		acquisition.setBackgroundKnowledge(backknow);

		// Param
		acquisition.setVerbose(expe.isVerbose());
		acquisition.setLog_queries(expe.isLog_queries());
		acquisition.setStrat(strat);
		acquisition.setMaxRand(expe.getMaxRand());

		/*
		 * Run
		 */
		chrono.start("total");
		boolean result;
		try {
			result = acquisition.process(chrono, expe.getMaxQueries());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		chrono.stop("total");

		/*
		 * Print results
		 */

		if (expe.getJson()) {
			JSONObject obj = new JSONObject();
			obj.put("time_unit", unit.toString());
			obj.put("nb_queries", statManager.getNbCompleteQuery());
			obj.put("constrSolvTime", chrono.getResult("solveQ", unit) + chrono.getResult("solve_network", unit));
			obj.put("constrSolvNbCall", chrono.nbInstances("solveQ") + chrono.nbInstances("solve_network"));
			obj.put("satSolvTime", chrono.getResult("satsolve", unit));
			obj.put("satSolvNbCall", chrono.nbInstances("satsolve"));
			obj.put("convTime", chrono.getResult("total", unit));
			obj.put("network", acquisition.getLearnedNetwork().toString());
			obj.put("unitPropTime", chrono.getResult("unit_propagate", unit));
			obj.put("unitPropNbCall", chrono.nbInstances("unit_propagate"));
			obj.put("quickxTime", chrono.getResult("quick_explain", unit));
			obj.put("quickxNbCall", chrono.nbInstances("quick_explain"));
			obj.put("buildFormulaTime", chrono.getResult("build_formula", unit));
			obj.put("buildFormulaNbCall", chrono.nbInstances("build_formula"));
			obj.put("toNetworkTime", chrono.getResult("to_network", unit));
			obj.put("toNetworkNbCall", chrono.nbInstances("to_network"));
			obj.put("rate_bias_removed", acquisition.getPreprocessDiminution());
			System.out.println(obj.toString());
		} else {
			System.out.println((expe.isVerbose() ? "\n" : "") + statManager + "\n" + timeManager.getResults() + "\n"
					+ satTimeManager.getResults());
			DecimalFormat df = new DecimalFormat("0.00E0");
			double totalTime = (double) chrono.getResult("total", unit);
			double total_acq_time = (double) chrono.getLast("total_acq_time", unit);
			double mean_query_time = (double) chrono.getMean("gen_query", unit);

			System.out.println("------Execution times------");
			for (String serieName : chrono.getSerieNames()) {
				if (!serieName.contains("total") && !serieName.contains("TIMECOUNT")
						&& !serieName.contains("gen_query")) {
					double serieTime = (double) chrono.getResult(serieName, unit);
					System.out.println(serieName + " : " + df.format(serieTime) + unit + " (#call: "
							+ chrono.nbInstances(serieName) + ")");
				}
			}
			System.out.println("Preprocess removed " + acquisition.getPreprocessDiminution() + "% of the bias");
			System.out.println("Mean query generation time: " + df.format(mean_query_time) + unit);
			System.out.println("Convergence time : " + df.format(totalTime) + unit);
			System.out.println("Acquisition time : " + df.format(total_acq_time) + unit);
			if(acquisition.getLearnedNetwork()!=null) {
			System.out.println("\n*************Learned Network CL example ******");
			System.out.println(acquisition.getLearnedNetwork().toString());
			ACQ_Query q = solver.solveA(acquisition.getLearnedNetwork());
			System.out.println("query :: " + Arrays.toString(q.values));
			System.out.println("Classification :: " + learner.ask(q));
			}
			if (result)
				System.out.println("YES...Converged");
			else
				System.out.println("NO...Collapsed");
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_dss");
			LocalDateTime now = LocalDateTime.now();
			System.out.println(dtf.format(now));
			int convergenceRate = 0;
			double RelativeAcquisitionRate = 0;
			double AbsoluteAcquisitionRate = 0;
			ACQ_Network target_network = expe.createTargetNetwork();
			if(acquisition.getLearnedNetwork()!=null) {

			AcquisitionRate ar = new AcquisitionRate(acquisition.getLearnedNetwork(), target_network,
					solver.getDomain().getMin(0), solver.getDomain().getMax(0));
			try {
				convergenceRate = expe.convergenceRate(solver, target_network, acquisition.getLearnedNetwork());
				RelativeAcquisitionRate = ar.relativeAcquisitionRate;
				AbsoluteAcquisitionRate = ar.absoluteAcquisitionRate;

				System.out.println("convergenceRate: " + convergenceRate);
				System.out.println("RelativeAcquisitionRate: " + RelativeAcquisitionRate);
				System.out.println("AbsoluteAcquisitionRate: " + AbsoluteAcquisitionRate);

			} catch (Exception e) {
				System.out.print("Target network not implemented");
			}
			}
			if(acquisition.getLearnedNetwork()!=null) {

			FileManager.printResults(acquisition.getLearnedNetwork(),
					expe.getName() + "_" + dtf.format(now) + ".LearnedNetwork");
			String input = dtf.format(now) + "\t" + 1 + "\t" + acquisition.getLearnedNetwork().size() + "\t"
					+ RelativeAcquisitionRate + "\t" + AbsoluteAcquisitionRate + "\t" + convergenceRate + "\t"
					+ +(statManager.getNbCompleteQuery() + statManager.getNbPartialQuery()) + "\t"
					+ ((statManager.getNbCompleteQuery() + statManager.getNbPartialQuery())
							/ acquisition.getLearnedNetwork().size())
					+ "\t" + statManager.getNbCompleteQuery() + "\t" + statManager.getQuerySize() + "\t"
					+ df.format(total_acq_time) + "\t" + df.format(totalTime) + "\t" + df.format(timeManager.getMax())
					+ "\t" + 0 + statManager.getNon_asked_query() + "\t" + 0 + "\t"
					+ acquisition.getBias().getInitial_size() + "\t" + acquisition.getBias().getSize() + "\t"
					+ expe.getVrs() + "\t" + expe.getHeuristic();
			
			// FileManager.printFile(bias,"bias");

			// System.out.println("query :: " + Arrays.toString(q.values));
			// System.out.println("Classification :: " + learner.ask(q));

			// System.out.println("Network :: " + learned_network);

			FileManager.printResults(acquisition.getLearnedNetwork(),
					expe.getName() + "_" + dtf.format(now) + ".LearnedNetwork");

			FileManager.printResults(input, expe.getName() + "_" + instance + "_" + dtf.format(now) + ".results");
			}
		}
		return stats;
	}

	public static Collective_Stats executeHybridModeExperience(IExperience expe) {
		/*
		 * prepare bias
		 */
		int id = 0;
		ACQ_Bias bias = expe.createBias();

		/*
		 * prepare learner
		 */
		ACQ_Learner learner = expe.createLearner();
		ObservedLearner observedLearner = new ObservedLearner(learner);

		// observe learner for query stats
		final StatManager statManager = new StatManager(bias.getVars().size());
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

				}
			}
		};
		observedLearner.addPropertyChangeListener(queryListener);
		/*
		 * prepare solver
		 *
		 */

		// ACQ_Heuristic heuristic = expe.getHeuristic();
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setLimit(expe.getTimeout());

		TimeUnit unit = TimeUnit.S;

		// observe solver for time measurement
		final TimeManager timeManager = new TimeManager("CP");
		timeManager.setUnit(unit);
		Chrono chrono = new Chrono(expe.getClass().getName(), true);
		solver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					timeManager.add(secondToUnit((Float) evt.getNewValue(), unit));
				} else if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));
				}
			}
		});

		final SATSolver satSolver = expe.createSATSolver();
		System.out.print(expe.getTimeout());
		satSolver.setLimit(expe.getTimeout());

		// observe sat solver time measurement
		final TimeManager satTimeManager = new TimeManager("SAT");
		satTimeManager.setUnit(unit);
		satSolver.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("BEG")) {
					chrono.start(evt.getPropertyName().substring(4));
				} else if (evt.getPropertyName().startsWith("END")) {
					chrono.stop(evt.getPropertyName().substring(4));

					if (evt.getPropertyName().startsWith("END_TIMECOUNT")) {
						satTimeManager.add(chrono.getLast(evt.getPropertyName().substring(4), unit));
					}
				}
			}
		});

		/*
		 * Instantiate Strategies
		 */
		ArrayList<ACQ_Network> strat = expe.createStrategy(bias);

		/*
		 * Instantiate Acquisition algorithm
		 */
		ACQ_CONACQv1 acquisition = new ACQ_CONACQv1(observedLearner, bias, satSolver, solver, expe.getDataFile());
		if (expe.isVerbose())
			System.out.println(String.format("Bias size : %d", acquisition.getBias().getSize()));
		/*
		 * Instantiate Background knowledge
		 */
		ContradictionSet backknow = expe.createBackgroundKnowledge(bias, acquisition.mapping);
		if (expe.isVerbose() && backknow != null)
			System.out.println(String.format("BN size : %d", backknow.getSize()));
		acquisition.setBackgroundKnowledge(backknow);

		// Param
		acquisition.setVerbose(expe.isVerbose());
		acquisition.setLog_queries(expe.isLog_queries());
		acquisition.setStrat(strat);
		acquisition.setMaxRand(expe.getMaxRand());

		/*
		 * Run
		 */
		chrono.start("total");
		boolean result;
		try {
			result = acquisition.process(chrono, expe.getMaxQueries());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		chrono.stop("total");

		/*
		 * Print results
		 */

		if (expe.getJson()) {
			JSONObject obj = new JSONObject();
			obj.put("time_unit", unit.toString());
			obj.put("nb_queries", statManager.getNbCompleteQuery());
			obj.put("constrSolvTime", chrono.getResult("solveQ", unit) + chrono.getResult("solve_network", unit));
			obj.put("constrSolvNbCall", chrono.nbInstances("solveQ") + chrono.nbInstances("solve_network"));
			obj.put("satSolvTime", chrono.getResult("satsolve", unit));
			obj.put("satSolvNbCall", chrono.nbInstances("satsolve"));
			obj.put("convTime", chrono.getResult("total", unit));
			obj.put("network", acquisition.getLearnedNetwork().toString());
			obj.put("unitPropTime", chrono.getResult("unit_propagate", unit));
			obj.put("unitPropNbCall", chrono.nbInstances("unit_propagate"));
			obj.put("quickxTime", chrono.getResult("quick_explain", unit));
			obj.put("quickxNbCall", chrono.nbInstances("quick_explain"));
			obj.put("buildFormulaTime", chrono.getResult("build_formula", unit));
			obj.put("buildFormulaNbCall", chrono.nbInstances("build_formula"));
			obj.put("toNetworkTime", chrono.getResult("to_network", unit));
			obj.put("toNetworkNbCall", chrono.nbInstances("to_network"));
			obj.put("rate_bias_removed", acquisition.getPreprocessDiminution());
			System.out.println(obj.toString());
		}

		System.out.println("Finished Passive Learning");

		System.out.println("Bias Size: " + bias.getSize());

		/*
		 * prepare solver
		 *
		 */

		ACQ_Heuristic heuristic = expe.getHeuristic();
		final ACQ_ConstraintSolver solver_ = expe.createSolver();
		solver.setVars(bias.getVars());
		solver.setLimit(expe.getTimeout());
		// observe solver for time measurement
		final TimeManager timeManager_ = new TimeManager();
		Chrono chrono_ = new Chrono(expe.getClass().getName());
		solver.addPropertyChangeListener(new PropertyChangeListener() {
			private boolean printTimeInLogFile = true;

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().startsWith("TIMECOUNT")) {
					float time = (Float) evt.getNewValue();
					timeManager.add((Float) time);
					if (printTimeInLogFile)
						FileManager.printFile(time, "chrono");
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
		ACQ_QUACQ acquisition_ = new ACQ_QUACQ(solver, bias, observedLearner, heuristic);
		// Param
		acquisition_.setNormalizedCSP(expe.isNormalizedCSP());
		acquisition_.setShuffleSplit(expe.isShuffleSplit());
		acquisition_.setAllDiffDetection(expe.isAllDiffDetection());
		acquisition_.setVerbose(expe.isVerbose());
		acquisition_.setLog_queries(expe.isLog_queries());
		acquisition_.getLearnedNetwork().addAll(acquisition.getLearnedNetwork(), true);

		/*
		 * Run
		 */
		chrono.start("total");
		boolean result_ = acquisition_.process(chrono);
		chrono.stop("total");
		stats.saveChronos(id, chrono);
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		stats.saveBias(id, acquisition.getBias());
		stats.saveLearnedNetwork(id, acquisition.getLearnedNetwork());
		stats.saveResults(id, result);
		stats.ComputeGlobalStats(0, observedLearner, bias, (DefaultExperienceConacq) expe, learner.memory.size());

		/*
		 * Print results
		 */

		System.out.println("Learned Network Size: " + acquisition_.getLearnedNetwork().size());
		System.out.println("Initial Bias size: " + acquisition_.getBias().getInitial_size());
		System.out.println("Final Bias size: " + acquisition_.getBias().getSize());

		System.out.println(statManager + "\n" + timeManager.getResults());
		DecimalFormat df = new DecimalFormat("0.00E0");
		double totalTime = (double) chrono.getResult("total") / 1000.0;
		double total_acq_time = (double) chrono.getLast("total_acq_time") / 1000.0;

		System.out.println("------Execution times------");
		for (String serieName : chrono.getSerieNames()) {
			if (!serieName.contains("total")) {
				double serieTime = (double) chrono.getResult(serieName) / 1000.0;
				System.out.println(serieName + " : " + df.format(serieTime));
			}
		}
		System.out.println("Convergence time : " + df.format(totalTime));
		System.out.println("Acquisition time : " + df.format(total_acq_time));
		System.out.println("*************Learned Network CL example ******");
		ACQ_Query q = solver.solveA(acquisition.getLearnedNetwork());

		q.classify(learner.ask(q));
		FileManager.printResults(acquisition_.learned_network, expe.getName() + ".LearnedNetwork");

		FileManager.printFile(bias, "bias");

		// System.out.println("query :: " + Arrays.toString(q.values));
		// System.out.println("Classification :: " + learner.ask(q));

		// System.out.println("Network :: " + learned_network);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();

		String input = dateFormat.format(date) + "\t" + 1 + "\t" + acquisition.getLearnedNetwork().size() + "\t"
				+ (statManager.getNbCompleteQuery() + statManager.getNbPartialQuery()) + "\t"
				+ ((statManager.getNbCompleteQuery() + statManager.getNbPartialQuery())
						/ acquisition.getLearnedNetwork().size())
				+ "\t" + statManager.getNbCompleteQuery() + "\t" + statManager.getQuerySize() + "\t"
				+ df.format(total_acq_time) + "\t" + df.format(totalTime) + "\t" + df.format(timeManager.getMax())
				+ "\t" + 0 + statManager.getNon_asked_query() + "\t" + 0 + "\t"
				+ acquisition.getBias().getInitial_size() + "\t" + acquisition.getBias().getSize() + "\t"
				+ expe.getVrs() + "\t" + expe.getHeuristic();

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));
		FileManager.printResults(input, expe.getName() + instance + "_" + dtf.format(now) + ".results");

		return stats;
	}

	private static Float secondToUnit(Float f, TimeUnit unit) {
		switch (unit) {
		case S:
			return f;
		case MS:
			return f * 1000;
		case NS:
			return f * 1000000000;

		default:
			assert false : "unknow time unit";
			return null;
		}
	}

}
