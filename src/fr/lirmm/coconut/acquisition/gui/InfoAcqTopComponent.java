package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class InfoAcqTopComponent extends TopComponent{
	
    private JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	
    public InfoAcqTopComponent(String title, InfoViewer iviewer1, InfoViewer iviewer2,JPanel resultPane,JPanel queriesPanel,JPanel timePanel) {
        setName(title);
        setLayout(new BorderLayout());
        add(pane);
        JPanel p = new JPanel(new VerticalFlowLayout());
        p.add(timePanel);
        p.add(queriesPanel);
        p.add(iviewer1);
        p.add(iviewer2);
        p.add(resultPane);
        pane.setViewportView(p);
    }

}
