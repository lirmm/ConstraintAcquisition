package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.HashSet;
import java.util.Set;

public class ACQ_Formula {

	protected Set<ACQ_CNF> cnfs;
	protected ACQ_Clause atLeastAtMost;
	protected int atLeastLower;
	protected int atMostUpper;

	public ACQ_Formula() {
		cnfs = new HashSet<ACQ_CNF>();
		atLeastAtMost = null;
	}

	public void addCnf(ACQ_CNF cnf) {
		cnfs.add(cnf);
	}

	public void addClause(ACQ_Clause cl) {
		ACQ_CNF toadd = new ACQ_CNF();
		toadd.add(cl);
		cnfs.add(toadd);
	}

	public void setAtLeastAtMost(ACQ_Clause cl, int lower, int upper) {
		atLeastAtMost = cl;
		atLeastLower = lower;
		atMostUpper = upper;
	}

	public boolean hasAtLeastAtMost() {
		return atLeastAtMost != null;
	}

	public ACQ_Clause getAtLeastAtMost() {
		return atLeastAtMost;
	}

	public int atLeastLower() {
		return atLeastLower;
	}

	public int atMostUpper() {
		return atMostUpper;
	}

	public String toString() {
		String s = "";
		for (ACQ_CNF cnf : cnfs) {
			if (s.length() == 0)
				s += "[" + cnf.toString() + "]";
			else
				s += "\nand [" + cnf.toString() + "]";
		}

		if (hasAtLeastAtMost()) {
			if (s.length() == 0) {
				s += "atLeast(" + atLeastAtMost + ", " + atLeastLower + ")";
				s += "\natMost(" + atLeastAtMost + ", " + atMostUpper + ")";
			} else {
				s += "\natLeast(" + atLeastAtMost + ", " + atLeastLower + ")";
				s += "\natMost(" + atLeastAtMost + ", " + atMostUpper + ")";
			}
		}
		return s;

	}

	public Set<ACQ_CNF> getCnfs() {
		return cnfs;
	}
}
