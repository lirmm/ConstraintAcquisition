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
public abstract class ScalarConstraint extends ACQ_Constraint{
	int[] coeff;
    public ScalarConstraint(String name,int[] vars,int[] coeff, int cste) {
        super(name,vars);
        this.coeff=coeff;
    }
 
    @Override
	public boolean check(int[]value) {
        return check(value, coeff);
    }
    protected abstract boolean check(int []value,int[] coeff);
}
