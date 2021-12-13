package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.HashSet;
import java.util.Set;

public class Formula {
	
	protected Set<CNF> cnfs;
	protected Clause atLeastAtMost;
	protected int atLeastLower;
	protected int atMostUpper;
	
	
	public Formula() {
		cnfs = new HashSet<CNF>();
		atLeastAtMost = null;
	}
	
	public void addCnf(CNF cnf) {
		cnfs.add(cnf);
	}
	
	public void addClause(Clause cl) {
		CNF toadd = new CNF();
		toadd.add(cl);
		cnfs.add(toadd);
	}
	
	public void setAtLeastAtMost(Clause cl, int lower, int upper) {
		atLeastAtMost = cl;
		atLeastLower = lower;
		atMostUpper = upper;
	}
	
	public boolean hasAtLeastAtMost() {
		return atLeastAtMost != null;
	}
	
	public Clause getAtLeastAtMost() {
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
		for (CNF cnf : cnfs) {
			if (s.length() == 0)
				s += "[" + cnf.toString() + "]";
			else
				s += "\nand [" + cnf.toString() + "]";
		}
		
		if(hasAtLeastAtMost()) {
			if (s.length() == 0) {
				s += "atLeast("+ atLeastAtMost +", " + atLeastLower + ")";
				s += "\natMost("+ atLeastAtMost +", " + atMostUpper + ")";
			}
			else {
				s += "\natLeast("+ atLeastAtMost +", " + atLeastLower + ")";
				s += "\natMost("+ atLeastAtMost +", " + atMostUpper + ")";
			}
		}
		return s;
		
	}
	
	public Set<CNF> getCnfs(){
		return cnfs;
	}
}
