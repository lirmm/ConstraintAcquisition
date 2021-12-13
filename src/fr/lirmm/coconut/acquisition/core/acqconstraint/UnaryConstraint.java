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
public abstract class UnaryConstraint extends ACQ_Constraint{

    public UnaryConstraint(String name,int var) {
        super(name,new int[]{var});
    }

//    protected abstract boolean check(int value);

    @Override
	public boolean check(int... value) {
        return check(value[0]);
    }
    protected abstract boolean check(int value);
}
