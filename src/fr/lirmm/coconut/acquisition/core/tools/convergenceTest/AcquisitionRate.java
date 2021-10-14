package fr.lirmm.coconut.acquisition.core.tools.convergenceTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.tools.NameService;

public class AcquisitionRate {

	//////////////////////////////////////////////////////////////////////////
	//
	// ATTRIBUTES OF DATASET
	//
	//////////////////////////////////////////////////////////////////////////

	//
	// constraint network edges
	private ArrayList<ArrayList<Integer>> learnedNetwork, targetNetwork;
	// Number of constraints
	private String instance;
	public double relativeAcquisitionRate;
	public int absoluteAcquisitionRate;
	private int nbVars = 0;
	private double time;
	private int[] domain;
	private int targetSize;
	private Model learnedModel;

	//////////////////////////////////////////////////////////////////////////
	//
	// CONSTRUCTORS OF DATASET
	//
	//////////////////////////////////////////////////////////////////////////

	public AcquisitionRate(String clPath, String ctPath, int minDom, int maxDom) throws IOException {

		domain = new int[] { minDom, maxDom };
		learnedNetwork = parseDataset(clPath);

		learnedModel = buildModel(learnedNetwork);

		targetNetwork = parseDataset(ctPath);

		targetSize = targetNetwork.size();
		// target network

		absoluteAcquisitionRate = targetSize - computeAAR();
		relativeAcquisitionRate = (absoluteAcquisitionRate / targetSize);

	}
	public AcquisitionRate(ACQ_Network learnedNetwork,ACQ_Network targetNetwork, int minDom, int maxDom) {

		domain = new int[] { minDom, maxDom };

		targetSize = targetNetwork.size();
		// target network
		int cpt=computeAAR(targetNetwork,learnedNetwork);
		absoluteAcquisitionRate = targetSize - cpt;
		relativeAcquisitionRate = (absoluteAcquisitionRate / targetSize);

	}

	private ArrayList<ArrayList<Integer>> parseDataset(String filepath) throws NumberFormatException, IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		String line;
		ArrayList<ArrayList<Integer>> constraints = new ArrayList<ArrayList<Integer>>();

		while (((line = reader.readLine()) != null)) {

			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%') {
				continue;
			}

			// instance name. Ex. @sudoku
			if (line.charAt(0) == '@') {
				if (instance == null)
					instance = line.substring(1);
				else if (!instance.equals(line.substring(1)))
					System.exit(0);
				continue;
			}

			String[] lineSplited = line.split(" ");

			ArrayList<Integer> constraint = new ArrayList<>();

			for (int i = 0; i < lineSplited.length; i++) {

				Integer val = Integer.parseInt(lineSplited[i]);
				if (i != 0 && val + 1 > nbVars)
					nbVars = val + 1;
				constraint.add(val);
			}

			constraints.add(constraint);

		}

		// close the input file
		reader.close();
		return constraints;

	}

	//////////////////////////////////////////////////////////////////////////
	//
	// MOTHODS ON DATASET
	//
	//////////////////////////////////////////////////////////////////////////

	private int computeAAR() {

		int cpt = 0;

		for (ArrayList<Integer> cst : this.targetNetwork) {

			// Negated constraint

			Constraint chocoCst = null;

			int constraintType = cst.get(0);

			IntVar[] tab = learnedModel.retrieveIntVars(false);
			
			// add the negation of the constraint
			switch (constraintType) {
			case 0:
				chocoCst = learnedModel.arithm(learnedModel.retrieveIntVars(false)[cst.get(1)], "=",
						learnedModel.retrieveIntVars(false)[cst.get(2)]);
				break;
			default:
				chocoCst = null;
			}

			learnedModel.post(chocoCst);

			if (learnedModel.getSolver().solve())
				{
				System.out.println("constraint IS misssing:"  +cst);
				cpt++;
				}
			else
				System.out.println("constraint NOT misssing:"  +cst);


			learnedModel.unpost(chocoCst);

		}
		return cpt;
	}
	private int computeAAR(ACQ_Network targetNetwork,ACQ_Network learnedNetwork) {
		Model learnedModel = buildModel(learnedNetwork);

		int cpt = 0;
		ArrayList<IntVar> intVars1 = new ArrayList<>();
		ArrayList<IntVar> intVars = new ArrayList<>();

		for (IntVar intVar1 : learnedModel.retrieveIntVars(false)) {
			intVars1.add(intVar1);
		}
		for (IntVar v : intVars1) {
			targetNetwork.getVariables().forEach(numvar1 -> {
				if (NameService.getVarName(numvar1).equals(v.getName()))
					intVars.add(v);
			});

		}

		IntVar[] varArray = intVars.toArray(new IntVar[intVars.size()]);

		for (ACQ_IConstraint cst : targetNetwork) {

			// Negated constraint

			Constraint chocoCst = cst.getNegation().getChocoConstraints(learnedModel, varArray)[0];


			
			
				learnedModel.post(chocoCst);
			

			try{
				if(learnedModel.getSolver().solve())
				{
				//System.out.println("constraint IS misssing:"  +cst);
				cpt++;
				}else {
					//System.out.println("constraint NOT misssing:"  +cst);

				}
				}
			catch(Exception e) {

			}
			learnedModel.unpost(chocoCst);

		}
		return cpt;
	}
	private Model buildModel(ArrayList<ArrayList<Integer>> network) {

		Model model = new Model();

		IntVar[] vars = new IntVar[nbVars];

		for (int i = 0; i < vars.length; i++) {

				vars[i] = model.intVar("c_" + i, domain[0], domain[1], false);
			
		}

		for (ArrayList<Integer> cst : network) {

			// Negated constraint

			int constraintType = cst.get(0);

			switch (constraintType) {
			case 0:
				model.arithm(vars[cst.get(1)], "!=", vars[cst.get(2)]).post();
				break;
			}

		}
		return model;

	}
	
	private Model buildModel(ACQ_Network network) {

		Model model = new Model();
		ACQ_Scope vars = network.getVariables();
		ArrayList<IntVar> intVars1 = new ArrayList<>();
		ArrayList<IntVar> intVars = new ArrayList<>();

		for (int numvar1 : vars) {
			IntVar intVar1 = model.intVar(NameService.getVarName(numvar1), domain[0], domain[1]);
			intVars1.add(intVar1);
		}
		for (IntVar v : intVars1) {
			network.getVariables().forEach(numvar1 -> {
				if (NameService.getVarName(numvar1).equals(v.getName()))
					intVars.add(v);
			});

		}

		IntVar[] varArray = intVars.toArray(new IntVar[intVars.size()]);

		// add constraints in model
		for (ACQ_IConstraint c : network.getConstraints()) {

			model.post(c.getChocoConstraints(model, varArray));
		}

		
		return model;

	}

	//////////////////////////////////////////////////////////////////////////
	//
	// GETTERS / SETTERS FOR DATASET
	//
	//////////////////////////////////////////////////////////////////////////

	public String getInstance() {
		return instance;
	}

	public ArrayList<ArrayList<Integer>> getConstraints() {
		return learnedNetwork;
	}

	public String toString() {
		String line = "------------------------------\n";
		String results = "";

		results += line;
		results += this.getInstance() + "\n";
		results += "learned network size: " + this.learnedNetwork.size() + "\n";
		results += "target network size: " + this.targetNetwork.size() + "\n";
		results += "acquisition rate (A): " + this.absoluteAcquisitionRate + "\n";
		results += "acquisition rate (R): " + this.relativeAcquisitionRate * 100 + " %\n";
		results += "CPU time (s): " + this.time + "\n";
		results += line;

		return results;

	}

	//////////////////////////////////////////////////////////////////////////
	//
	// MAIN
	//
	//////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws IOException {

		System.out.println(new AcquisitionRate("./dataset/jsudoku/jsudokujava.cl", "./dataset/jsudoku/jsudoku.ct", 1, 9));

	}

}
