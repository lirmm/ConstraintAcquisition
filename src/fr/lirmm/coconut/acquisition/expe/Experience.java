package fr.lirmm.coconut.acquisition.expe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.tools.StatManager;
import fr.lirmm.coconut.acquisition.core.workspace.IExperience;

public abstract class Experience implements IExperience{
	
	protected boolean auto_learn;
	protected ACQ_Heuristic heuristic = ACQ_Heuristic.SOL;
	protected ACQ_ConstraintSolver solver;
	protected ACQ_Bias bias;
    protected ACQ_Learner learner;

	protected StatManager stats;
	
	public Experience(boolean auto_learn) {
		this.auto_learn = auto_learn;
		this.bias=createBias();
		this.learner=createLearner();
		this.solver= createSolver();
		this.solver.setVars(bias.getVars());
		stats = new StatManager(bias.getVars().size());		
	}
	
	public abstract ACQ_Bias createBias();
	
	public abstract void learn_Parallel();
	public abstract ACQ_Learner createLearner();
	
	public abstract ACQ_ConstraintSolver createSolver();
	
	public void setHeuristic(ACQ_Heuristic H) {
		this.heuristic=H;
	}
	
	public boolean getAnswer(ACQ_Query e) {
    	System.out.println(e.learnerAskingFormat());
		System.out.println("Is it a solution ? (y/n)");
		Scanner in = new Scanner(System.in);
		String userAnswer;
		do {
			userAnswer = in.next();
			System.out.println("You entered:" + userAnswer);
			if(!userAnswer.equals("y") && !userAnswer.equals("n")) {
				System.out.println("Incorrect answer.\n Please enter y or n.");
			}
		} while(!userAnswer.equals("y") && !userAnswer.equals("n"));
		if(userAnswer.equals("y")) {
			stats.update(e);
			return true;
		}
		else {
			stats.update(e);
			return false;
		}
    }
	
	public static boolean getAnswer(ACQ_Query e, StatManager stats) {
    	System.out.println("QUACQ");
    	System.out.println(e.learnerAskingFormat());
		System.out.println("Is it a solution ? (y/n)");
		Scanner in = new Scanner(System.in);
		String userAnswer;
		do {
			userAnswer = in.next();
			System.out.println("You entered:" + userAnswer);
			if(!userAnswer.equals("y") && !userAnswer.equals("n")) {
				System.out.println("Incorrect answer.\n Please enter y or n.");
			}
		} while(!userAnswer.equals("y") && !userAnswer.equals("n"));
		if(userAnswer.equals("y")) {
			stats.update(e);
			return true;
		}
		else {
			stats.update(e);
			return false;
		}
    }
	
	public ACQ_Learner defaultLearner(StatManager stats) {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {
				return getAnswer(e, stats);
			}
		};
	}
	public StatManager getStatManager() {
		return stats;
	}
	public List<List<ACQ_Scope>> Partition_bias(Integer Users){
		Set<ACQ_Scope> scopes= new HashSet<>();
	    List<List<ACQ_Scope>> partitions = new ArrayList<>(Users);
	    for(int i = 0; i < Users; i++)
	    	partitions.add(new ArrayList<>());
		for (ACQ_IConstraint cst : bias.getConstraints()) {
			
			scopes.add(cst.getScope());
		}
		List<ACQ_Scope> scopes1 = new ArrayList<>( scopes ) ;
		Collections.shuffle( scopes1 ) ;
		Iterator<ACQ_Scope> iterator = scopes.iterator();
		 
		 for(int i = 0; iterator.hasNext(); i++)
			 partitions.get(i % Users).add(iterator.next());



		return partitions;
		
		
	}
	@Override
	public boolean isNormalizedCSP() {
		return true;
	}

	@Override
	public ACQ_Heuristic getHeuristic() {
		return heuristic;
	}

	@Override
	public boolean isShuffleSplit() {
		return true;
	}

	@Override
	public boolean isAllDiffDetection() {
		return false;
	}
	public ACQ_Learner getLearner(){
		return learner;
	}
	public ACQ_Bias getBias(){
		return bias;
	}

}

