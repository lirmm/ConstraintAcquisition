package fr.lirmm.coconut.acquisition.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;


public abstract class TopComponent extends JPanel implements PropertyChangeListener{

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}
	public void close(){
	}
}
