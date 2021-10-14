package fr.lirmm.coconut.acquisition.core.parallel;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqconstraint.BinaryArithmetic;
import fr.lirmm.coconut.acquisition.core.acqconstraint.BinaryConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Operator;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintFactory.ConstraintSet;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;
import fr.lirmm.coconut.acquisition.core.tools.FileManager;

public class ACQ_PACQ_Manager implements IManager {

	public ACQ_Bias bias;
	public ACQ_Bias bias_i;
	public ConcurrentHashMap<String, ACQ_Bias> bias_partitions;
	ConcurrentHashMap<String, ACQ_Bias> biases_in_use;
	CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox;
	HashMap<Integer, ACQ_Bias> partitions;
	boolean query_sharing = true;
	private ACQ_Network learned_network;
	public Semaphore semaphore;
	// public boolean running = true;

	private ACQ_Partition partitionType = ACQ_Partition.RANDOM; // {RANDOM, SCOPEBASED, NEIGHBORHOOD,
																// NEGATIONBASED, ARITHMBASED, ARITHM_NEGATIONBASED}
	private Long acqTime;
	private ThreadMXBean threadBean;

	public ACQ_PACQ_Manager(CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox) {
		this.queries_mailbox = queries_mailbox;
		this.biases_in_use = new ConcurrentHashMap<>();
		this.semaphore = new Semaphore(1);
		this.threadBean = ManagementFactory.getThreadMXBean();

		// currentGroup = Thread.currentThread().getThreadGroup();

	}

	@Override
	public void setBias(ACQ_Bias bias) {
		this.bias = bias;
	}

	@Override
	public synchronized ACQ_Bias getBias() {

		return this.bias;
	}

	public synchronized ACQ_Bias getBias_i() {

		return bias_i;
	}

	public synchronized void setBias_i(ACQ_Bias bias_i) {
		this.bias_i = bias_i;
	}

	@Override
	public boolean isQuery_sharing() {
		return query_sharing;
	}

	public void applyPartitioning(int Users) {
		switch (partitionType) {
		case RANDOM:
			this.bias_partitions = Partition_Bias_Random(Users);
			break;
		case SCOPEBASED:
			this.bias_partitions = Bias_Partition_ScopeBased(Users);
			break;
		case NEIGHBORHOOD:
			this.bias_partitions = Bias_Partition_Neighborhood(Users);
			break;
		case NEGATIONBASED:
			this.bias_partitions = Partition_Bias_NegationBased(Users);
			break;
		case RELATIONBASED:
			this.bias_partitions = Partition_Bias_ArithmBased(Users);
			break;
		case RELATION_NEGATIONBASED:
			this.bias_partitions = Partition_Bias_NegationArithmBased(Users);
			break;
		case RULESBASED:
			this.bias_partitions = Partition_Bias_TransitiveBased(Users);
			break;

		}

	}

	@Override
	public void setQuery_sharing(boolean memory_enabled) {
		this.query_sharing = memory_enabled;
	}

	/**
	 * 
	 * @param example
	 */

	@Override
	public synchronized boolean ask_query(ACQ_Query example) {
		boolean asked_query = false;

		if (query_sharing) {
			if (!queries_mailbox.isEmpty())

				for (ACQ_QueryMessage query_m : queries_mailbox) {
					if ((example.extend(query_m.getQuery()) && query_m.getQuery().isNegative())
							|| (query_m.getQuery().extend(example) && query_m.getQuery().isPositive())) {
						// FileManager.printFile("Thread :"+Thread.currentThread().getName() +
						// "::"+example , "asked_query");

						example.classify_as(query_m.getQuery());
						asked_query = true;
						break;
					}
				}

		}
		return asked_query;

	}

	public boolean ask(ACQ_Query e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void non_asked_query(ACQ_Query query) {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 * @param
	 */
	public synchronized boolean send(ACQ_QueryMessage query_m) {

		if (query_sharing) {
			queries_mailbox.add(query_m);
			return true;
		}
		return false;
	}

	public void setLearnedNetwork(ACQ_Scope vars) {

		// ConstraintFactory factory= new ConstraintFactory();
		this.learned_network = new ACQ_Network(bias.getNetwork().getFactory(), vars);
	}

	public synchronized ACQ_Bias getPartition(String id) {
		if (bias_partitions.get(id) == null) {
			// ConstraintFactory factory= new ConstraintFactory();
			bias_partitions.put(id, new ACQ_Bias(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars())));
		}
		biases_in_use.put(id, bias_partitions.get(id));
		setBias_i(bias_partitions.get(id));

		return getBias_i();
	}
	/*
	 * public ACQ_Bias get_Partition(String id) {
	 * 
	 * int i =0; while(true) { i = new
	 * Random(System.currentTimeMillis()).nextInt(bias_partitions.size());
	 * if(!biases_in_use.containsValue(bias_partitions.get(i))) {
	 * biases_in_use.put(id,bias_partitions.get(i));
	 * setBias_i(bias_partitions.get(i)); break; }
	 * 
	 * } return getBias_i(); } public void release_partition(String id) {
	 * biases_in_use.remove(id); }
	 * 
	 * 
	 * public ACQ_Scope get_Vars() { BitSet vars =
	 * this.bias.getVars().getVariables(); List<Integer> scopes = bits2Ints(vars);
	 * Collections.shuffle(scopes); BitSet bs = new BitSet(); for(int i : scopes) {
	 * bs.set(i); } return new ACQ_Scope(bs); }
	 */

	public ConcurrentHashMap<String, ACQ_Bias> Partition_Bias_Random(int Users) {

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		List<ACQ_Network> networks = new ArrayList<>(Users);

		List<Integer> id_users = new ArrayList<>();
		for (int i = 0; i < Users; i++) {
			// ConstraintFactory constraintFactory=new ConstraintFactory();

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
			id_users.add(i);
		}
		Collections.shuffle(id_users);
		int id = 0;
		for (ACQ_IConstraint cst : bias.getConstraints()) {
			networks.get(id_users.get(id)).add(cst, false);
			id = (id + 1) % Users;
		}
		for (int i = 0; i < Users; i++) {
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));
			System.out.println("bias-"+i+"::"+ networks.get(i).size());

		}

		return partitions;

	}

	public ConcurrentHashMap<String, ACQ_Bias> Partition_Bias_NegationBased(int Users) {

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		List<ACQ_Network> networks = new ArrayList<>(Users);
		List<Integer> id_users = new ArrayList<>();

		ACQ_Network constraints = new ACQ_Network(bias.getNetwork().getFactory(), bias.getNetwork().getConstraints());

		for (int i = 0; i < Users; i++) {

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
			id_users.add(i);
		}
		Collections.shuffle(id_users);
		int id = 0;
		for (ACQ_IConstraint cst : constraints) {
			networks.get(id_users.get(id)).add(cst, false);
			networks.get(id_users.get(id)).add(cst.getNegation(), false);
			constraints.remove(cst);
			constraints.remove(cst.getNegation());
			id = (id + 1) % Users;
		}
		for (int i = 0; i < Users; i++) {
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));
		}

		return partitions;

	}

	public ConcurrentHashMap<String, ACQ_Bias> Partition_Bias_NegationArithmBased(int Users) {

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		List<ACQ_Network> networks = new ArrayList<>(Users);
		Set<String> language = bias.getLanguage();
		List<Integer> id_users = new ArrayList<>();
		ConcurrentHashMap<String, ACQ_Network> arithms = new ConcurrentHashMap<String, ACQ_Network>();

		ACQ_Network constraints = new ACQ_Network(bias.getNetwork().getFactory(), bias.getNetwork().getConstraints());

		for (int i = 0; i < Users; i++) {

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
			id_users.add(i);
		}
		for (String l : language) {
			ACQ_Network network = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());

			for (ACQ_IConstraint c : constraints) {
				if (c.getName().equals(l)) {
					network.add(c, true);
					network.add(c.getNegation(), true);
					constraints.remove(c);
					constraints.remove(c.getNegation());
				}
			}
			if (network.size() > 0)
				arithms.put(l, network);

		}

		int id = 0;
		int id1 = 0;

		int size = 0;
		for (ACQ_Network net : arithms.values()) {
			if (size < (arithms.size() / 2)) {
				for (ACQ_IConstraint cst : net.getConstraints()) {

					networks.get(id).add(cst, false);
					id = (id + 1) % ((Users / 2));

				}

				size++;
			} else {
				id1 = (Users / 2);
				for (ACQ_IConstraint cst : net.getConstraints()) {

					networks.get(id1).add(cst, false);
					id1 = ((id1 + 1)) % (Users);
					if (id1 == 0)
						id1 = (Users / 2);

				}
				size++;
			}

		}

		for (int i = 0; i < Users; i++) {
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));
		}

		return partitions;

	}

	public ConcurrentHashMap<String, ACQ_Bias> Partition_Bias_ArithmBased(int Users) {

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		ConcurrentHashMap<String, ACQ_Network> arithms = new ConcurrentHashMap<String, ACQ_Network>();
		Set<String> Language = bias.getLanguage();
		for (String l : Language) {
			ACQ_Network network = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());

			for (ACQ_IConstraint c : bias.getConstraints()) {
				if (c.getName().equals(l))
					network.add(c, true);

			}
			arithms.put(l, network);
		}
		List<ACQ_Network> networks = new ArrayList<>(Users);

		for (int i = 0; i < Users; i++) {

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
		}

		int id = 0;
		int id1 = 0;

		int size = 0;
		for (ACQ_Network net : arithms.values()) {
			if (size < (arithms.size() / 2)) {
				for (ACQ_IConstraint cst : net.getConstraints()) {

					networks.get(id).add(cst, false);
					id = (id + 1) % ((Users / 2));

				}

				size++;
			} else {
				id1 = (Users / 2);
				for (ACQ_IConstraint cst : net.getConstraints()) {

					networks.get(id1).add(cst, false);
					id1 = ((id1 + 1)) % (Users);
					if (id1 == 0)
						id1 = (Users / 2);

				}
				size++;
			}

		}

		for (int i = 0; i < Users; i++) {
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));
		}

		return partitions;

	}

	public ConcurrentHashMap<String, ACQ_Bias> Partition_Bias_PosetBased(int Users) {

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		ConcurrentHashMap<String, ACQ_Network> arithms = new ConcurrentHashMap<String, ACQ_Network>();
		Set<String> Language = bias.getLanguage();

		String[] relations = getRelationsNames(ACQ_Relations.class);
		String[] as_relations = getRelationsNames(ACQ_ASRelations.class);
		String[] trans_relations = getRelationsNames(ACQ_ASRelations.class);

		for (String l : Language) {
			ACQ_Network network = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());

			for (ACQ_IConstraint c : bias.getConstraints()) {
				if (c.getName().equals(l))
					network.add(c, true);

			}
			arithms.put(l, network);
		}
		List<ACQ_Network> networks = new ArrayList<>(Users);

		for (int i = 0; i < Users; i++) {

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
		}

		for (int i = 0; i < relations.length && arithms.get(relations[i]) != null; i++) {

			ACQ_Network net = arithms.get(relations[i]);

			for (ACQ_IConstraint cst : net.getConstraints()) {
				int index = antisymetry(as_relations, networks, cst);
				if (index == -1)
					index = transitivity(trans_relations, networks, cst);
				if (index != -1)
					networks.get(index).add(cst, true);
				else
					networks.get(minSize(networks)).add(cst, true);
			}

		}

		for (int i = 0; i < Users; i++) {
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));
		}

		return partitions;

	}

	public ConcurrentHashMap<String, ACQ_Bias> Partition_Bias_TransitiveBased(int Users) {

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		List<ACQ_Network> arithms = new ArrayList<ACQ_Network>();
		List<ACQ_Network> prePartition = new ArrayList<ACQ_Network>();

		Set<String> Language = bias.getLanguage();

		String[] trans_relations = getRelationsNames(ACQ_TRelations.class);

		List<Integer> id_users = new ArrayList<>();

		for (String l : Language) {
			ACQ_Network network = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());

			for (ACQ_IConstraint c : bias.getConstraints()) {
				if (c.getName().equals(l))
					network.add(c, true);

			}
			arithms.add(network);
		}
		List<ACQ_Network> networks = new ArrayList<>(Users);

		for (int i = 0; i < Users; i++) {

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
			id_users.add(i);

		}
		prePartition = rulesDetection(arithms, trans_relations);
		int index = 0;
		for (ACQ_Network net : prePartition) {
			networks.add(index, net);
			index = (index + 1) % Users;

		}
		Collections.shuffle(id_users);
		index = 0;
		for (ACQ_Network net : arithms)
			for (ACQ_IConstraint cst : net) {
				networks.get(id_users.get(index)).add(cst, false);
				index = (index + 1) % Users;
			}

		for (int i = 0; i < Users; i++) {
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));
		}
		return partitions;

	}

	private List<ACQ_Network> rulesDetection(List<ACQ_Network> arithms, String[] trans_rel) {

		List<ACQ_Network> prePartition = new ArrayList<ACQ_Network>();

		for (ACQ_Network net : arithms) {
			ACQ_IConstraint[] csts = net.getArrayConstraints();
			if (Arrays.stream(trans_rel).anyMatch(csts[0].getName()::equals))
				for (int i = 0; i < csts.length - 1; i++)
					for (int j = i + 1; j < csts.length - 1; j++) {
						if (csts[i].getVariables()[1] == csts[j].getVariables()[0] // ci and cj -> ck
								&& net.contains(new BinaryArithmetic(csts[i].getName(), csts[i].getVariables()[0],
										Operator.NEQ, csts[j].getVariables()[1], csts[i].getNegName()))) {

							net.remove(csts[i]);
							net.remove(csts[j]);
							net.remove(new BinaryArithmetic(csts[i].getName(), csts[i].getVariables()[0],
									Operator.NEQ, csts[j].getVariables()[1], csts[i].getNegName()));
							ACQ_Network tmp_net = new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars());
							tmp_net.add(csts[i], true);
							tmp_net.add(csts[j], true);
							tmp_net.add(new BinaryArithmetic(csts[i].getName(), csts[i].getVariables()[0],
									Operator.NEQ, csts[j].getVariables()[1], csts[i].getNegName()), true);
							prePartition.add(tmp_net);

						}
					}

		}

		return prePartition;
	}

	private int minSize(List<ACQ_Network> networks) {

		int size = 0;
		int index = 0;
		for (ACQ_Network net : networks) {
			if (net.size() < size) {
				size = net.size();
				index = networks.indexOf(net);
			}

		}
		return index;
	}

	private int transitivity(String[] trans_rel, List<ACQ_Network> networks, ACQ_IConstraint cst) {

		if (Arrays.stream(trans_rel).anyMatch(cst.getName()::equals))
			for (ACQ_Network net : networks) {
				ACQ_IConstraint[] csts = net.getArrayConstraints();
				for (int i = 0; i < csts.length - 1; i++)
					for (int j = i + 1; j < csts.length; j++) {
						if (csts[i] instanceof BinaryConstraint && csts[j] instanceof BinaryConstraint
								&& cst.getName().equals(csts[i].getName())
								&& csts[i].getName().equals(csts[j].getName())
								&& csts[i].getVariables()[1] == csts[j].getVariables()[0]
								&& csts[i].getVariables()[0] == cst.getVariables()[0]
								&& csts[j].getVariables()[1] == cst.getVariables()[1]) {
							FileManager.printFile(csts[i] + "::" + csts[j] + "::" + cst, "trans");
							return networks.indexOf(net);
						}

					}

			}
		return -1;
	}

	private int antisymetry(String[] as_rel, List<ACQ_Network> networks, ACQ_IConstraint cst) {

		for (ACQ_Network net : networks) {
			ACQ_IConstraint[] csts = net.getArrayConstraints();
			for (int i = 0; i < csts.length - 1; i++)
				for (int j = i + 1; j < csts.length; j++) {
					if (cst.getName().equals(ACQ_Relations.EqualXY.toString()) && csts[i] instanceof BinaryConstraint
							&& csts[j] instanceof BinaryConstraint && csts[i].getName().equals(csts[j].getName())
							&& Arrays.stream(as_rel).anyMatch(csts[i].getName()::equals)
							&& csts[i].getVariables()[0] == csts[j].getVariables()[1]
							&& csts[i].getVariables()[1] == csts[j].getVariables()[0]
							&& Arrays.stream(cst.getVariables()).allMatch(csts[i].getVariables()::equals)) {
						FileManager.printFile(csts[i] + "::" + csts[j] + "::" + cst, "anti");
						return networks.indexOf(net);
					}
				}

		}
		return -1;
	}

	public static String[] getRelationsNames(Class<? extends Enum<?>> e) {
		return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
	}

	private ConcurrentHashMap<String, ACQ_Bias> Bias_Partition_ScopeBased(int Users) {

		Set<ACQ_Scope> scopes = new HashSet<>();

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		List<ACQ_Network> networks = new ArrayList<>(Users);

		List<Integer> id_users = new ArrayList<>();
		for (int i = 0; i < Users; i++) {

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
			id_users.add(i);
		}

		Collections.shuffle(id_users);

		int id = 0, nb_scopes = 0;

		for (ACQ_IConstraint cst : bias.getConstraints()) {

			scopes.add(cst.getScope());

			if (nb_scopes < scopes.size()) {

				networks.get(id_users.get(id)).addAll(bias.getProjection(cst.getScope()), false);

				nb_scopes++;
				id = (id + 1) % Users;

			}

		}

		for (int i = 0; i < Users; i++)
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));

		return partitions;

	}

	private ConcurrentHashMap<String, ACQ_Bias> Bias_Partition_Neighborhood(int Users) {

		ACQ_Scope[] neighborhoods = bias.getVars().split_into(Users);

		ConcurrentHashMap<String, ACQ_Bias> partitions = new ConcurrentHashMap<String, ACQ_Bias>();
		List<ACQ_Network> networks = new ArrayList<>(Users);

		List<Integer> id_users = new ArrayList<>();
		for (int i = 0; i < Users; i++) {

			networks.add(new ACQ_Network(bias.getNetwork().getFactory(), bias.getVars()));
			id_users.add(i);
		}

		int id;
		for (ACQ_IConstraint cst : bias.getConstraints()) {

			id = getNeighborhood_id(neighborhoods, cst.getScope());

			networks.get(id_users.get(id)).addAll(bias.getProjection(cst.getScope()), false);

		}

		for (int i = 0; i < Users; i++)
			partitions.put("pool-1-thread-" + (i + 1), new ACQ_Bias(networks.get(i)));
		for (int i = 0; i < Users; i++)
			System.err.println(neighborhoods[i]);

		return partitions;

	}

	private int getNeighborhood_id(ACQ_Scope[] neighborhoods, ACQ_Scope scope) {

		List<ACQ_Scope> intList = Arrays.asList(neighborhoods);

		Collections.shuffle(intList);

		intList.toArray(neighborhoods);

		for (int i = 0; i < neighborhoods.length; i++)
			if (neighborhoods[i].containsAll(scope))
				return i;

		for (int i = 0; i < neighborhoods.length; i++)
			if (neighborhoods[i].intersect(scope))
				return i;

		return -1;
	}

	public synchronized void Reduce(ACQ_IConstraint c) {
		bias.reduce(c);
		for (ACQ_Bias b : biases_in_use.values()) {
			b.reduce(c);
		}

	}

	public synchronized void Reduce(ConstraintSet set) {
		bias.reduce(set);
		for (ACQ_Bias b : biases_in_use.values()) {
			b.reduce(set);

		}
	}

	public synchronized void Reduce(ACQ_Query query) {
		bias.reduce(query);
		for (ACQ_Bias b : biases_in_use.values()) {
			b.reduce(query);
		}

	}

	@Override
	public synchronized ACQ_Network getLearned_network() {
		return learned_network;
	}

	@Override
	public synchronized void setLearned_network(ACQ_Network learned_network) {
		this.learned_network = learned_network;
	}

	/*
	 * ThreadGroup currentGroup; public void Interrupt() { running=false;
	 * 
	 * }
	 * 
	 * public void killrest() { int noThreads = currentGroup.activeCount(); Thread[]
	 * lstThreads = new Thread[noThreads]; currentGroup.enumerate(lstThreads); for
	 * (int i = 1; i < noThreads; i++) { if(!lstThreads[i].getName().equals("main")
	 * ) lstThreads[i].stop(); }
	 * 
	 * }
	 */

	@Override
	public void visited_scopes() {

	}

	public Long getAcqTime() {
		return acqTime;
	}

	public void setAcqTime() {
		acqTime = threadBean.getCurrentThreadCpuTime();
	}

	public ThreadMXBean getThreadBean() {
		// TODO Auto-generated method stub
		return threadBean;
	}

	public ACQ_Partition getPartitionType() {
		// TODO Auto-generated method stub
		return this.partitionType;
	}

	public void setPartitionType(ACQ_Partition partition) {

		this.partitionType = partition;
	}

}
