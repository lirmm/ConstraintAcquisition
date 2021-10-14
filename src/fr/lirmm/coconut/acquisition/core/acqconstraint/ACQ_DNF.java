package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.ArrayList;
import java.util.Iterator;

public class ACQ_DNF implements Iterable<Conj> {
	protected ArrayList<Conj> conjuncs;

	public ACQ_DNF() {
		conjuncs = new ArrayList<Conj>();
	}

	public void add(Conj conj) {
		conjuncs.add(conj);
	}

	public String toString() {
		String s = "";
		for (Conj conj : conjuncs) {
			if (s.length() == 0)
				s += "(" + conj.toString() + ")";
			else
				s += " or (" + conj.toString() + ")";
		}
		return s;
	}

	public int getSize() {
		return conjuncs.size();
	}

	public Conj get(int i) {
		return conjuncs.get(i);
	}

	@Override
	public Iterator<Conj> iterator() {
		return conjuncs.iterator();
	}

}
