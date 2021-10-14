/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.expe;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Scanner;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.BinaryArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintMapping;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ContradictionSet;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Operator;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ChocoSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_IDomain;
import fr.lirmm.coconut.acquisition.core.acqsolver.MiniSatSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.SATSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ValSelector;
import fr.lirmm.coconut.acquisition.core.acqsolver.VarSelector;
import fr.lirmm.coconut.acquisition.core.combinatorial.AllPermutationIterator;
import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Algorithm;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_Partition;
import fr.lirmm.coconut.acquisition.core.workspace.ACQ_WS;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;

/**
 * AllDiff experiment
 * @author NADJIB
 */
public class ExpeTOY extends DefaultExperience{

	boolean auto_learn;
	private int nb_vars=4;
	private static boolean gui=false;
	private static boolean parallel=true;
	public ExpeTOY(boolean auto_learn) {
		this.auto_learn=auto_learn;
	}

	ValSelector vls;
	VarSelector vrs;
	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			}

			@Override
			public int getMax(int numvar) {
				return nb_vars;
			}
		},vrs.DomOverWDeg.toString(),vls.IntDomainRandom.toString());
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {
				if(auto_learn) {
					int[] tuple = e.getTuple();  				
					for(int i=0; i<tuple.length-1; i++)
						for(int j=i+1; j<tuple.length; j++)
							if(tuple[i]==tuple[j]) {
								e.classify(false);
								return false;
							}
					e.classify(true);
					return true;
				}else 
					return getAnswer(e);

			}

		};
	}
	public boolean getAnswer(ACQ_Query e) {
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
		
		return userAnswer.equals("y") ;
    }

	public ACQ_Bias createBias() {
		int NB_VARIABLE = nb_vars;
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		// build Constraints
		ConstraintFactory constraintFactory=new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();
		// génère tous les couples de deux variables parmi NB_VARIABLE
		CombinationIterator iterator = new CombinationIterator(NB_VARIABLE, 2);
		// tant qu'il reste des couples
		while (iterator.hasNext()) {
			// assignation du couple
			int[] vars = iterator.next();
			// génère les permutations entre deux variables
			AllPermutationIterator pIterator = new AllPermutationIterator(2);
			// tant qu'il reste des permutations
			while (pIterator.hasNext()) {
				// assignation de la permutation
				int[] pos = pIterator.next();
				if(vars[pos[0]]< vars[pos[1]])		//NL: commutative relations
				{
					// création de la contrainte X != Y
					constraints.add(new BinaryArithmetic("DifferentXY", vars[pos[0]], Operator.NEQ, vars[pos[1]], "EqualXY"));
					// création de la contrainte X == Y
					constraints.add(new BinaryArithmetic("EqualXY", vars[pos[0]], Operator.EQ, vars[pos[1]], "DifferentXY"));
				}
				// X >= Y
			//	constraints.add(new BinaryArithmetic("GreaterEqualXY", vars[pos[0]], Operator.GE, vars[pos[1]]));
			}
		}
		ACQ_Network network = new ACQ_Network(constraintFactory,allVarSet, constraints);
		return new ACQ_Bias(network);
	}
	@Override
	public void process() {

		switch (algo) {
		case QUACQ:
			ACQ_WS.executeExperience(this);
			break;
		case PACQ:
			ACQ_WS.executeExperience(this, this.getAlgo(), this.getNb_threads(), this.getPartition());
			break;
		case CONACQ1:
			ACQ_WS.executeConacqV1Experience(this);
			break;
		case CONACQ2:
			ACQ_WS.executeConacqV2Experience(this);
			break;
		default:
			ACQ_WS.executeExperience(this, this.getAlgo(), this.getNb_threads(), this.getPartition());

			break;

		}

	}
	@Override
	public ArrayList<ACQ_Bias> createDistBias() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ACQ_Learner createDistLearner(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public ACQ_Network createTargetNetwork() {
		// TODO Auto-generated method stub
		return this.createTargetNetwork();
	}

	@Override
	public ArrayList<ACQ_Network> createStrategy(ACQ_Bias bias) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContradictionSet createBackgroundKnowledge(ACQ_Bias bias, ConstraintMapping mapping) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SATSolver createSATSolver() {
		return new MiniSatSolver();

	}

	@Override
	public boolean getJson() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDataFile() {
		// TODO Auto-generated method stub
		return examplesfile;
	}

	@Override
	public int getMaxRand() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxQueries() {
		// TODO Auto-generated method stub
		return maxqueries;
	}

	@Override
	public ACQ_Network createInitNetwork() {
		// TODO Auto-generated method stub
		return null;
	}
		
	
}
