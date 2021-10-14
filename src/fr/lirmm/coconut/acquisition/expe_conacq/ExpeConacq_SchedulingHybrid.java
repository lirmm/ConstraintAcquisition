package fr.lirmm.coconut.acquisition.expe_conacq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Relation;
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
import fr.lirmm.coconut.acquisition.expe.ExpeFromParser;
import fr.lirmm.coconut.acquisition.expe.ExpeParser;

public class ExpeConacq_SchedulingHybrid extends DefaultExperience {

	private static boolean gui=true;
	private ExpeParser exp;

	public ExpeConacq_SchedulingHybrid() throws IOException {

		
	}

	static String vls = ValSelector.IntDomainRandom.toString();
	static String vrs = VarSelector.RandomVar.toString();

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

				for (ArrayList<String> cst : exp.getTN())
					tn.add(ACQ_Constraint.CstrFactory.getConstraint(cst), true);
				return tn;

			}

			@Override
			public boolean ask(ACQ_Query e) {
				this.setTargetNetwork();
				if (this.getTargetNetwork().isEmpty()) {
					boolean answer =getAnswer(e,exp.getNbVars());
				e.classify(answer);
				return answer;
				}else
					for (ACQ_IConstraint cst : this.getTargetNetwork().getConstraints()) {
						if (e.getScope().containsAll(cst.getScope()) && !cst.checker(e.getProjection(cst.getScope()))) {
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

		int NB_VARIABLE = exp.getNbVars();
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);

		ACQ_Scope allVarSet = new ACQ_Scope(bs);

		ConstraintFactory constraintFactory = new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();

		constraints = (convert(exp));

		ACQ_Network network = new ACQ_Network(constraintFactory, allVarSet, constraints);
		System.out.print(network.size());
		return new ACQ_Bias(network);
	}

	private ConstraintSet convert(ExpeParser experiment) {
		ConstraintFactory cf = new ConstraintFactory();
		ConstraintSet constraints = cf.createSet();
		int[] duration = getDuraitons(instance);

		for (ArrayList<String> c : experiment.getBias()) {
			constraints.add(ACQ_Constraint.CstrFactory.getConstraint(c));
		}

		for (ArrayList<String> r : experiment.getGamma()) {
			ACQ_Relation rel = ACQ_Relation.valueOf(r.get(0));
			
			CombinationIterator iterator = new CombinationIterator(experiment.getNbVars(), rel.getArity());

			while (iterator.hasNext()) {
				int[] vars = iterator.next();
				AllPermutationIterator pIterator = new AllPermutationIterator(rel.getArity());

				// Binary constraints
				if (rel.getArity() == 2 && !rel.IsPrecedence()) {
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
				if (rel.IsPrecedence()) {
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();

						constraints.add(new ScalarArithmetic(rel.name(), new int[] { vars[pos[0]], vars[pos[1]] },
								new int[] { 1, -1 }, rel.getOperator(), -1 * duration[vars[pos[0]]],
								rel.getNegation().name()));

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
	
	public static void main(String[] args) throws IOException {
		new ExpeConacq_SchedulingHybrid().process();
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
		//return new Z8SATSolver();
		return new MiniSatSolver();
	}

	@Override
	public ACQ_Network createTargetNetwork() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void process() {
		ExpeConacq_SchedulingHybrid expe;
		try {
			expe = new ExpeConacq_SchedulingHybrid();
			expe.setParams(true, // normalized csp
					true, // shuffle_split,
					60000, // timeout
					ACQ_Heuristic.SOL, 
				 true,true
			);
			expe.setLog_queries(true);
			ACQ_WS.executeHybridModeExperience(expe);
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

