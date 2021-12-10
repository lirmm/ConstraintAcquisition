import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;

public class ACQ_SelectionHeuristic {
	HashMap<String,Integer> labels = new HashMap<String,Integer>();
	int index =0;
	public ACQ_SelectionHeuristic() {
		labels.put("PrecedesXY",3);
		labels.put("IsPrecededXY",3);
		labels.put("MeetsXY",2);
		labels.put("IsMetXY",2);
		labels.put("OverlapsXY",4);
		labels.put("IsOverlappedXY",4);
		labels.put("StartsXY",2);
		labels.put("IsStartedXY",2);
		labels.put("DuringXY",4);
		labels.put("ContainsXY",3);
		labels.put("FinishXY",2);
		labels.put("IsFinishedXY",2);
		labels.put("ExactXY",2);

	}
	
	
	public int[] WeightedSelection(List<int[]> variables,HashMap<String,ArrayList<String>> L) {
	
		int[] min =null;
		int minval=Integer.MAX_VALUE;
		int j=-1;
		for(int[] var:variables) {
			String scope = var[0]+","+var[1];

			ArrayList<String> temp = L.get(scope);
			int i =0;
			for(String c : temp)
				i+=labels.get(c);
			if(i<minval) {
				min=var;
				minval=i;
				j++;
			}	
		
		}
		index=j;
		return min;
	}
	public int[] PathHeuristic(Graph<String, DefaultEdge> completeGraph,int[] c_ij,int n) {
        

		
		 int []c_star =new int[2];
		 
		 Random r= new Random(System.currentTimeMillis());
		 
		 System.out.println(":::"+n);
		 for(int i = 0 ; i< n;i++) {
			 int k=(int) (10 * Math.random()) & 1;

		 DefaultEdge edge=completeGraph.getEdge(i+"", c_ij[0]+"");
		 DefaultEdge edge1=completeGraph.getEdge(c_ij[1]+"", i+"");
		 ArrayList<DefaultEdge> edgs= new ArrayList<DefaultEdge>();
		 edgs.add(edge);
		 edgs.add(edge1);
		 if(edge!=null&&edge1==null) {
			 String e =edge.toString();
				e=e.replace("(", "");
				 e=e.replace(")", "");
				 e=e.replace(" ", "");
				 int id1 =Integer.parseInt(e.split(":")[0]);
				 int id2 =Integer.parseInt(e.split(":")[1]);
				if(k==0) {
			 	 c_star[0]=id1;
			 	 c_star[1]=id2;
				}else {
				 	 c_star[1]=id2;
				 	 c_star[0]=id1; 
				}
			 	 completeGraph.removeEdge(edge1);

			 	 break;
		 }
		 if(edge==null&&edge1!=null) {
			 
			 String e =edge1.toString();
				e=e.replace("(", "");
				 e=e.replace(")", "");
				 e=e.replace(" ", "");
				 int id1 =Integer.parseInt(e.split(":")[0]);
				 int id2 =Integer.parseInt(e.split(":")[1]);
				if(k==0) {
			 	 c_star[0]=id1;
			 	 c_star[1]=id2;
				}else {
				 	 c_star[1]=id2;
				 	 c_star[0]=id1; 
				}
			 	 completeGraph.removeEdge(edge1);

			 	 break;
		 }
		 if(edge!=null&&edge1!=null) {
			 Collections.shuffle(edgs);

			 String e =edgs.get(0).toString();
			e=e.replace("(", "");
			 e=e.replace(")", "");
			 e=e.replace(" ", "");
			 int id1 =Integer.parseInt(e.split(":")[0]);
			 int id2 =Integer.parseInt(e.split(":")[1]);
		 	 c_star[0]=id1;
		 	 c_star[1]=id2;
			
		 	 completeGraph.removeEdge(edgs.get(0));

		 	 break;

		 }
		 
		 
		 }
							
			
		
		return c_star;
	}

	public int getIndex() {
		return index;
	}


	public void setIndex(int index) {
		this.index = index;
	}
	
	 private static Graph<Integer, DefaultEdge> buildEmptySimpleGraph()
	    {
	        return GraphTypeBuilder
	            .<Integer, DefaultEdge> undirected().allowingMultipleEdges(false)
	            .allowingSelfLoops(false).edgeClass(DefaultEdge.class).weighted(false).buildGraph();
	    }
	
}
