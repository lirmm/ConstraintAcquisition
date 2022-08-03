/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GEQCA;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_DisjunctionConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Relation;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_TemporalVariable;
import fr.lirmm.coconut.acquisition.core.acqconstraint.BinaryArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintMapping;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ContradictionSet;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Operator;
import fr.lirmm.coconut.acquisition.core.acqconstraint.OverlapArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.OverlapConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ScalarArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.TemporalArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.TemporalConstraint;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ChocoSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_IDomain;
import fr.lirmm.coconut.acquisition.core.acqsolver.SATSolver;
import fr.lirmm.coconut.acquisition.core.combinatorial.AllPermutationIterator;
import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

/*****************
 * 
 * @author NASSIM
 *
 */

public class ExpeFromParser extends DefaultExperience {

	private static int nb_mark = 4; // NL:: add instance parameter for golomb
	private static int nb_dist = (nb_mark * (nb_mark - 1)) / 2; // m * (m - 1))
																// / 2
	private static boolean gui = false;

	private ExpeParser exp;

	public ExpeFromParser(ExpeParser exp) {

		this.exp = exp;
	}

	public String getInstanceName() {
		return exp.getInstanceName();
	}

	public int getMinDom() {
		return exp.getMinDom();
	}

	public int getMaxDom() {
		return exp.getMaxDom();
	}

	public int getNbVars() {
		return exp.getNbVars();
	}

//	static String vls = ValSelector.IntDomainRandom.toString();
//	static String vrs = VarSelector.RandomVar.toString();

	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return exp.getMinDom();
			}

			@Override
			public int getMax(int numvar) {
				return exp.getMaxDom();
			}
		}, vrs, vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {

			@Override
			public ACQ_Network buildTargetNetwork() {
				int NB_VARIABLE = exp.getNbVars();
				// build All variables set
				BitSet bs = new BitSet();
				bs.set(0, NB_VARIABLE);

				ACQ_Scope allVarSet = new ACQ_Scope(bs);

				ConstraintFactory constraintFactory = new ConstraintFactory();

				ConstraintSet constraints = constraintFactory.createSet();

				ACQ_Network tn = new ACQ_Network(constraintFactory, allVarSet, constraints);

				for (ArrayList<String> cst : exp.getTN()) {
					if (cst.size() <= 3) {
						tn.add(ACQ_Constraint.CstrFactory.getConstraint(cst), true);
					} else {
						ConstraintFactory cf = new ConstraintFactory();

						ConstraintSet cs = cf.createSet();
						ArrayList<String> data = new ArrayList<String>(cst.subList(cst.size() - 2, cst.size()));
						ArrayList<String> csts = new ArrayList<String>(cst.subList(0, cst.size() - 2));
						for (int i = 0; i < csts.size(); i++) {
							ArrayList<String> constraint = new ArrayList<String>();
							constraint.add(csts.get(i));
							constraint.addAll(data);
							cs.add(ACQ_Constraint.CstrFactory.getConstraint(constraint));

						}
						tn.add(new ACQ_DisjunctionConstraint(cs), true);
					}

				}
				return tn;

			}

			@Override
			public boolean ask(ACQ_IConstraint e) {
				this.setTargetNetwork();
				boolean answer = false;
				answer = askTEMACQ(e, this.getTargetNetwork());

				return answer;
			}
		};

	}

	public ACQ_Bias createBias() {

		int NB_VARIABLE = exp.getNbVars();
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);

		ACQ_Scope allVarSet = new ACQ_Scope(bs);

		ConstraintFactory constraintFactory = new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();
		constraints = (convert(exp));

		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);
		System.out.println("Bias size = " + network.size());

		return new ACQ_Bias(network);
	}

	private ConstraintSet convert(ExpeParser experiment) {
		ConstraintFactory cf = new ConstraintFactory();
		ConstraintSet constraints = cf.createSet();
		HashMap<String, ArrayList<ACQ_IConstraint>> mapping = new HashMap<String, ArrayList<ACQ_IConstraint>>();

		for (ArrayList<String> c : experiment.getBias()) {
			constraints.add(ACQ_Constraint.CstrFactory.getConstraint(c));
		}
		// For Disjunction constraints
		ConstraintFactory cfs = new ConstraintFactory();

		CombinationIterator iterator1 = new CombinationIterator(experiment.getNbVars(), 2);

		while (iterator1.hasNext()) {
			int[] vars = iterator1.next();

			AllPermutationIterator pIterator = new AllPermutationIterator(2);
			while (pIterator.hasNext()) {

				int[] pos = pIterator.next();
				if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

				if (vars[pos[0]] < vars[pos[1]]) {
					ArrayList<ACQ_IConstraint> csts = new ArrayList<ACQ_IConstraint>();

					mapping.put(vars[pos[0]] + "-" + vars[pos[1]], csts);

				}}

			}

		}

//===================================================
		for (ArrayList<String> r : experiment.getGamma()) {
			ACQ_Relation rel = ACQ_Relation.valueOf(r.get(0));

			CombinationIterator iterator = new CombinationIterator(experiment.getNbVars(), rel.getArity());

			while (iterator.hasNext()) {
				int[] vars = iterator.next();
				AllPermutationIterator pIterator = new AllPermutationIterator(rel.getArity());

				// Binary constraints
				if (rel.getArity() == 2 && !rel.IsAllen()) {
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();

						if (rel.isSymmetric() && vars[pos[0]] < vars[pos[1]]) {
							constraints.add(new BinaryArithmetic(rel.name(), vars[pos[0]], rel.getOperator(),
									vars[pos[1]], rel.getNegation().name()));

						} else {
							constraints.add(new BinaryArithmetic(rel.name(), vars[pos[0]], rel.getOperator(),
									vars[pos[1]], rel.getNegation().name()));

						}
					}
				}
				if (rel.IsAllen()) {
					ConstraintFactory constraintFactory = new ConstraintFactory();

					// pIterator = new AllPermutationIterator(rel.getArity());
					switch (r.get(0)) {
					case "PrecedesXY":
						while (pIterator.hasNext()) {

							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {
								if (vars[pos[0]] < vars[pos[1]]) {
									ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
											vars[pos[0]] + 1);
									ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
											vars[pos[1]] + 1);

									constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.LT,
											variable2, false, Operator.NONE, "NotPrecedesXY",false));
									mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
											.add(new TemporalArithmetic(rel.name(), variable1, Operator.LT, variable2,
													false, Operator.NONE, "NotPrecedesXY",false));
								}
							}
						}
						break;
					case "IsPrecededXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

								if (vars[pos[0]] < vars[pos[1]]) {
									ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
											vars[pos[0]] + 1);
									ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
											vars[pos[1]] + 1);

									constraints.add(new TemporalArithmetic(rel.name(), variable2, Operator.LT,
											variable1, false, Operator.NONE, "IsNotPrecededXY",true));
									mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
											.add(new TemporalArithmetic(rel.name(), variable2, Operator.LT, variable1,
													false, Operator.NONE, "IsNotPrecededXY",true));
								}
							}
						}
						break;

					case "MeetsXY":
						while (pIterator.hasNext()) {

							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {
								if (vars[pos[0]] < vars[pos[1]]) {
									ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
											vars[pos[0]] + 1);
									ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
											vars[pos[1]] + 1);

									constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ,
											variable2, false, Operator.NONE, "NotMeetXY",false));
									mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
											.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ, variable2,
													false, Operator.NONE, "NotMeetXY",false));
								}
							}
						}
						break;
					case "IsMetXY":
						while (pIterator.hasNext()) {

							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {
								if (vars[pos[0]] < vars[pos[1]]) {
									ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
											vars[pos[0]] + 1);
									ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
											vars[pos[1]] + 1);

									constraints.add(new TemporalArithmetic(rel.name(), variable2, Operator.EQ,
											variable1, false, Operator.NONE, "IsNotMetXY",true));
									mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
											.add(new TemporalArithmetic(rel.name(), variable2, Operator.EQ, variable1,
													false, Operator.NONE, "IsNotMetXY",true));
								}
							}
						}
						break;

					case "OverlapsXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new OverlapArithmetic(rel.name(), variable1, Operator.LT,
										variable2, "NotOverlapsXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]]).add(new OverlapArithmetic(rel.name(), variable1, Operator.LT,
										variable2, "NotOverlapsXY",false));
							}
							}
						}
						break;
					case "IsOverlappedXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new OverlapArithmetic(rel.name(), variable2, Operator.LT,
										variable1, "IsNotOverlappedXY",true));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]]).add(new OverlapArithmetic(rel.name(), variable2, Operator.LT,
										variable1, "IsNotOverlappedXY",true));
							}
							}
						}
						break;
					case "StartsXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {
								if (vars[pos[0]] < vars[pos[1]]) {
									ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
											vars[pos[0]] + 1);
									ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
											vars[pos[1]] + 1);
									constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ,
											variable2, true, Operator.LT, "NotStartsXY",false));
									mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
											.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ, variable2,
													true, Operator.LT, "NotStartsXY",false));
									// constraints.add(new TemporalArithmetic("StartsXY", vars[pos[0]], Operator.EQ,
									// vars[pos[1]], 0,0,Operator.LT, "NotStartsXY"));
									// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
									// TemporalArithmetic("StartsXY", vars[pos[0]], Operator.EQ, vars[pos[1]],0, 0,
									// Operator.LT,"NotStartsXY"));

								}
							}

						}
						break;
					case "IsStartedXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable2, Operator.EQ,
										variable1, true, Operator.LT, "IsNotStartedXY",true));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable2, Operator.EQ, variable1,
												true, Operator.LT, "IsNotStartedXY",true));
								// constraints.add(new TemporalArithmetic("IsStartedXY", vars[pos[1]],
								// Operator.EQ, vars[pos[0]], 0,0,Operator.LT, "IsNotStartsXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
								// TemporalArithmetic("IsStartedXY", vars[pos[1]], Operator.EQ, vars[pos[0]],
								// 0,0,Operator.LT, "IsNotStartsXY"));

							}
							}

						}
						break;
					case "DuringXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT,
										variable2, true, Operator.LT, "NotDuringXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT, variable2,
												true, Operator.LT, "NotDuringXY",false));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[0]], Operator.GT,
								// vars[pos[1]], 0,0,Operator.LT, "NotDuringXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
								// TemporalArithmetic("DuringXY", vars[pos[0]], Operator.GT, vars[pos[1]],
								// 0,0,Operator.LT, "NotDuringXY"));

							}
							}
						}
						break;
					case "ContainsXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable2, Operator.GT,
										variable1, true, Operator.LT, "NotContainsXY",true));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable2, Operator.GT, variable1,
												true, Operator.LT, "NotContainsXY",true));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[1]], Operator.GT,
								// vars[pos[0]], 0,0,Operator.LT, "NotDuringXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add(new
								// TemporalArithmetic("DuringXY", vars[pos[1]], Operator.GT, vars[pos[0]],
								// 0,0,Operator.LT, "NotDuringXY"));

							}
							}
						}
						break;

					case "ExactXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ,
										variable2, true, Operator.EQ, "NotExactXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ, variable2,
												true, Operator.EQ, "NotExactXY",false));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[0]], Operator.EQ,
								// vars[pos[1]], 0,0,Operator.EQ, "NotExactXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
								// TemporalArithmetic("ExactXY", vars[pos[0]], Operator.EQ, vars[pos[1]],
								// 0,0,Operator.EQ, "NotExactXY"));

							}
							}
						}
						break;
					case "FinishXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT,
										variable2, true, Operator.EQ, "NotFinishXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT, variable2,
												true, Operator.EQ, "NotFinishXY",false));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[0]], Operator.GT,
								// vars[pos[1]], 0,0,Operator.EQ, "NotFinishXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add(new
								// TemporalArithmetic("FinishXY", vars[pos[0]], Operator.GT, vars[pos[1]],
								// 0,0,Operator.EQ, "NotFinishXY"));

							}
							}
						}

						break;
					case "IsFinishedXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable2, Operator.GT,
										variable1, true, Operator.EQ, "IsNotFinishedXY",true));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable2, Operator.GT, variable1,
												true, Operator.EQ, "IsNotFinishedXY",true));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[1]], Operator.GT,
								// vars[pos[0]], 0,0,Operator.EQ, "IsNotFinishedXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add(new
								// TemporalArithmetic("IsFinishedXY", vars[pos[1]], Operator.GT, vars[pos[0]],
								// 0,0,Operator.EQ, "IsNotFinishedXY"));

							}
							}
						}

						break;
					case "DisconnectedXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new OverlapArithmetic(rel.name(), variable2, Operator.LT,
										variable1, "DisconnectedXY",true));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]]).add(new OverlapArithmetic(rel.name(), variable2, Operator.LT,
										variable1, "DisconnectedXY",true));
							}
							}
						}
						break;
					case "ExternallyConnectedXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {
								if (vars[pos[0]] < vars[pos[1]]) {
									ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
											vars[pos[0]] + 1);
									ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
											vars[pos[1]] + 1);
									constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ,
											variable2, true, Operator.LT, "ExternallyConnectedXY",false));
									mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
											.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ, variable2,
													true, Operator.LT, "ExternallyConnectedXY",false));
									// constraints.add(new TemporalArithmetic("StartsXY", vars[pos[0]], Operator.EQ,
									// vars[pos[1]], 0,0,Operator.LT, "NotStartsXY"));
									// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
									// TemporalArithmetic("StartsXY", vars[pos[0]], Operator.EQ, vars[pos[1]],0, 0,
									// Operator.LT,"NotStartsXY"));

								}
							}

						}
						break;
					case "TangentialProperPartXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable2, Operator.EQ,
										variable1, true, Operator.LT, "TangentialProperPartXY",true));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable2, Operator.EQ, variable1,
												true, Operator.LT, "TangentialProperPartXY",true));
								// constraints.add(new TemporalArithmetic("IsStartedXY", vars[pos[1]],
								// Operator.EQ, vars[pos[0]], 0,0,Operator.LT, "IsNotStartsXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
								// TemporalArithmetic("IsStartedXY", vars[pos[1]], Operator.EQ, vars[pos[0]],
								// 0,0,Operator.LT, "IsNotStartsXY"));

							}
							}

						}
						break;
					case "TangentialProperPartInverseXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							constraintFactory = new ConstraintFactory();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT,
										variable2, true, Operator.LT, "TangentialProperPartInverseXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT, variable2,
												true, Operator.LT, "TangentialProperPartInverseXY",false));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[0]], Operator.GT,
								// vars[pos[1]], 0,0,Operator.LT, "NotDuringXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
								// TemporalArithmetic("DuringXY", vars[pos[0]], Operator.GT, vars[pos[1]],
								// 0,0,Operator.LT, "NotDuringXY"));

							}
							}
						}
						break;
					case "PartiallyOverlappingXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable2, Operator.GT,
										variable1, true, Operator.LT, "PartiallyOverlappingXY",true));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable2, Operator.GT, variable1,
												true, Operator.LT, "PartiallyOverlappingXY",true));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[1]], Operator.GT,
								// vars[pos[0]], 0,0,Operator.LT, "NotDuringXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add(new
								// TemporalArithmetic("DuringXY", vars[pos[1]], Operator.GT, vars[pos[0]],
								// 0,0,Operator.LT, "NotDuringXY"));

							}
							}
						}
						break;

					case "NonTangentialProperPartXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ,
										variable2, true, Operator.EQ, "NonTangentialProperPartXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable1, Operator.EQ, variable2,
												true, Operator.EQ, "NonTangentialProperPartXY",false));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[0]], Operator.EQ,
								// vars[pos[1]], 0,0,Operator.EQ, "NotExactXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add( new
								// TemporalArithmetic("ExactXY", vars[pos[0]], Operator.EQ, vars[pos[1]],
								// 0,0,Operator.EQ, "NotExactXY"));

							}
							}
						}
						break;
					case "NonTangentialProperPartInverseXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT,
										variable2, true, Operator.EQ, "NonTangentialProperPartInverseXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT, variable2,
												true, Operator.EQ, "NonTangentialProperPartInverseXY",false));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[0]], Operator.GT,
								// vars[pos[1]], 0,0,Operator.EQ, "NotFinishXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add(new
								// TemporalArithmetic("FinishXY", vars[pos[0]], Operator.GT, vars[pos[1]],
								// 0,0,Operator.EQ, "NotFinishXY"));

							}
							}
						}

						break;
					case "REqualXY":
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();
							if (vars[pos[0]] % 2 == 0&&vars[pos[1]] % 2 == 0) {

							if (vars[pos[0]] < vars[pos[1]]) {
								ACQ_TemporalVariable variable1 = new ACQ_TemporalVariable(vars[pos[0]],
										vars[pos[0]] + 1);
								ACQ_TemporalVariable variable2 = new ACQ_TemporalVariable(vars[pos[1]],
										vars[pos[1]] + 1);
								constraints.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT,
										variable2, true, Operator.EQ, "REqualXY",false));
								mapping.get(vars[pos[0]] + "-" + vars[pos[1]])
										.add(new TemporalArithmetic(rel.name(), variable1, Operator.GT, variable2,
												true, Operator.EQ, "REqualXY",false));
								// constraints.add(new TemporalArithmetic(rel.name(), vars[pos[0]], Operator.GT,
								// vars[pos[1]], 0,0,Operator.EQ, "NotFinishXY"));
								// mapping.get(vars[pos[0]]+"-"+vars[pos[1]]).add(new
								// TemporalArithmetic("FinishXY", vars[pos[0]], Operator.GT, vars[pos[1]],
								// 0,0,Operator.EQ, "NotFinishXY"));

							}
							}
						}

						break;
					default:
						while (pIterator.hasNext()) {
							int[] pos = pIterator.next();

							constraints.add(new ScalarArithmetic(rel.name(), new int[] { vars[pos[0]], vars[pos[1]] },
									new int[] { 1, -1 }, rel.getOperator(), -1 * 0, rel.getNegation().name()));

						}
						break;
					}

				}
				// Ternary Constraints
				if (rel.getArity() == 3) {
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();

						if (vars[pos[0]] > vars[pos[1]] && vars[pos[1]] > vars[pos[2]]) {
							constraints.add(new ScalarArithmetic(rel.name(),
									new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] }, new int[] { 1, -2, 1 },
									rel.getOperator(), 0, rel.getNegation().name()));

						}

					}

				}
				// Quaternary Constraints

				if (rel.getArity() == 4) {
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();

						if (vars[pos[0]] > vars[pos[1]] && vars[pos[2]] > vars[pos[3]] && vars[pos[0]] > vars[pos[2]]) {
							constraints.add(new ScalarArithmetic(rel.name(),
									new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
									new int[] { 1, -1, -1, 1 }, rel.getOperator(), 0, rel.getNegation().name()));

						}

					}

				}
			}
		}
		
		return constraints;
	}

	private ArrayList<ACQ_IConstraint> CreateDisjunctions(ArrayList<ACQ_IConstraint> set, int n) {
		ArrayList<ACQ_IConstraint> constraints = new ArrayList<ACQ_IConstraint>();
		for (int i = 2; i < n + 1; i++) {
			CombinationIterator iterator = new CombinationIterator(set.size(), i);
			while (iterator.hasNext()) {
				int[] vars = iterator.next();
				ConstraintFactory cf = new ConstraintFactory();
				ConstraintSet s = cf.createSet();
				for (int id : vars) {
					s.add(set.get(id));
				}
				constraints.add(new ACQ_DisjunctionConstraint(s));

			}
		}

		return constraints;
	}

	@Override
	public void process() {

		switch (mode) {
		case LQCN:
			ACQ_Utils.executeLQCNExperience(this);
			break;
		case GEQCA:
			ACQ_Utils.executeGEQCAExperience(this);
			break;
		case GEQCA_IQ:
			ACQ_Utils.executeGEQCAIQExperience(this);
			break;
	
		case GEQCA_BK_IQ:
			ACQ_Utils.executeGEQCA_BK_IQ_Experience(this);
			break;
		default:
			ACQ_Utils.executeLQCNExperience(this);
			break;

		}

	}

	

	@Override
	public ACQ_Network createTargetNetwork() {
		return this.createLearner().buildTargetNetwork();
	}

	
	public boolean ask(ACQ_Query e, ACQ_Network target) {
		for (ACQ_IConstraint cst : target.getConstraints()) {
			// System.out.println(cst+"Projection ::
			// "+Arrays.toString(cst.getProjection(e)));

			boolean check = false;
			if (cst instanceof ACQ_DisjunctionConstraint)
				check = ((ACQ_DisjunctionConstraint) cst).check(e);

			else
				if(cst  instanceof TemporalConstraint || cst instanceof OverlapConstraint) {
					check=((ACQ_Constraint)cst).check(e);
					}
					else
					check = cst.checker(cst.getProjection(e));
			ACQ_Scope check1 = e.getScope().diff(cst.getScope());

			// System.out.println(cst+" "+check+" "+ check1.size());
			if (check == true && check1.size() == 0) {
				e.classify(true);
				return true;
			}
		}
		e.classify(false);
		return false;
	}

	public boolean askTEMACQ(ACQ_IConstraint e, ACQ_Network target) {
		
		for (ACQ_IConstraint cst : target.getConstraints()) {
			
			if (cst instanceof ACQ_DisjunctionConstraint) {
				ConstraintSet set = ((ACQ_DisjunctionConstraint) cst).constraintSet;
				for (ACQ_IConstraint cst2 : set) {
					if (cst2.getName().equals(e.getName()) && cst.getScope().diff(e.getScope()).size() == 0)
						return true;
				}
			}

			else {

				if (cst.getName().equals(e.getName()) && cst.getScope().diff(e.getScope()).size() == 0)

					return true;
			}

		}
		return false;
	}
	
	public boolean askQUACQ(ACQ_Query e, ACQ_Network target) {

		for (ACQ_IConstraint cst : target.getConstraints()) {
			// System.out.println(cst+"Projection ::
			// "+Arrays.toString(cst.getProjection(e)));

			boolean check = false;
			if (cst instanceof ACQ_DisjunctionConstraint)
				check = ((ACQ_DisjunctionConstraint) cst).check(e);

			else
				if(cst  instanceof TemporalConstraint || cst instanceof OverlapConstraint) {
					check=((ACQ_Constraint)cst).check(e);
					}
					else
					check = cst.checker(cst.getProjection(e));
			ACQ_Scope check1 = e.getScope().diff(cst.getScope());

			// System.out.println(cst+" "+check+" "+ check1.size());
			if (check == true && check1.size() == 0) {
				e.classify(false);
				return false;
			}
		}
		e.classify(true);
		return true;

		/*
		 * for (ACQ_IConstraint cst : target.getConstraints()) { if
		 * (e.getScope().containsAll(cst.getScope()) &&
		 * !cst.checker(e.getProjection(cst.getScope()))) { e.classify(false); return
		 * false; } } e.classify(true); return true;
		 */

	}

	public void setDeadline(int deadline) {
		
		this.deadline=deadline;
		// TODO Auto-generated method stub
		
	}
public void setAlgebraType(String type) {
		
		this.algebratype=type;
		// TODO Auto-generated method stub
		
	}


}