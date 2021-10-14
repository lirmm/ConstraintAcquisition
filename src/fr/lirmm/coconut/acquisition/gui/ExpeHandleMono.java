package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Constraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ChocoSolver;
import fr.lirmm.coconut.acquisition.core.acqsolver.ACQ_ConstraintSolver;
import fr.lirmm.coconut.acquisition.core.algorithms.ACQ_QUACQ;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Bias;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;
import fr.lirmm.coconut.acquisition.core.learner.ObservedLearner;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;

public class ExpeHandleMono extends ExpeHandle implements PropertyChangeListener{

	public ExpeHandleMono(DefaultExperience expe) {
		this(expe, null, null);
	}

	public ExpeHandleMono(DefaultExperience expe, String learned_csspath, String bias_csspath) {
		super(expe,learned_csspath,bias_csspath);
		prepareExpe();
	}

	protected void prepareExpe() {
		// initialize the problem
		final ACQ_Bias bias = expe.createBias();
		ObservedLearner observedLearner = new ObservedLearner(expe.createLearner());
		final ACQ_ConstraintSolver solver = expe.createSolver();
		solver.setVars(bias.getVars());
		acquisition = new ACQ_QUACQ(solver, bias, observedLearner, expe.getHeuristic());
		// Param
		acquisition.setNormalizedCSP(expe.isNormalizedCSP());
		acquisition.setShuffleSplit(expe.isShuffleSplit());
		acquisition.setAllDiffDetection(expe.isAllDiffDetection());
		ACQ_Network learnedNetwork = acquisition.getLearnedNetwork();
		final GSViewer learnedNetworkViewer = new GSViewer(learned_csspath,true);
		final GSViewer biasNetworkViewer = new GSViewer(bias_csspath,false);
		learnedNetwork.addPropertyChangeListener(learnedNetworkViewer);
		bias.getNetwork().addPropertyChangeListener(biasNetworkViewer);
		// the graphs view
		graphStreamTopComponent = new GraphStreamTopComponent(expe.getClass().getName(), learnedNetworkViewer,
				biasNetworkViewer);
		// the board view with toolbar
		AskViewer plateau = null;
		if (expe.getDimension() > 0) {
			// the square board view
			if (isAQueensExpe(expe)) {
				plateau = new PlateauQueens(expe) {

					@Override
					public void selection(int x, int y, boolean selected) {
						String varLabel = "" + x;
						learnedNetworkViewer.selectVariable(x, selected, varLabel);
						if (biasNetworkViewer != null) {
							biasNetworkViewer.selectVariable(x, selected, varLabel);
						}
					}
				};
			} else {
				plateau = new PlateauClassic(expe) {

					@Override
					public void selection(int x, int y, boolean selected) {
						String varLabel = "[" + (y + 1) + ":" + (x + 1) + "]";
						learnedNetworkViewer.selectVariable(y * expe.getDimension() + x, selected, varLabel);
						if (biasNetworkViewer != null) {
							biasNetworkViewer.selectVariable(y * expe.getDimension() + x, selected, varLabel);
						}
					}
				};
			}
		} else if (isAZebraExpe(expe)) {
			plateau = new ZebraViewer();
		}
		// toolbar
		JToolBar toolbar = new JToolBar();
		// Actions
		toolbar.add(new AbstractAction("Start", GUI_Utils.getImageIcon("pause-play-button.png")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (init) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							stepByStep = true;
							init = false;
							if (acquisition.process()) {
								System.out.println("YES");
								currentThread = null;
								// printResult();
							} else {
								System.out.println("NO");
							}
						}
					};
					currentThread = new MyThread(runnable);
					currentThread.start();
				} else if (currentThread != null) {
					if (pause) {
						currentThread.wake_up();
					} else {
						currentThread.dodo();
					}
				}
			}
		});
		toolbar.add(new AbstractAction("Forward", GUI_Utils.getImageIcon("forward-button.png")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (init) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							stepByStep = false;
							init = false;
							if (acquisition.process()) {
								System.out.println("YES");
								currentThread = null;
								currentThread = null;
							} else {
								System.out.println("NO");
							}
						}
					};
					currentThread = new MyThread(runnable);
					currentThread.start();
				} else if (currentThread != null) {
					stepByStep = false;
					currentThread.wake_up();
				}
			}
		});
		toolbar.add(new AbstractAction("Stop", GUI_Utils.getImageIcon("stop-button.png")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentThread != null) {
					if (pause) {
					} else {
						currentThread.interrupt();
						// currentThread.dodo();
					}

				}
			}
		});
		if (expe.getDimension() > 0) {
			toolbar.add(new AbstractAction("Grid layout", GUI_Utils.getImageIcon("grid.png")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					layoutEnabled = false;
					if (biasNetworkViewer != null) {
						biasNetworkViewer.springLayout(layoutEnabled);
					}
					learnedNetworkViewer.springLayout(layoutEnabled);
					for (int x = 0; x < expe.getDimension(); x++) {
						for (int y = 0; y < expe.getDimension(); y++) {
							if (biasNetworkViewer != null) {
								biasNetworkViewer.setVariablePosition(y * expe.getDimension() + x, x * 100, -y * 100,
										0);
							}
							learnedNetworkViewer.setVariablePosition(y * expe.getDimension() + x, x * 100, -y * 100, 0);
						}
					}
				}
			});
		}
		toolbar.add(new AbstractAction("Spring layout", GUI_Utils.getImageIcon("share.png")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				layoutEnabled = true;
				if (biasNetworkViewer != null) {
					biasNetworkViewer.springLayout(layoutEnabled);
				}
				learnedNetworkViewer.springLayout(layoutEnabled);
			}
		});
		toolbar.add(new AbstractAction("Reset", GUI_Utils.getImageIcon("reset.png")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (resetAction != null)
					resetAction.actionPerformed(null);
			}
		});
		toolbar.add(new AbstractAction("Solve", GUI_Utils.getImageIcon("gear.png")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (solver instanceof ACQ_ChocoSolver) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							Model model = new Model("solve");
							ACQ_ChocoSolver chocoSolver = (ACQ_ChocoSolver) expe.createSolver();
							chocoSolver.setVars(bias.getVars());
							IntVar[] chocoVars = chocoSolver.buildModel(model, learnedNetwork, false);
							Solver solver2 = model.getSolver();
							if (solver2.solve()) {
								if (solver2.getSolutionCount() != 0) {
									// network_B.remove(cst);
									int[] tuple = new int[chocoVars.length];
									for (int i = 0; i < chocoVars.length; i++) {
										tuple[i] = chocoVars[i].getValue();
									}
									ACQ_Query query = new ACQ_Query(learnedNetwork.getVariables(), tuple);
									ask_silently=true;
									observedLearner.ask(query);
									ask_silently=false;
								}
							}
						}
					};
					new Thread(runnable).start();
				} else
					System.out.println("coucou");
			}
		});

		// the board window
		boardTopComponent = new PlateauTopComponent(plateau, toolbar);
		observedLearner.addPropertyChangeListener(boardTopComponent);
		observedLearner.addPropertyChangeListener(this);
		// the info viewer
		InfoViewer infoViewer1 = new InfoViewer("Learned network",false);
		learnedNetwork.addPropertyChangeListener(infoViewer1);
		InfoViewer infoViewer2 = new InfoViewer("Bias network",true);
		bias.getNetwork().addPropertyChangeListener(infoViewer2);
		// results viewer
		JPanel resultPanel = new JPanel(new BorderLayout());
		resultPanel.setBorder(BorderFactory.createTitledBorder("Learned constraints"));
		final DefaultListModel<String> model = new DefaultListModel<>();
		JList<String> constraintJList = new JList<>(model);
		JScrollPane scrollPane = new JScrollPane(constraintJList);
		constraintJList.setVisibleRowCount(12);
		resultPanel.add(scrollPane, BorderLayout.CENTER);
		learnedNetwork.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				switch (evt.getPropertyName()) {
				case "ADD_CONSTRAINT":
					StringBuffer bf = new StringBuffer();
					ACQ_Constraint cst = (ACQ_Constraint) evt.getNewValue();
					bf.append(cst.getName());
					boolean init = true;
					for (int numvar : cst.getVariables()) {
						if (init) {
							bf.append("(");
						} else {
							bf.append(",");
						}
						init = false;
						if (expe.getDimension() > 0 && !isAQueensExpe(expe)) {
							int x = (numvar / expe.getDimension()) + 1;
							int y = (numvar % expe.getDimension()) + 1;
							bf.append("[" + x + ":" + y + "]");
						} else {
							bf.append("" + numvar);
						}
					}
					bf.append(")" + "\n");
					model.addElement(bf.toString());
				}
			}
		});
		// queries viewer
		final JLabel label_partial_positive = new JLabel("", SwingConstants.RIGHT);
		label_partial_positive.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel label_partial_negative = new JLabel("", SwingConstants.RIGHT);
		label_partial_negative.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel label_complete_positive = new JLabel("", SwingConstants.RIGHT);
		label_complete_positive.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel label_complete_negative = new JLabel("", SwingConstants.RIGHT);
		label_complete_negative.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel total_partial = new JLabel("", SwingConstants.RIGHT);
		total_partial.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel total_complete = new JLabel("", SwingConstants.RIGHT);
		total_complete.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel total_negative = new JLabel("", SwingConstants.RIGHT);
		total_negative.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel total_positive = new JLabel("", SwingConstants.RIGHT);
		total_positive.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		final JLabel total = new JLabel("", SwingConstants.RIGHT);
		total.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

		JPanel queryPanel = new JPanel(new GridLayout(4, 4, 2, 2));
		queryPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		queryPanel.add(new JLabel(""));
		queryPanel.add(new JLabel("positive", SwingConstants.CENTER));
		queryPanel.add(new JLabel("negative", SwingConstants.CENTER));
		queryPanel.add(new JLabel("total", SwingConstants.CENTER));
		queryPanel.add(new JLabel("partial", SwingConstants.RIGHT));
		queryPanel.add(label_partial_positive);
		queryPanel.add(label_partial_negative);
		queryPanel.add(total_partial);

		queryPanel.add(new JLabel("complete", SwingConstants.RIGHT));
		queryPanel.add(label_complete_positive);
		queryPanel.add(label_complete_negative);
		queryPanel.add(total_complete);
		queryPanel.add(new JLabel("total", SwingConstants.RIGHT));
		queryPanel.add(total_positive);
		queryPanel.add(total_negative);
		queryPanel.add(total);
		queryPanel.setBorder(BorderFactory.createTitledBorder("Queries"));
		observedLearner.addPropertyChangeListener(new PropertyChangeListener() {
			int complete_positive = 0;
			int complete_negative = 0;
			int partial_positive = 0;
			int partial_negative = 0;

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (ask_silently==false && "ASK".equals(evt.getPropertyName())) {
					Boolean ret = (Boolean) evt.getOldValue();
					ACQ_Query query = (ACQ_Query) evt.getNewValue();
					boolean complete = query.getScope().size() == bias.getNetwork().getVariables().size();
					if (complete) {
						if ((ret)) {
							complete_positive++;
						} else {
							complete_negative++;
						}
					} else if (ret) {
						partial_positive++;
					} else {
						partial_negative++;
					}
					label_partial_negative.setText("" + partial_negative);
					label_partial_positive.setText("" + partial_positive);
					label_complete_negative.setText("" + complete_negative);
					label_complete_positive.setText("" + complete_positive);
					total_complete.setText("" + (complete_positive + complete_negative));
					total_partial.setText("" + (partial_positive + partial_negative));
					total_positive.setText("" + (partial_positive + complete_positive));
					total_negative.setText("" + (partial_negative + complete_negative));
					total.setText("" + (partial_negative + complete_negative + partial_positive + complete_positive));
				}
			}
		});

		JPanel timePanel = new JPanel(new GridLayout(1, 2));
		timePanel.add(new JLabel("total time:", SwingConstants.RIGHT));
		final JLabel timeLabel = new JLabel("", SwingConstants.LEFT);
		observedLearner.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (chrono == null) {
					chrono = new Chrono("mon_chrono");
					chrono.start("total");
				}
				if ("ASK".equals(evt.getPropertyName())) {
					chrono.stop("total");
					long time = chrono.getResult();
					timeLabel.setText("" + time + " ms");
					chrono.start("total");
				}
			}
		});
		timePanel.add(timeLabel);

		infoTopComponent = new InfoAcqTopComponent("Learned network infos", infoViewer1, infoViewer2, resultPanel,
				queryPanel, timePanel);

	}
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		switch (evt.getPropertyName()) {
		case "ASK":
			if (currentThread != null && stepByStep) {
				currentThread.dodo();
			}
		case "ASKED_QUERY":
		case "MEMORY_UP":
		}
	}
}
