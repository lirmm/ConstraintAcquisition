package fr.lirmm.coconut.acquisition.core.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Learner;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

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

		File file = new File(getExpDir() + "/logFiles/" + file_name + ".csv");
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

	public static HashMap<String, String> parseCompTable() {
		HashMap<String, String> mapping = new HashMap<String, String>();
		try {
		BufferedReader csvReader = new BufferedReader(new FileReader("CompositionTableAllen.csv"));
		String row;
			int i=0;
			String[] header= new String[13];
			while ((row = csvReader.readLine()) != null) {
			    String[] data = row.split("\t");
			    
				if(i==0) {
					header=Arrays.copyOfRange(data, 1, data.length);
					i++;
					continue;}
				for(int j = 0 ; j<header.length;j++ ) {
					ArrayList<String>result=new ArrayList<String>();
					mapping.put(data[0]+" - "+header[j],data[j+1] );

					
				}
			    // do something with the data
			}
		
		csvReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mapping;
	}
	public static void parseSchedulingInstance(String instance,int nResource,int UB,int nTasks,int[] capacities,int[] durations,int[][] requirements,HashMap<Integer, ArrayList<Integer>> precedencies ) throws NumberFormatException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(instance + ".dzn"));
		String line;
		String str;

		while (((line = reader.readLine()) != null)) {
			if (line.isEmpty() == true) {
				continue;
			}

			// split the line according to spaces
			String[] lineSplited = line.split(" ");

			if (line.startsWith("n_res")) {
				nResource = Integer.parseInt(lineSplited[2].replace(";", ""));
				continue;
			}
			if (line.startsWith("rc")) {
				capacities = new int[nResource];
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				for (int i = 1; i <= nResource; i++) {
					capacities[i - 1] = Integer.parseInt(strSplited[i]);

				}
			}

			if (line.startsWith("n_tasks")) {
				nTasks = Integer.parseInt(lineSplited[2].replace(";", ""));
				continue;
			}

			if (line.startsWith("d")) {
				durations = new int[nTasks];
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				for (int i = 1; i <= nTasks; i++) {
					durations[i - 1] = Integer.parseInt(strSplited[i]);
					UB = UB + durations[i - 1];
				}
				continue;
			}

			if (line.startsWith("rr")) {
				requirements = new int[nResource][nTasks];
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");

				for (int i = 1; i <= nTasks; i++) {
					requirements[0][i - 1] = Integer.parseInt(strSplited[i]);

				}

			}
			int i = 1;
			while (!line.startsWith("rr") && line.contains("|")) {
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				for (int j = 1; j <= nTasks; j++) {
					requirements[i][j - 1] = Integer.parseInt(strSplited[j]);

				}
				line = reader.readLine();
				i++;
			}

			if (line.startsWith("suc")) {
				str = line;
				str = str.replaceAll("\\D", " ");
				ArrayList<Integer> task = new ArrayList<Integer>();
				String[] strSplited = str.split(" +");
				for (int j = 1; j < strSplited.length; j++) {
					task.add(Integer.parseInt(strSplited[j]) - 1);
				}
				precedencies.put(0, task);
			}
			i = 1;
			while (line != null && line.contains("{") && !line.startsWith("suc")) {
				str = line;
				str = str.replaceAll("\\D", " ");
				String[] strSplited = str.split(" +");
				ArrayList<Integer> task = new ArrayList<Integer>();
				for (int j = 1; j < strSplited.length; j++) {
					task.add(Integer.parseInt(strSplited[j]) - 1);
				}
				line = reader.readLine();
				precedencies.put(i, task);
				i++;
			}
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
	
	
}