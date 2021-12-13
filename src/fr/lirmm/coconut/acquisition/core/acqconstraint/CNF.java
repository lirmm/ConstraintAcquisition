package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fr.lirmm.coconut.acquisition.core.tools.Chrono;

public class CNF implements Iterable<Clause> {
	
	protected Set<Clause> clauses;
	
	public CNF() {
		clauses = new HashSet<Clause>();
	}

	
	public CNF clone() {
		CNF res = new CNF();
		for (Clause cl : clauses) {
			res.add(cl.clone());
		}
		return res;
	}
	
	public int size() {
		return clauses.size();
	}
	
	public void add(Clause clause) {
		clauses.add(clause);
	}
	
	public void addChecked(Clause clause) {
		Iterator<Clause> iter = clauses.iterator();
		while(iter.hasNext()) {
			Clause cl = iter.next();
			if (cl.subsumed(clause)) {
				return;
			}
			else if (clause.subsumed(cl)){
				iter.remove();
			}
		}
		clauses.add(clause);
	}
	
	public Boolean isMonomial() {
		for (Clause cl : clauses) {
			int size = cl.getSize();
			assert(size > 0);
			if (size > 1) {
				return false;
			}
		}
		return true;
	}
	
	public Clause getUnmarkedNonUnaryClause() {
		for (Clause cl : clauses) {
			if (cl.isMarked() == false && cl.getSize()>1) {
				return cl;
			}
		}
		return new Clause(); // return empty clause
	}
	
	
	
	public void concat(CNF cnf) {
		for (Clause cl : cnf) {
			this.add(cl);
		}
	}
	
	public void removeIfExists(Clause clause) {
		clauses.remove(clause);
	}
	
	public void remove(Clause clause) {
		boolean removed = clauses.remove(clause);
		assert removed : "Nothing to remove";
	}
	
	public ACQ_IConstraint[] getMonomialPositive() {
		assert(this.isMonomial());
		ArrayList<ACQ_IConstraint> constrs = new ArrayList<ACQ_IConstraint>();
		for (Clause cl : clauses) {
			Unit unit = cl.get(0);
			if(!unit.isNeg()) {
				constrs.add(unit.getConstraint());
			}
		}
		ACQ_IConstraint[] res = new ACQ_IConstraint[constrs.size()];
		for (int i =0; i < constrs.size(); i++) {
			res[i] = constrs.get(i);
		}
		return res;
	}
	
	public ACQ_IConstraint[] getMonomialNegative() {
		assert(this.isMonomial());
		ArrayList<ACQ_IConstraint> constrs = new ArrayList<ACQ_IConstraint>();
		for (Clause cl : clauses) {
			Unit unit = cl.get(0);
			if(unit.isNeg()) {
				constrs.add(unit.getConstraint());
			}
		}
		ACQ_IConstraint[] res = new ACQ_IConstraint[constrs.size()];
		for (int i =0; i < constrs.size(); i++) {
			res[i] = constrs.get(i);
		}
		return res;
	}
	
	public String toString() {
		String s = "";
		for(Clause cl : clauses) {
			if(s.length() == 0)
				s += "(" + cl.toString() + ")";
			else
				s += " and (" + cl.toString() + ")";
		}
		return s;
	}

	
	public void unitPropagate(Chrono chrono) {
		chrono.start("unit_propagate");
		
		boolean change;
		
		do {
			change = false; //removeDuplicates();
			
			if(this.isMonomial())
				break;
			
			ArrayList<Unit> forced = new ArrayList<Unit>();
			
			for (Clause cl : clauses) {
				if (cl.getSize() == 1) {
					Unit clunit = cl.get(0);
					Boolean in = false;
					for(Unit u : forced) {
						if(u.equals(clunit)) {
							in = true;
						}
					}
					if(!in)
						forced.add(cl.get(0));
				}
			}
			
			CNF toadd = new CNF();
			Iterator<Clause> iter = clauses.iterator();
			while(iter.hasNext()) {
				Clause cl = iter.next();
				if(cl.getSize() > 1) {
					Clause newcl = cl.clone();
					for (Unit unit : forced) {
						if(cl.contains(unit)) {
							iter.remove(); // remove cl
							change = true;
							newcl = null;
							break;
						}
						
						if (newcl.remove((Unit u) -> {
							if(unit.equalsConstraint(u) && unit.isNeg() != u.isNeg()) {
								return true;
							}
							else return false;
						}) == true) {
							change = true;
							
						}
					}
					if (newcl != null & change) {
						iter.remove();
						newcl.unmark();
						toadd.add(newcl);
					}
				}
			}
			concat(toadd);
		} while(change); 
		
		chrono.stop("unit_propagate");
		
	}
	
	public void unitPropagate(Unit unit, Chrono chrono) {
		chrono.start("unit_propagate");
		
		if(!this.isMonomial()) {
			ArrayList<Unit> forcedlist = new ArrayList<Unit>();
			forcedlist.add(unit);
			
			for (int i = 0; i < forcedlist.size(); i++) {
				Unit forced = forcedlist.get(i); 
				boolean change = false;
				
				CNF toadd = new CNF();
				Iterator<Clause> iter = clauses.iterator();
				while(iter.hasNext()) {
					Clause cl = iter.next();
					if(cl.getSize() > 1) {
						Clause newcl = cl.clone();
						
						if(cl.contains(forced)) {
							iter.remove(); // remove cl
							newcl = null;
							break;
						}
						
						if (newcl.remove((Unit u) -> {
							if(forced.equalsConstraint(u) && forced.isNeg() != u.isNeg()) {
								return true;
							}
							else return false;
						}) == true) {
							change = true;
							if (newcl.getSize() == 1) {
								forcedlist.add(newcl.get(0));
							}
							
						}
						
						if (newcl != null & change) {
							iter.remove();
							newcl.unmark();
							toadd.add(newcl);
						}
					}
				}
				concat(toadd);
			}
			
			
		}
			
		chrono.stop("unit_propagate");
		
	}


	@Override
	public Iterator<Clause> iterator() {
		return clauses.iterator();
	}
	
	
}
