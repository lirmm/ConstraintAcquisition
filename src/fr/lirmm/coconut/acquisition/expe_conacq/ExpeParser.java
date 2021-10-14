package fr.lirmm.coconut.acquisition.expe_conacq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintMapping;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ContradictionSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.SATSolver;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;

public class ExpeParser extends DefaultExperience {

	private int nbVars;
	private int minDom;
	private int maxDom;
	private ArrayList<ArrayList<String>> language;
	private ArrayList<ArrayList<String>> biasConstraints;
	private ArrayList<ArrayList<String>> targetConstraints;
	private final File benchDir = new File("./benchmarks/");
	private final String biasExtension = ".bias";
	private final String targetExtension = ".target";

	public ExpeParser(String name) throws IOException {

		Pattern p_bias = Pattern.compile(name + "*"+biasExtension);
		Pattern p_target = Pattern.compile(name + "*"+targetExtension);

		String bias_path = findPath(benchDir, p_bias);
		String target_path = findPath(benchDir, p_target);
		
		language = new ArrayList<ArrayList<String>>();
		biasConstraints = new ArrayList<ArrayList<String>>();
		targetConstraints = new ArrayList<ArrayList<String>>();

		parser(bias_path, true); // parsing bias file
		try {
		parser(target_path, false); // parsing target file
		}catch(Exception e) {
			System.out.println("Missing Target File, Switching to Manual Mode ");
		}
	}

	public void parser(String benchPath, boolean bias) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(benchPath));
		String line;
		boolean gamma = false;

		// for each line until the end of the file
		while (((line = reader.readLine()) != null)) {

			// if the line is a comment or is empty
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == 'c' || line.charAt(0) == '%') {
				continue;
			}

			// split the line according to spaces
			String[] lineSplited = line.split(" ");

			// first line is a metadata
			if (bias) {
				if (line.startsWith("nbVars")) {
					nbVars = Integer.parseInt(lineSplited[1]);
					continue;
				}
				if (line.startsWith("domainSize")) {
					minDom = Integer.parseInt(lineSplited[1]);
					maxDom = Integer.parseInt(lineSplited[2]);
					continue;

				}
				if (line.startsWith("Gamma")) {
					gamma = true;
					continue;
				}
				if (line.startsWith("Constraints")) {

					gamma = false;
					continue;
				}
			}

			if (gamma) {
				ArrayList<String> newLine = new ArrayList<>();
				newLine.add(line);
				language.add(newLine);
			} else {
				ArrayList<String> newLine = new ArrayList<>(Arrays.asList(lineSplited));

				if (bias)
					biasConstraints.add(newLine);

				else
					targetConstraints.add(newLine);
			}

		}

		// close the input file
		reader.close();

	}

	public static String findPath(File folder, Pattern filename) {

		String path = null;
		if (folder.isDirectory()) {
			List<File> files = Arrays.asList(folder.listFiles());
			Iterator<File> fileIterator = files.iterator();
			while (fileIterator.hasNext() && path == null) {
				path = findPath(fileIterator.next(), filename);
			}
		} else {
			Matcher matcher = filename.matcher(folder.getName().toLowerCase());
			if (matcher.find()) {
				path = folder.getAbsolutePath();
			}
		}
		return path;
	}

	public static void main(String[] args) throws IOException {
		ExpeParser test = new ExpeParser("alldiff");
		test.process();
		System.out.println("test");
	}

	public int getNbVars() {
		return nbVars;
	}

	public int getMinDom() {
		return minDom;
	}

	public int getMaxDom() {
		return maxDom;
	}
	
	public ArrayList<ArrayList<String>> getTN() {
		return targetConstraints;
	}
	public ArrayList<ArrayList<String>> getBias() {
		return biasConstraints;
	}
	@Override
	public ACQ_Bias createBias() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ACQ_Learner createLearner() {
		// TODO Auto-generated method stub
		return null;
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
	public ACQ_Network createTargetNetwork() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ACQ_ConstraintSolver createSolver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
		
	}

	public ArrayList<ArrayList<String>> getGamma() {
		// TODO Auto-generated method stub
		return language;
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
