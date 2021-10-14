/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.expe;

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
import fr.lirmm.coconut.acquisition.core.acqconstraint.UnaryArithmetic;
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

/**
 *
 * @author NASSIM
 */
public class ExpeZebra extends DefaultExperience {

	// Heuristic heuristic = Heuristic.SOL;
	// ACQ_ConstraintSolver solver;
	// ACQ_Bias bias;
	// private final ACQ_Learner learner;
	private static int nb_houses = 5; // m
	private static boolean gui = true;


	public ExpeZebra() {
	}

	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			}

			@Override
			public int getMax(int numvar) {
				return nb_houses;
			}
		}, vrs, vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {

				if (e.getScope().size() < 25)
					return partial_ask(e);

				return MQ_ask(e);

			}

			private boolean partial_ask(ACQ_Query e) {

				int vars[] = new int[nb_houses * nb_houses];
				for (int numvar : e.getScope()) {
					vars[numvar] = e.getValue(numvar);

				}

				int ukr = vars[0];
				int norge = vars[1];
				int eng = vars[2];
				int spain = vars[3];
				int jap = vars[4];

				int red = vars[5];
				int blue = vars[6];
				int yellow = vars[7];
				int green = vars[8];
				int ivory = vars[9];

				int oldGold = vars[10];
				int parly = vars[11];
				int kools = vars[12];
				int lucky = vars[13];
				int chest = vars[14];

				int zebra = vars[15];
				int dog = vars[16];
				int horse = vars[17];
				int fox = vars[18];
				int snails = vars[19];

				int coffee = vars[20];
				int tea = vars[21];
				int h2o = vars[22];
				int milk = vars[23];
				int oj = vars[24];

				if (eng > 0 && red > 0 && eng != red) { // Constraint:: eng == red (vars[2]==vars[5]
					e.classify(false);
					return false;
				} // 2. the Englishman lives in the red house
				if (spain > 0 && dog > 0 && spain != dog) { // constraint:: spain == dog (vars[3]==vars[16])
					e.classify(false);
					return false; // 3. the Spaniard owns a dog
				}
				if (coffee > 0 && green > 0 && coffee != green) { // constraint:: coffee == green (vars[20]==vars[8])
					e.classify(false);
					return false; // 4. coffee is drunk in the green house
				}
				if (ukr > 0 && tea > 0 && ukr != tea) { // constraint:: ukr == tea (vars[0]==vars[21])
					e.classify(false);
					return false; // 5. the Ukr drinks tea
				}
				if (ivory > 0 && green > 0 && ivory + 1 != green) { // constraint:: ivory == green (vars[9]==vars[8])
					e.classify(false);
					return false; // 6. green house is to right of ivory house
				}
				if (oldGold > 0 && snails > 0 && oldGold != snails) { // constraint:: oldGold == snails
																		// (vars[10]==vars[19])
					e.classify(false);
					return false; // 7. oldGold smoker owns snails
				}
				if (kools > 0 && yellow > 0 && kools != yellow) { // constraint:: kools == yellow (vars[12]==vars[7])
					e.classify(false);
					return false; // 8. kools are smoked in the yellow house
				}
				if (milk > 0 && milk != 3) { // constraint:: milk == 3 (vars[23]==3)
					e.classify(false);
					return false; // 9. milk is drunk in the middle house
				}
				if (norge > 0 && norge != 1) { // constraint:: norge == 1 (vars[1]==1)
					e.classify(false);
					return false; // 10. Norwegian lives in first house on the left
				}
				if (chest > 0 && fox > 0 && Math.abs(chest - fox) != 1) { // constraint:: chest == fox+1
																			// (vars[14]==vars[18]+1)
					e.classify(false);
					return false; // 11. chesterfield smoker lives next door to the fox owner
				}
				if (kools > 0 && horse > 0 && Math.abs(kools - horse) != 1) { // constraint:: kools == horse+1
																				// (vars[12]==vars[17]+1)
					e.classify(false);
					return false; // 12. kools smoker lives next door to the horse owner
				}
				if (lucky > 0 && oj > 0 && lucky != oj) { // constraint:: lucky == oj (vars[13]==vars[24])
					e.classify(false);
					return false; // 13. lucky smoker drinks orange juice
				}
				if (jap > 0 && parly > 0 && jap != parly) { // constraint:: jap == parly (vars[4]==vars[11])
					e.classify(false);
					return false; // 14. Japanese smokes parliament
				}
				if (norge > 0 && blue > 0 && Math.abs(blue - norge) != 1) { // constraint:: norge == blue+1
																			// (vars[1]==vars[6]+1)
					e.classify(false);
					return false; // 15. Norwegian lives next to the blue house
				}

				for (int i = 0; i < nb_houses - 1; i++) {
					for (int j = i + 1; j < nb_houses; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}
					}
				}
				for (int i = nb_houses; i < nb_houses * 2 - 1; i++) {
					for (int j = i + 1; j < nb_houses * 2; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}
					}
				}
				for (int i = nb_houses * 2; i < nb_houses * 3 - 1; i++) {
					for (int j = i + 1; j < nb_houses * 3; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}
					}
				}

				for (int i = nb_houses * 3; i < nb_houses * 4 - 1; i++) {
					for (int j = i + 1; j < nb_houses * 4; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}
					}

				}

				for (int i = nb_houses * 4; i < nb_houses * 5 - 1; i++) {
					for (int j = i + 1; j < nb_houses * 5; j++) {
						if ((vars[i] > 0 && vars[j] > 0) && vars[i] == vars[j]) {
							e.classify(false);
							return false;
						}

					}
				}

				e.classify(true);

				return true;

			}

			private boolean MQ_ask(ACQ_Query e) {

				int[] s = new int[] { 2, 1, 3, 4, 5, 3, 2, 1, 5, 4, 3, 5, 1, 4, 2, 5, 4, 2, 1, 3, 5, 2, 1, 3, 4 };

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
		int NB_VARIABLE = nb_houses * nb_houses;

		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		ConstraintFactory constraintFactory = new ConstraintFactory();
		// build binary Constraints
		ConstraintSet constraints = constraintFactory.createSet();
		// Unary Constraints
		for (int i = 0; i < nb_houses * nb_houses; i++) {
			for (int j = 1; j <= nb_houses; j++) {

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
				// X-Y != 1
				constraints.add(new BinaryArithmetic("DistDiffXY", vars[pos[0]], Operator.Dist, vars[pos[1]],
						Operator.NEQ, 1, "DistEqXY"));
				// X-Y = 1
				constraints.add(new BinaryArithmetic("DistEqXY", vars[pos[0]], Operator.Dist, vars[pos[1]], Operator.EQ,
						1, "DistDiffXY"));

			}
		}

		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);

		return new ACQ_Bias(network);
	}

	public static void main(String args[]) {
	//	ExpeZebra.process(args);
	}
	public  void process(String args[]) {

		ACQ_Algorithm mode = ACQ_Algorithm.PACQ;

		int index = 0;
		if (args.length != 0) {
			if (!args[0].equals("mono") && !args[0].equals("port"))
				index = 1;

			if (args[index].equals("mono")) {
				mode = ACQ_Algorithm.QUACQ;
			} else {
				mode = ACQ_Algorithm.PACQ;
				nb_threads = Integer.parseInt(args[index + 1]);
			}
		}

		ExpeZebra expe = new ExpeZebra();
		expe.setParams(true, // normalized csp
				false, // shuffle_split,
				500, // timeout,
				ACQ_Heuristic.SOL, // heuristic
				false,false
		);
		if(gui)
			GUI_Utils.executeExperience(mode,expe, nb_threads);
		else
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

	public static void process(ACQ_Algorithm mode, boolean shuffle, boolean normalizedCSP, long timeout,
			ACQ_Heuristic heuristic, int nb_threads, ACQ_Partition partition,String vrs_, String vls_, boolean verbose, boolean log_queries) throws IOException {
				vrs = vrs_;
		vls = vls_;
		
		ExpeZebra expe = new ExpeZebra();
		expe.setParams(normalizedCSP, // normalized csp
				shuffle, // shuffle_split,
				timeout,
				heuristic,
				verbose,log_queries
		);

		switch (mode) {
		case QUACQ:
			ACQ_WS.executeExperience(expe);
			break;
		default:
			ACQ_WS.executeExperience(expe, mode, nb_threads, partition);
			break;

		}

	}
	@Override
	public void process() {
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
		return new ACQ_Network();
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
