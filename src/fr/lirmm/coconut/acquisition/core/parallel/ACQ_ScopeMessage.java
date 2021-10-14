package fr.lirmm.coconut.acquisition.core.parallel;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

public class ACQ_ScopeMessage extends ACQ_QueryMessage {



	private ACQ_Scope scope_m;
	
	
	public ACQ_ScopeMessage(String sender, ACQ_Query query, ACQ_Scope scope) {
		super(sender, query);
		this.scope_m=scope;

	}

	public ACQ_Scope getScope() {
		// TODO Auto-generated method stub
		return scope_m;
	}

	@Override
	public String toString() {
		return "ACQ_ScopeMessage [scope=" + scope_m + ", Sender=" + getSender() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scope_m == null) ? 0 : scope_m.hashCode());
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
		ACQ_ScopeMessage other = (ACQ_ScopeMessage) obj;
		if (scope_m == null) {
			if (other.scope_m != null)
				return false;
		} else if (!scope_m.equals(other.scope_m))
			return false;
		return true;
	}



}
