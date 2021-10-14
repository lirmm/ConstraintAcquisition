package fr.lirmm.coconut.acquisition.core.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ChocoSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_IDomain;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.tools.StatManager;

public class FileManager {

	/**
	 * Regex pattern for variables
	 */
	public static final Pattern VALID_VAR_REGEX = Pattern.compile("^[1-9][0-9]*, *[0-9]+, *[0-9]+;$",
			Pattern.CASE_INSENSITIVE);
	/**
	 * Regex pattern for binary constraints
	 */
	public static final Pattern VALID_BINARY_REGEX = Pattern
			.compile("(Diff||Equal||Greater||Less||GreaterOrEqual||LessOrEqual)XY;$", Pattern.CASE_INSENSITIVE);
	/**
	 * Regex pattern for unary constraints
	 */
	public static final Pattern VALID_UNARY_REGEX = Pattern.compile(
			"(Diff||Equal||Greater||Less||GreaterOrEqual||LessOrEqual)X - [-0-9]+;$", Pattern.CASE_INSENSITIVE);
	/**
	 * Stream factory
	 */
	private Supplier<Stream<String>> supplier;

	private String filePath;

	/**
	 * Constructor
	 * 
	 * @param fileName path of the .acq file
	 * @throws IOException
	 */
	public FileManager(String filePath) throws IOException {
		this.filePath = filePath;
		List<String> allLines = Files.readAllLines(Paths.get(filePath));
		supplier = () -> allLines.stream();

	}

	/**
	 * Empty constructor
	 */
	public FileManager() {
	}

	public void display() {
		supplier.get().forEach(e -> System.out.println(e.toString()));
	}

	public List<String> getVariables() {
		List<String> vars = new ArrayList<String>();
		for (String s : toLineList()) {
			Matcher matcher = VALID_VAR_REGEX.matcher(s);
			if (matcher.find()) {
				vars.add(s);
			}
		}
		return vars;
	}

	public int getNbVar() {
		return getVariables().size();
	}

	public int[] getDomain() throws IOException {
		List<String> varList = getVariables();
		List<Integer> mins = new ArrayList<Integer>();
		List<Integer> maxs = new ArrayList<Integer>();
		for (String s : varList) {
			String[] split = s.substring(0, s.length() - 1).replaceAll("\\s+", "").split(",");
			mins.add(Integer.parseInt(split[1]));
			maxs.add(Integer.parseInt(split[2]));
		}
		int[] domain = new int[2];
		domain[0] = Collections.min(mins);
		domain[1] = Collections.min(maxs);
		return domain;
	}

	public List<String> getBinaryConstraints() {
		List<String> csts = new ArrayList<String>();
		for (String s : toLineList()) {
			Matcher matcher = VALID_BINARY_REGEX.matcher(s);
			if (matcher.find())
				csts.add(s.substring(0, s.length() - 1));
		}
		return csts;
	}

	public List<String> getUnaryConstraints() {
		List<String> csts = new ArrayList<String>();
		for (String s : toLineList()) {
			Matcher matcher = VALID_UNARY_REGEX.matcher(s);
			if (matcher.find())
				csts.add(s.substring(0, s.length() - 1));
		}
		return csts;
	}

	public List<String> getConstraints() {
		List<String> constraints = new ArrayList<String>();
		constraints.addAll(getBinaryConstraints());
		constraints.addAll(getUnaryConstraints());
		return constraints;
	}

	public List<String> toLineList() {
		return supplier.get().collect(Collectors.toList());
	}

	public static void saveTextToFile(String content, File file) {
		try {
			PrintWriter writer;
			writer = new PrintWriter(file);
			writer.println(content);
			writer.close();
		} catch (IOException ex) {
//			Logger.getLogger(class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void printFile(Object something, String file_name) {

		
		String directoryName = getExpDir() + "/logFiles/";
		
	    File directory = new File(directoryName);
		File file = new File(directoryName + file_name + ".log");
	    
	    if (! directory.exists()){
	        directory.mkdir();
	        // If you require it to make the entire directory path including parents,
	        // use directory.mkdirs(); here instead.
	    }

		try {

			// check whether the file is existed or not
			if (!file.exists()) {

				// create a new file if the file is not existed
				file.createNewFile();
			}

			// new a writer and point the writer to the file
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			writer.append(something.toString() + "\n");

			// writer the content to the file

			// always remember to close the writer
			writer.close();
			writer = null;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public static void printResults(Object something, String file_name) {

		
	String directoryName = getExpDir() + "/results/";
		
	    File directory = new File(directoryName);
		File file = new File(directoryName + file_name);
	    
	    if (! directory.exists()){
	        directory.mkdir();
	        // If you require it to make the entire directory path including parents,
	        // use directory.mkdirs(); here instead.
	    }

		
		try {

			// check whether the file is existed or not
			if (!file.exists()) {

				// create a new file if the file is not existed
				file.createNewFile();
				if(file_name.contains(".results")) {
				BufferedWriter writer1 = new BufferedWriter(new FileWriter(file));
				writer1.append("Date \t - \t CL size \t RelativeAcquisitionRate \t AbsoluteAcquisitionRate  \t ConvergenceRate \t #Queries \t (#Queries/CLsize) \t #MembershipQueries \t Query size \t Acquisition time \t Running Time \t "
						+ "Max Waiting Time \t - \t #NonAskedQueries \t BiasInit Size \t Bias Final Size \t VRS Heuristic \t QueryGeneration Heuristic" + "\n");
				
				writer1.close();}
			}
			

			// new a writer and point the writer to the file
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			writer.append(something.toString() + "\n");

			// writer the content to the file

			// always remember to close the writer
			writer.close();
			writer = null;
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public static String getExpDir() {

		return System.getProperty("user.dir");

	}

	public static void deleteLogFiles() {

		File folder = new File(getExpDir() + "/logFiles/");

		File[] files = folder.listFiles();
		if (files != null) { 
			for (File f : files) {
					f.delete();
			}
		}

	}

	public static ACQ_Learner learnerFromJar(String jarPath, StatManager stats) {
		return new ACQ_Learner() {
			@Override
			public boolean ask(ACQ_Query e) {
				try {
					final Process p = Runtime.getRuntime().exec("java -jar " + jarPath + " " + e.learnerAskingFormat());
					int res = p.waitFor();
					if (res == 2) {
						System.out.println("true");
						e.classify(true);
						stats.update(e);
						return true;
					} else if (res == 3) {
						System.out.println("false");
						e.classify(false);
						stats.update(e);
						return false;
					} else {
						System.err.println("Incorrect exit value from jar learner");
					}
				} catch (IOException | InterruptedException excep) {
					System.err.println("Incorrect exit value from jar learner");
				}
				return (Boolean) null;
			}
		};
	}
}