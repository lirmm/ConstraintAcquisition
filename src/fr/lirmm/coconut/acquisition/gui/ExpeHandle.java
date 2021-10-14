package fr.lirmm.coconut.acquisition.gui;

import javax.swing.AbstractAction;

import fr.lirmm.coconut.acquisition.core.algorithms.ACQ_QUACQ;
import fr.lirmm.coconut.acquisition.core.tools.Chrono;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;
import fr.lirmm.coconut.acquisition.core.workspace.IExperience;

public abstract class ExpeHandle  {

	boolean with_ui = true;
	Chrono chrono = null;
	// PrintStream outputStream;
	DefaultExperience expe;
	ACQ_QUACQ acquisition;
	String learned_csspath;
	String bias_csspath;
	MyThread currentThread = null;
	boolean pause = false;
	boolean init = true;
	boolean layoutEnabled = true;
	boolean stepByStep = true;
	boolean ask_silently=false;
	PlateauTopComponent boardTopComponent;

	GraphStreamTopComponent graphStreamTopComponent;
	InfoAcqTopComponent infoTopComponent;
	AbstractAction resetAction = null;

	public ExpeHandle(DefaultExperience expe) {
		this(expe, null, null);
	}

	public ExpeHandle(DefaultExperience expe, String learned_csspath, String bias_csspath) {
		this.expe = expe;
		this.bias_csspath = bias_csspath;
		this.learned_csspath = learned_csspath;
	}

	abstract protected void prepareExpe(); 
	/**
	 * @return the boardTopComponent
	 */
	public PlateauTopComponent getBoardTopComponent() {
		return boardTopComponent;
	}

	/**
	 * @return the graphStreamTopComponent
	 */
	public GraphStreamTopComponent getGraphStreamTopComponent() {
		return graphStreamTopComponent;
	}

	/**
	 * @return the infoTopComponent
	 */
	public InfoAcqTopComponent getInfoTopComponent() {
		return infoTopComponent;
	}


	void variableSelected(int x, int y, boolean selected) {

	}

	class MyThread extends Thread {

		MyThread(Runnable runnable) {
			super(runnable);
		}

		synchronized void wake_up() {
			if (chrono != null) {
				chrono.start("total");
			}
			pause = false;
			notifyAll();
		}

		synchronized void dodo() {
			pause = true;
			if (chrono != null) {
				chrono.stop("total");
			}
			while (pause) {

				try {
					this.wait();

				} catch (InterruptedException ignore) {
					// log.debug("interrupted: " + ignore.getMessage());
				}
			}
		}

	}

	static boolean isAQueensExpe(IExperience expe) {
		return expe.getClass().getName().toLowerCase().contains("queens");
	}

	static boolean isAZebraExpe(IExperience expe) {
		return expe.getClass().getName().toLowerCase().contains("zebra");
	}

	public void reset() {
		if (currentThread != null) {
			currentThread.interrupt();
			currentThread = null;
		}
		pause = false;
		init = true;
		layoutEnabled = true;
		stepByStep = true;
		prepareExpe();

	}

	public void setResetAction(AbstractAction abstractAction) {
		resetAction = abstractAction;

	}
}
