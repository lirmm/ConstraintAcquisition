package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.ArrayList;
import java.util.Iterator;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;

public class Conj implements Iterable<Unit>{

	protected ArrayList<Unit> units;
	protected int size;
	protected Boolean marked = false; 
	
	public Conj() {
		units = new ArrayList<Unit>();
		size = 0;
	}
	
	public Conj(Iterable<Unit> iterator) {
		this(); // Call Conj() constructor
		for (Unit unit : iterator) {
			this.add(unit);
		}
	}
	
	public void add(Unit unit) {
		units.add(unit);
		size = size +1;
	}
	
	public String toString() {
		String s = "";
		for (Unit unit : units) {
			if(s.length() == 0)
				s += unit.toString();
			else
				s += " and " + unit.toString();
		}
		return s;
	}

	@Override
	public Iterator<Unit> iterator() {
		return units.iterator();
	}
	
}
