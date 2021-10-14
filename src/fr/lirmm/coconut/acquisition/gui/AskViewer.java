package fr.lirmm.coconut.acquisition.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

public abstract class AskViewer extends JPanel{
   public AskViewer(){
       this(new FlowLayout());
   }
   public AskViewer(LayoutManager layout){
       super(layout);
   }
   public abstract void display(ACQ_Query query);
   
   protected class ResizeFontLabel extends JLabel {

       public static final int MIN_FONT_SIZE = 3;
       public static final int MAX_FONT_SIZE = 240;
       Graphics g;
       int currFontSize = 0;

       public ResizeFontLabel(String text) {
           super(text);
           currFontSize = this.getFont().getSize();
           init();
       }

       protected void init() {
           addComponentListener(new ComponentAdapter() {
               public void componentResized(ComponentEvent e) {
                   adaptLabelFont();
               }
           });
       }

       protected void adaptLabelFont() {
           if (g == null) {
               return;
           }
           currFontSize = this.getFont().getSize();

           Rectangle r = getBounds();
           r.x = 0;
           r.y = 0;
           int fontSize = Math.max(MIN_FONT_SIZE, currFontSize);
           Font f = getFont();

           Rectangle r1 = new Rectangle(getTextSize(getFont()));
           while (!r.contains(r1)) {
               fontSize--;
               if (fontSize <= MIN_FONT_SIZE) {
                   break;
               }
               r1 = new Rectangle(getTextSize(f.deriveFont(f.getStyle(), fontSize)));
           }

           Rectangle r2 = new Rectangle();
           while (fontSize < MAX_FONT_SIZE) {
               r2.setSize(getTextSize(f.deriveFont(f.getStyle(), fontSize + 1)));
               if (!r.contains(r2)) {
                   break;
               }
               fontSize++;
           }

           setFont(f.deriveFont(f.getStyle(), fontSize));
           repaint();
       }

       private Dimension getTextSize(Font f) {
           Dimension size = new Dimension();
           //g.setFont(f);   // superfluous.
           FontMetrics fm = g.getFontMetrics(f);
           size.width = fm.stringWidth(getText());
           size.height = fm.getHeight();
           return size;
       }

       protected void paintComponent(Graphics g) {
           super.paintComponent(g);
           this.g = g;
       }
   }
}
