package fr.lirmm.coconut.acquisition.expe_conacq;

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

public class ExpeConacq_SUDOKU9 extends DefaultExperience{

	private static boolean gui=true;

	public ExpeConacq_SUDOKU9() {
		
		setDimension(9);
	}
	ValSelector vls;
	VarSelector vrs;
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
		},vrs.DomOverWDeg.toString(),vls.IntDomainBest.toString());
	}
	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {

				int s[][]= new int[9][9];	
				int l,c;

				for(int numvar:e.getScope())
				{
					l=numvar/9; 						
					c=numvar%9;
					s[l][c]=e.getValue(numvar);

				}

				// row checker
				for(int row = 0; row < 9; row++)
					for(int col = 0; col < 8; col++)
						for(int col3 = col + 1; col3 < 9; col3++)
							if(s[row][col]!=0 && s[row][col]==s[row][col3])
							{
								e.classify(false);
								return false;
							}

				// column checker
				for(int col = 0; col < 9; col++)
					for(int row = 0; row < 8; row++)
						for(int row3 = row + 1; row3 < 9; row3++)
							if( s[row][col]!=0 &&  s[row][col]==s[row3][col])
							{
								e.classify(false);
								return false;
							}

				// grid checker
				for(int row = 0; row < 9; row += 3)
					for(int col = 0; col < 9; col += 3)
						// row, col is start of the 8 by 8 grid
						for(int pos = 0; pos < 8; pos++)
							for(int pos3 = pos + 1; pos3 < 9; pos3++)
								if(s[row + pos%3][col + pos/3]!=0 &&  s[row + pos%3][col + pos/3]==s[row + pos3%3][col + pos3/3])
								{
									e.classify(false);
									return false;
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
		ConstraintFactory constraintFactory=new ConstraintFactory();

		ConstraintSet constraints = constraintFactory.createSet();
		CombinationIterator iterator = new CombinationIterator(NB_VARIABLE, 3);
		while (iterator.hasNext()) {
			int[] vars = iterator.next();
			AllPermutationIterator pIterator = new AllPermutationIterator(3);
			while (pIterator.hasNext()) {
				int[] pos = pIterator.next();

				if(vars[pos[0]]< vars[pos[1]])		//NL: commutative relations
				{
					// X != Y
					constraints.add(new BinaryArithmetic("DifferentXY", vars[pos[0]], Operator.NEQ, vars[pos[1]],"EqualXY"));
					// X == Y
					constraints.add(new BinaryArithmetic("EqualXY", vars[pos[0]], Operator.EQ, vars[pos[1]], "DifferentXY"));
				}
				
				
				// X >= Y
				constraints.add(new BinaryArithmetic("GreaterEqualXY", vars[pos[0]], Operator.GE, vars[pos[1]], "LessXY"));
				// X >= Y
				constraints.add(new BinaryArithmetic("LessEqualXY", vars[pos[0]], Operator.LE, vars[pos[1]], "GreaterXY"));
			
				// X < Y
				constraints.add(new BinaryArithmetic("LessXY", vars[pos[0]], Operator.LT, vars[pos[1]], "GreaterEqualXY"));
				// X > Y
				constraints.add(new BinaryArithmetic("GreaterXY", vars[pos[0]], Operator.GT, vars[pos[1]], "LessEqualXY"));}
				
			
		}
		ACQ_Network network = new ACQ_Network(constraintFactory,allVarSet, constraints);
		return new ACQ_Bias(network);
	}
 
	
	public static void main(String[] args) {
		new ExpeConacq_SUDOKU9().process();
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
		ExpeConacq_SUDOKU9 expe;
		try {
			expe = new ExpeConacq_SUDOKU9();
			expe.setParams(true, // normalized csp
					true, // shuffle_split,
					60000, // timeout
					ACQ_Heuristic.SOL, 
				 true,true
			);
			expe.setLog_queries(true);
			ACQ_WS.executeConacqV1Experience(expe);
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

