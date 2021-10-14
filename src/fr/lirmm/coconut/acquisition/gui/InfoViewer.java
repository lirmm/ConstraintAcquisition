package fr.lirmm.coconut.acquisition.gui;

import static javax.swing.SwingConstants.LEFT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_IConstraint;
import fr.lirmm.coconut.acquisition.core.acqconstraint.ACQ_Network;
import fr.lirmm.coconut.acquisition.core.learner.ACQ_Scope;

public class InfoViewer extends JPanel implements PropertyChangeListener{
		
    private HashMap<String,MyKeyValuePanel> constraintPanels=new HashMap<>();
    private MyKeyValuePanel varPanel;
    private MyKeyValuePanel totalPanel=null;
    private JPanel mainPanel;
    public InfoViewer(String title,boolean displayTotal){
        super(new BorderLayout());
        mainPanel = new JPanel(new VerticalFlowLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder(title));
        add(mainPanel,BorderLayout.CENTER);
        varPanel=new MyKeyValuePanel("Variables",-1, true, Color.blue, null);
        mainPanel.add(varPanel);
        if(displayTotal){
        totalPanel=new MyKeyValuePanel("Constraints",0, true, Color.red, null);
        mainPanel.add(totalPanel);
        }
    }
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "INIT_GRAPH": {
                ACQ_Network network = (ACQ_Network) evt.getNewValue();
                varPanel.setValue(network.getVariables().size());
                 for (ACQ_IConstraint constraint : network.getConstraints()) {
                    addConstraint(constraint);
                    if(totalPanel!=null) totalPanel.incr();
                }
                break;
            }
            case "ADD_CONSTRAINT": {
                ACQ_IConstraint constraint = (ACQ_IConstraint) evt.getNewValue();
                addConstraint(constraint);
                 if(totalPanel!=null) totalPanel.incr();;
                break;
            }
            case "REMOVE_CONSTRAINT": {
                ACQ_IConstraint constraint = (ACQ_IConstraint)evt.getOldValue();
                MyKeyValuePanel p=constraintPanels.get(constraint.getName());
                if(p!=null) p.decr();
                 if(totalPanel!=null) totalPanel.decr();
                break;
            }
             case "ADD_VARIABLES": {
                ACQ_Scope variables = (ACQ_Scope) evt.getOldValue();
                ACQ_Scope scope = (ACQ_Scope) evt.getNewValue();
                ACQ_Scope newVariables = variables.diff(scope);
                for (int numvar : newVariables) {
                    varPanel.incr();
                }
                break;
            }
        }
    }
    private void addConstraint(ACQ_IConstraint constraint) {
                MyKeyValuePanel p=constraintPanels.get(constraint.getName());
                if(p==null)
                {
                    p=new MyKeyValuePanel(constraint.getName(),1,false,Color.red,null);
                    constraintPanels.put(constraint.getName(),p);
                    mainPanel.add(p);
                }
                else p.incr();
    }
    private class MyKeyValuePanel extends JPanel{
        private JLabel valueLabel=null;
        private int counter;
        public MyKeyValuePanel(String textKey,int value){
            this(textKey,value,false,Color.BLUE,null);
        }
        public MyKeyValuePanel(String textKey,JButton button){
            super(new FlowLayout(FlowLayout.LEFT, 0, 1));
            JLabel keyLabel=new JLabel(textKey,JLabel.RIGHT);
            keyLabel.setPreferredSize(new Dimension(120,12));
            add(keyLabel);
            add(new JLabel("     "));
            add(button);
            setBackground(Color.WHITE);
        }
        private String getTextValue(int value){
            return value<0?"-":(""+value);
        }
        public MyKeyValuePanel(String textKey,int value,boolean big,Color color, Icon icon) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 1));
            counter=value;
            JLabel keyLabel=new JLabel(textKey,JLabel.RIGHT);
            keyLabel.setPreferredSize(new Dimension(120,12));
            add(keyLabel);
            add(new JLabel("     "));
            valueLabel=new JLabel(getTextValue(value),icon,JLabel.LEFT);
            valueLabel.setFont(new Font("SansSerif", big ? Font.BOLD : Font.PLAIN, big ? 14 : 12));
            valueLabel.setForeground(color);
            valueLabel.setHorizontalTextPosition(LEFT);
            add(valueLabel);
            if(!big) setBackground(Color.WHITE);
        }
        void setTextValue(String txt){
        }
        void incr(){
            setValue(++counter);
        }
        void decr(){
            setValue(--counter);
        }
        void setValue(int value){
            counter=value;
            if(valueLabel!=null)
                valueLabel.setText(getTextValue(value<0?0:value));
        }
    }
    
}
