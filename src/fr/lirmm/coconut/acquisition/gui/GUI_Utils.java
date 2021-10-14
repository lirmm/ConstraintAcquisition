package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Algorithm;
import fr.lirmm.coconut.acquisition.core.workspace.DefaultExperience;

public class GUI_Utils {

	public static void executeExperience(DefaultExperience expe) {
		executeExperience(expe, null, null);
	}

	public static void executeExperience(DefaultExperience expe, String learned_cssPath, String bias_cssPath) {
		ExpeHandle expH = new ExpeHandleMono(expe, learned_cssPath, // learned_cssPath
				bias_cssPath // bias_cssPath
		);
		show1Frame(expH,expe.getClass().getName());
//		show2Frames(expH,expe.getClass().getName());
	}

	public static void executeExperience(String className) {
		executeExperience(className, null, null);
	}
	public static void executeExperience(String className, String learned_cssPath, String bias_cssPath) {
		try {
			DefaultExperience expe = (DefaultExperience) Class.forName(className).newInstance();
			executeExperience(expe, learned_cssPath, // learned_cssPath
					bias_cssPath // bias_cssPath
			);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static void executeExperience(ACQ_Algorithm mode,DefaultExperience expe,int nbThread) {
		executeExperience(mode,expe, nbThread, null, null);
	}
	public static void executeExperience(ACQ_Algorithm mode,DefaultExperience expe,int nbThread, String learned_cssPath, String bias_cssPath) {
		switch(mode){
		case PACQ: 
			executeCoop(expe, nbThread,learned_cssPath,bias_cssPath);
			break;
		default: 
			executePortFolio(expe, nbThread,learned_cssPath,bias_cssPath);							
		}
	}
	
	public static void executeCoop(DefaultExperience expe,int nbThread) {
		executeCoop(expe, nbThread, null, null);
	}

	public static void executeCoop(DefaultExperience expe,int nbThread, String learned_cssPath, String bias_cssPath) {
		ExpeHandleParallel expH = new ExpeHandleParallel(ACQ_Algorithm.PACQ,expe,nbThread, learned_cssPath, // learned_cssPath
				bias_cssPath // bias_cssPath
		);
		show1Frame(expH,expe.getClass().getName());
//		show2Frames(expH,expe.getClass().getName());
	}

	public static void executeCoop(String className,int nbThread) {
		executeCoop(className, nbThread, null, null);
	}

	public static void executeCoop(String className,int nbThread, String learned_cssPath, String bias_cssPath) {
		try {
			DefaultExperience expe = (DefaultExperience) Class.forName(className).newInstance();
			executeCoop(expe, nbThread,learned_cssPath, // learned_cssPath
					bias_cssPath // bias_cssPath
			);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static void executePortFolio(DefaultExperience expe,int nbThread) {
		executePortFolio(expe, nbThread, null, null);
	}

	public static void executePortFolio(DefaultExperience expe, int nbThread, String learned_cssPath, String bias_cssPath) {
		ExpeHandleParallel expH = new ExpeHandleParallel(ACQ_Algorithm.PACQ,expe, nbThread, learned_cssPath, // learned_cssPath
				bias_cssPath // bias_cssPath
		);
		show1Frame(expH,expe.getClass().getName());
//		show2Frames(expH,expe.getClass().getName());
	}

	public static void executePortFolio(String className,int nbThread) {
		executePortFolio(className, nbThread, null, null);
	}

	public static void executePortFolio(String className, int nbThread, String learned_cssPath, String bias_cssPath) {
		try {
			DefaultExperience expe = (DefaultExperience) Class.forName(className).newInstance();
			executePortFolio(expe, nbThread, learned_cssPath, // learned_cssPath
					bias_cssPath // bias_cssPath
			);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	private static JComponent create2FramePanel1(ExpeHandle expH){
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(expH.getGraphStreamTopComponent(), BorderLayout.CENTER);
		mainPanel.add(expH.getInfoTopComponent(), BorderLayout.EAST);
		return mainPanel;
	}
	private static JComponent create2FramePanel2(ExpeHandle expH){
		return expH.getBoardTopComponent();
	}
	private static void show2Frames(ExpeHandle expH,String title) {
		JFrame frame1 = new JFrame();
		frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		CardLayout cardLayout1=new CardLayout();
		JPanel cardPanel1=new JPanel(cardLayout1);
		JComponent p=create2FramePanel1(expH);
		cardPanel1.add(p,"init");
		frame1.getContentPane().add(cardPanel1);
		frame1.setSize(800, 600);
		frame1.setLocationRelativeTo(null);
		frame1.setVisible(true);
		JFrame frame2 = new JFrame(title);
		frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		CardLayout cardLayout2=new CardLayout();
		JPanel cardPanel2=new JPanel(cardLayout2);
		JComponent p2=create2FramePanel2(expH);
		cardPanel2.add(p2,"init");
		frame2.getContentPane().add(cardPanel2);
		frame2.setSize(400, 450);
		frame2.setVisible(true);
		expH.setResetAction(new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				expH.reset();
				JComponent p=create2FramePanel1(expH);
				cardPanel1.add(p,"init");
				cardLayout1.show(cardPanel1, "init");
				JComponent p2=create2FramePanel2(expH);
				cardPanel2.add(p2,"init");
				cardLayout2.show(cardPanel2, "init");
			}
		});
	}
	private static JComponent create1FramePanel(ExpeHandle expH){
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(expH.getGraphStreamTopComponent(), BorderLayout.CENTER);
		mainPanel.add(expH.getInfoTopComponent(), BorderLayout.EAST);
		//Create a split pane with the two scroll panes in it.
		JPanel leftPanel=new JPanel(new GridLayout(2,1));
		leftPanel.add(expH.getBoardTopComponent());
		leftPanel.add(new Logo());
	     JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	    		leftPanel , mainPanel);
	     splitPane.setOneTouchExpandable(true);
	     splitPane.setDividerLocation(250);

	//Provide minimum sizes for the two components in the split pane
	     Dimension minimumSize = new Dimension(200, 100);
	     mainPanel.setMinimumSize(minimumSize);
	     expH.getBoardTopComponent().setMinimumSize(minimumSize);
//	     leftPanel.setMinimumSize(minimumSize);
		return splitPane;
	}
	private static void show1Frame(ExpeHandle expH,String title) {
		JFrame frame1 = new JFrame();
		frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		CardLayout cardLayout=new CardLayout();
		JPanel cardPanel=new JPanel(cardLayout);
		JComponent p=create1FramePanel(expH);
		frame1.getContentPane().add(cardPanel);
		cardPanel.add(p,"init");
		expH.setResetAction(new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				expH.reset();
				JComponent p=create1FramePanel(expH);
				cardPanel.add(p,"init2");
				cardLayout.show(cardPanel, "init2");
			}
		});
		frame1.setSize(800, 600);
		frame1.setLocationRelativeTo(null);
		frame1.setVisible(true);
	}
	public final static class Logo extends Component {

	    private Image image;

	    public Logo() {
	        this.image = getImageIcon("quacq.png").getImage();	
	    }

	    @Override
	    public final void paint(final Graphics g) {
	        super.paint(g);
	        drawScaledImage(image, this, g);
//	        g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
	    }

	    public final Image getImage() {
	        return (image);
	    }

	    public final void setImage(final Image image) {
	        this.image = image;
	    }
	}
	public static ImageIcon getImageIcon(String key) {

		try {
			return new ImageIcon(ExpeHandle.class.getResource("/fr/lirmm/coconut/quacq/resource/" + key));
		} catch (NullPointerException e) {
			// image not found
			System.err.println("can't find image " + key);
		}

		return null;
	}
    public static void drawScaledImage(Image image, Component canvas, Graphics g) {
        int imgWidth = image.getWidth(null);
        int imgHeight = image.getHeight(null);
         
        double imgAspect = (double) imgHeight / imgWidth;
 
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
         
        double canvasAspect = (double) canvasHeight / canvasWidth;
 
        int x1 = 0; // top left X position
        int y1 = 0; // top left Y position
        int x2 = 0; // bottom right X position
        int y2 = 0; // bottom right Y position
         
        if (imgWidth < canvasWidth && imgHeight < canvasHeight) {
            // the image is smaller than the canvas
            x1 = (canvasWidth - imgWidth)  / 2;
            y1 = (canvasHeight - imgHeight) / 2;
            x2 = imgWidth + x1;
            y2 = imgHeight + y1;
             
        } else {
            if (canvasAspect > imgAspect) {
                y1 = canvasHeight;
                // keep image aspect ratio
                canvasHeight = (int) (canvasWidth * imgAspect);
                y1 = (y1 - canvasHeight) / 2;
            } else {
                x1 = canvasWidth;
                // keep image aspect ratio
                canvasWidth = (int) (canvasHeight / imgAspect);
                x1 = (x1 - canvasWidth) / 2;
            }
            x2 = canvasWidth + x1;
            y2 = canvasHeight + y1;
        }
 
        g.drawImage(image, x1, y1, x2, y2, 0, 0, imgWidth, imgHeight, null);
    }

}
