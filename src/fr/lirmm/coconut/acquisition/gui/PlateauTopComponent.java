package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;

import javax.swing.JPanel;
import javax.swing.JToolBar;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

public class PlateauTopComponent extends TopComponent {
	private AskViewer plateau;

	public PlateauTopComponent(AskViewer plateau, JToolBar toolbar) {
		this.plateau = plateau;
		if(plateau==null || ! (plateau instanceof Plateau))setName("No board");
		else setName("Board " + ((Plateau)plateau).getDimension() + "x" + ((Plateau)plateau).getDimension());
//		setLayout(new VerticalFlowLayout());
		setLayout(new BorderLayout());

		add(toolbar, BorderLayout.NORTH);
		if (plateau != null) {
			if(plateau instanceof Plateau){
			JPanel plateauPanel = new JPanel();
			plateauPanel.add(plateau);
			add(plateauPanel, BorderLayout.CENTER);
			}
			else add(plateau,BorderLayout.CENTER);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getPropertyName()) {
		case "ASK":
			if(plateau!=null) 
				plateau.display((ACQ_Query) evt.getNewValue());
			break;
		case "ASKED_QUERY":
		case "MEMORY_UP":
		}
	}

}
