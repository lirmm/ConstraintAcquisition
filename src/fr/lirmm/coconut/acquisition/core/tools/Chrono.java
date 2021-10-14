package fr.lirmm.coconut.acquisition.core.tools;

import java.util.ArrayList;
import java.util.HashMap;

public class Chrono {
	private String name;
	private boolean nano;
	protected HashMap<String,Long> current_chronos=new HashMap<String,Long>();
	protected HashMap<String,ArrayList<Long>> results=new HashMap<String,ArrayList<Long>>();
	public Chrono(String name,boolean nano)
	{
		this.name=name;
		this.nano=nano;
	}

	public Chrono(String name) {
		this(name, false);
	}

	public String getName() {
		return name;
	}

	public void start(String serieName) {
		current_chronos.put(serieName, nano ? System.nanoTime() : System.currentTimeMillis());
	}

	synchronized public void stop(String serieName) {
		Long currentTime = current_chronos.get(serieName);
		if (currentTime != null) {
			ArrayList<Long> result = results.get(serieName);
			if (result == null) {
				result = new ArrayList<Long>();
				results.put(serieName, result);
			}
			result.add(nano ? System.nanoTime() : System.currentTimeMillis() - currentTime);
		}
	}

	synchronized public long getResult(String serieName) {
		long l = 0L;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName))
				if (mesure != null)
					l += mesure;
		} else {
			System.err.println(serieName);
		}
		return l;
	}

	synchronized public long getLast(String serieName) {
		long l = 0L;
		if (results.get(serieName) != null) {

			if (results.get(serieName).get(results.get(serieName).size()-1) != null)
				l += results.get(serieName).get(results.get(serieName).size()-1);
		} else {
			System.err.println(serieName);
		}
		return l;
	}

	synchronized public ArrayList<Long> getResultArray(String serieName) {
		return results.get(serieName);
	}

	synchronized public long getResult() {
		long l = 0L;
		for (ArrayList<Long> mesures : results.values())
			for (Long mesure : mesures)
				l += mesure;
		return l;
	}

	synchronized public int getSerieCount() {
		return results.size();
	}

	synchronized public String[] getSerieNames() {
		return results.keySet().toArray(new String[results.keySet().size()]);
	}

	
	
	public double toUnit(double v, TimeUnit unit) {
		double res = v;
		if (nano) {
			switch (unit) {
			case S: // seconds
				return res / 1E9;
			case MS: // miliseconds
				return res / 1E6;
			case NS: // nanoseconds
				return res;
			default : assert false : "Unkown unit";
			}
		} 
		else {
			// milisecond
			
			switch (unit) {
			case S: // seconds
				return res / 1000.0;
			case MS: // miliseconds
				return res;
			case NS: // nanoseconds
				return res * 1E6;
			default : assert false : "Unkown unit";
			}
		}
		return res;
	}
	
	synchronized public double getResult(String serieName, TimeUnit unit) {
		double l = 0;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName))
				if (mesure != null)
					l += mesure;
		} else {
			System.err.println(serieName);
		}
		return toUnit(l, unit);
	}
	
	synchronized public double getMean(String serieName, TimeUnit unit) {
		double l = 0;
		if (results.get(serieName) != null) {
			for (Long mesure : results.get(serieName))
				if (mesure != null)
					l += mesure;
		} else {
			System.err.println(serieName);
		}
		return toUnit(l, unit) / nbInstances(serieName);
	}

	synchronized public int nbInstances(String serieName) {
		if (results.get(serieName) != null) {
			return results.get(serieName).size();
		} else {
			System.err.println(serieName);
		}
		return -1;
	}
	
	synchronized public double getLast(String serieName, TimeUnit unit) {
		double l = 0;
		if (results.get(serieName) != null) {

			if (results.get(serieName).get(results.get(serieName).size()-1) != null)
				l += results.get(serieName).get(results.get(serieName).size()-1);
		} else {
			System.err.println(serieName);
		}
		return toUnit(l, unit);
	}

	/*synchronized public ArrayList<Long> getResultArray(String serieName) {
		return results.get(serieName);
	}*/

	synchronized public double getResult(TimeUnit unit) {
		double l = 0;
		for (ArrayList<Long> mesures : results.values())
			for (Long mesure : mesures)
				l += mesure;
		return toUnit(l, unit);
	}

	

}
