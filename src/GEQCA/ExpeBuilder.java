package GEQCA;

import java.io.IOException;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Mode;



public class ExpeBuilder {
	protected static String exp_name;


	private static String instance;
	private static boolean verbose;
	protected static int nb_threads;
	protected static long timeout;
	private static ACQ_Mode mode;
	protected int propagationchoice;
	protected int deadline;

	private ACQ_SelectionHeuristics sheuristic;
	public ExpeBuilder() {

	}

	
	
	public ExpeBuilder setInstance(String instance) {
		this.instance = instance;
		return this;
	}


	public ExpeBuilder setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	

	public ExpeBuilder setMode(ACQ_Mode mode) {
		this.mode = mode;
		return this;
	}

	public ExpeBuilder setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}


	public ExpeBuilder setExpe(String exp) {
		this.exp_name = exp;
		this.instance=exp;
		return this;
	}

	public ExpeBuilder setPropagation(int prop) {
		this.propagationchoice = prop;
		return this;
	}
	public IExperience build() throws IOException {

			ExpeParser e = new ExpeParser(exp_name);
			ExpeFromParser exp = new ExpeFromParser(e);
			exp.setParams(timeout, verbose);
			exp.setInstance(instance);
			exp.setMode(mode);
			exp.setPropagation(propagationchoice);
			exp.setSelectionHeuristic(sheuristic);
			exp.setDeadline(deadline);
			return exp;

		

	}

	
	public ExpeBuilder setSelectionHeuristic(ACQ_SelectionHeuristics sheuristic) {
		this.sheuristic = sheuristic;
		return this;
	}

	public ExpeBuilder setDeadline(int deadline) {
		this.deadline=deadline;
		return this;
	}

}
