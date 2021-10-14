package fr.lirmm.coconut.acquisition.expe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
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
import fr.lirmm.coconut.acquisition.gui.GUI_Utils;

public class ExpeGTSUDOKU extends DefaultExperience {
	private static boolean gui = true;
	private static boolean parallel = true;
	HashMap<String, String> mappings = new HashMap();
	static String vls;
	static String vrs;
	public ExpeGTSUDOKU() {
		setDimension(9);
		mappings = Generate_GTSudoku(9, 3);
		System.out.print("generated");
	}


	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			} 

			@Override
			public int getMax(int numvar) {
				return 9;
			}
		},vrs,vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {

				int s[][] = new int[9][9];
				int l, c;

				for (int numvar : e.getScope()) {
					l = numvar / 9;
					c = numvar % 9;
					s[l][c] = e.getValue(numvar);

				}

				// row checker
				for (int row = 0; row < 9; row++)
					for (int col = 0; col < 8; col++)
						for (int col2 = col + 1; col2 < 9; col2++)
							if (s[row][col] != 0 && s[row][col] == s[row][col2]) {
								e.classify(false);
								return false;
							}

				// column checker
				for (int col = 0; col < 9; col++)
					for (int row = 0; row < 8; row++)
						for (int row2 = row + 1; row2 < 9; row2++)
							if (s[row][col] != 0 && s[row][col] == s[row2][col]) {
								e.classify(false);
								return false;
							}

				// grid checker
				for (int row = 0; row < 9; row += 3) {
					for (int col = 0; col < 9; col += 3) {
						// row, col is start of the 3 by 3 grid
						for (int pos = 0; pos < 8; pos++) {
							for (int pos2 = pos + 1; pos2 < 9; pos2++) {

								if (s[row + pos % 3][col + pos / 3] != 0
										&& s[row + pos % 3][col + pos / 3] == s[row + pos2 % 3][col + pos2 / 3]) {
									e.classify(false);
									return false;
								}

							}
						}
					}
				}
				// GreaterThan Grid Check
				for (int row = 0; row < 9; row++) {
					for (int col = 0; col < 8; col++) {
						for (int row1 = 0; row1 < 9; row1++) {
							for (int col1 = 0; col1 < 8; col1++) {
								int x = (row);
								int y = (col);
								int x1 = (row1);
								int y1 = (col1);
								String[] key = new String[] { Arrays.toString(new int[] { x, y }),
										Arrays.toString(new int[] { x1, y1 }) };

								String arithm = mappings.get(Arrays.toString(key));
								if (arithm != null) {
									if (s[row][col] != 0 && s[row1][col1] != 0
											&& !Check(s[row][col], s[row1][col1], arithm)) {
										e.classify(false);
										return false;
									}
								}
							}
						}
					}
				}
				e.classify(true);
				return true;
			}
		};
	}

	public ACQ_Bias createBias() {
		int NB_VARIABLE = 81;
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

	public HashMap<String, String> Generate_GTSudoku(int n, int m) {
		int s[][] = new int[n][n];
		String[] arithms = new String[] { "GreaterXY", "LesserXY" };
		Random rand = new Random();
		ACQ_Network network = createBias().getNetwork();
		ConstraintFactory f = new ConstraintFactory();
		ACQ_Network network1 = new ACQ_Network(f, network.getVariables(), f.createSet());
		ACQ_ChocoSolver solver = (ACQ_ChocoSolver) this.createSolver();
		solver.setVars(network1.getVariables());
		HashMap<String, String> mapping = new HashMap<>();
		for (int row = 0; row < n - 1; row += m) {
			for (int col = row + 1; col < n; col += m) {
				// row, col is start of the 3 by 3 grid
				for (int pos = 0; pos < n - 1; pos++) {
					for (int pos2 = pos; pos2 < n; pos2++) {
						String a = arithms[rand.nextInt(arithms.length)];

						int x = (row + pos % m);
						int y = (col + pos / m);
						int x1 = (row + pos2 % m);
						int y1 = (col + pos2 / m);
						int v;
						int v1;

						if (((x + 1 == x1) && (y == y1)) || ((y + 1 == y1) && (x == x1))) {
							v = (m * x + y);
							v1 = (m * x1 + y1);
							ACQ_IConstraint cst = null;
							if (a.equals("GreaterXY")) {
								cst = new BinaryArithmetic(a, v, Operator.GT, v1, a);
								network1.add(cst, true);
							} else if (a.equals("LesserXY")) {
								cst = new BinaryArithmetic(a, v, Operator.LT, v1, a);
								network1.add(cst, true);
							}
							System.out.print("");
							if (!solver.solveA(network1).isEmpty()) {
								String[] key = new String[] { Arrays.toString(new int[] { x, y }),
										Arrays.toString(new int[] { x1, y1 }) };
								mapping.put(Arrays.toString(key), a);
							} else {
								network1.remove(cst);

							}
						}

					}
				}
			}
		}

		return mapping;

	}

	public boolean Check(int a, int b, String s) {
		switch (s) {
		case "GreaterXY":
			return (a > b);
		case "LesserXY":
			return (a < b);
		default:
			return false;
		}

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
