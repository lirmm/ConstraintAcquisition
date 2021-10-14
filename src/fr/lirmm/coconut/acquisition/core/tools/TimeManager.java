package fr.lirmm.coconut.acquisition.core.tools;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimeManager {
	
	protected String name = null;
	
	protected TimeUnit unit = TimeUnit.MS;

	private List<Double> list = new ArrayList<Double>();
	
	public void add(double i) {
		list.add(i);
	}
	public TimeManager() {
	}
	public TimeManager(String name) {
		this.name = name;
	}
	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}


	public void display() {
		for(double i : list) {
			System.out.println(i);
		}
	}
	
	public int nbInstance() {
		return list.size();
	}
	
	public double getMax() {
		return list.size()==0 ? 0 : Collections.max(list);
	}
	
	public double getMin() {
		return list.size()==0 ? 0 : Collections.min(list);
	}
	
	public double getTotal() {
		double time = 0;
		for(double f : list) {
			time += f;
		}
		return time;
	}
	
	public double getAverage() {
		return getTotal()/nbInstance();
	}
	
	public double getMedian() {
		return list.size()==0 ? 0 : list.get((int)Math.floor((nbInstance()-1)/2));
	}
	
	public double getSD(){
		double sd = 0;
		for(double f : list) {
			sd += Math.pow(Math.abs(f - getAverage()), 2);
		}
		
		return Math.sqrt(sd/nbInstance());
	}
	
	public String getResults() {
		DecimalFormat df = new DecimalFormat("0.00E0");
		return "------Solving times------" +
				"\nTotal time : " + df.format(getTotal()) +
				"\nMax time : " + df.format(getMax()) +
				"\nMin time : " +  df.format(getMin()) +
				"\nAverage time : " + df.format(getAverage()) +
				"\nMedian : " + df.format(getMedian()) +
				"\nStandard deviation : " + df.format(getSD()) + 
				"\nInstances : " + nbInstance();
	}

}
