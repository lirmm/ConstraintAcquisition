package fr.lirmm.coconut.acquisition.core.acqsolver;


public class ACQ_Domain implements ACQ_IDomain{

	private static final int MIN = 0;		// min index
	private static final int MAX = 1;		// max index

	private int min, max;					// min/max of all variables
	private int[][] intervals;				// min/max of each variable
	private boolean uniform_domains;  		// same domain for each variable




	public ACQ_Domain(int nbvars) {

		super();

		this.intervals = new int[nbvars][2];

		this.uniform_domains = false;
	}

	public ACQ_Domain(int min, int max) {

		super();

		this.min = min;
		
		this.max = max;

		this.uniform_domains = true;
	}

	@Override
	public int getMin(int numvar) {

		if(uniform_domains)

			return min;

		else

			return intervals[numvar][MIN];
	}

	@Override
	public int getMax(int numvar) {

		if(uniform_domains)

			return max;

		else

			return intervals[numvar][MAX];
	}

	public void setDomain(int numvar, int min, int max) {

		uniform_domains=false;

		intervals[numvar][MIN]= min;

		intervals[numvar][MAX]= max;

	}


	public void setMin(int min) {

		uniform_domains=true;

		this.min=min;

	}

	public void setMax(int max) {

		uniform_domains=true;

		this.max=max;

	}

}
