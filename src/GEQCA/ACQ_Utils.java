package GEQCA;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ObservedLearner;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.tools.Collective_Stats;
import fr.lirmm.coconut.acquisition.core.tools.StatManager;
import fr.lirmm.coconut.acquisition.core.tools.TimeManager;

public class ACQ_Utils {
	public static Collective_Stats stats = new Collective_Stats();
	public static int instance_ = 0;
	private static boolean printLearnedNetworkInLogFile = false;
	private static boolean printBiasInLogFile = false;

	
	public static Collective_Stats executeTACQExperience(IExperience expe) {

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

		ACQ_SelectionHeuristics heuristic = expe.getSelectionHeuristic();
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
		ACQ_Network target_network = expe.createTargetNetwork();
		HashMap<String,String> target = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> L = new HashMap<String, ArrayList<String>>();
		int i=0;
			CombinationIterator iterator1 = new CombinationIterator(bias.getVars().size()/2, 2);
			while (iterator1.hasNext()) {
				ArrayList<String> l= new ArrayList<String>();
				for(String s : bias.getLanguage())
					l.add(s);
				int[] vars = new int[2];
				vars = iterator1.next();
				if(vars[0]<vars[1]) {
					

					L.put(vars[0]+","+vars[1], l);
					
			}
				}
			
		
		 i=0;
		for (ACQ_IConstraint cst : target_network.getConstraints()) {
			String scope;
			if(cst.getVariables()[0]/2>cst.getVariables()[2]/2)
			scope = cst.getVariables()[2]/2+","+cst.getVariables()[0]/2;
			else
				scope = cst.getVariables()[0]/2+","+cst.getVariables()[2]/2;

			target.put(scope, cst.getName());
			i++;
		}
		
		LearningQCN acquisition = new LearningQCN(solver, L, target,learner,bias.getVars().size()/2, heuristic,expe.getAlgerbraType());
		// Param
		//System.out.print(solver.solveA(learner.buildTargetNetwork()));

		/*
		 * Run
		 */
		chrono.start("total");
		acquisition.process(chrono);
		chrono.stop("total");
		System.out.println("Total Queries: " + (acquisition.nPositives + acquisition.nNegatives));
		System.out.println("Positive Queries: " + acquisition.nPositives);
		System.out.println("Negative Queries: " + acquisition.nNegatives);
		stats.saveChronos(id, chrono);
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		stats.saveLearnedNetwork(id, acquisition.getLearnedNetwork());
		
		/*
		 * Print results
		 */
		System.out.println("Learned Network Size: " + acquisition.getLearnedNetwork().size());

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
	
		

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();

		double tmax = acquisition.times.size()==0 ? 0 : Collections.max(acquisition.times);
		for(long t : acquisition.times)
			totalTime+=t;
		totalTime=totalTime/1000;
		System.out.println("Tmax time : " + tmax);

		String input =dateFormat.format(date)+"\t"+expe.getName()+"\t"+((acquisition.nPositives + acquisition.nNegatives))+"\t"+acquisition.nPositives+"\t"+acquisition.nNegatives+"\t"+(tmax/1000)+"\t"+(totalTime/(acquisition.nPositives + acquisition.nNegatives))+"\t"+(totalTime);
		


		FileManager.printFile(input, "Tasks"+bias.getVars().size()/2+"_"+heuristic);

		return stats;

	}

	public static Collective_Stats executeLQCNExperience(IExperience expe) {

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

		ACQ_SelectionHeuristics heuristic = expe.getSelectionHeuristic();
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
		ACQ_Network target_network = expe.createTargetNetwork();
		HashMap<String,String> target = new HashMap<String, String>();
		HashMap<String,ArrayList<String>> L = new HashMap<String,ArrayList<String>>();
		
		int i=0;
			CombinationIterator iterator1 = new CombinationIterator(bias.getVars().size()/2, 2);
			while (iterator1.hasNext()) {
				ArrayList<String> l= new ArrayList<String>();
				for(String s : bias.getLanguage())
					l.add(s);
				int[] vars = new int[2];
				vars = iterator1.next();
				if(vars[0]<vars[1]) {
					

					L.put(vars[0]+","+vars[1], l);
					
			}
				}
			
		
		 i=0;
		for (ACQ_IConstraint cst : target_network.getConstraints()) {
			String scope;
			if(cst.getVariables()[0]/2>cst.getVariables()[2]/2)
			scope = cst.getVariables()[2]/2+","+cst.getVariables()[0]/2;
			else
				scope = cst.getVariables()[0]/2+","+cst.getVariables()[2]/2;

			target.put(scope, cst.getName());
			i++;
		}
		LearningQCN acquisition = new LearningQCN(solver, L, target,learner,bias.getVars().size()/2, heuristic,expe.getAlgerbraType());
		// Param
		acquisition.setExperience(expe);
		
		/*
		 * Run
		 */
		chrono.start("total");
		acquisition.process_v1(chrono);
		chrono.stop("totalMeetsXY\n" + 
				"");
		System.out.println("Total Queries: " + (acquisition.nPositives + acquisition.nNegatives));
		System.out.println("Positive Queries: " + acquisition.nPositives);
		System.out.println("Negative Queries: " + acquisition.nNegatives);
		stats.saveChronos(id, chrono);
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		//stats.saveBias(id, acquisition.getBias());
		stats.saveLearnedNetwork(id, acquisition.getLearnedNetwork());
		
		/*
		 * Print results
		 */
		int ln =0;
		for(ArrayList<String> l : L.values())
			if(!l.isEmpty())
				ln+=1;
		System.out.println("Learned Network Size: " + ln);

		System.out.println("Learned Network : " + acquisition.L);
		//System.out.println("ACQRate: " +expe.ACQRate(target_network, acquisition.getLearnedNetwork()));

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
	
		double tmax = acquisition.times.size()==0 ? 0 : Collections.max(acquisition.times);
		for(long t : acquisition.times)
			totalTime+=t;
		totalTime=totalTime/1000;

		System.out.println("Tmax time : " + tmax/1000);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();
		
		String input =dateFormat.format(date)+"\t"+expe.getName()+"\t"+((acquisition.nPositives + acquisition.nNegatives))+"\t"+acquisition.nPositives+"\t"+acquisition.nNegatives+"\t"+(tmax/1000)+"\t"+(totalTime/(acquisition.nPositives + acquisition.nNegatives))+"\t"+(totalTime);
		


		FileManager.printFile(input, "Tasks"+bias.getVars().size()/2+"_"+heuristic);

		return stats;


	}
	public static Collective_Stats executeGEQCAExperience(IExperience expe) {

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

		ACQ_SelectionHeuristics heuristic = expe.getSelectionHeuristic();
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
		ACQ_Network target_network = expe.createTargetNetwork();
		HashMap<String,String> target = new HashMap<String, String>();
		HashMap<String,ArrayList<String>> L = new HashMap<String,ArrayList<String>>();
		
		int i=0;
			CombinationIterator iterator1 = new CombinationIterator(bias.getVars().size()/2, 2);
			while (iterator1.hasNext()) {
				ArrayList<String> l= new ArrayList<String>();
				for(String s : bias.getLanguage())
					l.add(s);
				int[] vars = new int[2];
				vars = iterator1.next();
				if(vars[0]<vars[1]) {
					

					L.put(vars[0]+","+vars[1], l);
					
			}
				}
			
		
		 i=0;
		for (ACQ_IConstraint cst : target_network.getConstraints()) {
			String scope;
			if(cst.getVariables()[0]/2>cst.getVariables()[2]/2)
			scope = cst.getVariables()[2]/2+","+cst.getVariables()[0]/2;
			else
				scope = cst.getVariables()[0]/2+","+cst.getVariables()[2]/2;

			target.put(scope, cst.getName());
			i++;
		}
		GEQCA acquisition = new GEQCA(solver, L, target,learner,bias.getVars().size()/2, heuristic,expe.getAlgerbraType());
		// Param

		acquisition.setExperience(expe);
		acquisition.setPropagation(expe.getPropagation());
		acquisition.instance=expe.getName();
		acquisition.setDeadline(expe.getDeadline());
		try {
			acquisition.parseSchedulingInstance();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * Run
		 */
		chrono.start("total");
		acquisition.process_v1(chrono);
		chrono.stop("totalMeetsXY\n" + 
				"");
		System.out.println("Total Queries: " + (acquisition.nPositives + acquisition.nNegatives));
		System.out.println("Positive Queries: " + acquisition.nPositives);
		System.out.println("Negative Queries: " + acquisition.nNegatives);
		stats.saveChronos(id, chrono);
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		//stats.saveBias(id, acquisition.getBias());
		stats.saveLearnedNetwork(id, acquisition.getLearnedNetwork());
		
		/*
		 * Print results
		 */
		int ln =0;
		for(ArrayList<String> l : L.values())
			if(!l.isEmpty())
				ln+=1;
		System.out.println("Learned Network Size: " + ln);

		System.out.println("Learned Network : " + acquisition.L);
		//System.out.println("ACQRate: " +expe.ACQRate(target_network, acquisition.getLearnedNetwork()));

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
	
		double tmax = acquisition.times.size()==0 ? 0 : Collections.max(acquisition.times);
		for(long t : acquisition.times)
			totalTime+=t;
		totalTime=totalTime/1000;

		System.out.println("Tmax time : " + tmax/1000);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();
		
		String input =dateFormat.format(date)+"\t"+expe.getName()+"\t"+((acquisition.nPositives + acquisition.nNegatives))+"\t"+acquisition.nPositives+"\t"+acquisition.nNegatives+"\t"+(tmax/1000)+"\t"+(totalTime/(acquisition.nPositives + acquisition.nNegatives))+"\t"+(totalTime);
		for(String vars : acquisition.L.keySet())
			FileManager.printFile(vars+"::"+acquisition.L.get(vars), "cl"+bias.getVars().size()/2+"_"+heuristic);



		FileManager.printFile(input, "Tasks"+bias.getVars().size()/2+"_"+heuristic);

		return stats;


	}

	
	
	
	
	public static Collective_Stats executeGEQCAIQExperience(IExperience expe) {

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

		ACQ_SelectionHeuristics heuristic = expe.getSelectionHeuristic();
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
		ACQ_Network target_network = expe.createTargetNetwork();
		HashMap<String,String> target = new HashMap<String, String>();
		HashMap<String,ArrayList<String>> L = new HashMap<String,ArrayList<String>>();
		
		int i=0;
			CombinationIterator iterator1 = new CombinationIterator(bias.getVars().size()/2, 2);
			while (iterator1.hasNext()) {
				ArrayList<String> l= new ArrayList<String>();
				for(String s : bias.getLanguage())
					l.add(s);
				int[] vars = new int[2];
				vars = iterator1.next();
				if(vars[0]<vars[1]) {
					

					L.put(vars[0]+","+vars[1], l);
					
			}
				}
			
		
		 i=0;
		for (ACQ_IConstraint cst : target_network.getConstraints()) {
			String scope;
			if(cst.getVariables()[0]/2>cst.getVariables()[2]/2)
			scope = cst.getVariables()[2]/2+","+cst.getVariables()[0]/2;
			else
				scope = cst.getVariables()[0]/2+","+cst.getVariables()[2]/2;

			target.put(scope, cst.getName());
			i++;
		}
		GEQCA_IQ acquisition = new GEQCA_IQ(solver, L, target,learner,bias.getVars().size()/2, heuristic,expe.getAlgerbraType());
		// Param

		acquisition.setExperience(expe);
		/*try {
			acquisition.parseSchedulingInstance();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 * Run
		 */
		chrono.start("total");
		acquisition.process_v1(chrono);
		chrono.stop("totalMeetsXY\n" + 
				"");
		System.out.println("Total Qualitative Queries: " + (acquisition.nPositives + acquisition.nNegatives));

		System.out.println("Positive Queries: " + acquisition.nPositives);
		System.out.println("Negative Queries: " + acquisition.nNegatives);
		
		System.out.println("Total Independence Queries: " + (acquisition.iqPositives + acquisition.iqNegatives));

		System.out.println("Positive Independence Queries: " + acquisition.iqPositives);
		System.out.println("Negative Independence Queries: " + acquisition.iqNegatives);
		stats.saveChronos(id, chrono);
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		//stats.saveBias(id, acquisition.getBias());
		stats.saveLearnedNetwork(id, acquisition.getLearnedNetwork());
		
		/*
		 * Print results
		 */
		int ln =0;
		for(ArrayList<String> l : L.values())
			if(!l.isEmpty())
				ln+=1;
		System.out.println("Learned Network Size: " + ln);

		System.out.println("Learned Network : " + acquisition.L);
		//System.out.println("ACQRate: " +expe.ACQRate(target_network, acquisition.getLearnedNetwork()));

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
	
		double tmax = acquisition.times.size()==0 ? 0 : Collections.max(acquisition.times);
		for(long t : acquisition.times)
			totalTime+=t;
		totalTime=totalTime/1000;

		System.out.println("Tmax time : " + tmax/1000);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();
		
		String input =dateFormat.format(date)+"\t"+expe.getName()+"\t"+((acquisition.nPositives + acquisition.nNegatives))+"\t"+acquisition.nPositives+"\t"+acquisition.nNegatives+"\t"+((acquisition.iqPositives + acquisition.iqNegatives))+"\t"+acquisition.iqPositives+"\t"+acquisition.iqNegatives+"\t"+(tmax/1000)+"\t"+(totalTime/(acquisition.nPositives + acquisition.nNegatives))+"\t"+(totalTime);
		for(String vars : acquisition.L.keySet())
			FileManager.printFile(vars+"::"+acquisition.L.get(vars), "cl"+bias.getVars().size()/2+"_"+heuristic);



		FileManager.printFile(input, "Tasks"+bias.getVars().size()/2+"_GEQCAIQ_"+heuristic);

		return stats;


	}
	
	
	public static Collective_Stats executeGEQCA_BK_IQ_Experience(IExperience expe) {

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

		ACQ_SelectionHeuristics heuristic = expe.getSelectionHeuristic();
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
		ACQ_Network target_network = expe.createTargetNetwork();
		HashMap<String,String> target = new HashMap<String, String>();
		HashMap<String,ArrayList<String>> L = new HashMap<String,ArrayList<String>>();
		
		int i=0;
			CombinationIterator iterator1 = new CombinationIterator(bias.getVars().size()/2, 2);
			while (iterator1.hasNext()) {
				ArrayList<String> l= new ArrayList<String>();
				for(String s : bias.getLanguage())
					l.add(s);
				int[] vars = new int[2];
				vars = iterator1.next();
				if(vars[0]<vars[1]) {
					

					L.put(vars[0]+","+vars[1], l);
					
			}
				}
			
		
		 i=0;
		for (ACQ_IConstraint cst : target_network.getConstraints()) {
			String scope;
			if(cst.getVariables()[0]/2>cst.getVariables()[2]/2)
			scope = cst.getVariables()[2]/2+","+cst.getVariables()[0]/2;
			else
				scope = cst.getVariables()[0]/2+","+cst.getVariables()[2]/2;

			target.put(scope, cst.getName());
			i++;
		}
		GEQCA_BK_IQ acquisition = new GEQCA_BK_IQ(solver, L, target,learner,bias.getVars().size()/2, heuristic,expe.getAlgerbraType());
		// Param

		acquisition.setExperience(expe);
		acquisition.setPropagation(expe.getPropagation());
		acquisition.instance=expe.getName();
		acquisition.setDeadline(expe.getDeadline());
		try {
			acquisition.parseSchedulingInstance();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*try {
			acquisition.parseSchedulingInstance();
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 * Run
		 */
		chrono.start("total");
		acquisition.process_v1(chrono);
		chrono.stop("totalMeetsXY\n" + 
				"");
		System.out.println("Total Qualitative Queries: " + (acquisition.nPositives + acquisition.nNegatives));

		System.out.println("Positive Queries: " + acquisition.nPositives);
		System.out.println("Negative Queries: " + acquisition.nNegatives);
		
		System.out.println("Total Independence Queries: " + (acquisition.iqPositives + acquisition.iqNegatives));

		System.out.println("Positive Independence Queries: " + acquisition.iqPositives);
		System.out.println("Negative Independence Queries: " + acquisition.iqNegatives);
		stats.saveChronos(id, chrono);
		stats.saveTimeManager(id, timeManager);
		stats.savestatManager(id, statManager);
		//stats.saveBias(id, acquisition.getBias());
		stats.saveLearnedNetwork(id, acquisition.getLearnedNetwork());
		
		/*
		 * Print results
		 */
		int ln =0;
		for(ArrayList<String> l : L.values())
			if(!l.isEmpty())
				ln+=1;
		System.out.println("Learned Network Size: " + ln);

		System.out.println("Learned Network : " + acquisition.L);
		//System.out.println("ACQRate: " +expe.ACQRate(target_network, acquisition.getLearnedNetwork()));

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
	
		double tmax = acquisition.times.size()==0 ? 0 : Collections.max(acquisition.times);
		for(long t : acquisition.times)
			totalTime+=t;
		totalTime=totalTime/1000;

		System.out.println("Tmax time : " + tmax/1000);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 2020/04/17 16:15:43
		Date date = new Date();
		
		String input =dateFormat.format(date)+"\t"+expe.getName()+"\t"+((acquisition.nPositives + acquisition.nNegatives))+"\t"+acquisition.nPositives+"\t"+acquisition.nNegatives+"\t"+((acquisition.iqPositives + acquisition.iqNegatives))+"\t"+acquisition.iqPositives+"\t"+acquisition.iqNegatives+"\t"+(tmax/1000)+"\t"+(totalTime/(acquisition.nPositives + acquisition.nNegatives))+"\t"+(totalTime);
		for(String vars : acquisition.L.keySet())
			FileManager.printFile(vars+"::"+acquisition.L.get(vars), "cl"+bias.getVars().size()/2+"_"+heuristic);



		FileManager.printFile(input, "Tasks"+bias.getVars().size()/2+"_GEQCAIQ_"+heuristic);

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


	public ACQ_IConstraint random(ACQ_Network coll) {
		int num = (int) (Math.random() * coll.size());
		for (ACQ_IConstraint t : coll)
			if (--num < 0)
				return t;
		throw new AssertionError();
	}



}
