package fr.lirmm.coconut.acquisition.core.acqsolver;

import java.util.ArrayList;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_CNF;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Clause;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_DNF;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Formula;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Conj;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ConstraintMapping;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Unit;
import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;

public class MiniSatSolver extends SATSolver {

	
	ConstraintMapping mapping;
	int timeout;
	int nvars = 0;
	boolean timeoutReached = false;
	
	public MiniSatSolver() {
		mapping = new ConstraintMapping();
	}
	
	protected VecInt toMiniSatClause(ACQ_Clause cl) {
		int[] minisatcl = new int[cl.getSize()];
		for (int i = 0; i < cl.getSize(); i++) {
			Unit unit = cl.get(i);
			minisatcl[i] = unit.toMiniSat();
		}
		return new VecInt(minisatcl);
	}
	
	@Override
	public SATModel solve(ACQ_CNF T) {
		fireSolverEvent("BEG_satsolve", false, true);
		
		ISolver solv = SolverFactory.newDefault();
		solv.setTimeout(this.timeout);
		
		solv.newVar(this.nvars);
		//solv.setExpectedNumberOfClauses(T.getSize());
		
		SATModel res = null;
		
		try {
			
			for (ACQ_Clause cl : T) {
				assert cl.getSize() > 0 : "empty clause";
				solv.addClause(toMiniSatClause(cl));	
			}
			
			IProblem problem = solv;
			
			fireSolverEvent("BEG_TIMECOUNT", false, true);
			boolean sat = problem.isSatisfiable();
			fireSolverEvent("END_TIMECOUNT", true, false);
			
			if (sat) {
				ArrayList<Integer> model = convert(problem.model());
				res = new MiniSatModel(model, mapping);
			}
		} 
		catch (ContradictionException e) {/* the constraint network is not satisfiable */} 
		catch (TimeoutException e) {
			this.timeoutReached = true;
		}
		
		fireSolverEvent("END_satsolve", true, false);
		return res;
	}

	@Override
	public SATModel solve(ACQ_Formula F) {
		fireSolverEvent("BEG_satsolve", false, true);
		
		ISolver solv = SolverFactory.newDefault();
		solv.setTimeout(this.timeout);
		
		SATModel res = null;
		
		try {
			for (ACQ_CNF T : F.getCnfs()) {
				for (ACQ_Clause cl : T) {
					VecInt minisatclause = toMiniSatClause(cl);					
					solv.addClause(minisatclause);
				}
			}
			
			if (F.hasAtLeastAtMost()) {
				int nextvar = nvars + 1;
				AtLeastAtMostResult al = atLeastSeqCounter(F, nextvar); // atLeast(F, nextvar);
				nextvar = al.nextvar;
				for(VecInt cl : al.res) {
					solv.addClause(cl);
				}
				AtLeastAtMostResult am = atMostSeqCounter(F, nextvar); // atMost(F, nextvar);
				nextvar = am.nextvar;
				for(VecInt cl : am.res) {
					solv.addClause(cl);
				}
			}
			
			
			IProblem problem = solv;
			fireSolverEvent("BEG_TIMECOUNT", false, true);
			boolean sat = problem.isSatisfiable();
			fireSolverEvent("END_TIMECOUNT", true, false);
			if (sat) {
				ArrayList<Integer> model = convert(problem.model());
				res = new MiniSatModel(model, mapping);
			}
		} 
		catch (ContradictionException e) {/* the constraint network is not satisfiable */} 
		catch (TimeoutException e) {
			this.timeoutReached = true;
		}
		
		fireSolverEvent("END_satsolve", true, false);
		return res;
	}
	
	protected AtLeastAtMostResult atLeast(ACQ_Formula F, int nextvar) {
		ACQ_Clause cl = F.getAtLeastAtMost();
		int lower = F.atLeastLower();
		assert(lower > 0 && lower < cl.getSize());
		ArrayList<VecInt> result = new ArrayList<>();
		CombinationIterator iterator = new CombinationIterator(cl.getSize(), cl.getSize() - lower + 1);
		while (iterator.hasNext()) {
			int[] indexes = iterator.next();
			int[] newclause = new int[indexes.length];
			int i = 0;
			for (int index : indexes) {
				newclause[i] = cl.get(index).toMiniSat();
				i++;
			}
			result.add(new VecInt(newclause));
		}
		return new AtLeastAtMostResult(result, nextvar);
	}
	
	public AtLeastAtMostResult atMost(ACQ_Formula F, int nextvar) {
		int upper = F.atMostUpper();
		ACQ_Clause cl = F.getAtLeastAtMost();
		assert(upper > 0 && upper < cl.getSize());
		ArrayList<VecInt> result = new ArrayList<>();
		CombinationIterator iterator = new CombinationIterator(cl.getSize(), upper + 1);
		while (iterator.hasNext()) {
			int[] indexes = iterator.next();
			int[] newclause = new int[indexes.length];
			int i = 0;
			for (int index : indexes) {
				newclause[i] = - cl.get(index).toMiniSat();
				i++;
			}
			result.add(new VecInt(newclause));
		}
		return new AtLeastAtMostResult(result, nextvar);
	}
	
	
	
	/*
	 * Encoding of atLeast(k) adapted from "Towards an Optimal CNF Encoding of Boolean Cardinality Constraints"
	 * of Carsten Sinz
	 */
	public AtLeastAtMostResult atLeastSeqCounter(ACQ_Formula F, int nextvar){
		int lower = F.atLeastLower();
		ACQ_Clause cl = F.getAtLeastAtMost();
		assert cl.getSize() > 1 : "Encoding works only if cl.getSize() > 1"; 
		
		// <= k(x1, ..., xn) iff >= (n-k)(-x1, ..., -xn)
		ACQ_Clause newcl = cl.clone();
		for(Unit unit : newcl) {
			if(unit.isNeg()) {
				unit.unsetNeg();
			} else {
				unit.setNeg();
			}
		}
		
		int upper = newcl.getSize() - lower;
		assert(upper > 0 && upper < cl.getSize());
		return atMostSeqCounter(newcl, upper, nextvar); 
		
	}
	
	/*
	 * Encoding of atMost(k) from "Towards an Optimal CNF Encoding of Boolean Cardinality Constraints"
	 * of Carsten Sinz
	 */
	public AtLeastAtMostResult atMostSeqCounter(ACQ_Formula F, int nextvar){
		int upper = F.atMostUpper();
		ACQ_Clause cl = F.getAtLeastAtMost();
		assert cl.getSize() > 1 : "Encoding works only if cl.getSize() > 1"; 
		assert(upper > 0 && upper < cl.getSize());
		return atMostSeqCounter(cl, upper, nextvar);
	}
	
	public AtLeastAtMostResult atMostSeqCounter(ACQ_Clause cl, int upper, int nextvar){
		ArrayList<VecInt> res = new ArrayList<>();
		
		// Set new variables
		int[][] newvars = new int[cl.getSize()-1][upper];
		
		for(int index = 0; index < upper*(cl.getSize()-1); index++) {
			int l = index / upper;
			int c = index % upper;
			newvars[l][c] = index + nextvar;
		}
		
		int x0 = cl.get(0).toMiniSat();
		res.add(new VecInt(new int[] {-x0, newvars[0][0]}));
		
		for(int j = 1; j < upper; j++) {
			res.add(new VecInt(new int[] {-newvars[0][j]}));
		}
		
		for(int i = 1; i < cl.getSize() -1; i++) {
			int xi = cl.get(i).toMiniSat();
			res.add(new VecInt(new int[] {-xi, newvars[i][0]}));
			res.add(new VecInt(new int[] {-newvars[i-1][0], newvars[i][0]}));
			for(int j = 1; j < upper; j++) {
				res.add(new VecInt(new int[] {-xi, -newvars[i-1][j-1], newvars[i][j]}));
				res.add(new VecInt(new int[] {-newvars[i-1][j], newvars[i][j]}));
			}
			res.add(new VecInt(new int[] {-xi, -newvars[i-1][upper-1]}));
		}
		
		int n = cl.getSize()-1;
		int xn = cl.get(n).toMiniSat();
		res.add(new VecInt(new int[] {-xn, -newvars[n-1][upper-1]}));
		
		return new AtLeastAtMostResult(res, nextvar + upper*(cl.getSize()-1));
	}
	
	protected ArrayList<Integer> convert(int[] l) {
		ArrayList<Integer> res = new ArrayList<>();
		for (int i : l) {
			res.add(i);
		}
		return res;
	}

	@Override
	public void setVars() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLimit(Long timeout) {
		this.timeout = timeout.intValue();
		
	}

	@Override
	public Unit addVar(ACQ_IConstraint constr, String name) {
		this.nvars++;
		Unit unit = new Unit(constr, nvars, false);
		//mapping.add(constr, unit);
		return unit;
	}

	@Override
	public Boolean isTimeoutReached() {
		return this.timeoutReached;
	}
	
	protected class AtLeastAtMostResult {
		int nextvar;
		ArrayList<VecInt> res;
		
		public AtLeastAtMostResult(ArrayList<VecInt> res , int nextvar) {
			this.res = res;
			this.nextvar = nextvar;
		}
	}
	
}
