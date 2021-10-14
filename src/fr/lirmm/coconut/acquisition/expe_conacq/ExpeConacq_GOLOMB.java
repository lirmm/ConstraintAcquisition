/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.expe_conacq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.BinaryArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintMapping;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ContradictionSet;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Operator;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ScalarArithmetic;
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
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_Partition;
import fr.lirmm.coconut.acquisition.core.workspace.ACQ_WS;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperienceConacq;

/**
 *
 * @author NASSIM
 */
public class ExpeConacq_GOLOMB extends DefaultExperience {

	// Heuristic heuristic = Heuristic.SOL;
	// ACQ_ConstraintSolver solver;
	// ACQ_Bias bias;
	// private final ACQ_Learner learner;
	private static int nb_mark=4  ; // NL:: add instance parameter for golomb				
	private static int nb_dist = (nb_mark * (nb_mark - 1)) / 2; // m * (m - 1))
																// / 2

	public ExpeConacq_GOLOMB() {

	}

	static String vls = ValSelector.IntDomainRandom.toString();
	static String vrs=VarSelector.RandomVar.toString();

	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			}

			@Override
			public int getMax(int numvar) {
				return nb_mark * nb_mark;
			}
		}, vrs, vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {
				int mark[] = new int[nb_mark];
				int diffs[] = new int[nb_dist];

				for (int i = 0; i < mark.length; i++)
					mark[i] = -1;

				for (int numvar : e.getScope())
					mark[numvar] = e.getValue(numvar);

				for (int i = 0; i < nb_mark - 1; i++) {
					if (mark[i] >= 0 && mark[i + 1] >= 0 && mark[i] >= mark[i + 1]) { // lex
																						// checker
						e.classify(false);
						return false;
					}

				}

				for (int i = 0, k = 0; i < nb_mark - 1; i++) {
					for (int j = i + 1; j < nb_mark; j++, k++) {
						if (mark[i] >= 0 && mark[j] >= 0)
							diffs[k] = mark[j] - mark[i];
						else
							diffs[k] = -1;
					}
				}
				// distance diff checker
				for (int i = 0; i < nb_dist - 1; i++) {
					for (int j = i + 1; j < nb_dist; j++) {
						if (diffs[i] >= 0 && diffs[j] >= 0 && diffs[i] == diffs[j]) {
							e.classify(false);
							return false;
						}
					}
				}
				e.classify(true);

				return true;

			}

		};

	}

	public ACQ_Bias createBias() {
		int NB_VARIABLE = nb_mark;
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		ConstraintFactory constraintFactory = new ConstraintFactory();
		// build binary Constraints
		ConstraintSet constraints = constraintFactory.createSet();
		CombinationIterator iterator = new CombinationIterator(NB_VARIABLE, 2);
		while (iterator.hasNext()) {
			int[] vars = iterator.next();
			AllPermutationIterator pIterator = new AllPermutationIterator(2);
			while (pIterator.hasNext()) {
				int[] pos = pIterator.next();

				if (vars[pos[0]] < vars[pos[1]]) // NL: commutative relations
				{
					// X != Y
					constraints.add(
					 new BinaryArithmetic("DifferentXY", vars[pos[0]], Operator.NEQ, vars[pos[1]],
					 "EqualXY"));
					// X == Y
					 constraints.add(
					 new BinaryArithmetic("EqualXY", vars[pos[0]], Operator.EQ, vars[pos[1]],
					 "DifferentXY"));

				
				// X >= Y
				constraints
						.add(new BinaryArithmetic("GreaterEqualXY", vars[pos[0]], Operator.GE, vars[pos[1]], "LessXY"));
				// X <= Y
				constraints
				 .add(new BinaryArithmetic("LessEqualXY", vars[pos[0]], Operator.LE,
				 vars[pos[1]], "GreaterXY"));
				
				// X > Y
				 constraints
				 .add(new BinaryArithmetic("GreaterXY", vars[pos[0]], Operator.GT,
				 vars[pos[1]], "LessEqualXY"));
				// X < Y
				constraints
						.add(new BinaryArithmetic("LessXY", vars[pos[0]], Operator.LT, vars[pos[1]], "GreaterEqualXY"));
				}
			}
		}
		System.out.println("after binary "+constraints.size());

		CombinationIterator bin_iterator1 = new CombinationIterator(NB_VARIABLE, 3);
		while (bin_iterator1.hasNext()) {
			int[] vars = bin_iterator1.next();
			AllPermutationIterator pIterator1 = new AllPermutationIterator(3);
			while (pIterator1.hasNext()) {
				int[] pos = pIterator1.next();
				if (vars[pos[0]] > vars[pos[1]] && vars[pos[1]] > vars[pos[2]]) {

					constraints.add(
							new ScalarArithmetic("DistDiff", new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] },
									new int[] { 1, -2, 1 }, Operator.NEQ, 0, "DistEqual"));
					constraints.add(
							new ScalarArithmetic("DistEqual", new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] },
									new int[] { 1, -2, 1 }, Operator.EQ, 0, "DistDiff"));
					constraints.add(
							new ScalarArithmetic("DistGreater", new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] },
									new int[] { 1, -2, 1 }, Operator.GT, 0, "DistLessEqual"));
					constraints.add(
							new ScalarArithmetic("DistLess", new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] },
									new int[] { 1, -2, 1 }, Operator.LT, 0, "DistGreaterEqual"));
					constraints.add(
							new ScalarArithmetic("DistGreaterEqual", new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] },
									new int[] { 1, -2, 1 }, Operator.GE, 0, "DistLess"));
					constraints.add(
							new ScalarArithmetic("DistLessEqual", new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] },
									new int[] { 1, -2, 1 }, Operator.LE, 0, "DistGreater"));
	
				}

			}
		}
		System.out.println("after ternary "+constraints.size());

		CombinationIterator bin_iterator2 = new CombinationIterator(NB_VARIABLE, 4);
		while (bin_iterator2.hasNext()) {
			int[] vars = bin_iterator2.next();
			AllPermutationIterator pIterator2 = new AllPermutationIterator(4);
			while (pIterator2.hasNext()) {
				int[] pos = pIterator2.next();
				if (vars[pos[0]] > vars[pos[1]] && vars[pos[2]] > vars[pos[3]] && vars[pos[0]] > vars[pos[2]]) {
					constraints.add(new ScalarArithmetic("DistDiff",
							new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
							new int[] { 1, -1, -1, 1 }, Operator.NEQ, 0,"DistEqual"));
					constraints.add(new ScalarArithmetic("DistEqual",
							new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
							new int[] { 1, -1, -1, 1 }, Operator.EQ, 0,"DistDiff"));
					constraints.add(new ScalarArithmetic("DistGreater",
							new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
							new int[] { 1, -1, -1, 1 }, Operator.GT, 0,"DistLessEqual"));
					constraints.add(new ScalarArithmetic("DistLess",
							new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
							new int[] { 1, -1, -1, 1 }, Operator.LT, 0,"DistGreaterEqual"));
					constraints.add(new ScalarArithmetic("DistGreaterEqual",
							new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
							new int[] { 1, -1, -1, 1 }, Operator.GE, 0,"DistLess"));
					constraints.add(new ScalarArithmetic("DistLessEqual",
							new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
							new int[] { 1, -1, -1, 1 }, Operator.LE, 0,"DistGreater"));
				
				}

			}
		}
		System.out.println("after quaternary "+constraints.size());
		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);
		return new ACQ_Bias(network);
	}

	
	
	public static void main(String[] args) {
		
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
	public SATSolver createSATSolver() {
		//return new Z3SATSolver();
		return new MiniSatSolver();
	}

	@Override
	public ACQ_Network createTargetNetwork() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void process() {
		nb_mark = getInstance() ;

			ACQ_WS.executeConacqV2Experience(this);
		
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
	public boolean getJson() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDataFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMaxRand() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxQueries() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ACQ_Network createInitNetwork() {
		// TODO Auto-generated method stub
		return null;
	}
}

