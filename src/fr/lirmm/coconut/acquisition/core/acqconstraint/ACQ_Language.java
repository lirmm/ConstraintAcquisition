package fr.lirmm.coconut.acquisition.core.acqconstraint;

public enum ACQ_Language {

	Arith(new String[] { "EqualXY", "DifferentXY", "GreaterXY", "LessXY", "GreaterEqualXY", "LessEqualXY" }),
	PArith(new String[] { "EqualXY", "DifferentXY","PEqualXY", "PDiffXY", "PGreaterXY", "PLessEqualXY","POverlapXY","PNotOverlapXY","PDuringXY","PNotDuringXY","PExactXY","PNotExactXY","PFinishXY","PNotFinishXY" }),

	ArithDist(new String[] { "EqualXY", "DifferentXY", "GreaterXY", "LessXY", "GreaterEqualXY", "LessEqualXY",
			"DistDiffXYZ", "DistEqualXYZ", "DistGreaterXYZ", "DistLessEqualXYZ", "DistLessXYZ", "DistGreaterEqualXYZ",
			"DistDiffXYZT", "DistEqualXYZT", "DistGreaterXYZT", "DistLessEqualXYZT", "DistLessXYZT",
			"DistGreaterEqualXYZT" }),

	ArithDiag(new String[] { "EqualXY", "DifferentXY", "GreaterXY", "LessXY", "GreaterEqualXY", "LessEqualXY",
			"OutDiag1", "InDiag1", "OutDiag2", "InDiag2" }),;

	final String[] gamma;

	ACQ_Language(String[] relations) {
		this.gamma = relations;
	}

	public String[] getRelations() {
		return gamma;
	}
}
