
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


public abstract class DefaultExperience implements IExperience {
	protected ACQ_SelectionHeuristics heuristic1 = ACQ_SelectionHeuristics.Lex;

	protected static String vls = ValSelector.IntDomainRandom.toString();
	protected static String vrs = VarSelector.DomOverWDeg.toString();
	public String instance = "1";
	private int nTasks;

	public static ACQ_Mode mode;
	private long timeout = 5000;
	private boolean verbose;
	
	public File directory;
	protected int propagationchoice;
	protected int deadline;


	protected HashMap<Integer, List<Integer>> agents;
	

	public void setParams( long timeout, 
			boolean verbose) {
		this.timeout = timeout;
		
		this.verbose = verbose;

	}

	public boolean isVerbose() {
		return verbose;
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


	public Long getTimeout() {
		return timeout;
	}

	

	public void setSelectionHeuristic(ACQ_SelectionHeuristics heuristic) {
		this.heuristic1 = heuristic;
	}

	public ACQ_SelectionHeuristics getSelectionHeuristic() {
		return heuristic1;
	}

	public ACQ_Mode getMode() {
		return mode;
	}

	public void setMode(ACQ_Mode mode) {
		this.mode = mode;
	}

	public int getInstance() {
		return Integer.parseInt(instance);
	}

	public void setInstance(String instance) {
		this.instance = instance;
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
	@Override
	public int ACQRate(ACQ_Network target_network, ACQ_Network learned_network) {

        ArrayList<ACQ_Constraint> set = new ArrayList<ACQ_Constraint>();
		int cpt = 0;
		for (ACQ_IConstraint cst1 : learned_network) {
		
		if(((ACQ_Constraint)cst1) instanceof ACQ_DisjunctionConstraint) {

		for (ACQ_IConstraint cst : target_network) {
			if(((ACQ_Constraint)cst) instanceof ACQ_DisjunctionConstraint) {
				for (ACQ_IConstraint c : ((ACQ_DisjunctionConstraint)cst).constraintSet) {
					 if(((ACQ_DisjunctionConstraint)cst1).constraintSet.contains(c))
							cpt++;
						else
							set.add((ACQ_Constraint) c);
				}
				}
			else if(((ACQ_DisjunctionConstraint)cst1).constraintSet.contains(cst))
				cpt++;
			else
				set.add((ACQ_Constraint) cst);
		}}
		else {
		
			for (ACQ_IConstraint cst : target_network) {
				if(((ACQ_Constraint)cst) instanceof ACQ_DisjunctionConstraint) {
					for (ACQ_IConstraint c : ((ACQ_DisjunctionConstraint)cst).constraintSet) {
						 if(cst1.getName().equals(c.getName())&& cst1.getScope().diff(c.getScope()).size()==0)
								cpt++;
							else
								set.add((ACQ_Constraint) cst);
					}
					}
				else if(cst1.getName().equals(cst.getName())&& cst1.getScope().diff(cst.getScope()).size()==0)
					cpt++;
				else
					set.add((ACQ_Constraint) cst);
			}
			
				
			}
		
		}
		return cpt;

	}
	

	
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
			nbvars=(int) Math.sqrt(nbvars);
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
					System.out.println(i*n+j);
					System.out.println(query.values[i * n + j]);
					if(query.getScope().contains(i*n+j))
					st.append(StringUtils.pad((query.values[i * n + j]) + "", -3, " ")).append(" |");
					else
						st.append(StringUtils.pad((0) + "", -3, " ")).append(" |");

				}
				st.append(MessageFormat.format("\n{0}", line));
			}
			st.append("\n\n\n");
			System.out.println(st.toString());
		} else {
			n = query.getScope().size();
			StringBuilder st = new StringBuilder();
			String line = "+";
			for (int i = 0; i < n; i++) {
				line += "----+";
			}
			line += "\n";
			st.append(line);
			for (int i = 0; i < n; i++) {
				st.append("|");
				for (int j = 0; j < n; j++) {
					if (j == query.values[i])
						st.append(StringUtils.pad(("Q") + "", -3, " ")).append(" |");
					else
						st.append(StringUtils.pad(("*") + "", -3, " ")).append(" |");

				}
				st.append(MessageFormat.format("\n{0}", line));
			}
			st.append("\n\n\n");
			System.out.println(st.toString());
		}
	}

	public String getName() {
		return instance;
	}

	public int getPropagationchoice() {
		return propagationchoice;
	}

	public void setPropagation(int propagationchoice) {
		this.propagationchoice = propagationchoice;
	}

	public int getPropagation() {
		// TODO Auto-generated method stub
		return this.propagationchoice;
	}
	public int getDeadline() {
		// TODO Auto-generated method stub
		return this.deadline;
	}
	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
}
