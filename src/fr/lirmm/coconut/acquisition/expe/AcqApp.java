package fr.lirmm.coconut.acquisition.expe;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_Heuristic;
import fr.lirmm.coconut.acquisition.core.acqsolver.ValSelector;
import fr.lirmm.coconut.acquisition.core.acqsolver.VarSelector;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Algorithm;
import fr.lirmm.coconut.acquisition.core.parallel.ACQ_Partition;
import fr.lirmm.coconut.acquisition.core.tools.FileManager;
import fr.lirmm.coconut.acquisition.core.workspace.IExperience;

public class AcqApp {

	private static String exp;
	private static String file;
	private static String examplesfile;
	private static long timeout;
	private static ACQ_Heuristic heuristic;
	private static boolean normalizedCSP;
	private static boolean shuffle;
	private static ACQ_Partition partition;
	private static ACQ_Algorithm mode;
	private static int nb_threads;
	private static int maxqueries;
	private static String instance;
	private static String vls;
	private static String vrs;
	private static boolean verbose;
	private static boolean log_queries;
	private static boolean gui;

	public static void main(String args[]) throws IOException, ParseException {

		final Options options = configParameters();
		final CommandLineParser parser = new DefaultParser();
		final CommandLine line = parser.parse(options, args);

		FileManager.deleteLogFiles();


		// print header
		printHeader();

		
		boolean helpMode = line.hasOption("help") || args.length == 0;
		if (helpMode) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("puacq", options, true);
			System.exit(0);
		}

		// defaults options
		exp = "sudoku";
		normalizedCSP = true;
		shuffle = true;
		mode = ACQ_Algorithm.QUACQ;
		timeout = 50000; // five seconds
		heuristic = ACQ_Heuristic.SOL;
		vls = ValSelector.IntDomainRandom.toString();
		vrs = VarSelector.Random.toString();
		partition = ACQ_Partition.RANDOM;
		verbose = true;
		log_queries = false;
		gui = false;
		nb_threads = 10;
		instance = "10";
		examplesfile="";
		file="";
		maxqueries=100;
		///////////////////////

		// Check arguments and options
		for (Option opt : line.getOptions()) {
			checkOption(line, opt.getLongOpt());
		}
		// Build Experience
		IExperience expe = new ExpeBuilder().setExpe(exp).setFile(file).setAlgo(mode).setMaxqueries(maxqueries).setExamplesFile(examplesfile).setPartition(partition).setNbThreads(nb_threads)
				.setInstance(instance).setNormalizedCSP(normalizedCSP).setShuffle(shuffle).setTimeout(timeout)
				.setHeuristic(heuristic).setVarSelector(vrs).setValSelector(vls).setVerbose(verbose).setPartition(partition)
				.setDirectory(new File("src/fr/lirmm/coconut/quacq/bench/")).setQueries(log_queries).setGui(gui)
				.build();
		// Launch Experience
		expe.process();

	}

	// Add options here
	private static Options configParameters() {

		final Option helpFileOption = Option.builder("h").longOpt("help").desc("Display help message").build();

		final Option expOption = Option.builder("e").longOpt("exp")
				.desc("Predefined Experience: random / purdey / zebra / meetings / target / sudoku / jsudoku / latin / queens")
				.hasArg(true).argName("experience").required(false).build();

		final Option fileOption = Option.builder("f").longOpt("file")
				.desc("Customized Experience: fileName (for fileName*.bias and/or fileName*.target")
				.hasArg(true).argName("file").required(false).build();
		
		final Option examplefileOption = Option.builder("ef").longOpt("examplesfile")
				.desc("Conacq v1 classified examples file: fileName (for fileName*.queries")
				.hasArg(true).argName("examplesfile").required(false).build();
		
		final Option maxqueries = Option.builder("maxq").longOpt("maxqueries")
				.desc("Conacq v2 maximum queries")
				.hasArg(true).argName("maxqueries").required(false).build();

		final Option limitOption = Option.builder("t").longOpt("timeout").hasArg(true).argName("timeout in ms")
				.desc("Set the timeout limit to the specified time").required(false).build();

		final Option heuristicOption = Option.builder("heur").longOpt("heuristic").hasArg(true)
				.argName("generate-example heuristic").desc("Heuristic : SOL / MIN / MAX").required(false).build();

		final Option normalizedCSPOption = Option.builder("ncsp").longOpt("notnormalized").hasArg(false)
				.desc("Specify this option to set that not normalized CSP").required(false).build();

		final Option shuffleOption = Option.builder("s").longOpt("shuffle").hasArg(false)
				.desc("Specify this option to set shuffle findscope split to true").required(false).build();

		final Option guiOption = Option.builder("g").longOpt("gui").hasArg(false)
				.desc("Specify this option to launch graphical user interface").required(false).build();

		final Option partitionOption = Option.builder("p").longOpt("partition").hasArg(true).argName("Bias partition")
				.desc("partition: rand / scope / neigh / neg / rel / relneg / rule").required(false).build();

		final Option modeOption = Option.builder("a").longOpt("algo").hasArg(true).argName("Acquisition Algorithm")
				.desc("algo: conacq1, conacq2, quacq, pacq").required(false).build();

		final Option threadOption = Option.builder("th").longOpt("threads").hasArg(true).argName("nb threads")
				.desc("number of thread").required(false).build();

		final Option instOption = Option.builder("i").longOpt("instance").hasArg(true).argName("instance")
				.desc("instance, number of variables").required(false).build();

		final Option verboseOption = Option.builder("v").longOpt("verbose").hasArg(false).desc("verbose mode")
				.required(false).build();
		final Option queriesOption = Option.builder("q").longOpt("queries").hasArg(false).desc("log queries mode")
				.required(false).build();

		final Option VrsOption = Option.builder("vrs").longOpt("Vrs").hasArg(true).argName("Vrs")
				.desc("Vrs, variable selection heuristic : "
						+ "ActivityBased / AntiFirstFail / Cyclic / DomOverWDeg / FirstFail / GeneralizedMinDomVar / ImpactBased / InputOrder / Largest / MaxDelta"
						+ " / MaxRegret / MinDelta / Occurrence / Random / RandomVar / Smallest / BiasDeg")
				.required(false).build();

		final Option VlsOption = Option.builder("vls").longOpt("Vls").hasArg(true).argName("Vls").desc(
				"Vls, value selection heuristic : IntDomainBest / IntDomainImpact / IntDomainLast / IntDomainMax / IntDomainMedian / IntDomainMiddle\n"
						+ "IntDomainMinIntDomainRandom / IntDomainRandomBound / RealDomainMax / RealDomainMiddle / RealDomainMin / SetDomainMin")
				.required(false).build();
		// Create the options list
		final Options options = new Options();
		options.addOption(expOption);
		options.addOption(fileOption);
		options.addOption(guiOption);
		options.addOption(limitOption);
		options.addOption(heuristicOption);
		options.addOption(normalizedCSPOption);
		options.addOption(shuffleOption);
		options.addOption(helpFileOption);
		options.addOption(partitionOption);
		options.addOption(modeOption);
		options.addOption(threadOption);
		options.addOption(instOption);
		options.addOption(VrsOption);
		options.addOption(VlsOption);
		options.addOption(verboseOption);
		options.addOption(queriesOption);
		options.addOption(examplefileOption);
		options.addOption(maxqueries);

		return options;
	}

	// Check all parameters values
	public static void checkOption(CommandLine line, String option) {

		switch (option) {

		case "exp":
			exp = line.getOptionValue(option);
			break;
		case "file":
			file = line.getOptionValue(option);
			break;
		case "examplesfile":
			examplesfile = line.getOptionValue(option);
			break;
		case "maxqueries":
			maxqueries = Integer.parseInt(line.getOptionValue(option));
			break;
		case "timeout":
			timeout = Long.parseLong(line.getOptionValue(option));
			break;
		case "heuristic":
			heuristic = getHeuristic(line.getOptionValue(option));
			break;
		case "notnormalized":
			normalizedCSP = false;
			break;
		case "shuffle":
			shuffle = true;
			break;
		case "partition":
			
			partition = getPartition(line.getOptionValue(option));
			break;
		case "algo":
			mode = getMode(line.getOptionValue(option));
			break;
		case "threads":
			nb_threads = Integer.parseInt(line.getOptionValue(option));
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
		case "verbose":
			verbose = true;
			break;
		case "queries":
			log_queries = true;
			break;
			
		case "gui":
			gui = true;
			break;
		default: {
			System.err.println("Bad arg parameter: " + option);
			System.exit(2);
		}

		}

	}

	public static ACQ_Partition getPartition(String name) {

		switch (name) {
		case "rand":
			return ACQ_Partition.RANDOM;
		case "scope":
			return ACQ_Partition.SCOPEBASED;
		case "neigh":
			return ACQ_Partition.NEIGHBORHOOD;
		case "neg":
			return ACQ_Partition.NEGATIONBASED;
		case "rel":
			return ACQ_Partition.RELATIONBASED;
		case "relneg":
			return ACQ_Partition.RELATION_NEGATIONBASED;
		case "rule":
			return ACQ_Partition.RULESBASED;
		default: {

			System.err.println("Bad partition parameter: " + name);
			System.exit(2);
		}

		}
		return null;
	}

	public static ACQ_Algorithm getMode(String name) {

		return ACQ_Algorithm.valueOf(name.toUpperCase());

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

	private static void printHeader() {

		String header = "--------------------------------------------------------------------------\n";
		header += "|                                                                         |\n";
		header += "|              CONSTRAINT ACQUISITION PLATEFORM                           |\n";
		header += "|                                                                         |\n";
			header += "--------------------------------------------------------------------------\n";
		System.out.println(header);

	}

}
