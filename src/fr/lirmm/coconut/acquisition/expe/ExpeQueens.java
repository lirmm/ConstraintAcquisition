package fr.lirmm.coconut.acquisition.expe;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

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
import fr.lirmm.coconut.acquisition.gui.GUI_Utils;

public class ExpeQueens extends DefaultExperience {

	// Heuristic heuristic = Heuristic.SOL;
	// ACQ_ConstraintSolver solver;
	// ACQ_Bias bias;
	// private final ACQ_Learner learner;
	static int nb_vars = 8; // m
	static final Integer Users = 1;
	static final boolean parallel = false;
	static HashMap<String, ACQ_Query> memory = new HashMap<>();
	private static boolean gui = false;
	static ACQ_Network CL;

	public ExpeQueens() {
		ConstraintFactory constraintFactory = new ConstraintFactory();
		// build binary Constraints
		ConstraintSet constraints = constraintFactory.createSet();
		CL = new ACQ_Network(constraintFactory, null, constraints);

		setDimension(nb_vars);

	}

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
		}, vrs, vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {

			@Override
			public boolean ask(ACQ_Query e) {

				int queens[] = new int[nb_vars];

				for (int numvar : e.getScope())
					queens[numvar] = e.getValue(numvar);

				for (int i = 0; i < nb_vars - 1; i++) {
					for (int j = i + 1; j < nb_vars; j++) {
						if ((queens[i] > 0 && queens[j] > 0) && (queens[i] == queens[j]
								|| queens[i] == queens[j] - (j - i) || queens[i] == queens[j] + (j - i))) { // lex
																											// checker
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
		int NB_VARIABLE = nb_vars;
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		// build Constraints
		ConstraintFactory constraintFactory = new ConstraintFactory();

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
							new BinaryArithmetic("DifferentXY", vars[pos[0]], Operator.NEQ, vars[pos[1]], "EqualXY"));
					// X == Y
					constraints.add(
							new BinaryArithmetic("EqualXY", vars[pos[0]], Operator.EQ, vars[pos[1]], "DifferentXY"));

					// Xi and Xj Out diag1
					constraints.add(new BinaryArithmetic("OutDiag1", vars[pos[0]], Operator.NEQ, vars[pos[1]],
							Operator.PL, (vars[pos[1]] - vars[pos[0]]), "InDiag1"));
					// Xi and Xj In diag1
					constraints.add(new BinaryArithmetic("InDiag1", vars[pos[0]], Operator.EQ, vars[pos[1]],
							Operator.PL, (vars[pos[1]] - vars[pos[0]]), "OutDiag1"));

					// Xi and Xj Out diag2
					constraints.add(new BinaryArithmetic("OutDiag2", vars[pos[0]], Operator.NEQ, vars[pos[1]],
							Operator.PL, (vars[pos[0]] - vars[pos[1]]), "InDiag2"));
					// Xi and Xj In diag2
					constraints.add(new BinaryArithmetic("InDiag2", vars[pos[0]], Operator.EQ, vars[pos[1]],
							Operator.PL, (vars[pos[0]] - vars[pos[1]]), "OutDiag2"));

					/*
					 * //Nqueens case OutDiag1: return t[0]+this.ind1 != t[1]+this.ind2; case
					 * OutDiag2: return t[0]-this.ind1 != t[1]-this.ind2; case InDiag1: return
					 * t[0]+this.ind1 == t[1]+this.ind2; case InDiag2: return t[0]-this.ind1 ==
					 * t[1]-this.ind2;
					 */
					// X >= Y
					constraints.add(
							new BinaryArithmetic("GreaterEqualXY", vars[pos[0]], Operator.GE, vars[pos[1]], "LessXY"));
					// X <= Y
					constraints.add(
							new BinaryArithmetic("LessEqualXY", vars[pos[0]], Operator.LE, vars[pos[1]], "GreaterXY"));
					// X > Y
					constraints.add(
							new BinaryArithmetic("GreaterXY", vars[pos[0]], Operator.GT, vars[pos[1]], "LessEqualXY"));
					// X < Y
					constraints.add(
							new BinaryArithmetic("LessXY", vars[pos[0]], Operator.LT, vars[pos[1]], "GreaterEqualXY"));

				}

			}
		}

		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);
		return new ACQ_Bias(network);
	}

	public boolean isNormalizedCSP() {
		return false;
	}

	public boolean isShuffleSplit() {
		return false;
	}

	public boolean isLearningCheck() {
		return false;
	}

	@Override
	public void process() {
		nb_vars=getInstance();
		if(gui) {
			GUI_Utils.executeCoop(this, this.getNb_threads());
		}else 
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

	

	public void setGui(boolean gui) {
		this.gui=gui;		
	}

	

	@Override
	public ACQ_Network createTargetNetwork() {
		// TODO Auto-generated method stub
		return null;
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
