package GEQCA;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Algorithm;



public class ExpeGEQCA {

	private static  ExpeParser experiment;
	private static String exp;
	private static long timeout;
	private static ACQ_Heuristic heuristic;
	private static ACQ_SelectionHeuristics sheuristic;
	private static boolean normalizedCSP;
	private static boolean shuffle;
	private static ACQ_Algorithm mode;
	private static int nb_threads;
	private static String instance;
	private static String vls;
	private static String vrs;
	private static boolean verbose;
	private static boolean log_queries;
	private static boolean gui;
	private static boolean isdisjunction;
	private static int disjunctionlevel;
	private static int propagationchoice;
	private static int deadline;
	private static String algebratype;

	public static void main(String args[]) throws IOException, ParseException {
		
		final Options options = configParameters();
		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		//FileManager.deleteLogFiles();
		
		boolean helpMode = line.hasOption("help") || args.length == 0;
		if (helpMode) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("puacq", options, true);
			System.exit(0);
		}

		// defaults options
		exp="tasks10_0.08_1";

		mode = ACQ_Algorithm.LQCN;
		timeout = 50000; // five seconds
		sheuristic = ACQ_SelectionHeuristics.Lex;
		verbose = false;
		instance="10";
		propagationchoice=0;
		deadline=0;
		algebratype="Allen";

		///////////////////////

		// Check arguments and options
		for (Option opt : line.getOptions()) {
			checkOption(line, opt.getLongOpt());
		}
		//Build Experience
		IExperience expe = new ExpeBuilder().setExpe(exp).
				setMode(mode).
				setInstance(exp).
				setTimeout(timeout).
				setSelectionHeuristic(sheuristic).
				setVerbose(verbose)
				.setInstance(exp)
				.setPropagation(propagationchoice)
				.setDeadline(deadline)
				.setAlgebraType(algebratype)

				.build();
		//Launch Experience
				expe.process();
		
	}

	// Add options here
	private static Options configParameters() {

		final Option helpFileOption = Option.builder("h").longOpt("help").desc("Display help message").build();

		final Option expOption = Option.builder("e").longOpt("exp")
				.desc("Experience: random / purdey / zebra / meetings / target / sudoku / jsudoku / latin / queens")
				.hasArg(true).argName("experience").required(false).build();

		final Option limitOption = Option.builder("t").longOpt("timeout").hasArg(true).argName("timeout in ms")
				.desc("Set the timeout limit to the specified time").required(false).build();

		final Option selectionheuristicOption = Option.builder("sh").longOpt("selectionheuristic").hasArg(true)
				.argName("constraint selection heuristic").desc("Selection Heuristic : Lex / Random / Weighted/Path").required(false).build();

		
		final Option propagationOption = Option.builder("prop").longOpt("prop").hasArg(true).argName("Propagation Mode")
				.desc("Propagation Mode : 0 (simple propagation), 1 (solve propagation w L), 2 (solve propagation w/ L), 3 ( 0 + 1), 4 ( 0 + 2 )").required(false).build();
		final Option deadline = Option.builder("deadline").longOpt("deadline").hasArg(true).argName("Task Deadline")
				.desc("Deadline").required(false).build();

		
		final Option modeOption = Option.builder("m").longOpt("mode").hasArg(true).argName("LQCN or GEQCA")
				.desc("mode: LQCN, GEQCA,GEQCA_IQ").required(false).build();

	
		final Option instOption = Option.builder("i").longOpt("instance").hasArg(true).argName("instance")
				.desc("instance, number of variables").required(false).build();
		final Option algtypeOption = Option.builder("at").longOpt("algebra").hasArg(true).argName("algebra")
				.desc("set algebra type ex : Allen, RCC-8...").required(false).build();

		final Option verboseOption = Option.builder("v").longOpt("verbose").hasArg(false)
				.desc("verbose mode").required(false).build();
		
		final Option VlsOption = Option.builder("vls").longOpt("Vls").hasArg(true).argName("Vls").desc(
				"Vls, value selection heuristic : IntDomainBest / IntDomainImpact / IntDomainLast / IntDomainMax / IntDomainMedian / IntDomainMiddle\n"
						+ "IntDomainMinIntDomainRandom / IntDomainRandomBound / RealDomainMax / RealDomainMiddle / RealDomainMin / SetDomainMin")
				.required(false).build();
		// Create the options list
		final Options options = new Options();
		options.addOption(expOption);
		options.addOption(limitOption);
		options.addOption(selectionheuristicOption);
		options.addOption(helpFileOption);
		options.addOption(propagationOption);
		options.addOption(deadline);
		options.addOption(modeOption);
		options.addOption(instOption);
		options.addOption(VlsOption);
		options.addOption(verboseOption);
		options.addOption(algtypeOption);

		return options;
	}

	// Check all parameters values
	public static void checkOption(CommandLine line, String option) {

		switch (option) {
		case "exp":
			exp = line.getOptionValue(option);
			break;
		case "timeout":
			timeout = Long.parseLong(line.getOptionValue(option));
			break;
		case "selectionheuristic":
			sheuristic = getSelectionHeuristic(line.getOptionValue(option));
			break;
		

		case "mode":
			mode = getMode(line.getOptionValue(option));
			break;
		
		case "instance":
			instance = line.getOptionValue(option);
			break;
		case "Vrs":
			vrs = line.getOptionValue(option);
			break;
		case "Vls":
			vls = line.getOptionValue(option);
			break;
		
		case "prop":
			propagationchoice = Integer.parseInt(line.getOptionValue(option));
			break;
		case "deadline":
			deadline = Integer.parseInt(line.getOptionValue(option));
			break;
		case "algebra":
			algebratype = line.getOptionValue(option);
			break;
		case "verbose":
			verbose = true;
			break;
		
				default: {
			System.err.println("Bad parameter: " + option);
			System.exit(2);
		}

		}

	}


	public static ACQ_Algorithm getMode(String name) {

		switch (name) {
		case "lqcn":
			return ACQ_Algorithm.LQCN;
		case "geqca":
			return ACQ_Algorithm.GEQCA;
		case "geqca_iq":
			return ACQ_Algorithm.GEQCA_IQ;
		
		case "geqca_bk_iq":
			return ACQ_Algorithm.GEQCA_BK_IQ;
	
		default: {
			System.err.println("Bad mode parameter: " + name);
			System.exit(2);
		}

		}
		return null;
	}

	public static ACQ_Heuristic getHeuristic(String name) {

		switch (name) {
		case "sol":
			return ACQ_Heuristic.SOL;
		case "max":
			return ACQ_Heuristic.MAX;
		case "min":
			return ACQ_Heuristic.MIN;
		default: {
			System.err.println("Bad heuristic parameter: " + name);
			System.exit(2);
		}

		}
		return null;
	}
	public static ACQ_SelectionHeuristics getSelectionHeuristic(String name) {

		switch (name) {
		case "lex":
			return ACQ_SelectionHeuristics.Lex;
		case "pathw":
			return ACQ_SelectionHeuristics.PathWeighted;
		case "path":
			return ACQ_SelectionHeuristics.Path;
		case "random":
			return ACQ_SelectionHeuristics.Random;
		case "weighted":
			return ACQ_SelectionHeuristics.Weighted;
		default: {
			System.err.println("Bad Selection heuristic parameter: " + name);
			System.exit(2);
		}

		}
		return null;
	}
}