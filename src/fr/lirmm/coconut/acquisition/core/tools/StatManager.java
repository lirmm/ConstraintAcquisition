package fr.lirmm.coconut.acquisition.core.tools;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

public class StatManager {
	
	
	private int nb_var;
	
	// Stats counters
	
	private int nb_partial_query;
	private int nb_complete_query;
	private int nb_positive_query;
	private int nb_negative_query;
	private int query_size;
	private int non_asked_query;
	private int visited_scopes;
	
	public StatManager(int nb) {
		nb_partial_query = 0;
		nb_complete_query = 0;
		nb_positive_query = 0;
		nb_negative_query = 0;
		query_size = 0;
		non_asked_query=0;
		nb_var = nb;
		visited_scopes=0;
	}
	
	public void update(ACQ_Query e) {
		if(e.isPositive()) {
			nb_positive_query++;
			if(e.getScope().size() == nb_var)
				nb_complete_query++;
			else 
				nb_partial_query++;
		}else {
			nb_negative_query++;
			if(e.getScope().size() == nb_var)
				nb_complete_query++;
			else
				nb_partial_query++;
		}
		query_size += e.getScope().size();
	}
	
	public void update_non_asked_query(ACQ_Query e) {
		
			non_asked_query++;
			
	}
	
	public void update_visited_scopes() {
		visited_scopes++;
		
}
	
	public int getNbPartialQuery() {
		return nb_partial_query;
	}
	
	public int getNbCompleteQuery() {
		return nb_complete_query;
	}
	
	public int getNbPositiveQuery() {
		return nb_positive_query;
	}
	
	public int getNbNegativeQuery() {
		return nb_negative_query;
	}
	
	public float getQuerySize() {
		return (float)query_size/(nb_partial_query+nb_complete_query);
	}
	
	
	public String toString() {
		String res = "-----Queries stats-----";
		res += "\nTotal queries : " + (nb_complete_query + nb_partial_query);
		res += "\nComplete queries : " + nb_complete_query;
		res += "\nPartial queries : " + nb_partial_query;
		res += "\nPositive queries : " + nb_positive_query;
		res += "\nNegative queries : " + nb_negative_query;
		res += "\nNon-Asked queries : " + non_asked_query;
		res += "\nVisited Scopes : " + visited_scopes;
		float size = getQuerySize();
		int rounded = (int)(size*100);
		res += "\nAverage query size : " + rounded/100d;
		return res;
	}

	public int getNon_asked_query() {
		return non_asked_query;
	}

	public void setNon_asked_query(int non_asked_query) {
		this.non_asked_query = non_asked_query;
	}

	public int getVisited_scopes() {
		return visited_scopes;
	}

	public void setVisited_scopes(int visited_scopes) {
		this.visited_scopes = visited_scopes;
	}
	
	
}
