package fr.lirmm.coconut.acquisition.core.learner;

public enum Query_type {
MQ,			// membership query
findc1,		// partial query sol(L_Y and partial_not Delta_Y) (IJCAI13
findc2,		// AIJ20
findscope,	// partial query
XPQ,		//Extended Partial Queries
matchmaker,	// matchmaker agent
EQ,			//equivalence query
UAMQ, 		// Unspecified Attribute Values-MQ
UAEQ,		// unspecified Attribute Values-EQ
SubQ, 		// subset query
SupQ,		// superset query
DisQ,		// Disjointness queries
ExhQ,		// Exhaustiveness queries
//NL: Friedrich's arguments?
}
