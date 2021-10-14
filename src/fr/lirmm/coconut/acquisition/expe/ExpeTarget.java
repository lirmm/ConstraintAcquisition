/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.expe;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

/**
 *
 * @author Nassim
 */
public class ExpeTarget extends DefaultExperience{

	//	Heuristic heuristic = Heuristic.SOL;
	//	ACQ_ConstraintSolver solver;
	//	ACQ_Bias bias;
	//	private final ACQ_Learner learner;
	
	private static String inst="25_5_100";
	
	
	public ExpeTarget() {
		
		
	}

	public ACQ_ConstraintSolver createSolver() {
		return new ACQ_ChocoSolver(new ACQ_IDomain() {
			@Override
			public int getMin(int numvar) {
				return 1;
			}

			@Override
			public int getMax(int numvar) {
				return domain;
			}
		},vrs,vls);
	}

	public ACQ_Learner createLearner() {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {

				int vars[]= new int[nb_targets];	

				for(int numvar:e.getScope())
					vars[numvar]=e.getValue(numvar);


					for (int i = 0; i < vars.length - 1; i++) {   
						for (int j = i + 1 ; j < vars.length; j++) {
							for(int m =0 ; m<nb_targets; m++) {

									if(visibility[m][i]==1 && visibility[m][j]==1 && compatibility[i][j]==1) {

										if(vars[i]>0 && vars[j]>0 && vars[i]==vars[j])	{
											e.classify(false);
											return false;}
					}
						
						if(visibility[m][i]==0 && visibility[m][j]==1) {
							if(vars[i]>0 && vars[j]>0 && vars[i]==vars[j])	{
								e.classify(false);
								return false;}
						}
						if(visibility[m][i]==1 && visibility[m][j]==0) {
							if(vars[i]>0 && vars[j]>0 && vars[i]==vars[j])	{
								e.classify(false);
								return false;}
						}}
					}}
				
				e.classify(true);

				return true;

			}

		};

	}


	public ACQ_Bias createBias() {
		int NB_VARIABLE = nb_targets;
		// build All variables set
		BitSet bs = new BitSet();
		bs.set(0, NB_VARIABLE);
		ACQ_Scope allVarSet = new ACQ_Scope(bs);
		ConstraintFactory constraintFactory=new ConstraintFactory();
		// build binary Constraints
		ConstraintSet constraints = constraintFactory.createSet();
		CombinationIterator iterator = new CombinationIterator(NB_VARIABLE, 2);
		while (iterator.hasNext()) {
			int[] vars = iterator.next();
			AllPermutationIterator pIterator = new AllPermutationIterator(2);
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
				// X <= Y
				constraints.add(new BinaryArithmetic("LessEqualXY", vars[pos[0]], Operator.LE, vars[pos[1]], "GreaterXY"));
				
				// X < Y
				constraints.add(new BinaryArithmetic("LessXY", vars[pos[0]], Operator.LT, vars[pos[1]], "GreaterEqualXY"));
				// X > Y
				constraints.add(new BinaryArithmetic("GreaterXY", vars[pos[0]], Operator.GT, vars[pos[1]], "LessEqualXY"));
				
			}
		}
		
		


		ACQ_Network network = new ACQ_Network(constraintFactory,allVarSet, constraints);
		

		return new ACQ_Bias(network);
	}
	
		@Override
		public  void process() {
			

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
		public void setDirectory(File directory) {
			this.directory=directory;
		}

	

		@Override
		public ACQ_Network createTargetNetwork() {
			// TODO Auto-generated method stub
			return this.createTargetNetwork();
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
