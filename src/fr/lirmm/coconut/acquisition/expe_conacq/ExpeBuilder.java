package fr.lirmm.coconut.acquisition.expe_conacq;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Algorithm;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_Partition;
import fr.lirmm.coconut.acquisition.core.workspace.IExperience;
import fr.lirmm.coconut.acquisition.core.workspace.IExperienceConacq;

public class ExpeBuilder {
	protected static String exp;

	protected ACQ_Heuristic heuristic;
	private boolean normalizedCSP;
	private boolean shuffle;
	private boolean allDiff_detection;
	private boolean parallel;
	private static String instance;
	private static String vls;
	private static String vrs;
	private static boolean verbose;
	private static boolean log_queries;
	private static boolean gui;
	protected static int nb_threads;
	protected static ACQ_Partition partition;
	protected static long timeout;
	private static ACQ_Algorithm algo;
	private File directory;
	
	public ExpeBuilder() {

	}

	public ExpeBuilder setHeuristic(ACQ_Heuristic h) {
		this.heuristic = h;
		return this;
	}

	public ExpeBuilder setNormalizedCSP(boolean norm) {
		this.normalizedCSP = norm;
		return this;
	}

	public ExpeBuilder setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
		return this;
	}

	public ExpeBuilder setParallel(boolean parallel) {
		this.parallel = parallel;
		return this;
	}

	public ExpeBuilder setAllDiffDetection(boolean detection) {
		this.allDiff_detection = detection;
		return this;

	}

	public ExpeBuilder setVarSelector(String vrs) {
		this.vrs = vrs;
		return this;
	}

	public ExpeBuilder setValSelector(String vls) {
		this.vls = vls;
		return this;
	}

	public ExpeBuilder setInstance(String instance) {
		this.instance = instance;
		return this;
	}

	public ExpeBuilder setNbThreads(int nb_threads) {
		this.nb_threads = nb_threads;
		return this;
	}
	public ExpeBuilder setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}
	public ExpeBuilder setPartition(ACQ_Partition partition) {
		this.partition = partition;
		return this;
	}
	public ExpeBuilder setMode(ACQ_Algorithm mode) {
		this.algo = mode;
		return this;
	}
	public ExpeBuilder setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public ExpeBuilder setQueries(boolean log_queries) {
		this.log_queries = log_queries;
		return this;
	}
	public ExpeBuilder setDirectory(File directory) {
		this.directory = directory;
		return this;
	}
	public ExpeBuilder setExpe(String exp) {
		this.exp = exp;
		return this;
	}
	
	public ExpeBuilder setGui(boolean gui) {
		this.gui=gui;
		return this;
	}
	public IExperience build() throws IOException {

		switch (exp) {
		case "random":
			ExpeConacq_Random random = new  ExpeConacq_Random();

			random.setParams(normalizedCSP, shuffle, timeout, heuristic, verbose, log_queries);
			random.setVls(vls);
			random.setVrs(vrs);
			random.setInstance(instance);
			random.setPartition(partition);
			random.setNb_threads(nb_threads);
			random.setAlgo(algo);
			random.setInstance(instance);
			return random;
		case "purdey":
			ExpeConacq_Purdey prudey = new  ExpeConacq_Purdey();

			prudey.setParams(normalizedCSP, shuffle, timeout, heuristic, verbose, log_queries);
			prudey.setVls(vls);
			prudey.setVrs(vrs);
			prudey.setPartition(partition);
			prudey.setNb_threads(nb_threads);
			prudey.setAlgo(algo);
			return prudey;
		
		
		case "sudoku4":
			ExpeConacq_SUDOKU9 sudoku4 = new  ExpeConacq_SUDOKU9();

			sudoku4.setParams(normalizedCSP, shuffle, timeout, heuristic, verbose, log_queries);
			sudoku4.setVls(vls);
			sudoku4.setVrs(vrs);
			sudoku4.setPartition(partition);
			sudoku4.setNb_threads(nb_threads);
			sudoku4.setAlgo(algo);
			sudoku4.setPuzzlePrint(true);

			return sudoku4;
		
			
		case "latin":
			ExpeConacq_LatinSquare latin = new  ExpeConacq_LatinSquare();

			latin.setParams(normalizedCSP, shuffle, timeout, heuristic, verbose, log_queries);

			latin.setVls(vls);
			latin.setVrs(vrs);
			latin.setInstance(instance);
			latin.setPartition(partition);
			latin.setNb_threads(nb_threads);
			latin.setAlgo(algo);
			latin.setPuzzlePrint(true);

			return latin;
		
		case "golomb":
			ExpeConacq_GOLOMB golomb = new ExpeConacq_GOLOMB();
			golomb.setParams(normalizedCSP, shuffle, timeout, heuristic, verbose, log_queries);
			golomb.setVls(vls);
			golomb.setVrs(vrs);
			golomb.setInstance(instance);
			golomb.setPartition(partition);
			golomb.setNb_threads(nb_threads);
			golomb.setAlgo(algo);
			golomb.setInstance(instance);
			return golomb;
		default:
			ExpeParser e = new ExpeParser(exp);
			ExpeFromParser exp_ = new ExpeFromParser(e);
			System.out.print(instance);
			exp_.setParams(normalizedCSP, shuffle, timeout, heuristic, verbose, log_queries);
			exp_.setVls(vls);
			exp_.setVrs(vrs);
			exp_.setName(exp);
			exp_.setInstance(instance);
			exp_.setPartition(partition);
			exp_.setNb_threads(nb_threads);
			exp_.setAlgo(algo);
			if (puzzleProblem(exp)) {
				exp_.setPuzzlePrint(true);
			}
			return (IExperience) exp_;

		
		}
		

	}

	private boolean puzzleProblem(String exp_name) {

		String[] problems = new String[] { "sudoku", "jsudoku", "queens" };
		Pattern name = Pattern.compile(exp_name + "*");

		for (String pb : problems) {
			Matcher matcher1 = name.matcher(pb.toLowerCase());
			Pattern name2 = Pattern.compile(pb + "*");
			Matcher matcher2 = name2.matcher(name.pattern().toLowerCase());

			if (matcher1.find() || matcher2.find()) {
				return true;
			}

		}

		return false;
	}

}
