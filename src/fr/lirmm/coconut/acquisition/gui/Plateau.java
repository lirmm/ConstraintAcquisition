package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public abstract class Plateau extends AskViewer implements MouseListener {

    private static final Font FONT = new Font("Verdana",
            Font.CENTER_BASELINE,
            20);

    private final ResizeFontLabel[][] grid;
    private final Map<ResizeFontLabel, Point> mapFieldToCoordinates
            = new HashMap<>();

    private final int dimension;

    public Plateau(int dimension) {
        this.grid = new ResizeFontLabel[dimension][dimension];
        this.dimension = dimension;

        for (int y = 0; y < dimension; ++y) {
            for (int x = 0; x < dimension; ++x) {
                ResizeFontLabel field = new ResizeFontLabel(" ");
                field.addMouseListener(this);
                field.setHorizontalAlignment(SwingConstants.CENTER);
//                field.addKeyListener(new SudokuCellKeyListener(this));
                mapFieldToCoordinates.put(field, new Point(x, y));
                grid[y][x] = field;
            }
        }
        Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
        Dimension fieldDimension = new Dimension(30, 30);

        for (int y = 0; y < dimension; ++y) {
            for (int x = 0; x < dimension; ++x) {
                ResizeFontLabel field = grid[y][x];
                field.setBorder(border);
                field.setFont(FONT);
                field.setPreferredSize(fieldDimension);
            }
        }
        int minisquareDimension = (int) Math.sqrt(dimension);
        if (minisquareDimension * minisquareDimension == dimension) {
            setLayout(new GridLayout(minisquareDimension,
                    minisquareDimension));

            JPanel[][] minisquarePanels = new JPanel[minisquareDimension][minisquareDimension];

            Border minisquareBorder = BorderFactory.createLineBorder(Color.BLACK, 1);

            for (int y = 0; y < minisquareDimension; ++y) {
                for (int x = 0; x < minisquareDimension; ++x) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridLayout(minisquareDimension,
                            minisquareDimension));
                    panel.setBorder(minisquareBorder);
                    minisquarePanels[y][x] = panel;
                    add(panel);
                }
            }

            for (int y = 0; y < dimension; ++y) {
                for (int x = 0; x < dimension; ++x) {
                    int minisquareX = x / minisquareDimension;
                    int minisquareY = y / minisquareDimension;

                    minisquarePanels[minisquareY][minisquareX].add(grid[y][x]);
                }
            }
        } else {
            JPanel panel = new JPanel();
            setLayout(new BorderLayout());
            panel.setLayout(new GridLayout(dimension,dimension));
            add(panel,BorderLayout.CENTER);
            for (int y = 0; y < dimension; ++y) {
                for (int x = 0; x < dimension; ++x) {

                    panel.add(grid[y][x]);
                }
            }
        }
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    }

    public int getDimension() {
        return dimension;
    }

    public void set(int x, int y, String value) {
        grid[x][y].setText(value);
    }

    public void set(int x, int y, Icon icon) {
    	grid[x][y].setText("");
        grid[x][y].setIcon(icon);
    }

    private void setBorder(ResizeFontLabel field, boolean selected) {
        Border border;
        if (selected) {
            border = BorderFactory.createLineBorder(Color.RED, 2);
        } else {
            border = BorderFactory.createLineBorder(Color.BLACK, 1);
        }
        field.setBorder(border);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        ResizeFontLabel field = (ResizeFontLabel) e.getComponent();
        setBorder(field, true);
        Point p = mapFieldToCoordinates.get(field);
        selection(p.x, p.y, true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        ResizeFontLabel field = (ResizeFontLabel) e.getComponent();
        setBorder(field, false);
        Point p = mapFieldToCoordinates.get(field);
        selection(p.x, p.y, false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {
        ResizeFontLabel field = (ResizeFontLabel) e.getComponent();
        setBorder(field, false);
        Point p = mapFieldToCoordinates.get(field);
        selection(p.x, p.y, false);
    }


    @Override
    public Dimension getPreferredSize() {
        // Relies on being the only component
        // in a layout that will center it without
        // expanding it to fill all the space.
        Dimension d = this.getParent().getSize();
        int newSize = d.width > d.height ? d.height : d.width;
        newSize = newSize == 0 ? 100 : newSize;
        return new Dimension(newSize, newSize);
    }

    public abstract void selection(int x, int y, boolean selected);
}
