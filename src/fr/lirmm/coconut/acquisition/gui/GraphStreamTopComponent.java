package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

public class GraphStreamTopComponent extends TopComponent{
    static {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    }
    GSViewer learnedNetworkViewer, biasNetworkViewer;
 public GraphStreamTopComponent() {
     this(null, null, null);
 }

 public GraphStreamTopComponent(String expeName) {
     this(expeName, null, null);
 }

 public GraphStreamTopComponent(String expeName, GSViewer learnedNetworkViewer, GSViewer biasNetworkViewer) {
     this.learnedNetworkViewer=learnedNetworkViewer;
     this.biasNetworkViewer=biasNetworkViewer;
     setName("Learning experience: " + expeName);
//     setHtmlDisplayName("<html><i>A view to learned network</i>: <b>" + expeName + "</b></html>");
//     setToolTipText("Visualize graphs classification");
//     associateLookup(Lookups.fixed(this, expe));
     setLayout(new BorderLayout());

     JPanel viewer = new JPanel(new BorderLayout());
     JScrollPane lnvScrollPane = new JScrollPane(learnedNetworkViewer);
     if(biasNetworkViewer!=null){
     JScrollPane biasScrollPane = new JScrollPane(biasNetworkViewer);
//Create a split pane with the two scroll panes in it.
     JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
             biasScrollPane, lnvScrollPane);
     splitPane.setOneTouchExpandable(true);
     splitPane.setDividerLocation(150);

//Provide minimum sizes for the two components in the split pane
     Dimension minimumSize = new Dimension(100, 50);
     lnvScrollPane.setMinimumSize(minimumSize);
     biasScrollPane.setMinimumSize(minimumSize);
     viewer.add(splitPane, BorderLayout.CENTER);
     }
     else{
         viewer.add(lnvScrollPane,BorderLayout.CENTER);
     }
     add(viewer, BorderLayout.CENTER);
     
 }

}
