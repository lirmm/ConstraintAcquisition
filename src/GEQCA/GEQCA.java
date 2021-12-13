package GEQCA;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.util.SupplierUtil;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint.CstrFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Relation;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.tools.FileManager;

public class GEQCA {

	protected ACQ_Network learned_network;
	protected ACQ_ConstraintSolver solver;
	protected HashMap<String, ArrayList<String>> L;
	protected final HashMap<String, String> target;
	protected ACQ_Heuristic heuristic;
	protected ACQ_SelectionHeuristics sheuristic;
	protected ACQ_Learner learner;
	protected ConstraintFactory constraintFactory;
	protected boolean with_collapse_state = false;
	protected boolean verbose = true;
	protected boolean log_queries = false;
	protected boolean log_constraints = true;
	public int nPositives = 0;
	public int nNegatives = 0;
	static List<Long> times = new ArrayList<Long>();
	boolean propagate = false;
	protected int nb_vars;
	protected IExperience exp;
	protected Map<List<Integer>, List<ACQ_Relation>> learnedRelations;
	protected int propagationchoice;
	protected int deadline;

	String instance = "instance00";

	int nResource = 0;
	int nTasks = 0;
	int[] durations = null;
	int[] capacities = null;
	int[][] requirements = null;
	int UB = 0;

	// Read Instance
	HashMap<Integer, ArrayList<Integer>> precedencies = new HashMap<Integer, ArrayList<Integer>>();

	public GEQCA(ACQ_ConstraintSolver solver, HashMap<String, ArrayList<String>> bias,
			HashMap<String, String> target, ACQ_Learner learner, int nb_vars, ACQ_SelectionHeuristics heuristic) {

		// NL: config part
		this.sheuristic = heuristic;
		this.solver = solver;
		this.L = bias;
		this.target = target;
		this.learner = learner;
		this.nb_vars = nb_vars;

	}

	public void parseSchedulingInstance() throws NumberFormatException, IOException {
		System.out.print(instance);
		BufferedReader reader = new BufferedReader(
				new FileReader("./benchmarks/scheduling/rcpsp/" + instance + ".dzn"));
		String line;
		String str;

		while (((line = reader.readLine()) != null)) {
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
					UB = UB + durations[i - 1];
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

	public HashMap<String, ArrayList<String>> getBias() {
		return L;
	}

	public void setExperience(IExperience exp) {
		this.exp = exp;
	}

	public ACQ_Network getLearnedNetwork() {
		return learned_network;
	}

	public ACQ_Query query_gen(ACQ_IConstraint cst) {
		ConstraintFactory factory = new ConstraintFactory();
		ConstraintSet set = factory.createSet();
		set.add(cst);
		ACQ_Network network1 = new ACQ_Network(factory, set);
		return solver.solveA(network1);

	}

	public void process(Chrono chrono) {

		// assert(learned_network.size()==0);
		chrono.start("total_acq_time");
		process_v1(chrono);
		chrono.stop("total_acq_time");

		if (verbose) {
			System.out.println(learned_network);
		}

	}

	public void process_v1(Chrono chrono) {

		switch (sheuristic) {
		case Random:
			chrono.start("total_acq_time");
			process_random();
			chrono.stop("total_acq_time");
			break;
		case Weighted:
			chrono.start("total_acq_time");
			process_compositionweighted();
			chrono.stop("total_acq_time");
			break;
		case Path:
			chrono.start("total_acq_time");
			process_compositionPath();
			chrono.stop("total_acq_time");
			break;

		default:
			chrono.start("total_acq_time");
			process_composition();
			chrono.stop("total_acq_time");
			break;
		}

	}

	public void process_naive() {
		CombinationIterator iterator1 = new CombinationIterator(nb_vars, 2);
		List<int[]> variables = new ArrayList<int[]>();
		while (iterator1.hasNext()) {
			int[] vars = new int[2];
			vars = iterator1.next();

			if (vars[0] % 2 == 0 && vars[1] % 2 == 0 && vars[0] < vars[1]) {
				variables.add(new int[] { vars[0], vars[1] });
			}
		}
		// Collections.shuffle(variables);
		ArrayList<int[]> V = new ArrayList<int[]>();

		int i = 0;
		for (int[] vars : variables) {

			if (vars[0] < vars[1]) {

				String scope = vars[0] + "," + vars[1];
				// System.out.println(scope);
				ArrayList<String> L_s = L.get(scope);

				relationsForPair(L_s, scope, V);

				if (L_s.size() > 1) {
					if (verbose)
						System.out.println("Clause number " + i++ + " has been learned");
				} else if (L_s.size() == 1) {
					if (verbose)
						System.out.println("Constraint number " + i++ + " has been learned");
				}

			}
		}
	}

	public void process_composition() {
		CombinationIterator iterator1 = new CombinationIterator(nb_vars, 2);
		List<int[]> variables = new ArrayList<int[]>();
		while (iterator1.hasNext()) {
			int[] vars = new int[2];
			vars = iterator1.next();

			if (vars[0] < vars[1]) {
				variables.add(new int[] { vars[0], vars[1] });
			}
		}
		// Collections.shuffle(variables);
		long end = System.currentTimeMillis();
		int i = 0;
		ArrayList<int[]> V = new ArrayList<int[]>();

		for (int[] vars : variables) {

			long start = System.currentTimeMillis();

			String scope = vars[0] + "," + vars[1];
			System.out.println(scope);
			if (propagationchoice == 0 || propagationchoice == 3 || propagationchoice == 4)
				Propagate(vars, scope);
			ArrayList<String> L_s = L.get(scope);
			
			System.out.println("L::"+L);
			
			ArrayList<Long> timesrelations =relationsForPair(L_s, scope, V);

			if (L_s.size() > 1) {
				if (verbose)
					System.out.println("Clause number " + i++ + " has been learned");
			} else if (L_s.size() == 1) {
				if (verbose)
					System.out.println("Constraint number " + i++ + " has been learned");
			}

			boolean nocollapse = true;
			if (propagate) {
				nocollapse = PathConsistency(timesrelations);
				propagate = false;
			}
			System.out.println(L);

			end = System.currentTimeMillis();
			String input = (nPositives + nNegatives) + "\t" + nPositives + "\t" + nNegatives + "\t"
					+ ((end - start) / 1000) + "\t";
			FileManager.printFile(input, "Tasks" + nb_vars / 2 + "_" + exp.getName() + "_" + sheuristic + ".csv");
			times.add((end - start));
		}
		// boolean nocollapse = PathConsistency();

		// System.out.println("Converage :: " + nocollapse);

	}

	public void process_random() {
		CombinationIterator iterator1 = new CombinationIterator(nb_vars, 2);
		List<int[]> variables = new ArrayList<int[]>();
		while (iterator1.hasNext()) {
			int[] vars = new int[2];
			vars = iterator1.next();

			if (vars[0] < vars[1]) {
				variables.add(new int[] { vars[0], vars[1] });
			}
		}
		/*
		 * variables.add(new int[] { 0, 3 }); variables.add(new int[] { 0, 2 });
		 * variables.add(new int[] { 0, 1 }); variables.add(new int[] { 1, 2 });
		 * variables.add(new int[] { 2, 3 });
		 */
		Collections.shuffle(variables);
		long end = System.currentTimeMillis();
		int i = 0;
		ArrayList<int[]> V = new ArrayList<int[]>();
		for (int[] vars : variables) {

			long start = System.currentTimeMillis();
			// V.add(vars);
			String scope = vars[0] + "," + vars[1];
			System.out.println(scope);
			ArrayList<String> L_s = L.get(scope);
			if (propagationchoice == 0 || propagationchoice == 3 || propagationchoice == 4)
				Propagate(vars, scope);
			ArrayList<Long> timesrelations =relationsForPair(L_s, scope, V);

			if (L_s.size() > 1) {
				if (verbose)
					System.out.println("Clause number " + i++ + " has been learned");
			} else if (L_s.size() == 1) {
				if (verbose)
					System.out.println("Constraint number " + i++ + " has been learned");
			}

			boolean nocollapse = true;
			if (propagate) {
				nocollapse = PathConsistency(timesrelations);
				propagate = false;
			}

			end = System.currentTimeMillis();
			String input = (nPositives + nNegatives) + "\t" + nPositives + "\t" + nNegatives + "\t" + ((end - start))
					+ "\t";
			FileManager.printFile(input, "Tasks" + nb_vars / 2 + "_" + exp.getName() + "_" + sheuristic + ".csv");
			times.add((end - start));
		}

		//boolean nocollapse = PathConsistency(timesrelations);

		//System.out.println("Converage :: " + nocollapse);

	}

	public void process_compositionweighted() {

		CombinationIterator iterator1 = new CombinationIterator(nb_vars, 2);
		List<int[]> variables = new ArrayList<int[]>();
		while (iterator1.hasNext()) {
			int[] vars = new int[2];
			vars = iterator1.next();

			if (vars[0] < vars[1]) {
				variables.add(new int[] { vars[0], vars[1] });
			}
		}
		// Collections.shuffle(variables);
		ACQ_SelectionHeuristic selection = new ACQ_SelectionHeuristic();
		long end = System.currentTimeMillis();
		int i = 0;
		ArrayList<int[]> V = new ArrayList<int[]>();

		while (!variables.isEmpty()) {
			long start = System.currentTimeMillis();

			int[] vars = selection.WeightedSelection(variables, L);

			String scope = vars[0] + "," + vars[1];
			variables.remove(vars);
			if (propagationchoice == 0 || propagationchoice == 3 || propagationchoice == 4)
				Propagate(vars, scope);
			// System.out.println(scope);
			ArrayList<String> L_s = L.get(scope);

			ArrayList<Long> timesrelations =relationsForPair(L_s, scope, V);

			if (L_s.size() > 1) {
				if (verbose)
					System.out.println("Clause number " + i++ + " has been learned");
			} else if (L_s.size() == 1) {
				if (verbose)
					System.out.println("Constraint number " + i++ + " has been learned");
			}
			boolean nocollapse = true;
			if (propagate) {
				nocollapse = PathConsistency(timesrelations);
				propagate = false;
			}

			// if(nocollapse==false){
			// System.exit(0);
//				}
			end = System.currentTimeMillis();
			String input = (nPositives + nNegatives) + "\t" + nPositives + "\t" + nNegatives + "\t"
					+ ((end - start) / 1000) + "\t";
			FileManager.printFile(input, "Tasks" + nb_vars / 2 + "_" + exp.getName() + "_" + sheuristic + ".csv");
			times.add((end - start));
		
		}
		
		//boolean nocollapse = PathConsistency();

		//System.out.println("Converage :: " + nocollapse);

	}

	public void process_compositionPath() {
		Supplier<String> vSupplier = new Supplier<String>() {
			private int id = 0;

			@Override
			public String get() {
				return "" + id++;
			}
		};
		Graph<String, DefaultEdge> completeGraph = new SimpleGraph<>(vSupplier,
				SupplierUtil.createDefaultEdgeSupplier(), false);
		CombinationIterator iterator1 = new CombinationIterator(nb_vars, 2);
		List<int[]> variables = new ArrayList<int[]>();
		while (iterator1.hasNext()) {
			int[] vars = new int[2];
			vars = iterator1.next();

			if (vars[0] < vars[1]) {
				variables.add(new int[] { vars[0], vars[1] });
				completeGraph.addVertex(vars[0] + "");
				completeGraph.addVertex(vars[1] + "");
				completeGraph.addEdge(vars[0] + "", vars[1] + "");
				completeGraph.addEdge(vars[1] + "", vars[0] + "");

			}
		}
		// Collections.shuffle(variables);
		ACQ_SelectionHeuristic selection = new ACQ_SelectionHeuristic();
		long end = System.currentTimeMillis();
		int i = 0;
		int[] vars = variables.get(0);
		ArrayList<int[]> V = new ArrayList<int[]>();

		while (!variables.isEmpty()) {
			long start = System.currentTimeMillis();

			vars = selection.PathHeuristic(completeGraph, vars, nb_vars);
			int idx = 0;
			for (int[] v : variables) {
				if (vars[0] == v[0] && vars[1] == v[1])
					idx = variables.indexOf(v);
			}
			if (vars[0] == 0 && vars[1] == 0)
				break;

			String scope = vars[0] + "," + vars[1];
			variables.remove(variables.get(idx));
			System.out.println(scope);
			long t =0;
			long t1 =0;
			if (propagationchoice == 0 || propagationchoice == 3 || propagationchoice == 4 || propagationchoice == 5) {
			
				t = System.currentTimeMillis();
				Propagate(vars, scope);
				t1= System.currentTimeMillis();
			
			}

			ArrayList<String> L_s = L.get(scope);
			ArrayList<Long> relationtimes=relationsForPair(L_s, scope, V);
			
			relationtimes.set(0, relationtimes.get(0)+(t1-t));
			
			
			if (L_s.size() > 1) {
				if (verbose)
					System.out.println("Clause number " + i++ + " has been learned");
			} else if (L_s.size() == 1) {
				if (verbose)
					System.out.println("Constraint number " + i++ + " has been learned");
			}
			boolean nocollapse = true;
			if (propagate) {
				nocollapse = PathConsistency(relationtimes);
				propagate = false;
				
			}

			end = System.currentTimeMillis();
		/*	String input = (nPositives + nNegatives) + "\t" + nPositives + "\t" + nNegatives + "\t"
					+ ((end - start) / 1000) + "\t";
			FileManager.printFile(input, "Tasks" + nb_vars / 2 + "_" + exp.getName() + "_" + sheuristic + ".csv");
			*/
			for(long time : relationtimes)
				times.add(time);
		}

		//boolean nocollapse = PathConsistency(null);

		//System.out.println("Converage :: " + nocollapse);

	}

	private void Propagate(int[] vars, String scope) {

		// Remove by duration
		if (durations[vars[0]] > durations[vars[1]]) {
			L.get(scope).remove("FinishXY");
			L.get(scope).remove("StartsXY");
			L.get(scope).remove("DuringXY");
			L.get(scope).remove("ExactXY");
			if (durations[vars[0]] - durations[vars[1]] == 1)
				L.get(scope).remove("ContainsXY");
			if (durations[vars[1]] == 1) {
				L.get(scope).remove("IsOverlappedXY");
			}

		}

		if (durations[vars[0]] < durations[vars[1]]) {
			L.get(scope).remove("IsFinishedXY");
			L.get(scope).remove("IsStartedXY");
			L.get(scope).remove("ContainsXY");
			L.get(scope).remove("ExactXY");
			if (durations[vars[1]] - durations[vars[0]] == 1)
				L.get(scope).remove("DuringXY");
			if (durations[vars[0]] == 1)
				L.get(scope).remove("OverlapsXY");
		}

		if (durations[vars[0]] == durations[vars[1]]) {
			L.get(scope).remove("FinishXY");
			L.get(scope).remove("IsFinishedXY");
			L.get(scope).remove("StartsXY");
			L.get(scope).remove("IsStartedXY");
			L.get(scope).remove("DuringXY");
			L.get(scope).remove("ContainsXY");
			if (durations[vars[0]] == 1 && durations[vars[1]] == 1) {
				L.get(scope).remove("OverlapsXY");
				L.get(scope).remove("IsOverlappedXY");
			}
		}

		// Remove by Resource
		if (exceedResource(vars)) {
			L.get(scope).remove("OverlapsXY");
			L.get(scope).remove("IsOverlappedXY");
			L.get(scope).remove("StartsXY");
			L.get(scope).remove("IsStartedXY");
			L.get(scope).remove("DuringXY");
			L.get(scope).remove("ContainsXY");
			L.get(scope).remove("FinishXY");
			L.get(scope).remove("IsFinishedXY");
			L.get(scope).remove("ExactXY");
		}
	}

	private boolean exceedResource(int[] vars) {
		for (int i = 0; i < nResource; i++) {
			if (requirements[i][vars[0]] + requirements[i][vars[1]] > capacities[i])
				return true;
		}
		return false;
	}

	protected ArrayList<Long> relationsForPair(ArrayList<String> L_s, String scope, ArrayList<int[]> V) {
		boolean removed = false;
		int[] pair = new int[] { Integer.parseInt(scope.split(",")[0]), Integer.parseInt(scope.split(",")[1]) };
		//HashMap<int[], ArrayList<String>> Q = getConstraintsComplement(V);
		HashMap<int[], ArrayList<String>> Q = getConstraints(V);
		ArrayList<Long> times = new ArrayList<Long>();
		long start = 0;
		long time =0;
		for (int i = 0; i < L_s.size(); i++) {
			String cst = L_s.get(i);
			start = System.currentTimeMillis();
			boolean e = generateSchedulingSolution(Q, V, cst, pair);
			time=System.currentTimeMillis();
			times.add((time-start));
			boolean answer = askTEMACQ(cst, scope);
			if (log_queries)
				FileManager.printFile(L_s.get(i), "TemporalConstraintsAsked");
			if(!answer)
				removed = true;
			if (!e || !answer) {
				//removed = true;
				L_s.remove(cst);
				for (String s : L.get(scope))
					if (s.equals(cst)) {
						L.get(scope).remove(s);
					}
				i--;
				if (e)
					nNegatives++;
				propagate = true;
			} else {
				nPositives++;
			}

		}
		if(removed)
			V.add(pair);//Put only if a relation is removed
		return times;
	}
	
	private HashMap<int[], ArrayList<String>> getConstraintsComplement(ArrayList<int[]> V) {
		HashMap<int[], ArrayList<String>> Q = new HashMap<int[], ArrayList<String>>();
		ArrayList<String> full = new ArrayList<String>();
		full.add("OverlapsXY");
		full.add("IsOverlappedXY");
		full.add("StartsXY");
		full.add("IsStartedXY");
		full.add("DuringXY");
		full.add("ContainsXY");
		full.add("FinishXY");
		full.add("IsFinishedXY");
		full.add("ExactXY");
		full.add("PrecedesXY");
		full.add("IsPrecededXY");
		full.add("MeetsXY");
		full.add("IsMetXY");

		for (int[] vars : V) {
			ArrayList<String> cst2 = new ArrayList<String>();
			String scope = vars[0] + "," + vars[1];
			ArrayList<String> cst = L.get(scope);
			for (String c : full) {
				if (!cst.contains(c))
					cst2.add(c);

			}
			Q.put(vars, cst2);

		}

		return Q;
	}

	private HashMap<int[], ArrayList<String>> getConstraints(ArrayList<int[]> V) {
		HashMap<int[], ArrayList<String>> Q = new HashMap<int[], ArrayList<String>>();


		for (int[] vars : V) {
			String scope = vars[0] + "," + vars[1];
			ArrayList<String> cst = L.get(scope);
			
			Q.put(vars, cst);

		}

		return Q;
	}

	
	private ArrayList<int[]> build(ArrayList<int[]> V) {
		for (int[] v : V) {
			for (int[] v1 : V) {

			}
		}

		return null;
	}

	public void setVerbose(boolean verbose) {

		this.verbose = verbose;
	}

	public void setPropagation(int prop) {

		this.propagationchoice = prop;
	}
	public void setDeadline(int deadline) {

		this.deadline = deadline;
	}
	public void setLog_queries(boolean logqueries) {

		this.log_queries = logqueries;
	}

	public class Tuple<X, Y> {
		public final X x;
		public final Y y;

		public Tuple(X x, Y y) {
			this.x = x;
			this.y = y;
		}
	}

	public boolean askTEMACQ(String e, String s) {
		if (target.get(s) == null)
			return false;
		if (target.get(s).contains(e)) {

			return true;
		}

		return false;

	}

	public boolean askTEMACQMultiple(ArrayList<String> e, ArrayList<String> s) {
		int i = 0;
		for (String scope : s) {
			if (target.get(scope) == null)
				return false;
			if (!target.get(scope).contains(e.get(i))) {

				return false;
			}
			i++;
		}

		return true;

	}

	public boolean generateSchedulingSolution(HashMap<int[], ArrayList<String>> Q, ArrayList<int[]> V, String relation,
			int[] pair) {
		if (propagationchoice == 1 || propagationchoice == 2 || propagationchoice == 3 || propagationchoice == 4
				|| propagationchoice == 5) {
			// The CP model
			Model model = new Model("RCPSP");

			// Variables
			IntVar[] X = model.intVarArray("Start", nTasks, 1, UB);

			Constraint cr = getRelation(X, model, relation, pair);
			model.post(cr);

			// Deadline constraint: To Do: put deadline as argument
			for (int i = 0; i < nTasks; i++) {
				model.arithm(X[i], "<=", deadline - 1 * durations[i]).post();
			}
			
			//Precedence constraints
			/*
			for (int i = 0; i < nTasks; i++) {
				ArrayList<Integer> prec = precedencies.get(i);

				for (int a : prec) {

					model.post(model.arithm(X[i], "-", X[a], "<=", -1 * durations[i]));

				}
			}
			*/
			
			// Cumulative constraints
			if (propagationchoice != 5) {

				for (int i = 0; i < nResource; i++) {
					model.cumulative(X, durations, requirements[i], capacities[i]);
				}
			}
			if (propagationchoice == 1 || propagationchoice == 3) {

				// Learned constraints
				for (int[] key : Q.keySet()) {
					ArrayList<String> c = Q.get(key);
					Constraint[] constraints = new Constraint[c.size()];
			        if(c.contains("PrecedesXY") || c.contains("MeetsXY"))
			        	model.post(model.arithm(X[key[0]], "-", X[key[1]], "<=", -1*durations[key[0]]));
			        else
			        	model.post(model.arithm(X[key[1]], "-", X[key[0]], "<=", -1*durations[key[1]]));
				     
					/*
					for (String cst : c) {
						// model.post(getRelation(X, model, cst, key).getOpposite());
						model.post(getRelationNegation(X, model, cst, key));
					}
					*/
					
					

				}
			}

			Solver solver = model.getSolver();

			// Select strategy of search:
			// solver.setLNS(INeighborFactory.random(X));
			// solver.setSearch(new DomOverWDeg(model.retrieveIntVars(false),
			// System.currentTimeMillis(),
			// new IntDomainRandom(System.currentTimeMillis())));

			// Set solving timeout
			 solver.limitTime(this.solver.getLimit());
			

			Solution s1 = solver.findSolution();
			solver.printShortStatistics();
			if(!solver.getSearchState().equals(solver.getSearchState().TERMINATED))
				return true;
			if (solver.getSolutionCount() == 0)
				FileManager.printFile("s1 ::" + relation + " :: " + Arrays.toString(pair) + " :: " + s1, "solving");
			if (s1 != null)
				return true;

			return false;
		} else {

			return true;
		}
	}

	private Constraint getRelationNegation(IntVar[] X, Model model, String relation, int[] pair) {
		// Allen's relations
		Constraint PrecedesXY = model.arithm(X[pair[0]], "-", X[pair[1]], ">=", -durations[pair[0]]);
		Constraint IsPrecededXY = model.arithm(X[pair[1]], "-", X[pair[0]], ">=", -durations[pair[1]]);
		Constraint MeetsXY = model.arithm(X[pair[0]], "-", X[pair[1]], "!=", -durations[pair[0]]);
		Constraint IsMetXY = model.arithm(X[pair[1]], "-", X[pair[0]], "!=", -durations[pair[1]]);
		Constraint OverlapsXY = model.or(model.arithm(X[pair[0]], ">=", X[pair[1]]),
				model.arithm(X[pair[1]], "-", X[pair[0]], ">=", durations[pair[0]]));
		Constraint IsOverlappedXY = model.or(model.arithm(X[pair[1]], ">=", X[pair[0]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], ">=", durations[pair[1]]));
		Constraint StartsXY = model.or(model.arithm(X[pair[0]], "!=", X[pair[1]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], ">=", durations[pair[1]] - durations[pair[0]]));
		Constraint IsStartedXY = model.or(model.arithm(X[pair[1]], "!=", X[pair[0]]),
				model.arithm(X[pair[1]], "-", X[pair[0]], ">=", durations[pair[0]] - durations[pair[1]]));
		Constraint DuringXY = model.or(model.arithm(X[pair[0]], "<=", X[pair[1]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], ">=", durations[pair[1]] - durations[pair[0]]));
		Constraint ContainsXY = model.or(model.arithm(X[pair[1]], "<=", X[pair[0]]),
				model.arithm(X[pair[1]], "-", X[pair[0]], ">=", durations[pair[0]] - durations[pair[1]]));
		Constraint FinishXY = model.or(
				model.arithm(X[pair[0]], "-", X[pair[1]], "!=", durations[pair[1]] - durations[pair[0]]),
				model.arithm(X[pair[0]], "<=", X[pair[1]]));
		Constraint IsFinishedXY = model.or(
				model.arithm(X[pair[1]], "-", X[pair[0]], "!=", durations[pair[0]] - durations[pair[1]]),
				model.arithm(X[pair[1]], "<=", X[pair[0]]));
		Constraint ExactXY = model.or(model.arithm(X[pair[0]], "!=", X[pair[1]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], "!=", durations[pair[1]] - durations[pair[0]]));

		switch (relation) {
		case "PrecedesXY":
			return PrecedesXY;

		case "IsPrecededXY":
			return IsPrecededXY;

		case "MeetsXY":
			return MeetsXY;
		case "IsMetXY":
			return IsMetXY;
		case "OverlapsXY":
			return OverlapsXY;
		case "IsOverlappedXY":
			return IsOverlappedXY;
		case "StartsXY":
			return StartsXY;
		case "IsStartedXY":
			return IsStartedXY;
		case "DuringXY":
			return DuringXY;

		case "ContainsXY":
			return ContainsXY;
		case "FinishXY":
			return FinishXY;
		case "IsFinishedXY":
			return IsFinishedXY;
		case "ExactXY":
			return ExactXY;

		}
		return null;
	}

	private Constraint getRelation(IntVar[] X, Model model, String relation, int[] pair) {
		// Allen's relations
		Constraint PrecedesXY = model.arithm(X[pair[0]], "-", X[pair[1]], "<", -durations[pair[0]]);
		Constraint IsPrecededXY = model.arithm(X[pair[1]], "-", X[pair[0]], "<", -durations[pair[1]]);
		Constraint MeetsXY = model.arithm(X[pair[0]], "-", X[pair[1]], "=", -durations[pair[0]]);
		Constraint IsMetXY = model.arithm(X[pair[1]], "-", X[pair[0]], "=", -durations[pair[1]]);
		Constraint OverlapsXY = model.and(model.arithm(X[pair[0]], "<", X[pair[1]]),
				model.arithm(X[pair[1]], "-", X[pair[0]], "<", durations[pair[0]]));
		Constraint IsOverlappedXY = model.and(model.arithm(X[pair[1]], "<", X[pair[0]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], "<", durations[pair[1]]));
		Constraint StartsXY = model.and(model.arithm(X[pair[0]], "=", X[pair[1]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], "<", durations[pair[1]] - durations[pair[0]]));
		Constraint IsStartedXY = model.and(model.arithm(X[pair[1]], "=", X[pair[0]]),
				model.arithm(X[pair[1]], "-", X[pair[0]], "<", durations[pair[0]] - durations[pair[1]]));
		Constraint DuringXY = model.and(model.arithm(X[pair[0]], ">", X[pair[1]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], "<", durations[pair[1]] - durations[pair[0]]));
		Constraint ContainsXY = model.and(model.arithm(X[pair[1]], ">", X[pair[0]]),
				model.arithm(X[pair[1]], "-", X[pair[0]], "<", durations[pair[0]] - durations[pair[1]]));
		Constraint FinishXY = model.and(
				model.arithm(X[pair[0]], "-", X[pair[1]], "=", durations[pair[1]] - durations[pair[0]]),
				model.arithm(X[pair[0]], ">", X[pair[1]]));
		Constraint IsFinishedXY = model.and(
				model.arithm(X[pair[1]], "-", X[pair[0]], "=", durations[pair[0]] - durations[pair[1]]),
				model.arithm(X[pair[1]], ">", X[pair[0]]));
		Constraint ExactXY = model.and(model.arithm(X[pair[0]], "=", X[pair[1]]),
				model.arithm(X[pair[0]], "-", X[pair[1]], "=", durations[pair[1]] - durations[pair[0]]));

		switch (relation) {
		case "PrecedesXY":
			return PrecedesXY;

		case "IsPrecededXY":
			return IsPrecededXY;

		case "MeetsXY":
			return MeetsXY;
		case "IsMetXY":
			return IsMetXY;
		case "OverlapsXY":
			return OverlapsXY;
		case "IsOverlappedXY":
			return IsOverlappedXY;
		case "StartsXY":
			return StartsXY;
		case "IsStartedXY":
			return IsStartedXY;
		case "DuringXY":
			return DuringXY;

		case "ContainsXY":
			return ContainsXY;
		case "FinishXY":
			return FinishXY;
		case "IsFinishedXY":
			return IsFinishedXY;
		case "ExactXY":
			return ExactXY;

		}
		return null;
	}

	public static int[] convertIntegers(List<Integer> integers) {
		int[] ret = new int[integers.size()];
		Iterator<Integer> iterator = integers.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iterator.next().intValue();
		}
		return ret;
	}

	public ArrayList<String> Composition(ArrayList<String> parentsList, String child) {

		HashMap<String, String> mapping = new HashMap<String, String>();
		mapping.put("PrecedesXY", "p");
		mapping.put("IsPrecededXY", "P");
		mapping.put("MeetsXY", "m");
		mapping.put("IsMetXY", "M");
		mapping.put("OverlapsXY", "o");
		mapping.put("IsOverlappedXY", "O");
		mapping.put("StartsXY", "s");
		mapping.put("IsStartedXY", "S");
		mapping.put("DuringXY", "d");
		mapping.put("ContainsXY", "D");
		mapping.put("FinishXY", "f");
		mapping.put("IsFinishedXY", "F");
		mapping.put("ExactXY", "e");
		mapping.put("oFDseSdfO", "concur");
		mapping.put("pmoFDseSdfOMP", "full");

		HashMap<String, String> mapping1 = new HashMap<String, String>();
		mapping1.put("p", "PrecedesXY");
		mapping1.put("P", "IsPrecededXY");
		mapping1.put("m", "MeetsXY");
		mapping1.put("M", "IsMetXY");
		mapping1.put("o", "OverlapsXY");
		mapping1.put("O", "IsOverlappedXY");
		mapping1.put("s", "StartsXY");
		mapping1.put("S", "IsStartedXY");
		mapping1.put("d", "DuringXY");
		mapping1.put("D", "ContainsXY");
		mapping1.put("f", "FinishXY");
		mapping1.put("F", "IsFinishedXY");
		mapping1.put("e", "ExactXY");
		HashMap<String, String> inverse = new HashMap<String, String>();
		inverse.put("PrecedesXY", "IsPrecededXY");
		inverse.put("IsPrecededXY", "PrecedesXY");
		inverse.put("MeetsXY", "IsMetXY");
		inverse.put("IsMetXY", "MeetsXY");
		inverse.put("OverlapsXY", "IsOverlappedXY");
		inverse.put("IsOverlappedXY", "OverlapsXY");
		inverse.put("StartsXY", "IsStartedXY");
		inverse.put("IsStartedXY", "StartsXY");
		inverse.put("DuringXY", "ContainsXY");
		inverse.put("ContainsXY", "DuringXY");
		inverse.put("FinishXY", "IsFinishedXY");
		inverse.put("IsFinishedXY", "FinishXY");
		inverse.put("ExactXY", "ExactXY");
		
		HashMap<String, String> table = FileManager.parseCompTable();

		ArrayList<String> set = new ArrayList<String>();

		int[] scope = new int[] { Integer.parseInt(parentsList.get(0).split(",")[0]),
				Integer.parseInt(parentsList.get(0).split(",")[1]) };
		int[] scope1 = new int[] { Integer.parseInt(parentsList.get(1).split(",")[0]),
				Integer.parseInt(parentsList.get(1).split(",")[1]) };
		ArrayList<String> network1 = null;
		ArrayList<String> network2 = null;

		if (scope[0] > scope[1]) {
			String temp = scope[1] + "," + scope[0];
			network1 = L.get(temp);

		} else {
			network1 = L.get(parentsList.get(0));

		}
		if (scope1[0] > scope1[1]) {
			String temp = scope1[1] + "," + scope1[0];
			network2 = L.get(temp);

		} else {
			network2 = L.get(parentsList.get(1));

		}

		// ACQ_Network network1_ = new ACQ_Network(new ConstraintFactory(),
		// network1.getVariables());
		// ACQ_Network network2_ = new ACQ_Network(new ConstraintFactory(),
		// network2.getVariables());
		String[] network1_ = new String[network1.size()];
		String[] network2_ = new String[network2.size()];
		// 4 0 0 2

		if (scope[0] > scope[1]) {
			int i = 0;
			for (String c : network1) {
				// System.out.println(c+" :: "+c.getInverse());
				network1_[i] = inverse.get(c);
				// network1_.add(c.getInverse(), true);
				i++;
			}
		} else {
			int i = 0;
			for (String c : network1) {
				network1_[i] = c;
				i++;
				// network2_.add(c.getInverse(), true);
			}
		}
		if (scope1[0] > scope1[1]) {
			int i = 0;
			for (String c : network2) {

				network2_[i] = inverse.get(c);
				i++;
				// network2_.add(c.getInverse(), true);
			}
		} else {
			int i = 0;
			for (String c : network2) {
				network2_[i] = c;
				i++;
				// network2_.add(c.getInverse(), true);
			}
		}

		if (network1_.length == 13) {
			for (String c : network1_)
				set.add(c);
			return set;
		}
		if (network2_.length == 13) {
			for (String c : network2_)
				set.add(c);
			return set;
		}
		ArrayList<String> finalset = new ArrayList<String>();
		for (String c : network1_) {

			for (String c1 : network2_) {

				ComputeComposition(c, c1, table, mapping, mapping1, child, finalset);

			}

		}

		return finalset;

	}

	public boolean PathConsistency(ArrayList<Long> relationtimes) {

		ArrayList<int[]> Q = InitPropagationQueue();
		int i = 0;
		long end = System.currentTimeMillis();
		long elapsed = 0;
		int removed= 0;
		while (!Q.isEmpty()) {
			long start = System.currentTimeMillis();

			int[] C_ikj = Q.get(i);
			String scope_ij = C_ikj[0] + "," + C_ikj[2];

			ArrayList<String> parentsList = new ArrayList<String>();
			parentsList.add(C_ikj[0] + "," + C_ikj[1]);
			parentsList.add(C_ikj[1] + "," + C_ikj[2]);

			ArrayList<String> c_ij = L.get(scope_ij);
			// System.out.println(c_ij);
			ArrayList<String> composition = Composition(parentsList, scope_ij);

			ArrayList<ArrayList<String>> intersection = new ArrayList<ArrayList<String>>();
			intersection.add(c_ij);
			intersection.add(composition);
			if (isSubSet(c_ij, composition) == false) {
				ArrayList<String> intersect = Intersection(intersection);
				removed = removed + (L.get(scope_ij).size()-intersect.size());

				L.put(scope_ij, intersect);
			
				if (intersect.size() == 0) {
					FileManager.printFile(removed, "PC_Removed_"+instance);
					return false;
				}
				if (C_ikj[0] < C_ikj[1])
					Q.add(new int[] { C_ikj[0], C_ikj[2], C_ikj[1] });
				else
					Q.add(new int[] { C_ikj[1], C_ikj[2], C_ikj[0] });
				if (C_ikj[1] < C_ikj[2])
					Q.add(new int[] { C_ikj[1], C_ikj[0], C_ikj[2] });
				else
					Q.add(new int[] { C_ikj[2], C_ikj[0], C_ikj[1] });

			}

			Q.remove(C_ikj);
			end = System.currentTimeMillis();
			elapsed = end - start;
			relationtimes.set(relationtimes.size()-1, relationtimes.get(relationtimes.size()-1)+(elapsed));

				if (relationtimes.get(relationtimes.size()-1) >= solver.getLimit()) {
					FileManager.printFile(removed+"\t"+relationtimes.get(relationtimes.size()-1)+(elapsed), "PC_Removed_"+exp.getName()+sheuristic);
				return false;
				
			}


		}
		FileManager.printFile(removed+"\t"+relationtimes.get(relationtimes.size()-1)+(elapsed), "PC_Removed_"+exp.getName()+sheuristic);

		return true;
	}

	private boolean isSubSet(ArrayList<String> c_ij, ArrayList<String> composition) {

		int c = 0;

		for (String cst : c_ij) {
			for (String cst1 : composition) {
				if (cst.equals(cst1))
					c++;
			}

		}
		if (c == c_ij.size())
			return true;

		return false;
	}

	private ArrayList<int[]> InitPropagationQueue() {
		CombinationIterator iterator1 = new CombinationIterator(nb_vars, 2);

		ArrayList<int[]> Q = new ArrayList<int[]>();
		while (iterator1.hasNext()) {
			int[] vars = iterator1.next();

			if (vars[0] < vars[1]) {
				for (int i = 0; i < (nb_vars); i++)
					if (i != vars[0] && i != vars[1])
						Q.add(new int[] { vars[0], i, vars[1] });

			}
		}

		return Q;
	}

	private ArrayList<String> Intersection(ArrayList<ArrayList<String>> finalset) {
		ArrayList<String> set = new ArrayList<String>();
		for (String c : finalset.get(0))
			for (String c1 : finalset.get(1))

				if (c.equals(c1))
					set.add(c);

		return set;
	}

	private void ComputeComposition(String id, String id1, HashMap<String, String> table,
			HashMap<String, String> mapping, HashMap<String, String> mapping1, String child, ArrayList<String> set) {
		char[] result = table.get(mapping.get(id) + " - " + mapping.get(id1)).toCharArray();
		// System.out.println(mapping.get(id) + " - " + mapping.get(id1)+"::"+
		// String.valueOf(result));
		for (int i = 0; i < result.length; i++) {
			ArrayList<String> cst = new ArrayList<String>(
					Arrays.asList(mapping1.get(result[i] + ""), child.split(",")[0] + "", child.split(",")[1] + ""));

			ACQ_IConstraint c = CstrFactory.getConstraint(cst);
			if (!set.contains(c.getName()))
				set.add(c.getName());
			if (set.size() == 13)
				return;
		}

	}

}
