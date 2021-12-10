
import java.util.ArrayList;


public interface IExperience {

	public ACQ_Bias createBias();

	public ACQ_Learner createLearner();



	public ACQ_Network createTargetNetwork();

	public ACQ_ConstraintSolver createSolver();


	public ACQ_SelectionHeuristics getSelectionHeuristic();



	public Long getTimeout();

	public String getVrs();




	public void process();


	public boolean convergenceCheck(ACQ_Network target_network, ACQ_Network learned_network);

	public int convergenceRate(ACQ_ConstraintSolver solver, ACQ_Network target_network, ACQ_Network learned_network);
	
	public int ACQRate(ACQ_Network target_network, ACQ_Network learned_network);

	public String getName();

	public int getPropagation();
	public int getDeadline();


}
