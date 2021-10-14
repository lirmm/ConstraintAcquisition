package fr.lirmm.coconut.acquisition.expe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

public class ExpeJigSawSUDOKU extends DefaultExperience {
	private static boolean gui = true;
	private static boolean parallel = true;
	HashMap<String, ArrayList<String>> mappings = new HashMap<>();

	public ExpeJigSawSUDOKU() throws IOException {
		setDimension(9);
		mappings = Get_Regions(9);
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
		}, vrs, vls);
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

				// region Check

				for (String key : mappings.keySet()) {
					ArrayList<String> values = mappings.get(key);
					for (int i = 0; i < values.size(); i++) {
						int x = Integer.parseInt(String.valueOf(values.get(i).toCharArray()[0]));
						int y = Integer.parseInt(String.valueOf(values.get(i).toCharArray()[1]));
						for (int j = i + 1; j < values.size(); j++) {

							int x1 = Integer.parseInt(String.valueOf(values.get(j).toCharArray()[0]));
							int y1 = Integer.parseInt(String.valueOf(values.get(j).toCharArray()[1]));
							if (s[x][y] > 0 && s[x][y] == s[x1][y1]) {
								e.classify(false);
								return false;
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

				
				// X >= Y
				constraints
						.add(new BinaryArithmetic("GreaterEqualXY", vars[pos[0]], Operator.GE, vars[pos[1]], "LessXY"));
				// X <= Y
				constraints
						.add(new BinaryArithmetic("LessEqualXY", vars[pos[0]], Operator.LE, vars[pos[1]], "GreaterXY"));
				// X < Y
				constraints
						.add(new BinaryArithmetic("LessXY", vars[pos[0]], Operator.LT, vars[pos[1]], "GreaterEqualXY"));
				// X > Y
				constraints
						.add(new BinaryArithmetic("GreaterXY", vars[pos[0]], Operator.GT, vars[pos[1]], "LessEqualXY"));

				}
			}
		}
		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);
		return new ACQ_Bias(network);
	}

	public HashMap<String, ArrayList<String>> Get_Regions(int n) throws IOException {
		// File directory = new File("src/fr/lirmm/coconut/quacq/bench/JigSawSudoku/");

		File directory = new File("src/fr/lirmm/coconut/quacq/bench/JigSawSudoku/");

		String filePath = directory.getAbsolutePath() + "/problem1.txt";
		HashMap<String, String> mappings = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> mappings1 = new HashMap<>();
		ArrayList<String> cells;
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(":", 2);
			if (parts.length >= 2) {
				String key = parts[0];
				String value = parts[1];
				mappings.put(key, value);
			} else {
				System.out.println("ignoring line: " + line);
			}
		}

		for (String key : mappings.keySet()) {
			String[] str = mappings.get(key).split("]");
			cells = new ArrayList();
			for (String s : str) {

				cells.add(s.replaceAll("\\D+", ""));
			}
			mappings1.put(key, cells);

		}

		reader.close();

		return mappings1;

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
