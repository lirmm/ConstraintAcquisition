/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.lirmm.coconut.quacq.core.acqconstraint;

/**
 *
 * @author agutierr
 */
public abstract class BinaryConstraint extends ACQ_Constraint{

    public BinaryConstraint(String name,int var1,int var2) {
        super(name,new int[]{var1,var2});
    }
    public BinaryConstraint(String name, BinaryConstraint cst1, BinaryConstraint cst2,int[] variables) {
    	super(name,cst1,cst2,variables);
    
    }
	@Override
	public boolean check(int... value) {
        return check(value[0],value[1]);
    }
    protected abstract boolean check(int value1,int value2);
    
}
