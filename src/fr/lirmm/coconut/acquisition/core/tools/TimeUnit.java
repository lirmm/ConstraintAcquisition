package fr.lirmm.coconut.acquisition.core.tools;

public enum TimeUnit {
	S  ("s"), 
	MS ("ms"), 
	NS ("ns");
	
	String name;
	
	TimeUnit(String name) {
		this.name = name;
	}
	
	public String toString() {
		return this.name;
	}
	
}
