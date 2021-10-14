package fr.lirmm.coconut.acquisition.core.parallel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.CopyOnWriteArraySet;

public class ObservedProtfolioManager extends ACQ_PACQ_Manager {
	private final transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public ObservedProtfolioManager(CopyOnWriteArraySet<ACQ_QueryMessage> queries_mailbox) {
		super(queries_mailbox);
		
	}


	@Override
	public void visited_scopes() {
		pcs.firePropertyChange("VISITED_SCOPES", null, null);
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

}
