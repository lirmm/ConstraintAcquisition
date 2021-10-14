package fr.lirmm.coconut.acquisition.core.acqsolver;

public enum ValSelector {

	IntDomainBest, 			//Value selector for optimization problems: Branches on the value with the best objective bound (evaluated each possible assignment)
	IntDomainImpact, 		//Value selector for any type of problems: Branches on the value with the best/worst impact on domains cardinality (evaluated each possible assignment)
	IntDomainLast, 			//Value selector for optimization problems: Branches on the value in the last solution, if still in domain
	IntDomainMax, 			//Selects the variable upper bound
	IntDomainMedian, 		//Selects the median value in the variable domain.
	IntDomainMiddle, 		//Selects the value in the variable domain closest to the mean of its current bounds.
	IntDomainMin, 			//Selects the variable lower bound
	IntDomainRandom, 		//Selects randomly a value in the variable domain.
	IntDomainRandomBound, 	//Selects randomly between the lower and the upper bound of the variable
	RealDomainMax, 			//Selects the upper bound of a real variable
	RealDomainMiddle, 		//Selects a real value at the middle between the lower and the upper bound of the variable
	RealDomainMin, 			//Selects the lower bound of a real variable
	SetDomainMin, 			//Selects the first integer in the envelope and not in the kernel
}
