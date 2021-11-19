package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.Layouts;
import org.graphstream.ui.swingViewer.GraphRenderer;
import org.graphstream.ui.view.Viewer;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

public class GSViewer extends JPanel implements PropertyChangeListener {

    static {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    }
    protected Viewer viewer;    
    protected Graph graph;
    protected int nb_variables = 0;
    protected int nb_constraints = 0;
    protected JLabel label_constraints = new JLabel("?");
    protected JLabel label_variables = new JLabel("?");
    protected boolean threadColoring;

    public GSViewer(String cssPath,boolean threadColoring) {
        super(new BorderLayout());
        this.threadColoring=threadColoring;
        graph = new MultiGraph("network");
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");
        attachStyle(graph, cssPath);
        viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        GraphRenderer renderer = Viewer.newGraphRenderer();
        this.add(viewer.addView(Viewer.DEFAULT_VIEW_ID, renderer, false), BorderLayout.CENTER);
        Layout layouter = Layouts.newLayoutAlgorithm();
        viewer.enableAutoLayout(layouter);
//        this.add(new GSLegend(cssPath,new String[]{"EqualXY","DifferentXY"}),BorderLayout.EAST);
/*        JPanel statusBar = new JPanel();
        statusBar.add(new JLabel("Variables: "));
        statusBar.add(label_variables);
        statusBar.add(new JLabel("  Constraints: "));
        statusBar.add(label_constraints);
        this.add(statusBar, BorderLayout.SOUTH);
*/    }
    public void springLayout(boolean run){
        if(run) viewer.enableAutoLayout();
        else viewer.disableAutoLayout();        
    }
    public void close() {
        viewer.close();
    }

    public static void attachStyle(Graph g, String cssPath) {
        String cssString = null;
        if (cssPath != null) {
            try {
                cssString = new BufferedReader(new FileReader(cssPath)).lines().collect(Collectors.joining("\n"));
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        if (cssString == null) {
            InputStream inputStream = GSViewer.class.getResourceAsStream("/fr/lirmm/coconut/acquisition/resource/style.css");
            cssString = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
        }
        g.addAttribute("ui.stylesheet", cssString);
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "INIT_GRAPH": {
                ACQ_Network network = (ACQ_Network) evt.getNewValue();
                for (int numvar : network.getVariables()) {
                    Node n = graph.addNode("V" + numvar);
                    n.setAttribute("ui.class", "variable");
                    nb_variables++;
                    label_variables.setText("" + nb_variables);
                }
                for (ACQ_IConstraint constraint : network.getConstraints()) {
                    if (addConstraint(constraint,null)) {
                        nb_constraints++;
                    }
                    for (int numvar : constraint.getVariables()) {
                        graph.addEdge(constraint.toString() + "-V" + numvar, constraint.toString(), "V" + numvar);
                    }
                    
                    label_constraints.setText("" + nb_constraints);
                }
                break;
            }
            case "ADD_CONSTRAINT": {
                ACQ_IConstraint constraint = (ACQ_IConstraint) evt.getNewValue();
                String threadName=(String) evt.getOldValue();
                if (addConstraint(constraint,threadName)) {
                    nb_constraints++;
                }
                label_constraints.setText("" + nb_constraints);
                for (int numvar : constraint.getVariables()) {
                    Edge edge = graph.addEdge(constraint.toString() + "-V" + numvar, constraint.toString(), "V" + numvar);
                    if(threadName!=null && threadColoring)
                    	edge.setAttribute("ui.class", threadName);
                    else
                    	edge.setAttribute("ui.class", constraint.getName());
                }
                break;
            }
            case "REMOVE_CONSTRAINT": {
                Node n = graph.getNode(evt.getOldValue().toString());
                label_constraints.setText("" + nb_constraints);
                if (n == null) {
                } else {
                    graph.removeNode(evt.getOldValue().toString());
                    nb_constraints--;
                }
                break;
            }
            case "ADD_VARIABLES": {
                ACQ_Scope variables = (ACQ_Scope) evt.getOldValue();
                ACQ_Scope scope = (ACQ_Scope) evt.getNewValue();
                ACQ_Scope newVariables = variables.diff(scope);
                for (int numvar : newVariables) {
                    Node n = graph.addNode("V" + numvar);
                    n.setAttribute("ui.class", "variable");
                    nb_variables++;
                    label_variables.setText("" + nb_variables);
                }
                break;
            }
        }
    }

    private boolean addConstraint(ACQ_IConstraint constraint,String threadName) {
        
        Node n = graph.getNode(constraint.toString());
        if (n == null) {
            n = graph.addNode(constraint.toString());
            if(threadName!=null && threadColoring){                        
            	n.setAttribute("ui.class", threadName);
            }
            else{
            	n.setAttribute("ui.class", constraint.getName());
            }
            return true;
        } else {
            return false;
        }
    }
    public void setVariablePosition(int numvar,int x, int y,int z){
        Node n = graph.getNode("V"+numvar);
        n.setAttribute("xyz", x,y,z);
    }
    public void selectVariable(int numvar, boolean selected, String message) {
        Node node = graph.getNode("V" + numvar);
        if (node != null) {
            if (selected) {
                node.addAttribute("ui.label", message);
                node.addAttribute("ui.selected",true);
//                    node.addAttribute("size", "50px,50px");
            } else {
                node.removeAttribute("ui.label");
                node.removeAttribute("ui.selected");
            }
            
        }
    }
}
