package fr.lirmm.coconut.acquisition.core.acqconstraint;

public class ACQ_TemporalVariable {

	int start;
	int end;
	int startvalue;
	int endvalue;
	public ACQ_TemporalVariable(int start,int end) {
		this.start=start;
		this.end=end;

	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	
	public int getStartValue() {
		return startvalue;
	}
	public void setStartValue(int start) {
		this.startvalue = start;
	}
	public int getEndValue() {
		return endvalue;
	}
	public void setEndValue(int end) {
		this.endvalue = end;
	}
	@Override
	public String toString() {
		return "ACQ_TemporalVariable [start=" + start + ", end=" + end + "]=[ "+startvalue+" , "+endvalue+" ]";
	}
	
}
