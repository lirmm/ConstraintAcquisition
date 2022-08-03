package GEQCA;

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



public class ExpeParser  {

	private int nbVars;
	private int minDom;
	private int maxDom;
	private ArrayList<ArrayList<String>> language;
	private ArrayList<ArrayList<String>> biasConstraints;
	private ArrayList<ArrayList<String>> targetConstraints;
	private final File benchDir = new File("./benchmarks/");
	private final String biasExtension = ".bias";
	private final String targetExtension = ".target";
	private String instanceName;

	public ExpeParser(String name) throws IOException {

		Pattern p_bias = Pattern.compile(name + "*"+biasExtension);
		Pattern p_target = Pattern.compile(name + "*"+targetExtension);

		String bias_path = findPath(benchDir, p_bias);
		String target_path = findPath(benchDir, p_target);

		this.instanceName = name;
		
		language = new ArrayList<ArrayList<String>>();
		biasConstraints = new ArrayList<ArrayList<String>>();
		targetConstraints = new ArrayList<ArrayList<String>>();
		parser(bias_path, true); // parsing bias file
		try {
		parser(target_path, false); // parsing target file
		//disjunctionlevel=biasConstraints.size();
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

	

	public int getNbVars() {
		return nbVars;
	}

	public int getMinDom() {
		return minDom;
	}

	public int getMaxDom() {
		return maxDom;
	}
	
	public String getInstanceName() {
		return instanceName;
	}

	public ArrayList<ArrayList<String>> getTN() {
		return targetConstraints;
	}
	public ArrayList<ArrayList<String>> getBias() {
		return biasConstraints;
	}
	

	public ArrayList<ArrayList<String>> getGamma() {
		// TODO Auto-generated method stub
		return language;
	}

	

}
