package fr.lirmm.coconut.acquisition.core.parallel;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

public class ACQ_QueryMessage extends ACQ_Message{

	private ACQ_Query query_m;

	
	public ACQ_QueryMessage(String sender, ACQ_Query query) {
		super(sender);
		this.query_m=query;
	}
	@Override
	public String toString() {
		return "ACQ_QueryMessage [query=" + query_m + ", Sender=" + getSender() + "]";
	}
	public ACQ_Query getQuery() {
		// TODO Auto-generated method stub
		return query_m;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((query_m == null) ? 0 : query_m.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ACQ_QueryMessage other = (ACQ_QueryMessage) obj;
		if (query_m == null) {
			if (other.query_m != null)
				return false;
		} else if (!query_m.equals(other.query_m))
			return false;
		return true;
	}

}
