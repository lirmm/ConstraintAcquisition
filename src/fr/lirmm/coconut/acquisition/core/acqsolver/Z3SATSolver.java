package fr.lirmm.coconut.acquisition.core.acqsolver;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_CNF;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Clause;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_DNF;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Formula;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Conj;
import fr.lirmm.coconut.acquisition.core.acqconstraint.Unit;
import fr.lirmm.coconut.acquisition.core.combinatorial.CombinationIterator;

public class Z3SATSolver extends SATSolver {
	
	protected Context ctx;
	protected Long timeout;
	protected Boolean timeoutReached;
	
	public Z3SATSolver() {
		ctx = new Context();
		timeout = (long) 0;
		timeoutReached = false;
	}
	
	@Override
	public SATModel solve(ACQ_CNF T) {
		Solver solv = ctx.mkSolver();
		
		solv.add(toZ3(ctx, T));
		Status status = solv.check();
		if (status == Status.SATISFIABLE) {
			return new Z3SATModel(solv.getModel());
		}
		else if (status == Status.UNKNOWN) {
			timeoutReached = true;
		}
		return null;
	}

	@Override
	public SATModel solve(ACQ_Formula F) {
		Solver solv = ctx.mkSolver();
		
		solv.add(toZ3(ctx, F));
		Status status = solv.check();
		if (status == Status.SATISFIABLE) {
			return new Z3SATModel(solv.getModel());
		}
		else if(status == Status.UNKNOWN) {
			timeoutReached = true;
		}
		return null;
	}

	@Override
	public void setVars() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLimit(Long timeout) {
		this.timeout = timeout;
	}

	@Override
	public Unit addVar(ACQ_IConstraint constr, String name) {
		return new Unit(constr, ctx.mkBoolConst(name), false);
	}

	@Override
	public Boolean isTimeoutReached() {
		return timeoutReached;
	}
	
	protected BoolExpr toZ3(Context ctx, ACQ_Formula F) {
		BoolExpr result = ctx.mkTrue();
		for (ACQ_CNF cnf : F.getCnfs()) {
			result = ctx.mkAnd(result, toZ3(ctx, cnf));
		}
		
		/*for (DNF dnf : F.getDnfs()) {
			result = ctx.mkAnd(result, toZ3(ctx, dnf));
		}*/
		if (F.hasAtLeastAtMost()) {
			result = ctx.mkAnd(result, toZ3(ctx, atLeast(F)));
			result = ctx.mkAnd(result, toZ3(ctx, atMost(F)));
		}
		
		return result;
	}
	
	public ACQ_CNF atLeast(ACQ_Formula F) {
		int lower = F.atLeastLower();
		ACQ_Clause cl = F.getAtLeastAtMost();
		assert(lower > 0 && lower < cl.getSize());
		ACQ_CNF result = new ACQ_CNF();
		CombinationIterator iterator = new CombinationIterator(cl.getSize(), cl.getSize() - lower + 1);
		while (iterator.hasNext()) {
			int[] indexes = iterator.next();
			ACQ_Clause newclause = new ACQ_Clause();
			for (int index : indexes) {
				Unit unit = cl.get(index).clone();
				newclause.add(unit);
			}
			result.add(newclause);
		}
		return result;
	}
	
	public ACQ_CNF atMost(ACQ_Formula F) {
		int upper = F.atMostUpper();
		ACQ_Clause cl = F.getAtLeastAtMost();
		assert(upper > 0 && upper < cl.getSize());
		ACQ_CNF result = new ACQ_CNF();
		CombinationIterator iterator = new CombinationIterator(cl.getSize(), upper + 1);
		while (iterator.hasNext()) {
			int[] indexes = iterator.next();
			ACQ_Clause newclause = new ACQ_Clause();
			//System.out.print("indexes : (");
			for (int index : indexes) {
				Unit unit = cl.get(index).clone();
				if (unit.isNeg()) {
					// unit is already negated so to negate it we unset the negation
					unit.unsetNeg();
				}
				else {
					// unit is not negated so we negate it
					unit.setNeg();
				}
				newclause.add(unit);
			}
			result.add(newclause);
		}
		return result;
	}
	
	protected BoolExpr toZ3(Context ctx, ACQ_CNF cnf) {
		BoolExpr result = ctx.mkTrue();
		for (ACQ_Clause cl : cnf) {
			result = ctx.mkAnd(result, toZ3(ctx, cl));
		}
		return result;
	}
	
	protected BoolExpr toZ3(Context ctx, ACQ_DNF dnf) {
		BoolExpr result = ctx.mkFalse();
		for (Conj conj : dnf) {
			result = ctx.mkOr(result, toZ3(ctx, conj));
		}
		return result;
	}
	
	protected BoolExpr toZ3(Context ctx, ACQ_Clause cl) {
		BoolExpr result = ctx.mkFalse();
		for (Unit unit : cl) {
			result = ctx.mkOr(result, unit.toZ3(ctx));
		}
		return result;
	}
	
	protected BoolExpr toZ3(Context ctx, Conj conj) {
		BoolExpr result = ctx.mkTrue();
		for (Unit unit : conj) {
			result = ctx.mkAnd(result, unit.toZ3(ctx));
		}
		return result;
	}
	
	
}
