package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

public class ACQ_Clause implements Iterable<Unit> {

	protected ArrayList<Unit> units;
	protected Boolean marked = false;

	public ACQ_Clause() {
		units = new ArrayList<Unit>();
	}

	public ACQ_Clause(Unit unit) {
		units = new ArrayList<Unit>();
		units.add(unit);
	}

	public ACQ_Clause clone() {
		ACQ_Clause res = new ACQ_Clause();
		if (marked) {
			res.mark();
		}
		for (Unit unit : units) {
			res.add(unit.clone());
		}
		assert (res.getSize() == this.getSize());
		return res;
	}

	public void add(Unit unit) {
		assert (unit != null);
		units.add(unit);
	}

	public Unit get(int index) {
		return units.get(index);
	}

	public Boolean isMarked() {
		return marked;
	}

	public void mark() {
		marked = true;
	}

	public void unmark() {
		marked = false;
	}

	public int getSize() {
		return units.size();
	}

	public Boolean isEmpty() {
		return this.getSize() == 0;
	}

	public boolean remove(Function<Unit, Boolean> foo) {
		boolean res = false;
		for (int i = 0; i < units.size(); i++) {
			if (foo.apply(units.get(i))) {
				units.remove(i);
				res = true;
			}
		}
		return res;
	}

	public Boolean contains(Function<Unit, Boolean> foo) {
		for (int i = 0; i < units.size(); i++) {
			if (foo.apply(units.get(i))) {
				return true;
			}
		}
		return false;
	}

	public Boolean contains(ACQ_IConstraint constr) {
		for (Unit unit : units) {
			if (unit.equalsConstraint(constr)) {
				return true;
			}
		}
		return false;
	}

	public Boolean containsConstraint(Unit unit) {
		for (Unit u : units) {
			if (unit.equalsConstraint(u)) {
				return true;
			}
		}
		return false;
	}

	public Boolean contains(Unit unit) {
		for (Unit u : units) {
			if (unit.equals(u)) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		String s = "";
		for (Unit unit : units) {
			if (s.length() == 0)
				s += unit.toString();
			else
				s += " or " + unit.toString();

		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (Unit unit : units) {
			result = prime * result + unit.hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ACQ_Clause clause = (ACQ_Clause) obj;
		// System.out.println("In Clause.equals()");
		ACQ_Clause tmp = clause.clone();
		for (Unit unit : units) {
			if (tmp.contains(unit)) {
				assert tmp.remove((Unit u) -> {
					if (u.equals(unit))
						return true;
					else
						return false;
				});
			}
		}
		return tmp.isEmpty();
	}

	public boolean subsumed(ACQ_Clause cl) {
		for (Unit unit : units) {
			if (!cl.contains(unit))
				return false;
		}
		return false;
	}

	@Override
	public Iterator<Unit> iterator() {
		return units.iterator();
	}
}
