package fr.lirmm.coconut.acquisition.expe;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

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

public class ExpeMeetings extends DefaultExperience {

	// Heuristic heuristic = Heuristic.SOL;
	// ACQ_ConstraintSolver solver;
	// ACQ_Bias bias;
	// private final ACQ_Learner learner;

	private static boolean gui = false;
	private static boolean parallel = true;
	private static int inst = 27;

	public ExpeMeetings() {

	}

	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			}

			@Override
			public int getMax(int numvar) {
				return timeslots;
			}
		}, vrs, vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {

				int meetings[] = new int[nMeetings];
				for (int i = 0; i < meetings.length; i++)
					meetings[i] = -1;
				for (int numvar : e.getScope())
					meetings[numvar] = e.getValue(numvar);

				for (int i = 0; i < mAgents; i++) {

					for (int m1 = 0; m1 < nMeetings - 1; m1++) {
						for (int m2 = m1 + 1; m2 < nMeetings; m2++) {
							if (meetings[m1] > -1 && meetings[m2] > -1) {

								if (attendance[i][m1] == 1 && attendance[i][m2] == 1) {

									if (((Math.abs(meetings[m1] - meetings[m2]) <= distance[m1][m2]))) {
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

	public ACQ_Learner createDistLearner(int id) {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {

				int meetings[] = new int[nMeetings];
				for (int i = 0; i < meetings.length; i++)
					meetings[i] = -1;

				for (int numvar : e.getScope())
					meetings[numvar] = e.getValue(numvar);
				List<Integer> meeting = agents.get(id);

				for (int m1 = 0; m1 < meeting.size() - 1; m1++) {
					for (int m2 = m1 + 1; m2 < meeting.size(); m2++) {
						if (meetings[meeting.get(m1)] > -1 && meetings[meeting.get(m2)] > -1) {

							if ((((Math.abs(meetings[meeting.get(m1)]
									- meetings[meeting.get(m2)]) <= distance[meeting.get(m1)][meeting.get(m2)])))) {
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
		int NB_VARIABLE = nMeetings;
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
				if (vars[pos[0]] < vars[pos[1]]) {

					// abs(X-Y) == dist
					constraints.add(new BinaryArithmetic("AT_Equal", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.EQ, distance[vars[pos[0]]][vars[pos[1]]], "AT_Diff"));

					// abs(X-Y) != dist
					constraints.add(new BinaryArithmetic("AT_Diff", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.NEQ, distance[vars[pos[0]]][vars[pos[1]]], "AT_Equal"));

					// abs(X-Y) > dist
					constraints.add(new BinaryArithmetic("AT_GT", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.GT, distance[vars[pos[0]]][vars[pos[1]]], "AT_LE"));

					// abs(X-Y) < dist
					constraints.add(new BinaryArithmetic("AT_LT", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.LT, distance[vars[pos[0]]][vars[pos[1]]], "AT_GE"));
					// abs(X-Y) >= dist
					constraints.add(new BinaryArithmetic("AT_GE", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.GT, distance[vars[pos[0]]][vars[pos[1]]] - 1, "AT_LT"));

					// abs(X-Y) =< dist
					constraints.add(new BinaryArithmetic("AT_LE", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.LT, distance[vars[pos[0]]][vars[pos[1]]] + 1, "AT_GT"));

					// NL: distance constraint in Choco does not take into account op={=<,>=}
				}

			}
		}

		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);

		return new ACQ_Bias(network);
	}

	public ArrayList<ACQ_Bias> createDistBias() {
		ArrayList<ACQ_Bias> biases = new ArrayList<>();

		ConstraintFactory constraintFactory = new ConstraintFactory();
		// build binary Constraints
		for (List<Integer> s : agents.values()) {
			ACQ_Scope allVarSet = new ACQ_Scope(s);

			ConstraintSet constraints = constraintFactory.createSet();

			Integer[] vars = s.toArray(new Integer[s.size()]);
			AllPermutationIterator pIterator = new AllPermutationIterator(vars.length);
			while (pIterator.hasNext()) {
				int[] pos = pIterator.next();

				if (vars[pos[0]] < vars[pos[1]]) {

					// abs(X-Y) == dist
					constraints.add(new BinaryArithmetic("AT_Equal", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.EQ, distance[vars[pos[0]]][vars[pos[1]]], "AT_Diff"));

					// abs(X-Y) != dist
					constraints.add(new BinaryArithmetic("AT_Diff", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.NEQ, distance[vars[pos[0]]][vars[pos[1]]], "AT_Equal"));

					// abs(X-Y) > dist
					constraints.add(new BinaryArithmetic("AT_GT", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.GT, distance[vars[pos[0]]][vars[pos[1]]], "AT_LE"));

					// abs(X-Y) < dist
					constraints.add(new BinaryArithmetic("AT_LT", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.LT, distance[vars[pos[0]]][vars[pos[1]]], "AT_GE"));

					// abs(X-Y) >= dist
					constraints.add(new BinaryArithmetic("AT_GE", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.GT, distance[vars[pos[0]]][vars[pos[1]]] - 1, "AT_LT"));

					// abs(X-Y) =< dist
					constraints.add(new BinaryArithmetic("AT_LE", vars[pos[0]], Operator.Dist, vars[pos[1]],
							Operator.LT, distance[vars[pos[0]]][vars[pos[1]]] + 1, "AT_GT"));

					// NL: distance constraint in Choco does not take into account op={=<,>=}
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
				// X < Y
				constraints
						.add(new BinaryArithmetic("LessXY", vars[pos[0]], Operator.LT, vars[pos[1]], "GreaterEqualXY"));
				// X > Y
				constraints
						.add(new BinaryArithmetic("GreaterXY", vars[pos[0]], Operator.GT, vars[pos[1]], "LessEqualXY"));

			}
			ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);

			biases.add(new ACQ_Bias(network));

		}

		return biases;
	}

	@Override
	public void process() {
		directory = new File("./Meetings/");
		directory = new File("src/fr/lirmm/coconut/quacq/bench/Meetings/");

		switch (this.getAlgo()) {
		case QUACQ:
			ACQ_WS.instance = this.getInstance();
			ACQ_WS.executeExperience(this);
			break;
		case CONACQ1:
			ACQ_WS.instance = this.getInstance();
			ACQ_WS.executeConacqV1Experience(this);
			break;
		case CONACQ2:
			ACQ_WS.instance = this.getInstance();
			ACQ_WS.executeConacqV2Experience(this);
			break;
		default:		//PACQ version
			ACQ_WS.instance = this.getInstance();
			ACQ_WS.executeExperience(this, this.getAlgo(), this.getNb_threads(), this.getPartition());

			break;

		}

	}

	public void setDirectory(File directory) {
		this.directory = directory;
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
