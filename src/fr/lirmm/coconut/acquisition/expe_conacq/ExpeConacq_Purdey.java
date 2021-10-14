package fr.lirmm.coconut.acquisition.expe_conacq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.BinaryArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintMapping;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ContradictionSet;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Operator;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ScalarArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.UnaryArithmetic;
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

public class ExpeConacq_Purdey extends DefaultExperience {

	private static int purdey = 4; // m
	private static boolean gui = true;
	private static boolean parallel = true;

	public ExpeConacq_Purdey() {

	}

	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			}

			@Override
			public int getMax(int numvar) {
				return purdey;
			}
		}, vrs, vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {

				if (e.getScope().size() < 12)
					return partial_ask(e);

				return MQ_ask(e);

			}

			private boolean partial_ask(ACQ_Query e) {

				int vars[] = new int[purdey * 3];
				for (int numvar : e.getScope()) {
					vars[numvar] = e.getValue(numvar);

				}

				int Boyds = vars[0];
				int Garveys = vars[1];
				int Logans = vars[2];
				int Navarros = vars[3];

				int Flour = vars[4];
				int Kerosene = vars[5];
				int Muslin = vars[6];
				int sugar = vars[7];

				int cash = vars[8];
				int credit = vars[9];
				int tradedham = vars[10];
				int tradedpeas = vars[11];

				for (int i = 0; i < purdey - 1; i++) {
					for (int j = i + 1; j < purdey; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}
					}
				}
				for (int i = purdey; i < purdey * 2 - 1; i++) {
					for (int j = i + 1; j < purdey * 2; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}
					}
				}
				for (int i = purdey * 2; i < purdey * 3 - 1; i++) {
					for (int j = i + 1; j < purdey * 3; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}
					}
				}

				// Boyds bought Kerosene
				if (Boyds > 0 && Kerosene > 0 && Boyds != Kerosene) { // Constraint:: Boyds == Kerosene
																		// (vars[0]==vars[5]
					e.classify(false);
					return false;
				} // 2. the Boyds paid in cash
				if (Boyds > 0 && cash > 0 && Boyds != cash) { // constraint:: Boyds == cash (vars[0]==vars[8])
					e.classify(false);
					return false; // 3. the Garveys Bought Muslin
				}
				if (Garveys > 0 && Muslin > 0 && Garveys != Muslin) { // constraint:: Garveys == Muslin
																		// (vars[1]==vars[6])
					e.classify(false);
					return false; // 4. the Garveys paid by credit card
				}
				if (Garveys > 0 && credit > 0 && Garveys != credit) { // constraint:: Garveys == credit
																		// (vars[1]==vars[9])
					e.classify(false);
					return false; // 5. the Logans bought Flour
				}
				if (Logans > 0 && Flour > 0 && Logans != Flour) { // constraint:: Logans == Flour (vars[2]==vars[4])
					e.classify(false);
					return false; // 6. Logans traded had for Flour
				}
				if (Logans > 0 && tradedham > 0 && Logans != tradedham) { // constraint:: Logans == tradedham
																			// (vars[2]==vars[10])
					e.classify(false);
					return false; // 7. Navarros bought sugar
				}
				if (Navarros > 0 && sugar > 0 && Navarros != sugar) { // constraint:: Navarros == sugar
																		// (vars[3]==vars[7])
					e.classify(false);
					return false; // 8. Navarros traded peas for the sugar
				}
				if (Navarros > 0 && tradedpeas > 0 && Navarros != tradedpeas) { // constraint:: Navarros == tradedpeas
																				// (vars[3]==vars[11])
					e.classify(false);
					return false; // 9. Kerosene can only be paid by cash only
				}
				if (Kerosene > 0 && cash > 0 && Kerosene != cash) { // constraint:: Kerosene == cash (vars[5]==vars[8])
					e.classify(false);
					return false; // 10.Muslin can be bought by credit card only
				}
				if (Muslin > 0 && credit > 0 && Muslin != credit) { // constraint:: Muslin == credit (vars[6]==vars[9])
					e.classify(false);
					return false; // 11. Flour can be bought by trading ham
				}
				if (Flour > 0 && tradedham > 0 && Flour != tradedham) { // constraint:: Flour == tradedham
																		// (vars[4]==vars[10])
					e.classify(false);
					return false; // 12. sugar is bought by trading peas
				}
				if (sugar > 0 && tradedpeas > 0 && sugar != tradedpeas) { // constraint:: sugar == tradedpeas
																			// (vars[3]==vars[11])
					e.classify(false);
					return false;
				}

				if (Boyds > 0 && Boyds != 4) { // Constraint:: Boyds == 4 (vars[0]== 4)
					e.classify(false);
					return false;
				}

				if (Garveys > 0 && Garveys != 2) { // Constraint:: Garveys == 2 (vars[1]== 2)
					e.classify(false);
					return false;
				}

				if (Logans > 0 && Logans != 3) { // Constraint:: Logans == 3 (vars[2]==3)
					e.classify(false);
					return false;
				}

				e.classify(true);

				return true;

			}

			private boolean MQ_ask(ACQ_Query e) {

				int[] s = new int[] { 4, 2, 3, 1, 3, 4, 2, 1, 4, 2, 3, 1 };

				for (int numvar : e.getScope()) {
					if (s[numvar] != e.getValue(numvar)) {
						e.classify(false);
						return false;
					}
				}

				e.classify(true);

				return true;

			}

		};

	}

	public ACQ_Bias createBias() {
		int NB_VARIABLE = purdey * 3;

		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		ConstraintFactory constraintFactory = new ConstraintFactory();
		// unary constraints
		ConstraintSet constraints = constraintFactory.createSet();
		for (int i = 0; i < purdey * 3; i++) {
			for (int j = 1; j <= purdey; j++) {

				// X != cste
				constraints.add(new UnaryArithmetic("DifferentX_" + j + "", i, Operator.NEQ, j));
				// X == cste
				constraints.add(new UnaryArithmetic("EqualX_" + j + "", i, Operator.EQ, j));
				// X < cste
				constraints.add(new UnaryArithmetic("LessX_" + j + "", i, Operator.LT, j));
				// X > cste
				constraints.add(new UnaryArithmetic("GreaterX_" + j + "", i, Operator.GT, j));
				// X <= cste
				constraints.add(new UnaryArithmetic("LessEqualX_" + j + "", i, Operator.LE, j));
				// X >= cste
				constraints.add(new UnaryArithmetic("GreaterEqualX_" + j + "", i, Operator.GE, j));
			}

		}
		// Binary Constraints
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

				}
				// X >= Y
				constraints
						.add(new BinaryArithmetic("GreaterEqualXY", vars[pos[0]], Operator.GE, vars[pos[1]], "LessXY"));
				// X <= Y
				constraints
						.add(new BinaryArithmetic("LessEqualXY", vars[pos[0]], Operator.LE, vars[pos[1]], "GreaterXY"));

				// X > Y
				constraints
						.add(new BinaryArithmetic("GreaterXY", vars[pos[0]], Operator.GT, vars[pos[1]], "LessEqualXY"));
				// X < Y
				constraints
						.add(new BinaryArithmetic("LessXY", vars[pos[0]], Operator.LT, vars[pos[1]], "GreaterEqualXY"));
			}
		}

		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);

		return new ACQ_Bias(network);
	}


	
	public static void main(String[] args) {
		ExpeConacq_Purdey expe;
		try {
			expe = new ExpeConacq_Purdey();
			expe.setParams(true, // normalized csp
					true, // shuffle_split,
					60000, // timeout
					ACQ_Heuristic.SOL, 
				 true,true
			);
			expe.setLog_queries(true);

			ACQ_WS.executeConacqV2Experience(expe);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
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
		ExpeConacq_Purdey expe;
		try {
			expe = new ExpeConacq_Purdey();
			expe.setParams(true, // normalized csp
					true, // shuffle_split,
					60000, // timeout
					ACQ_Heuristic.SOL, 
				 true,true
			);
			expe.setLog_queries(true);

			ACQ_WS.executeConacqV2Experience(expe);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}		
	}
	@Override
	public ContradictionSet createBackgroundKnowledge(ACQ_Bias bias, ConstraintMapping mapping) {
		return null;
	}
	@Override
	public ArrayList<ACQ_Network> createStrategy(ACQ_Bias bias) {
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

