package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fr.lirmm.coconut.acquisition.core.tools.Chrono;

public class ACQ_CNF implements Iterable<ACQ_Clause> {

	protected Set<ACQ_Clause> clauses;

	public ACQ_CNF() {
		clauses = new HashSet<ACQ_Clause>();
	}

	public ACQ_CNF clone() {
		ACQ_CNF res = new ACQ_CNF();
		for (ACQ_Clause cl : clauses) {
			res.add(cl.clone());
		}
		return res;
	}

	public int size() {
		return clauses.size();
	}

	public void add(ACQ_Clause clause) {
		clauses.add(clause);
	}

	public void addChecked(ACQ_Clause clause) {
		Iterator<ACQ_Clause> iter = clauses.iterator();
		while (iter.hasNext()) {
			ACQ_Clause cl = iter.next();
			if (cl.subsumed(clause)) {
				return;
			} else if (clause.subsumed(cl)) {
				iter.remove();
			}
		}
		clauses.add(clause);
	}

	public Boolean isMonomial() {
		for (ACQ_Clause cl : clauses) {
			int size = cl.getSize();
			assert (size > 0);
			if (size > 1) {
				return false;
			}
		}
		return true;
	}

	public ACQ_Clause getUnmarkedNonUnaryClause() {
		for (ACQ_Clause cl : clauses) {
			if (cl.isMarked() == false && cl.getSize() > 1) {
				return cl;
			}
		}
		return new ACQ_Clause(); // return empty clause
	}

	public void concat(ACQ_CNF cnf) {
		for (ACQ_Clause cl : cnf) {
			this.add(cl);
		}
	}

	public void removeIfExists(ACQ_Clause clause) {
		clauses.remove(clause);
	}

	public void remove(ACQ_Clause clause) {
		boolean removed = clauses.remove(clause);
		assert removed : "Nothing to remove";
	}

	public ACQ_IConstraint[] getMonomialPositive() {
		assert (this.isMonomial());
		ArrayList<ACQ_IConstraint> constrs = new ArrayList<ACQ_IConstraint>();
		for (ACQ_Clause cl : clauses) {
			Unit unit = cl.get(0);
			if (!unit.isNeg()) {
				constrs.add(unit.getConstraint());
			}
		}
		ACQ_IConstraint[] res = new ACQ_IConstraint[constrs.size()];
		for (int i = 0; i < constrs.size(); i++) {
			res[i] = constrs.get(i);
		}
		return res;
	}

	public ACQ_IConstraint[] getMonomialNegative() {
		assert (this.isMonomial());
		ArrayList<ACQ_IConstraint> constrs = new ArrayList<ACQ_IConstraint>();
		for (ACQ_Clause cl : clauses) {
			Unit unit = cl.get(0);
			if (unit.isNeg()) {
				constrs.add(unit.getConstraint());
			}
		}
		ACQ_IConstraint[] res = new ACQ_IConstraint[constrs.size()];
		for (int i = 0; i < constrs.size(); i++) {
			res[i] = constrs.get(i);
		}
		return res;
	}

	public String toString() {
		String s = "";
		for (ACQ_Clause cl : clauses) {
			if (s.length() == 0)
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
			change = false; // removeDuplicates();

			if (this.isMonomial())
				break;

			ArrayList<Unit> forced = new ArrayList<Unit>();

			for (ACQ_Clause cl : clauses) {
				if (cl.getSize() == 1) {
					Unit clunit = cl.get(0);
					Boolean in = false;
					for (Unit u : forced) {
						if (u.equals(clunit)) {
							in = true;
						}
					}
					if (!in)
						forced.add(cl.get(0));
				}
			}

			ACQ_CNF toadd = new ACQ_CNF();
			Iterator<ACQ_Clause> iter = clauses.iterator();
			while (iter.hasNext()) {
				ACQ_Clause cl = iter.next();
				if (cl.getSize() > 1) {
					ACQ_Clause newcl = cl.clone();
					for (Unit unit : forced) {
						if (cl.contains(unit)) {
							iter.remove(); // remove cl
							change = true;
							newcl = null;
							break;
						}

						if (newcl.remove((Unit u) -> {
							if (unit.equalsConstraint(u) && unit.isNeg() != u.isNeg()) {
								return true;
							} else
								return false;
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
		} while (change);

		chrono.stop("unit_propagate");

	}

	public void unitPropagate(Unit unit, Chrono chrono) {
		chrono.start("unit_propagate");

		if (!this.isMonomial()) {
			ArrayList<Unit> forcedlist = new ArrayList<Unit>();
			forcedlist.add(unit);

			for (int i = 0; i < forcedlist.size(); i++) {
				Unit forced = forcedlist.get(i);
				boolean change = false;

				ACQ_CNF toadd = new ACQ_CNF();
				Iterator<ACQ_Clause> iter = clauses.iterator();
				while (iter.hasNext()) {
					ACQ_Clause cl = iter.next();
					if (cl.getSize() > 1) {
						ACQ_Clause newcl = cl.clone();

						if (cl.contains(forced)) {
							iter.remove(); // remove cl
							newcl = null;
							break;
						}

						if (newcl.remove((Unit u) -> {
							if (forced.equalsConstraint(u) && forced.isNeg() != u.isNeg()) {
								return true;
							} else
								return false;
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
	public Iterator<ACQ_Clause> iterator() {
		return clauses.iterator();
	}

}
