package fr.lirmm.coconut.acquisition.gui;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;

public abstract class PlateauClassic extends Plateau{
	private DefaultExperience expe;
	public PlateauClassic(DefaultExperience expe){
		super(expe.getDimension());
		this.expe=expe;
	}


	@Override
	public void display(ACQ_Query query) {
		for (int x = 0; x < expe.getDimension(); x++) {
			for (int y = 0; y < expe.getDimension(); y++) {
				int value = query.getValue(x * expe.getDimension() + y);
				set(x, y, value > 0 ? "" + value : "-");
			}
		}
		
	}
}
