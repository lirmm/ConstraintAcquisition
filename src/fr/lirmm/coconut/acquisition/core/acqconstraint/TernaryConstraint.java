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
public abstract class TernaryConstraint extends ACQ_Constraint{

    public TernaryConstraint(String name,int var1,int var2,int var3) {
        super(name,new int[]{var1,var2,var3});
    }

    @Override
	public boolean check(int... value) {
        return check(value[0],value[1],value[2]);
    }
    protected abstract boolean check(int value1,int value2,int value3);
}
