package fr.lirmm.coconut.acquisition.core.workspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.chocosolver.util.tools.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.acqsolver.ValSelector;
import fr.lirmm.coconut.acquisition.core.acqsolver.VarSelector;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Algorithm;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_Partition;
import fr.lirmm.coconut.acquisition.core.tools.FileManager;

public abstract class DefaultExperience implements IExperience {
	protected ACQ_Heuristic heuristic = ACQ_Heuristic.SOL;
	private boolean normalizedCSP = true;
	private boolean shuffle_split = true;
	private boolean allDiff_detection = true;
	private boolean parallel = false;
	protected static String vls = ValSelector.IntDomainRandom.toString();
	protected static String vrs = VarSelector.DomOverWDeg.toString();
	public String instance = "1";
	public int nb_threads = 1;
	public ACQ_Partition partition;
	public static ACQ_Algorithm algo;
	protected int dimension = -1;
	private long timeout = 5000;
	private boolean verbose;
	private boolean log_queries;
	boolean puzzleProblem = false;
	boolean queens = false;
	public String name;
	public File directory;
	public String examplesfile;
	public int maxqueries;

	protected int nMeetings; // number of meetings to be scheduled
	protected static int mAgents; // the number of agents
	protected int timeslots; // timeslots available
	protected int[][] attendance; // container for the first matrix of the input file (each agent and his meetings
	// attendance)
	protected int[][] distance; // container for the second matrix of the input file (distance between meetings)
	protected HashMap<Integer, List<Integer>> agents;
	// target
	protected int nb_targets;
	protected int nb_sensors;
	protected int domain;


	protected int[][] visibility;
	protected int[][] compatibility;

	public void setParams(boolean normalizedCSP, boolean shuffle_split, long timeout, ACQ_Heuristic heuristic,
			boolean verbose, boolean log_queries) {
		this.shuffle_split = shuffle_split;
		this.timeout = timeout;
		this.normalizedCSP = normalizedCSP;
		this.heuristic = heuristic;
		this.verbose = verbose;
		this.log_queries = log_queries;

	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setPuzzlePrint(boolean pp) {
		this.puzzleProblem = pp;
	}

	public boolean isPuzzlePrint() {
		return puzzleProblem;
	}

	public void setQueens(boolean queens) {
		this.queens = queens;
	}

	public boolean isQueens() {
		return queens;
	}

	public boolean isLog_queries() {
		return log_queries;
	}

	public void setLog_queries(boolean log_queries) {
		this.log_queries = log_queries;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getVrs() {
		return vrs;
	}

	public void setVrs(String vrs) {
		this.vrs = vrs;
	}

	public String getVls() {
		return vls;
	}

	public void setVls(String vls) {
		this.vls = vls;
	}

	// dimension of the board
	public int getDimension() {
		return dimension; //
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setDimension(int dimension) {
		this.dimension = dimension;
	}

	public boolean isShuffleSplit() {
		return shuffle_split;
	}

	public void setShuffleSplit(boolean shuffle_split) {
		this.shuffle_split = shuffle_split;
	}

	public boolean isAllDiffDetection() {
		return allDiff_detection;
	}

	public void setAllDiffDetection(boolean allDiff_detection) {
		this.allDiff_detection = allDiff_detection;
	}

	public void setNormalizedCSP(boolean normalizedCSP) {
		this.normalizedCSP = normalizedCSP;
	}

	public boolean isNormalizedCSP() {
		return normalizedCSP;
	}

	public ACQ_Heuristic getHeuristic() {
		return heuristic;
	}

	public void setHeuristic(ACQ_Heuristic heuristic) {
		this.heuristic = heuristic;
	}

	public boolean isParallel() {
		return parallel;
	}

	public void setParallel(boolean parallel) {
		this.parallel = parallel;
	}

	public int getNb_threads() {
		return nb_threads;
	}

	public void setNb_threads(int nb_threads) {
		this.nb_threads = nb_threads;
	}

	public ACQ_Partition getPartition() {
		return partition;
	}

	public void setPartition(ACQ_Partition partition) {
		this.partition = partition;
	}

	public ACQ_Algorithm getAlgo() {
		return algo;
	}

	public void setAlgo(ACQ_Algorithm Algo) {
		this.algo = Algo;
	}

	public int getInstance() {
		return Integer.parseInt(instance);
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}

	public void readDataset() {
		try (Scanner sc = new Scanner(
				new File("src/fr/lirmm/coconut/acquisition/bench/Meetings/problem" + instance + ".txt"))) {

			/* process input */
			nMeetings = sc.nextInt();
			mAgents = sc.nextInt();
			timeslots = sc.nextInt();

			attendance = new int[mAgents][nMeetings]; // this is only needed to compute which meetings can be in
														// parallel
			distance = new int[nMeetings][nMeetings]; // this is used to apply the travel contraints

			/* construct attendance matrix */
			for (int i = 0; i < mAgents; i++) {
				int n = 0;
				sc.next();
				for (int j = 0; j < nMeetings; j++) {
					attendance[i][j] = sc.nextInt();
				}
			}

			/* construct distance matrix */
			for (int i = 0; i < nMeetings; i++) {
				sc.next();
				for (int j = 0; j < nMeetings; j++) {
					distance[i][j] = sc.nextInt();
				}
			}
		} catch (Exception e) {

		}
		agents = getAgents();

	}

	public HashMap<Integer, List<Integer>> getAgents() {

		HashMap<Integer, List<Integer>> agents = new HashMap<>();
		Set<Set<Integer>> agents1 = new HashSet<>();

		for (int i = 0; i < mAgents; i++) {
			List<Integer> meetings = new ArrayList();
			for (int j = 0; j < nMeetings; j++) {
				if (attendance[i][j] == 1) {
					meetings.add(j);
				}
			}
			Collections.sort(meetings);
			agents.put(i, meetings);

		}

		return agents;
	}

	public void readTarget() {

//		File directory = new File("./TargetSensing/");

		File[] files = directory.listFiles();
		try {
			for (File file : files) {
				if (file.getName().contains("xml"))
					Parse_Problem(directory.getPath() + "/" + file.getName());

			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try (Scanner sc = new Scanner(
				new File(directory.getAbsolutePath() + "/TargetSensing/SensorDCSP_25_5_" + instance + ".txt"))) {

			/* process input */
			nb_targets = sc.nextInt();
			nb_sensors = sc.nextInt();
			domain = sc.nextInt();
			this.visibility = new int[nb_targets][nb_sensors];
			this.compatibility = new int[nb_sensors][nb_sensors];
			/* construct attendance matrix */
			for (int i = 0; i < nb_targets; i++) {
				int n = 0;
				sc.next();
				for (int j = 0; j < nb_sensors; j++) {
					visibility[i][j] = sc.nextInt();
				}
			}

			/* construct distance matrix */
			for (int i = 0; i < nb_sensors; i++) {
				sc.next();
				for (int j = 0; j < nb_sensors; j++) {
					compatibility[i][j] = sc.nextInt();
				}
			}
			System.out.print("Compatibility\n");

			for (int[] i : compatibility)
				System.out.println(Arrays.toString(i));
			System.out.print("----------------\n");
			System.out.print("Visibility\n");

			for (int[] i : visibility)
				System.out.println(Arrays.toString(i));
		} catch (Exception e) {
			System.out.print(e);
		}
		System.out.print("\n");
	}

	public void Parse_Problem(String file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new File(file));
		HashMap<String, Integer[]> target_domain = new HashMap();

		FileWriter writer = new FileWriter(file.replace(".xml", "") + ".txt", false);
		NodeList variables = document.getElementsByTagName("variable");
		NodeList domains = document.getElementsByTagName("domain");
		NodeList relations = document.getElementsByTagName("relation");
		ArrayList<String[]> sensor_relation = new ArrayList();

		for (int temp = 0; temp < variables.getLength(); temp++) {
			Node node = variables.item(temp);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) node;
				for (int temp1 = 0; temp1 < domains.getLength(); temp1++) {
					Node node1 = domains.item(temp1);
					if (node1.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement1 = (Element) node1;

						if (eElement.getAttribute("domain").equals(eElement1.getAttribute("name"))) {

							target_domain.put(eElement.getAttribute("name"),
									String_to_Integerarr(eElement1.getTextContent()));
						}
					}
				}
			}
		}
		for (int temp = 0; temp < relations.getLength(); temp++) {
			Node node = relations.item(temp);
			System.out.println(""); // Just a separator
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				// Print each employee's detail
				Element eElement = (Element) node;
				sensor_relation.add(eElement.getTextContent().split("\\|"));
			}
		}
		int max = getMax(target_domain);
		writer.write(target_domain.size() + " " + max + " " + max + "\n\n");
		String visibility = getvisibility(target_domain, max);
		writer.write(visibility + "\n\n");
		String compatibility = getcompatibility(max, sensor_relation);
		writer.write(compatibility);

		writer.close();

	}

	public Integer[] String_to_Integerarr(String s) {
		String[] temp = s.split(" ");
		Integer[] array = new Integer[temp.length];
		for (int i = 0; i < array.length; i++)
			array[i] = Integer.parseInt(temp[i]);
		return array;
	}

	public Integer getMax(HashMap<String, Integer[]> map) {
		Integer max = Integer.MIN_VALUE;
		for (Integer[] a : map.values())
			for (Integer i : a)
				if (i > max)
					max = i;
		return max;
	}

	public String getvisibility(HashMap<String, Integer[]> map, int max) {
		int[][] visibility = new int[map.size()][max + 1];
		String s = "";
		int i = 0;
		for (Integer[] a : map.values()) {
			for (int id : a) {
				visibility[i][id] = 1;
			}
			i++;
		}
		for (int k = 0; k < map.size(); k++) {
			for (int j = 0; j < max; j++) {
				s += visibility[k][j] + " ";
			}
			s += "\n";
		}
		return s;
	}

	public String getcompatibility(int size, ArrayList<String[]> map) {
		int[][] compatibility = new int[size + 1][size + 1];
		String s = "";
		for (String[] a : map) {
			for (String id : a) {
				int x = Integer.parseInt(id.split(" ")[0]);
				int y = Integer.parseInt(id.split(" ")[1]);

				compatibility[x][y] = 1;
			}
		}
		for (int k = 0; k < compatibility.length; k++) {
			for (int j = 0; j < compatibility.length; j++) {
				s += compatibility[k][j] + " ";
			}
			s += "\n";
		}
		return s;
	}

	public boolean convergenceCheck(ACQ_Network target_network, ACQ_Network learned_network) {
		return false;

	}

	public int convergenceRate(ACQ_ConstraintSolver solver, ACQ_Network target_network, ACQ_Network learned_network) {

		solver.setVars(learned_network.getVariables());

		int cpt = 0;
		FileManager.printFile(learned_network, "CL");

		for (ACQ_IConstraint cst : target_network) {

			if (!learned_network.getVariables().containsAll(cst.getScope()))
				cpt++;
			else if (!learned_network.contains(cst)) {
				ACQ_Network test = new ACQ_Network(learned_network.getFactory(), learned_network.getVariables(),
						learned_network.getConstraints());
				test.add(cst.getNegation(), true);
				if (solver.solve(test))
					{
					cpt++;
					FileManager.printFile(cst, "missingCST");
					}
			}

		}

		return cpt;

	}
	
	public boolean getAnswer(ACQ_Query e,int n) {
    	System.out.println("QUACQ");

		if(isPuzzlePrint()) {
			if(isQueens())
				puzzleprint(e,true,n);
				else
					puzzleprint(e, false,n);
		}else {
    	System.out.println(e.learnerAskingFormat());
		}
		System.out.println("Is it a solution ? (y/n)");
		Scanner in = new Scanner(System.in);
		String userAnswer;
		do {
			userAnswer = in.next();
			System.out.println("You entered:" + userAnswer);
			if(!userAnswer.equals("y") && !userAnswer.equals("n")) {
				System.out.println("Incorrect answer.\n Please enter y or n.");
			}
		} while(!userAnswer.equals("y") && !userAnswer.equals("n"));
		
		return userAnswer.equals("y") ;
    }
	private int nTasks;

	
	public int[] getDuraitons(String instance) {
		int [] duration = null;
		BufferedReader reader;//Bachir: To put as parameter
		try {
			reader = new BufferedReader(new FileReader("benchmarks/scheduling/rcpsp/"+instance+".data"));
			String line;
			String str;
			while (((line = reader.readLine()) != null)) {
				String[] lineSplited = line.split(" ");
				if (line.startsWith("n_tasks")) {
					nTasks = Integer.parseInt(lineSplited[2].replace(";", ""));
					continue;
				}
				
				if (line.startsWith("d")) {
					duration = new int[nTasks];
					str = line;
					str = str.replaceAll("\\D", " ");
					String[] strSplited = str.split(" +");
					for (int i = 1; i <= nTasks; i++) {
						duration[i - 1] = Integer.parseInt(strSplited[i]);
					}
					continue;
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return duration;
	}
	public static void puzzleprint(ACQ_Query query, boolean queens,int nbvars) {
		int n;
		if (!queens) {
			nbvars = (int) Math.sqrt(nbvars);

			n = (int) Math.sqrt(query.getScope().size());
			StringBuilder st = new StringBuilder();
			String line = "+";
			for (int i = 0; i < nbvars; i++) {
				line += "----+";
			}
			line += "\n";
			st.append(line);
			for (int i = 0; i < nbvars; i++) {
				st.append("|");
				for (int j = 0; j < nbvars; j++) {
					try {
					st.append(StringUtils.pad((query.values[i * n + j]) + "", -3, " ")).append(" |");
				}catch(Exception e) {
					st.append(StringUtils.pad(0 + "", -3, " ")).append(" |");

				}
					}
				st.append(MessageFormat.format("\n{0}", line));
			}
			st.append("\n\n\n");
			System.out.println(st.toString());
		} else {
			n = query.getScope().size();
			StringBuilder st = new StringBuilder();
			String line = "+";
			for (int i = 0; i < nbvars; i++) {
				line += "----+";
			}
			line += "\n";
			st.append(line);
			for (int i = 0; i < nbvars; i++) {
				st.append("|");
				for (int j = 0; j < nbvars; j++) {
					try {
						if (j == query.values[i])
							st.append(StringUtils.pad(("Q") + "", -3, " ")).append(" |");
						else
							st.append(StringUtils.pad(("*") + "", -3, " ")).append(" |");
					}catch(Exception e) {
						st.append(StringUtils.pad(("*") +  "", -3, " ")).append(" |");

					}
					
				}
				st.append(MessageFormat.format("\n{0}", line));
			}
			st.append("\n\n\n");
			System.out.println(st.toString());
		}
	}

	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}
	public void setName(String name) {
		// TODO Auto-generated method stub
		this.name= name;
	}

	public String getExamplesfile() {
		return examplesfile;
	}

	public void setExamplesfile(String examplesfile) {
		this.examplesfile = examplesfile;
	}

	public int getMaxqueries() {
		return maxqueries;
	}

	public void setMaxqueries(int maxqueries) {
		this.maxqueries = maxqueries;
	}
	
}
