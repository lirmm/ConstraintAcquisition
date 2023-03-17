package GEQCA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.tour.TwoApproxMetricTSP;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.util.SupplierUtil;

import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;


public class ACQ_SelectionHeuristic {
	HashMap<String, Integer> labels = new HashMap<String, Integer>();
	int index = 0;

	public ACQ_SelectionHeuristic() {
		labels.put("PrecedesXY", 3);
		labels.put("IsPrecededXY", 3);
		labels.put("MeetsXY", 2);
		labels.put("IsMetXY", 2);
		labels.put("OverlapsXY", 4);
		labels.put("IsOverlappedXY", 4);
		labels.put("StartsXY", 2);
		labels.put("IsStartedXY", 2);
		labels.put("DuringXY", 4);
		labels.put("ContainsXY", 3);
		labels.put("FinishXY", 2);
		labels.put("IsFinishedXY", 2);
		labels.put("ExactXY", 2);

		labels.put("DisconnectedXY", 4);
		labels.put("ExternallyConnectedXY", 3);
		labels.put("TangentialProperPartXY", 2);
		labels.put("TangentialProperPartInverseXY", 2);
		labels.put("PartiallyOverlappingXY", 5);
		labels.put("NonTangentialProperPartXY", 2);
		labels.put("NonTangentialProperPartInverseXY", 2);
		labels.put("REqualXY", 1);
	}

	public int[] WeightedSelection(List<int[]> variables, HashMap<String, ArrayList<String>> L) {

		int[] min = null;
		int minval = Integer.MAX_VALUE;
		int j = -1;
		for (int[] var : variables) {
			String scope = var[0] + "," + var[1];

			ArrayList<String> temp = L.get(scope);
			int i = 0;
			for (String c : temp)
				i += labels.get(c);
			if (i < minval) {
				min = var;
				minval = i;
				j++;
			}

		}
		index = j;
		return min;
	}

	public int[] PathHeuristic(Graph<String, DefaultEdge> completeGraph, int[] c_ij, int n) {
		int[] c_star = new int[2];

        
		Random r = new Random(System.currentTimeMillis());
		
		//System.out.println(":::" + n);
		for (int i = 0; i < n; i++) {
			int k = (int) (10 * Math.random()) & 1;

			DefaultEdge edge = completeGraph.getEdge(i + "", c_ij[0] + "");
			DefaultEdge edge1 = completeGraph.getEdge(c_ij[1] + "", i + "");
			ArrayList<DefaultEdge> edgs = new ArrayList<DefaultEdge>();
			edgs.add(edge);
			edgs.add(edge1);
			if (edge != null && edge1 == null) {
				String e = edge.toString();
				e = e.replace("(", "");
				e = e.replace(")", "");
				e = e.replace(" ", "");
				int id1 = Integer.parseInt(e.split(":")[0]);
				int id2 = Integer.parseInt(e.split(":")[1]);
				if (k == 0) {
					c_star[0] = id1;
					c_star[1] = id2;
				} else {
					c_star[1] = id2;
					c_star[0] = id1;
				}
				completeGraph.removeEdge(edge1);

				break;
			}
			if (edge == null && edge1 != null) {

				String e = edge1.toString();
				e = e.replace("(", "");
				e = e.replace(")", "");
				e = e.replace(" ", "");
				int id1 = Integer.parseInt(e.split(":")[0]);
				int id2 = Integer.parseInt(e.split(":")[1]);
				if (k == 0) {
					c_star[0] = id1;
					c_star[1] = id2;
				} else {
					c_star[1] = id2;
					c_star[0] = id1;
				}
				completeGraph.removeEdge(edge1);

				break;
			}
			if (edge != null && edge1 != null) {
				Collections.shuffle(edgs);

				String e = edgs.get(0).toString();
				e = e.replace("(", "");
				e = e.replace(")", "");
				e = e.replace(" ", "");
				int id1 = Integer.parseInt(e.split(":")[0]);
				int id2 = Integer.parseInt(e.split(":")[1]);
				c_star[0] = id1;
				c_star[1] = id2;

				completeGraph.removeEdge(edgs.get(0));

				break;

			}

		}

		return c_star;
	}

	public int[] PathWeightedHeuristic(Graph<String, DefaultEdge> completeGraph, int[] c_ij, int n,
			HashMap<String, ArrayList<String>> L, ArrayList<DefaultEdge> edgs) {

		int[] c_star = new int[2];
		int[] c_star1 = new int[2];

		int node0 = c_ij[0];
		int node1 = c_ij[1];
		c_star = checkIfPathCutt(completeGraph, node0, n, edgs);
		c_star1 = checkIfPathCutt(completeGraph, node1, n, edgs);
		if (c_star == null || c_star1 == null) {
			Pair first = getPath(completeGraph, L, n, node0, edgs);
			Pair second = getPath(completeGraph, L, n, node1, edgs);
			if ((first.pair[0] != 0 || first.pair[1] != 0) || (second.pair[0] != 0 || second.pair[1] != 0)) {

				DefaultEdge selected = null;

				if (first.min_weight <= second.min_weight) {
					selected = completeGraph.getEdge(first.pair[0] + "", first.pair[1] + "");

					return first.pair;
				} else {
					selected = completeGraph.getEdge(second.pair[0] + "", second.pair[1] + "");

					return second.pair;
				}
			} else {
				System.out.print(edgs);
				
				Set<DefaultEdge> eg = completeGraph.edgeSet();
				if(eg.size()>0) {

				
				String e = eg.iterator().next().toString();
				e = e.replace("(", "");
				e = e.replace(")", "");
				e = e.replace(" ", "");
				int id1 = Integer.parseInt(e.split(":")[0]);
				int id2 = Integer.parseInt(e.split(":")[1]);
				Pair first1 = getPath(completeGraph, L, n, id1, edgs);
				Pair second1 = getPath(completeGraph, L, n, id2, edgs);
				DefaultEdge selected = null;
				if (first1.min_weight <= second1.min_weight) {
					selected = completeGraph.getEdge(first1.pair[0] + "", first1.pair[1] + "");

					return first.pair;
				} else {
					selected = completeGraph.getEdge(second1.pair[0] + "", second1.pair[1] + "");

					return second.pair;
				}
				}
				return new int[2];
			}
		}else if(c_star != null && c_star1 == null) {
			return c_star;
		}else  {
			return c_star1;

		}

	}

	public int getIndex() {
		return index;
	}

	public Pair getPath(Graph<String, DefaultEdge> completeGraph, HashMap<String, ArrayList<String>> L, int n, int node,
			ArrayList<DefaultEdge> edgs) {
		DefaultEdge selected = null;
		int minweight = Integer.MAX_VALUE;
		int[] c_star = new int[2];

		for (int j = 0; j < n; j++) {

			DefaultEdge edg = completeGraph.getEdge(node + "", j + "");
			DefaultEdge edg_ = completeGraph.getEdge(j + "", node + "");

			if (node < j && edg != null) {
				DefaultEdge eg = completeGraph.getEdge(node + "", j + "");
				String e = eg.toString();
				e = e.replace("(", "");
				e = e.replace(")", "");
				e = e.replace(" ", "");
				int id1 = Integer.parseInt(e.split(":")[0]);
				int id2 = Integer.parseInt(e.split(":")[1]);
				String scope = id1 + "," + id2;

				ArrayList<String> temp = L.get(scope);
				int i = 0;
				for (String c : temp)
					i += labels.get(c);
				if (i < minweight) {
					selected = eg;
					minweight = i;
				}
				System.out.println(scope + " :: " + i);

			} else if (node > j && edg_ != null) {
				DefaultEdge eg = completeGraph.getEdge(j + "", node + "");

				String e = eg.toString();
				e = e.replace("(", "");
				e = e.replace(")", "");
				e = e.replace(" ", "");
				int id1 = Integer.parseInt(e.split(":")[0]);
				int id2 = Integer.parseInt(e.split(":")[1]);
				String scope = id1 + "," + id2;

				ArrayList<String> temp = L.get(scope);
				int i = 0;
				for (String c : temp)
					i += labels.get(c);
				System.out.println(scope + " :: " + i);

				if (i < minweight) {
					selected = eg;
					minweight = i;
				}

			}

		}

		if (selected != null) {
			String e = selected.toString();
			e = e.replace("(", "");
			e = e.replace(")", "");
			e = e.replace(" ", "");
			int id1 = Integer.parseInt(e.split(":")[0]);
			int id2 = Integer.parseInt(e.split(":")[1]);
			if (id1 < id2) {

				c_star[0] = id1;
				c_star[1] = id2;
			} else {
				c_star[1] = id1;
				c_star[0] = id2;
			}

		}else if(selected == null && completeGraph.edgeSet().size()>0){
			String e = completeGraph.edgeSet().iterator().next().toString();
			e = e.replace("(", "");
			e = e.replace(")", "");
			e = e.replace(" ", "");
			int id1 = Integer.parseInt(e.split(":")[0]);
			int id2 = Integer.parseInt(e.split(":")[1]);
			c_star[0] = id1;
			c_star[1] = id2;
		}

		return new Pair(c_star, minweight);
	}

	public int[] checkIfPathCutt(Graph<String, DefaultEdge> completeGraph, int node, int n,
			ArrayList<DefaultEdge> edgs) {
		int sum = 0;
		int sum1 = 0;
		int[] c_star = new int[2];

		for (int j = 0; j < n; j++) {
			DefaultEdge edg = completeGraph.getEdge(node + "", j + "");
			DefaultEdge edg_ = completeGraph.getEdge(j + "", node + "");
			if (edg == null) {
				sum++;
			}
			if (edg_ == null) {
				sum1++;
			}
		}
		if (sum == n - 1 || sum1 == n - 1) {
			Iterator it = completeGraph.edgeSet().iterator();
			if (it.hasNext()) {
				DefaultEdge eg = (DefaultEdge) it.next();
				String e = eg.toString();
				e = e.replace("(", "");
				e = e.replace(")", "");
				e = e.replace(" ", "");
				int id1 = Integer.parseInt(e.split(":")[0]);
				int id2 = Integer.parseInt(e.split(":")[1]);
				if (id1 < id2) {

					c_star[0] = id1;
					c_star[1] = id2;
				} else {
					c_star[1] = id1;
					c_star[0] = id2;
				}
				edgs.add(eg);

			}
			return c_star;
		}
		return null;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	private static Graph<Integer, DefaultEdge> buildEmptySimpleGraph() {
		return GraphTypeBuilder.<Integer, DefaultEdge>undirected().allowingMultipleEdges(false).allowingSelfLoops(false)
				.edgeClass(DefaultEdge.class).weighted(false).buildGraph();
	}

	public class Pair {
		int[] pair = new int[2];
		int min_weight = 0;

		public Pair(int[] pair, int min_weight) {
			this.pair = pair;
			this.min_weight = min_weight;
		}
	}

	public GraphPath<String, DefaultWeightedEdge> LeastCostPathHeuristic(Graph<String, DefaultWeightedEdge> completeGraph) {
		GraphPath<String, DefaultWeightedEdge> sp = null;
		try {
			sp = new TwoApproxMetricTSP().getTour(completeGraph);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.print(sp.getWeight());
		return sp;
	}

	public Graph Weightedbuild(CombinationIterator iterator1, List<String> variables, HashMap<String, ArrayList<String>> L) {

		// Collections.shuffle(variables);
		Supplier<String> vSupplier = new Supplier<String>() {
			private int id = 0;

			@Override
			public String get() {
				return "" + id++;
			}
		};
		Graph<String, DefaultWeightedEdge> completeGraph = new SimpleWeightedGraph<>(vSupplier,
				SupplierUtil.createDefaultWeightedEdgeSupplier());

		while (iterator1.hasNext()) {
			int[] vars = new int[2];
			vars = iterator1.next();
			String scope = vars[0] + "," + vars[1];

			ArrayList<String> temp = L.get(scope);
			int i = 0;
			for (String c : temp)
				i += labels.get(c);
			if(!variables.contains(scope)) {
			if (vars[0] < vars[1]) {
				completeGraph.addVertex(vars[0] + "");
				completeGraph.addVertex(vars[1] + "");
				DefaultWeightedEdge e1 =completeGraph.addEdge(vars[0] + "", vars[1] + "");

				completeGraph.setEdgeWeight(e1, i); 

			}
			}else {
				
				if (vars[0] < vars[1]) {
					completeGraph.addVertex(vars[0] + "");
					completeGraph.addVertex(vars[1] + "");
					DefaultWeightedEdge e1 =completeGraph.addEdge(vars[0] + "", vars[1] + "");

					completeGraph.setEdgeWeight(e1, 0); 
				}
			
		}}
		//System.out.println(completeGraph.edgeSet().size());
		return completeGraph;
	}
	}
	