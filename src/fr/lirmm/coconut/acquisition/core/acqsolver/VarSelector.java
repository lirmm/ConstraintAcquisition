package fr.lirmm.coconut.acquisition.core.acqsolver;

public enum VarSelector {

	
	ActivityBased, 			//Implementation of the search described in: "Activity-Based Search for Black-Box Constraint Propagramming Solver", Laurent Michel and Pascal Van Hentenryck, CPAIOR12.
	AntiFirstFail, 			//Anti first fail variable selector.
	Cyclic,					//A cyclic variable selector : Iterates over variables according to lexicographic ordering in a cyclic manner (loop back to the first variable)
	DomOverWDeg, 			//Implementation of DowOverWDeg[1]
	FirstFail, 				//First fail variable selector.
	GeneralizedMinDomVar, 	//First fail variable selector generalized to all variables.
	ImpactBased,			//Implementation of the search described in: "Impact-Based Search Strategies for Constraint Programming", Philippe Refalo, CP2004.
	InputOrder,				//Input order variable selector.
	Largest,		 		//Largest variable selector.
	MaxDelta, 				//Selects the variables maximising envelopeSize-kernelSize.
	MaxRegret, 				//Max regret variable selector.
	MinDelta, 				//Selects the variables minimising envelopeSize-kernelSize (quite similar to minDomain, or first-fail)
	Occurrence,				//Occurrence variable selector.
	Random,					//Random variable selector.
	RandomVar,				//Random variable selector & evaluator to be used with fast restart strategy
	Smallest, 				//Smallest variable selector.
	BiasDeg, 				//bias dominating variable selector.

}
