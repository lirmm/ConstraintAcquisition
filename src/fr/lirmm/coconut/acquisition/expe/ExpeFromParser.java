package fr.lirmm.coconut.acquisition.expe;

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
import fr.lirmm.coconut.acquisition.core.acqconstraint.UnaryArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ChocoSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
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
import fr.lirmm.coconut.acquisition.core.tools.FileManager;
import fr.lirmm.coconut.acquisition.core.workspace.ACQ_WS;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;

public class ExpeFromParser extends DefaultExperience {

	private static int nb_mark = 4; // NL:: add instance parameter for golomb
	private static int nb_dist = (nb_mark * (nb_mark - 1)) / 2; // m * (m - 1))
																// / 2
	private static boolean gui = false;

	public ExpeParser exp;

	public ExpeFromParser(ExpeParser exp) {

		this.exp = exp;
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
					boolean answer = getAnswer(e, exp.getNbVars());
					e.classify(answer);
					return answer;
				} else
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
		int[] duration = null;
		if (experiment.getName().contains("scheduling"))
			duration = getDuraitons(instance);
		for (ArrayList<String> c : experiment.getBias()) {
			constraints.add(ACQ_Constraint.CstrFactory.getConstraint(c));
		}

//		ArrayList<ACQ_Relation> relList = new ArrayList<>();

		for (ArrayList<String> r : experiment.getGamma()) {
			ACQ_Relation rel = ACQ_Relation.valueOf(r.get(0));
			
//			if(!rel.isSymmetric())relList.add(rel.getRightDirection());
			
			CombinationIterator iterator = new CombinationIterator(experiment.getNbVars(), rel.getArity());

			while (iterator.hasNext()) {
				int[] vars = iterator.next();
				AllPermutationIterator pIterator = new AllPermutationIterator(rel.getArity());

				// Binary constraints
				BinaryArithmetic cstr;
				if (rel.getArity() == 2 && !rel.IsPrecedence()) {
					while (pIterator.hasNext()) {
						int[] pos = pIterator.next();
						cstr = new BinaryArithmetic(rel.name(), vars[pos[0]], rel.getOperator(), vars[pos[1]],
								rel.getNegation().name());

				//		if (rel.isSymmetric() && vars[pos[0]] < vars[pos[1]]) {
						if ( vars[pos[0]] < vars[pos[1]]) {
							constraints.add(cstr);

						} 
				//		else if (!rel.isSymmetric() && !relList.contains(rel)) {
				//			constraints.add(cstr);

//						}

					
				}
			}if(rel.IsPrecedence())

	{
		while (pIterator.hasNext()) {
			int[] pos = pIterator.next();

			constraints.add(new ScalarArithmetic(rel.name(), new int[] { vars[pos[0]], vars[pos[1]] },
					new int[] { 1, -1 }, rel.getOperator(), -1 * duration[vars[pos[0]]], rel.getNegation().name()));

		}

	}

	// Ternary Constraints
	if(rel.getArity()==3)
	{
		while (pIterator.hasNext()) {
			int[] pos = pIterator.next();

			if (vars[pos[0]] > vars[pos[1]] && vars[pos[1]] > vars[pos[2]]) {
				constraints.add(new ScalarArithmetic(rel.name(), new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]] },
						new int[] { 1, -2, 1 }, rel.getOperator(), 0, rel.getNegation().name()));

			}

		}

	}
	// Quaternary Constraints

	if(rel.getArity()==4)
	{
		while (pIterator.hasNext()) {
			int[] pos = pIterator.next();

			if (vars[pos[0]] > vars[pos[1]] && vars[pos[2]] > vars[pos[3]] && vars[pos[0]] > vars[pos[2]]) {
				constraints.add(new ScalarArithmetic(rel.name(),
						new int[] { vars[pos[0]], vars[pos[1]], vars[pos[2]], vars[pos[3]] },
						new int[] { 1, -1, -1, 1 }, rel.getOperator(), 0, rel.getNegation().name()));

			}

		}

	}
	}}return constraints;

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
	public ACQ_Network createTargetNetwork() {
		int NB_VARIABLE = exp.getNbVars();
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE - 1);

		ACQ_Scope allVarSet = new ACQ_Scope(bs);

		ConstraintFactory constraintFactory = new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();

		ACQ_Network tn = new ACQ_Network(constraintFactory, allVarSet, constraints);

		for (ArrayList<String> cst : exp.getTN())
			tn.add(ACQ_Constraint.CstrFactory.getConstraint(cst), true);
		return tn;
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
	public ArrayList<ACQ_Network> createStrategy(ACQ_Bias bias) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SATSolver createSATSolver() {
		return new MiniSatSolver();

	}

	@Override
	public ContradictionSet createBackgroundKnowledge(ACQ_Bias bias, ConstraintMapping mapping) {
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
		int NB_VARIABLE = exp.getNbVars();
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);

		ACQ_Scope allVarSet = new ACQ_Scope(bs);

		ConstraintFactory constraintFactory = new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();

		ACQ_Network tn = new ACQ_Network(constraintFactory, allVarSet, constraints);

		for (ArrayList<String> cst : exp.getINIT())
			tn.add(ACQ_Constraint.CstrFactory.getConstraint(cst), true);
		return tn;

	}

}
