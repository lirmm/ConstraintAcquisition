package fr.lirmm.coconut.acquisition.core.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Language;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.BinaryArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Operator;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ChocoSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_IDomain;
import fr.lirmm.coconut.acquisition.core.acqsolver.ValSelector;
import fr.lirmm.coconut.acquisition.core.acqsolver.VarSelector;
import fr.lirmm.coconut.acquisition.core.combinatorial.AllPermutationIterator;
import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

public class ExpeGenerator {

	int domainSize;
	static int nResource = 0;
	static int nTasks = 0;
	static int[] durations = null;
	static int[] capacities = null;
	static int[][] requirements = null;
	static int UB = 0;
	static HashMap<Integer, ArrayList<Integer>> precedencies;

	public static void main(String args[]) throws IOException {

		//new ExpeGenerator().random(50, 122, 10);
		ExpeGenerator.Scheduling("schedulingpack007");

		//ExpeGenerator.latinBench(10);

		//ExpeGenerator.sudokuBench(4);
		//ExpeGenerator.sudokuBench(9);
		//ExpeGenerator.queensBench(8);
		//ExpeGenerator.queensBench(30);
		//ExpeGenerator.golombBench(8);
		//ExpeGenerator.jsudokuBench();

	}

	public static void latinBench(int n) {
		String bench = "latinSquare";
		File directory = new File("benchmarks/" + bench);
		String s = "";
		int nbVars = n * n;
		ArrayList<String> content = new ArrayList<>();
		if (!directory.exists()) {
			directory.mkdir();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.
		}
		try {
			File myObj = new File("benchmarks/" + bench + "/" + bench + "_" + nbVars + ".target");
			FileWriter myWriter = new FileWriter(myObj);

			// row checker
			for (int row = 0; row < n; row++)
				for (int col = 0; col < n - 1; col++)
					for (int col2 = col + 1; col2 < n; col2++) {
						s = "DifferentXY " + ((row * n) + col) + " " + ((row * n) + col2);
						if (!content.contains(s)) {
							content.add(s);
							myWriter.write(s + "\n");
						}
					}

			// column checker
			for (int col = 0; col < n; col++)
				for (int row = 0; row < n - 1; row++)
					for (int row2 = row + 1; row2 < n; row2++) {
						s = "DifferentXY " + ((row * n) + col) + " " + ((row2 * n) + col);
						if (!content.contains(s)) {
							content.add(s);
							myWriter.write(s + "\n");
						}
					}
			// if (s[row][col] != 0 && s[row][col] == s[row2][col]) {

			myWriter.close();

			System.out.println(bench + "_" + nbVars + " target network created!!");

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		biasGenerator(bench, nbVars, 1, n, ACQ_Language.Arith);
	}

	public static void biasGenerator(String bench, int n, int min, int max, ACQ_Language gamma) {
		File directory = new File("benchmarks/" + bench);
		String s = "";
		ArrayList<String> content = new ArrayList<>();
		if (!directory.exists()) {
			directory.mkdir();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.
		}
		try {
			File myObj = new File("benchmarks/" + bench + "/" + bench + "_" + n + ".bias");
			FileWriter myWriter = new FileWriter(myObj);

			myWriter.write("nbVars " + n + "\n");
			myWriter.write("domainSize " + min + " " + max + "\n\n");
			myWriter.write("Gamma \n");

			for (String relation : gamma.getRelations()) {
				myWriter.write(relation + "\n");

			}

			myWriter.close();

			System.out.println(bench + "_" + n + " complete bias created!!");

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	public static void sudokuBench(int n) {
		int s = (int) Math.sqrt(n);
		String bench = "sudoku";
		File directory = new File("benchmarks/" + bench);

		int nbVars = n * n;
		String elt = "";
		ArrayList<String> content = new ArrayList<>();

		if (!directory.exists()) {
			directory.mkdir();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.
		}
		try {
			File myObj = new File("benchmarks/" + bench + "/" + bench + "_" + nbVars + ".target");
			FileWriter myWriter = new FileWriter(myObj);

			// row checker
			for (int row = 0; row < n; row++)
				for (int col = 0; col < n - 1; col++)
					for (int col2 = col + 1; col2 < n; col2++) {
						elt = "DifferentXY " + ((row * n) + col) + " " + ((row * n) + col2);
						if (!content.contains(elt)) {
							content.add(elt);
							myWriter.write(elt + "\n");
						}
					}

			// column checker
			for (int col = 0; col < n; col++)
				for (int row = 0; row < n - 1; row++)
					for (int row2 = row + 1; row2 < n; row2++) {
						elt = "DifferentXY " + ((row * n) + col) + " " + ((row2 * n) + col);
						if (!content.contains(elt)) {
							content.add(elt);
							myWriter.write(elt + "\n");
						}
					}

			// grid checker
			for (int row = 0; row < n; row += s) {
				for (int col = 0; col < n; col += s) {
					for (int pos = 0; pos < n - 1; pos++) {
						for (int pos2 = pos + 1; pos2 < n; pos2++) {
							int x = row + pos % s;
							int y = col + pos / s;
							int x1 = row + pos2 % s;
							int y1 = col + pos2 / s;
							elt = "DifferentXY " + (((x * n) + y)) + " " + (((x1 * n) + y1));
							if (!content.contains(elt)) {
								content.add(elt);
								myWriter.write(elt + "\n");
							}

						}
					}
				}
			}
			myWriter.close();

			System.out.println(bench + "_" + nbVars + " target network created!!");

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		biasGenerator(bench, nbVars, 1, n, ACQ_Language.Arith);

	}
	
	public static void jsudokuBench() throws IOException {

		int n = 9;
		int s = (int) Math.sqrt(n);
		int nbVars = n * n;
		HashMap<String, ArrayList<String>> mappings = Get_Regions(9);

		String elt = "";
		ArrayList<String> content = new ArrayList<>();

		String bench = "jsudoku";
		File directory = new File("benchmarks/" + bench);
		if (!directory.exists()) {
			directory.mkdir();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.
		}
		try {
			File myObj = new File("benchmarks/" + bench + "/" + bench +"_" +nbVars+ ".target");
			FileWriter myWriter = new FileWriter(myObj);

			// row checker
			for (int row = 0; row < n; row++)
				for (int col = 0; col < n - 1; col++)
					for (int col2 = col + 1; col2 < n; col2++) {
						elt = "DifferentXY " + ((row * n) + col) + " " + ((row * n) + col2);
						if (!content.contains(elt)) {
							content.add(elt);
							myWriter.write(elt + "\n");
						}

					}

			// column checker
			for (int col = 0; col < n; col++)
				for (int row = 0; row < n - 1; row++)
					for (int row2 = row + 1; row2 < n; row2++) {
						elt = "DifferentXY " + ((row * n) + col) + " " + ((row2 * n) + col);
						if (!content.contains(elt)) {
							content.add(elt);
							myWriter.write(elt + "\n");
						}

					}

			// grid checker
			// region Check

			for (String key : mappings.keySet()) {
				ArrayList<String> values = mappings.get(key);
				for (int i = 0; i < values.size(); i++) {
					int x = Integer.parseInt(String.valueOf(values.get(i).toCharArray()[0]));
					int y = Integer.parseInt(String.valueOf(values.get(i).toCharArray()[1]));
					for (int j = i + 1; j < values.size(); j++) {

						int x1 = Integer.parseInt(String.valueOf(values.get(j).toCharArray()[0]));
						int y1 = Integer.parseInt(String.valueOf(values.get(j).toCharArray()[1]));
						elt = "DifferentXY " + (((x * n) + y)) + " " + (((x1 * n) + y1));
						if (!content.contains(elt)) {
							content.add(elt);
							myWriter.write(elt + "\n");
						}

					}
				}
			}
			myWriter.close();

			System.out.println(bench + "_" + nbVars + " target network created!!");

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		biasGenerator(bench, nbVars, 1, n, ACQ_Language.Arith);

	}

	public static void queensBench(int n) {
		String bench = "queens";
		File directory = new File("benchmarks/" + bench);
		if (!directory.exists()) {
			directory.mkdir();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.
		}
		try {
			File myObj = new File("benchmarks/" + bench + "/" + bench + "_" + n + ".target");
			FileWriter myWriter = new FileWriter(myObj);

			// row checker
			for (int i = 0; i < n; i++)
				for (int j = i + 1; j < n; j++)

					myWriter.write("DifferentXY " + i + " " + j + "\n");

			for (int i = 0; i < n; i++) {
				for (int j = i + 1; j < n; j++) {
					myWriter.write("OutDiag1 " + i + " " + j + "\n");
					myWriter.write("OutDiag2 " + i + " " + j + "\n");
				}
			}

			myWriter.close();

			System.out.println(bench + "_" + n + " target network created!!");

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		biasGenerator(bench, n, 1, n, ACQ_Language.ArithDiag);

	}

	public static void golombBench(int n) {
		String bench = "golomb";
		File directory = new File("benchmarks/" + bench);
		if (!directory.exists()) {
			directory.mkdir();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.
		}
		try {
			File myObj = new File("benchmarks/" + bench + "/" + bench + "_" + n + ".target");
			FileWriter myWriter = new FileWriter(myObj);

			// row checker
			for (int i = 0; i < n; i++)
				myWriter.write("LessXY " + i + " " + (i + 1) + "\n");

			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					for (int k = 0; k < n; k++) {
						for (int l = 0; l < n; l++) {
							if (i > j && k > l && i > k)
								myWriter.write("DistDiffXYZT " + i + " " + j + " " + k + " " + l + "\n");

						}
					}
				}
			}

			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					for (int k = 0; k < n; k++) {
						if (i > j && j > k)
							myWriter.write("DistDiffXYZ " + i + " " + j + " " + k + "\n");

					}
				}
			}
			myWriter.close();

			System.out.println(bench + "_" + n + " target network created!!");

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		biasGenerator(bench, n, 1, n * n, ACQ_Language.ArithDist);

	}

	public void random(int n, int c, int m) {
		String bench = "random";
		File directory = new File("benchmarks/" + bench);
		domainSize = m;

		if (!directory.exists()) {
			directory.mkdir();
			// If you require it to make the entire directory path including parents,
			// use directory.mkdirs(); here instead.
		}
		try {
			File myObj = new File("benchmarks/" + bench + "/" + bench + "_" + n + "_" + c + "_" + m
					+ System.currentTimeMillis() + ".target");
			FileWriter myWriter = new FileWriter(myObj);

			ACQ_Network network = this.createRandomTarget(n, c, m);

			for (ACQ_IConstraint cst : network) {

				myWriter.write(cst.getName() + " " + cst.getVariables()[0] + " " + cst.getVariables()[1] + "\n");

			}

			myWriter.close();

			System.out.println(bench + "_" + n + "_" + c + "_" + m + " target network created!!");
		} catch (

		IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		biasGenerator(bench, n, 1, m, ACQ_Language.Arith);

	}

	public ACQ_Network createRandomTarget(int n, int c, int m) {

		int NB_VARIABLE = n;
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
							new BinaryArithmetic("DifferentXY", vars[pos[0]], Operator.NEQ, vars[pos[1]], "EqualXY"));
					// X == Y
					constraints.add(
							new BinaryArithmetic("EqualXY", vars[pos[0]], Operator.NEQ, vars[pos[1]], "DifferentXY"));

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

		ACQ_Network network1 = new ACQ_Network(constraintFactory, network.getVariables(),
				constraintFactory.createSet());

		ACQ_Network network_temp = new ACQ_Network(constraintFactory, network.getVariables(),
				constraintFactory.createSet());

		ACQ_ChocoSolver solver = new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			}

			@Override
			public int getMax(int numvar) {
				return m;
			}
		}, VarSelector.DomOverWDeg.toString(), ValSelector.IntDomainMin.toString());
		solver.setVars(network.getVariables());
		int cpt = 0;
		int item = 0;
		int size = network.getConstraints().size();

		while (cpt < c && size > 0) {

			ACQ_IConstraint cst = network.getConstraints().get_Constraint(item);

			item = new Random().nextInt(size);
			network_temp = new ACQ_Network(constraintFactory, allVarSet, network1.getConstraints());
			network_temp.add(cst, true);
			network.getConstraints().remove(cst);
			network.getConstraints().remove(cst.getNegation());

			System.out
					.println("randomTarget::" + cpt + "  from::" + size + "  pointer::" + item + " constraint::" + cst);

			if (!solver.solveA(network_temp).isEmpty()) {
				{
					network1.add(cst, true);
					cpt++;
				}
			}
			size = network.size();
		}

		return network1;
	}



	public static HashMap<String, ArrayList<String>> Get_Regions(int n) throws IOException {

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

	public static void Scheduling(String instance) throws NumberFormatException, IOException {
		File directory = new File("benchmarks/scheduling/rcpsp/"+instance+".data");

		ParseScheduling(directory.getAbsolutePath());
		try {
			File myObj = new File("benchmarks/scheduling/rcpsp/"+instance+".target");
			FileWriter myWriter = new FileWriter(myObj);
			for (int i = 0; i < nTasks; i++) {
				ArrayList<Integer> prec = precedencies.get(i);
				
				for (int a : prec) {
					myWriter.write("PLessEqualXY " + i + " " + a +" "+ (-1 * durations[i]) +" \n");

				}
			}
			
			myWriter.close();

			System.out.println(instance + "_" + nTasks + " target network created!!");

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		biasGenerator(directory.getName(), nTasks, 1, UB, ACQ_Language.PArith);

	}	
	
	public static void ParseScheduling(String instance) throws NumberFormatException, IOException {
		precedencies = new HashMap<Integer, ArrayList<Integer>>();
		
		//Get Instance
		
		BufferedReader reader = new BufferedReader(new FileReader(instance));
		String line;
		String str;

		// for each line until the end of the file
		while (((line = reader.readLine()) != null)) {

			// if the line is a comment or is empty
			if (line.isEmpty() == true) {
				continue;
			}

			// split the line according to spaces
			String[] lineSplited = line.split(" ");

			if (line.startsWith("n_res")) {
				nResource = Integer.parseInt(lineSplited[2].replace(";", ""));
				continue;
			}
			if (line.startsWith("rc")) {
				capacities = new int[nResource];
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				for (int i = 1; i <= nResource; i++) {
					capacities[i - 1] = Integer.parseInt(strSplited[i]);

				}
			}

			if (line.startsWith("n_tasks")) {
				nTasks = Integer.parseInt(lineSplited[2].replace(";", ""));
				continue;
			}

			if (line.startsWith("d")) {
				durations = new int[nTasks];
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				for (int i = 1; i <= nTasks; i++) {
					durations[i - 1] = Integer.parseInt(strSplited[i]);
					UB = UB  + durations[i - 1];

				}
				continue;
			}

			if (line.startsWith("rr")) {
				requirements = new int[nResource][nTasks];
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				
				for (int i = 1; i <= nTasks; i++) {
					requirements[0][i - 1] = Integer.parseInt(strSplited[i]);
					
				}

			}
			int i = 1;
			while (!line.startsWith("rr") && line.contains("|")) {
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				for (int j = 1; j <= nTasks; j++) {
					requirements[i][j - 1] = Integer.parseInt(strSplited[j]);
					

				}
				line = reader.readLine();
				i++;
			}

			if (line.startsWith("suc")) {
				str = line;
				str = str.replaceAll("\\D", " ");
				ArrayList<Integer> task = new ArrayList<Integer>();
				String[] strSplited = str.split(" +");
				for (int j = 1; j < strSplited.length; j++) {
					task.add(Integer.parseInt(strSplited[j]) - 1);
				}
				precedencies.put(0, task);
			}
			i = 1;
			while (line != null && line.contains("{") && !line.startsWith("suc")) {
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				ArrayList<Integer> task = new ArrayList<Integer>();
				for (int j = 1; j < strSplited.length; j++) {
					task.add(Integer.parseInt(strSplited[j]) - 1);
				}
				line = reader.readLine();
				precedencies.put(i, task);
				i++;
			}

		}
	}	
	
}
