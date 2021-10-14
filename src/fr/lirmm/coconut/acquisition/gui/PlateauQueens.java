package fr.lirmm.coconut.acquisition.gui;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;

public abstract class PlateauQueens extends Plateau{
	private DefaultExperience expe;
	public PlateauQueens(DefaultExperience expe){
		super(expe.getDimension());
		this.expe=expe;
	}


	@Override
	public void display(ACQ_Query query) {
		for (int x = 0; x < expe.getDimension(); x++) {
			int val=query.getValue(x);
			for(int y1=0;y1<expe.getDimension();y1++){
				set(y1, x, val==y1+1? GUI_Utils.getImageIcon("queen.png") : GUI_Utils.getImageIcon("empty.png"));
			}
		}
		
	}
}
