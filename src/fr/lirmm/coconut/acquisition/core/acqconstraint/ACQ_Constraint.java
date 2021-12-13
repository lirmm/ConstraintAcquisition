/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.core.acqconstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

/**
 * Abstract class that defines the functions common to all constraints
 * 
 * @author agutierr
 */
public abstract class ACQ_Constraint implements ACQ_IConstraint {
	/**
	 * nameof this constraint
	 */
	protected String name;
	/**
	 * Variables of this constraint
	 */

	final int[] variables;
	final ACQ_TemporalVariable[] tempvariables;

	/**
	 * Constraints
	 */
	private ACQ_IConstraint cst1;
	private ACQ_IConstraint cst2;

	/**
	 * Constructor of this constraint
	 * 
	 * @param name      nameof this constraint
	 * @param variables Variables of this constraint
	 */
	public ACQ_Constraint(String name, int[] variables) {
		this.name = name;
		this.variables = variables;
		this.tempvariables = null;
	}
	public ACQ_Constraint(String name, ACQ_TemporalVariable[] variables) {
		this.name = name;
		this.tempvariables = variables;
		this.variables = new int[] {variables[0].getStart(),variables[0].getEnd(),variables[1].getStart(),variables[1].getEnd()};
	}
	public ACQ_Constraint(String name, Set<Integer> variables) {
		this.name = name;
		this.variables = new int[variables.size()];
		int i = 0;
		for (Integer ii : variables)
			this.variables[i++] = ii;
		this.tempvariables = null;

	}

	/**
	 * Constructor of this constraint
	 * 
	 * @param name        nameof this constraint
	 * @param Constraints
	 */
	public ACQ_Constraint(String name, ACQ_IConstraint cst1, ACQ_IConstraint cst2, int[] variables) {
		this.name = name;
		this.cst1 = cst1;
		this.cst2 = cst2;
		this.variables = variables;
		this.tempvariables = null;


	}

	/**
	 * Returns a sub array of the specified IntVar array, that only contains the
	 * variables involved into this constraint
	 * 
	 * @param fullVarSet All the variables of the solver model
	 * @return A sub array containing the variables of this constraint
	 * @author teddy
	 */
	public IntVar[] getVariables(IntVar[] fullVarSet) {
		ACQ_Scope scope = getScope();
		ArrayList<IntVar> intVars = new ArrayList<>();
		Iterator<Integer> iterator = scope.iterator();
		while (iterator.hasNext()) {
			intVars.add(fullVarSet[iterator.next()]);
		}
		return intVars.toArray(new IntVar[intVars.size()]);
	}

	/**
	 * Returns the nameof this constraint
	 * 
	 * @return nameof this constraint
	 */
	@Override
	public String getName() {
		return name;
	}
 
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the scope of this constraint
	 * 
	 * @return scope of this constraint
	 */
	@Override
	public ACQ_Scope getScope() {
		return new ACQ_Scope(variables);
	}
	/**
	 * Returns the scope of this constraint
	 * 
	 * @return scope of this constraint
	 */
	@Override
	public ACQ_Scope getTemporalScope() {
			int[] vars = new int[4];
			int i =0;
			for(ACQ_TemporalVariable t : tempvariables) {
				vars[i]=t.getStart();
				vars[i+1]=t.getEnd();
				i+=2;
			}
				
			ACQ_Scope s = new ACQ_Scope(vars);
			System.out.println(s.getVariables());
		return s;
	}

	/**
	 * Returns the number of variables of this constraint
	 * 
	 * @return number of variable of this constraint
	 */
	@Override
	public int getArity() {
		return variables.length;
	}

	/**
	 * Returns the number of variables of this constraint
	 * 
	 * @return number of variable of this constraint
	 */
	@Override
	public int[] getVariables() {
		return variables;
	}
	/**
	 * Returns the number of variables of this constraint
	 * 
	 * @return number of variable of this constraint
	 */
	@Override
	public ACQ_TemporalVariable[] getTemporalVariables() {
		return tempvariables;
	}

	/**
	 * Get the numeric values of the example query
	 * 
	 * @param query Example positive or negative
	 * @return numeric values of the example query
	 */
	@Override
	public int[] getProjection(ACQ_Query query) {
		int index = 0;
		int[] values = new int[variables.length];
		for (int numvar : variables)
			values[index++] = query.getValue(numvar);
		return values;
	}
	@Override
	public ACQ_TemporalVariable[] getProjectionTempVariables(ACQ_Query query) {
		int index = 0;
		ACQ_TemporalVariable[] values = new ACQ_TemporalVariable[tempvariables.length];
		if(!query.inverse) {
		for (int i = 0; i< variables.length;i+=2) {
			values[index]=new ACQ_TemporalVariable(variables[i],variables[i+1]);
			values[index].setStartValue(query.getValue(variables[i]));
			values[index].setEndValue(query.getValue(variables[i+1]));
			index++;
		}}else {
			for (int i = 0; i< query.variables.length;i+=2) {
				values[index]=new ACQ_TemporalVariable(query.variables[i],query.variables[i+1]);
				values[index].setStartValue(query.getValue(query.variables[i]));
				values[index].setEndValue(query.getValue(query.variables[i+1]));
				index++;
			}
			
		}
		return values;
	}
	@Override
	public int[] getExactProjection(ACQ_Query query) {

		int index = 0;


		int[] values = new int[query.getScope().size()];
		for (int numvar : variables) {
			values[index] = query.getValueAt(numvar);
			index++;
		}
		return values;
	}
	
	/**
	 * Checks if the constraint is violated for a given set of values
	 * 
	 * @param values set of values to check
	 * @return false if the set of values violate this constraint
	 */
	@Override
	public final boolean checker(int[] values) {
		return check(values);
	}

	/**
	 * Checks if the constraint is violated for a given set of values
	 * 
	 * @param value set of values to check
	 * @return false if the set of values violate this constraint
	 */
	public abstract boolean check(int... value);

	/**
	 * Checks if the constraint is violated for a given query
	 * 
	 * @param the query
	 * @return false if the query violates this constraint
	 */
	public abstract boolean check(ACQ_Query query);

	@Override
	public String toString() {
		return name + Arrays.toString(variables);
	}

	public Operator getOperator() {
		String name = this.getName();
		if (name.contains("Different"))
			return Operator.NEQ;
		else if (name.contains("Equal"))
			return Operator.EQ;
		else if (name.contains("LessEqual"))
			return Operator.LE;
		else if (name.contains("GreaterEqual"))
			return Operator.GE;
		else if (name.contains("Less"))
			return Operator.LT;
		else if (name.contains("Greater"))
			return Operator.GT;

		return Operator.NONE;
	}

	public static class CstrFactory {

		public static ACQ_Constraint getConstraint(ArrayList<String> cst) {
			ACQ_TemporalVariable variable1=new ACQ_TemporalVariable(Integer.parseInt(cst.get(1))*2, Integer.parseInt(cst.get(1))*2+1);
			ACQ_TemporalVariable variable2=new ACQ_TemporalVariable(Integer.parseInt(cst.get(2))*2, Integer.parseInt(cst.get(2))*2+1);

			switch (cst.get(0)) {
			case "DifferentXY":
				return new BinaryArithmetic("DifferentXY", Integer.parseInt(cst.get(1)), Operator.NEQ,
						Integer.parseInt(cst.get(2)), "EqualXY");
			case "EqualXY":
				return new BinaryArithmetic("EqualXY", Integer.parseInt(cst.get(1)), Operator.EQ,
						Integer.parseInt(cst.get(2)), "DifferentXY");

			case "GreaterXY":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.GT,
						Integer.parseInt(cst.get(2)), "LessEqualXY");

			case "LessXY":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.LT,
						Integer.parseInt(cst.get(2)), "GreaterEqualXY");

			case "GreaterEqualXY":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.GE,
						Integer.parseInt(cst.get(2)), "LessXY");

			case "LessEqualXY":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.LE,
						Integer.parseInt(cst.get(2)), "GreaterXY");

			case "EqualX_":
				return new UnaryArithmetic("DifferentX_" + cst.get(1), Integer.parseInt(cst.get(2)), Operator.EQ,
						Integer.parseInt(cst.get(2)));

			case "DifferentX_":
				return new UnaryArithmetic("DifferentX_" + cst.get(1), Integer.parseInt(cst.get(2)), Operator.NEQ,
						Integer.parseInt(cst.get(2)));

			case "LessX_":
				return new UnaryArithmetic("DifferentX_" + cst.get(1), Integer.parseInt(cst.get(2)), Operator.LT,
						Integer.parseInt(cst.get(2)));

			case "GreaterX_":
				return new UnaryArithmetic("DifferentX_" + cst.get(1), Integer.parseInt(cst.get(2)), Operator.GT,
						Integer.parseInt(cst.get(2)));

			case "LessEqualX_":
				return new UnaryArithmetic("DifferentX_" + cst.get(1), Integer.parseInt(cst.get(2)), Operator.LE,
						Integer.parseInt(cst.get(2)));

			case "OutDiag1":
				return new BinaryArithmetic("OutDiag1", Integer.parseInt(cst.get(1)), Operator.NEQ,
						Integer.parseInt(cst.get(2)), Operator.PL,
						(Integer.parseInt(cst.get(2)) - Integer.parseInt(cst.get(1))), "InDiag1");

			case "InDiag1":
				return new BinaryArithmetic("InDiag1", Integer.parseInt(cst.get(1)), Operator.EQ,
						Integer.parseInt(cst.get(2)), Operator.PL,
						(Integer.parseInt(cst.get(2)) - Integer.parseInt(cst.get(1))), "OutDiag1");

			case "OutDiag2":
				return new BinaryArithmetic("OutDiag2", Integer.parseInt(cst.get(1)), Operator.NEQ,
						Integer.parseInt(cst.get(2)), Operator.PL,
						(Integer.parseInt(cst.get(1)) - Integer.parseInt(cst.get(2))), "InDiag2");

			case "InDiag2":
				return new BinaryArithmetic("InDiag2", Integer.parseInt(cst.get(1)), Operator.EQ,
						Integer.parseInt(cst.get(2)), Operator.PL,
						(Integer.parseInt(cst.get(1)) - Integer.parseInt(cst.get(2))), "OutDiag2");

			case "GreaterEqualX_":
				return new UnaryArithmetic("DifferentX_" + cst.get(1), Integer.parseInt(cst.get(2)), Operator.GE,
						Integer.parseInt(cst.get(2)));

			case "DistDiffXY":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.NEQ, Integer.parseInt(cst.get(3)), "DistEqXY");

			case "DistEqXY":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.EQ, Integer.parseInt(cst.get(3)), "DistDiffXY");

			case "AT_Equal":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.EQ, Integer.parseInt(cst.get(3)), "AT_Diff");

			case "AT_Diff":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.NEQ, Integer.parseInt(cst.get(3)), "AT_Equal");

			case "AT_GT":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.GT, Integer.parseInt(cst.get(3)), "AT_LE");

			case "AT_LT":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.LT, Integer.parseInt(cst.get(3)), "AT_GE");

			case "AT_GE":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.GE, Integer.parseInt(cst.get(3)) - 1, "AT_LT");

			case "AT_LE":
				return new BinaryArithmetic(cst.get(0), Integer.parseInt(cst.get(1)), Operator.Dist,
						Integer.parseInt(cst.get(2)), Operator.LE, Integer.parseInt(cst.get(3)) + 1, "AT_GT");

			case "DistDiffQ":
				return new ScalarArithmetic("DistDiff",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.NEQ, 0, "DistEqual");

			case "DistEqualQ":
				return new ScalarArithmetic("DistEqual",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.EQ, 0, "DistDiff");

			case "DistGreaterQ":
				return new ScalarArithmetic("DistGreater",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.GT, 0, "DistLessEqual");

			case "DistLessQ":
				return new ScalarArithmetic("DistLess",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.LT, 0, "DistGreaterEqual");

			case "DistGreaterEqualQ":
				return new ScalarArithmetic("DistGreaterEqual",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.GE, 0, "DistLess");

			case "DistLessEqualQ":
				return new ScalarArithmetic("DistLessEqual",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.LE, 0, "DistGreater");

			case "DistDiffT":
				return new ScalarArithmetic("DistDiff", new int[] { Integer.parseInt(cst.get(1)),
						Integer.parseInt(cst.get(2)), Integer.parseInt(cst.get(3)) }, new int[] { 1, -2, 1 },
						Operator.NEQ, 0, "DistEqual");

			case "DistEqualT":
				return new ScalarArithmetic("DistDiff", new int[] { Integer.parseInt(cst.get(1)),
						Integer.parseInt(cst.get(2)), Integer.parseInt(cst.get(3)) }, new int[] { 1, -2, 1 },
						Operator.EQ, 0, "DistDiff");

			case "DistGreaterT":
				return new ScalarArithmetic("DistGreater",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)) },
						new int[] { 1, -2, 1 }, Operator.GT, 0, "DistLessEqual");

			case "DistLessT":
				return new ScalarArithmetic("DistLess",
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)) },
						new int[] { 1, -2, 1 }, Operator.LT, 0, "DistGreaterEqual");

			case "DistGreaterEqualT":
				return new ScalarArithmetic("DistGreaterEqual", new int[] { Integer.parseInt(cst.get(1)),
						Integer.parseInt(cst.get(2)), Integer.parseInt(cst.get(3)) }, new int[] { 1, -2, 1 },
						Operator.GE, 0, "DistLess");

			case "DistLessEqualT":
				return new ScalarArithmetic("DistLessEqual", new int[] { Integer.parseInt(cst.get(1)),
						Integer.parseInt(cst.get(2)), Integer.parseInt(cst.get(3)) }, new int[] { 1, -2, 1 },
						Operator.LE, 0, "DistGreater");

			case "DistDiffXYZ":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)) },
						new int[] { 1, -2, 1 }, Operator.NEQ, 0, "DistEqualXYZ");

			case "DistEqualXYZ":
				return new ScalarArithmetic(cst.get(0), new int[] { Integer.parseInt(cst.get(1)),
						Integer.parseInt(cst.get(2)), Integer.parseInt(cst.get(3)) }, new int[] { 1, -2, 1 },
						Operator.EQ, 0, "DistDiffXYZ");

			case "DistGreaterXYZ":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)) },
						new int[] { 1, -2, 1 }, Operator.GT, 0, "DistLessEqualXYZ");

			case "DistLessEqualXYZ":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)) },
						new int[] { 1, -2, 1 }, Operator.LE, 0, "DistGreaterXYZ");

			case "DistGreaterEqualXYZ":
				return new ScalarArithmetic(cst.get(0), new int[] { Integer.parseInt(cst.get(1)),
						Integer.parseInt(cst.get(2)), Integer.parseInt(cst.get(3)) }, new int[] { 1, -2, 1 },
						Operator.GE, 0, "DistLessXYZ");

			case "DistLessXYZ":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)) },
						new int[] { 1, -2, 1 }, Operator.LT, 0, "DistGreaterEqualXYZ");

			case "DistDiffXYZT":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.NEQ, 0, "DistEqualXYZT");

			case "DistEqualXYZT":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.EQ, 0, "DistDiffXYZT");

			case "DistGreaterXYZT":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.GT, 0, "DistLessEqualXYZT");

			case "DistLessEqualXYZT":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.LE, 0, "DistGreaterXYZT");

			case "DistGreaterEqualXYZT":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.GE, 0, "DistLessXYZT");

			case "DistLessXYZT":
				return new ScalarArithmetic(cst.get(0),
						new int[] { Integer.parseInt(cst.get(1)), Integer.parseInt(cst.get(2)),
								Integer.parseInt(cst.get(3)), Integer.parseInt(cst.get(4)) },
						new int[] { 1, -1, -1, 1 }, Operator.LT, 0, "DistGreaterEqualXYZT");
			case "PrecedesXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.LT, variable2, false,Operator.NONE, "NotPrecedesXY",false);
			case "IsPrecededXY":
				return new TemporalArithmetic(cst.get(0), variable2, Operator.LT, variable1, false,Operator.NONE, "IsNotPrecededXY",true);
			
			case "NotPrecedesXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.GE, variable2, false,Operator.NONE, "PrecedesXY",false);

			case "IsNotPrecededXY":
				return new TemporalArithmetic(cst.get(0), variable2, Operator.GE, variable1, false,Operator.NONE, "IsPrecededXY",true);
				
			case "NotMeetXY":
				new TemporalArithmetic(cst.get(0), variable1, Operator.NEQ,variable2, false, Operator.NONE, "MeetsXY",false);
			
			case "MeetsXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.EQ,variable2, false, Operator.NONE, "NotMeetXY",false);
			case "IsNotMetXY":
				
				return new TemporalArithmetic(cst.get(0), variable2, Operator.NEQ,
						variable1, false, Operator.NONE, "IsMetXY",true);
			case "IsMetXY":
			
				return new TemporalArithmetic(cst.get(0), variable2, Operator.EQ,
						variable1, false, Operator.NONE, "IsNotMetXY",true);
			case "OverlapsXY":
					
					return new OverlapArithmetic(cst.get(0), variable1, Operator.LT,
							variable2, "NotOverlapsXY",false);
			case "NotOverlapsXY":
				
				return new OverlapArithmetic(cst.get(0), variable1, Operator.GE,
						variable2, "OverlapsXY",false);			
			case "IsOverlappedXY":
				
						return new OverlapArithmetic(cst.get(0), variable2, Operator.LT,
								variable1, "IsNotOverlappedXY",true);
			case "IsNotOverlappedXY":
				

				return new OverlapArithmetic(cst.get(0), variable2, Operator.GE,
						variable1, "IsOverlappedXY",true);			
			case "StartsXY":
				
						return new TemporalArithmetic(cst.get(0), variable1, Operator.EQ,
								variable2, true, Operator.LT, "NotStartsXY",false);
			case "NotStartsXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.NEQ,
						variable2, true, Operator.LT, "StartsXY",false);
			case "IsStartedXY":
				return new TemporalArithmetic(cst.get(0), variable2, Operator.EQ,
						variable1, true, Operator.LT, "IsNotStartedXY",true);			
			case "IsNotStartedXY":
				return new TemporalArithmetic(cst.get(0), variable2, Operator.NEQ,
						variable1, true, Operator.LT, "IsStartedXY",true);						
			case "DuringXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.GT,
						variable2, true, Operator.LT, "NotDuringXY",false);
			case "ContainsXY":
				return new TemporalArithmetic(cst.get(0), variable2, Operator.GT,
						variable1, true, Operator.LT, "NotContainsXY",true);
			case "NotDuringXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.LE,
						variable2, true, Operator.GE, "DuringXY",false);
			case "NotContainsXY":
				return new TemporalArithmetic(cst.get(0), variable2, Operator.LE,
						variable1, true, Operator.GE, "ContainsXY",true);					
			case "ExactXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.EQ,
						variable2, true, Operator.EQ, "NotExactXY",false);
			case "NotExactXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.NEQ,
						variable2, true, Operator.NEQ, "ExactXY",false);							
			case "FinishXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.GT,
						variable2, true, Operator.EQ, "NotFinishXY",false);
			case "NotFinishXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.LE,
						variable2, true, Operator.NEQ, "FinishXY",false);						
			case "IsFinishedXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.GT,
						variable2, true, Operator.EQ, "IsNotFinishedXY",true);					
			case "IsNotFinishedXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.LE,
						variable2, true, Operator.NEQ, "IsFinishedXY",true);						 
				
			case "DisconnectedXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.LE,
						variable2, true, Operator.GE, "DuringXY",false);
			case "ExternallyConnectedXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.LE,
						variable2, true, Operator.GE, "ContainsXY",true);					
			case "TangentialProperPartXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.EQ,
						variable2, true, Operator.EQ, "NotExactXY",false);
			case "TangentialProperPartInverseXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.NEQ,
						variable2, true, Operator.NEQ, "ExactXY",false);							
			case "PartiallyOverlappingXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.GT,
						variable2, true, Operator.EQ, "NotFinishXY",false);
			case "NonTangentialProperPartXY":
				return new TemporalArithmetic(cst.get(0), variable1, Operator.LE,
						variable2, true, Operator.NEQ, "FinishXY",false);						
			case "NonTangentialProperPartInverseXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.GT,
						variable2, true, Operator.EQ, "IsNotFinishedXY",true);					
			case "REqualXY":
				
				return new TemporalArithmetic(cst.get(0), variable1, Operator.GT,
						variable2, true, Operator.EQ, "IsNotFinishedXY",true);					
			
			}
			throw new UnsupportedOperationException();
		}
	}
	@Override
	public
	ACQ_IConstraint getInverse() {
		return null;}
	public boolean check(ACQ_Query query, ACQ_Constraint cst) {
		// TODO Auto-generated method stub
		return false;
	}
	public boolean isInverse() {
		// TODO Auto-generated method stub
		return false;
	}

}
