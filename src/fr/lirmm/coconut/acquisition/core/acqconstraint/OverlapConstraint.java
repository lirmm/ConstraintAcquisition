/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.acquisition.core.acqconstraint;

/**
 *
 * @author agutierr
 */
public abstract class OverlapConstraint extends ACQ_Constraint{

    public OverlapConstraint(String name,ACQ_TemporalVariable var1,ACQ_TemporalVariable var2) {
        super(name,new ACQ_TemporalVariable[]{var1,var2});
    }
    public OverlapConstraint(String name, OverlapConstraint cst1, OverlapConstraint cst2,int[] variables) {
    	super(name,cst1,cst2,variables);
    
    }
    @Override
	public boolean check(int[] value) {
        return check(value[0],value[1], null);
    }
    protected abstract boolean check(int value1,int value2, Operator op);
    
}
