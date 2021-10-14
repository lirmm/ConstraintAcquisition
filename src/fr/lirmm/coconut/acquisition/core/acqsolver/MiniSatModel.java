package fr.lirmm.coconut.acquisition.core.acqsolver;

import java.util.ArrayList;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintMapping;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Unit;

public class MiniSatModel extends SATModel {

	ArrayList<Integer> model;
	ConstraintMapping mapping;
	
	public MiniSatModel(ArrayList<Integer> model, ConstraintMapping mapping) {
		this.model = model;
		this.mapping = mapping;
		
	}
	
	@Override
	public Boolean get(Unit unit) {
		assert !unit.isNeg() : "unit cannot be negative";
		int constrid = unit.getMiniSatVar();
		for (int i = 0; i < model.size(); i++) {
			if(model.get(i) == constrid || model.get(i) == -constrid) {
				assert model.get(i) != 0;
				if (model.get(i) < 0) {
					return false;
				}
				else {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		String res = "(";
		for (Unit unit : mapping.values()) {
			if (this.get(unit))
				res += unit.toString() + "= 1, ";
			else
				res += unit.toString() + "= 0, ";
		}
		res += ")";
		
		return res;
	}

}
