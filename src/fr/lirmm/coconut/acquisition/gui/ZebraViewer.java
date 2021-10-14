package fr.lirmm.coconut.acquisition.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import fr.lirmm.coconut.acquisition.core.learner.ACQ_Query;

public class ZebraViewer extends AskViewer {
	private final String[] properties = new String[] { "ukr", "norge", "eng", "spain", "jap",

			"red", "blue", "yellow", "green", "ivory",

			"oldGold", "parly", "kools", "lucky", "chest",

			"zebra", "dog", "horse", "fox", "snails",

			"coffee", "tea", "h2o", "milk", "oj"

	};
	ZebraHouse[] houses = new ZebraHouse[5];
	ZebraHouse categCol = new ZebraHouse(-1);

	ZebraViewer() {
		super(new GridLayout(1, 6));
		add(categCol);
		for (int i = 0; i < 5; i++) {
			houses[i] = new ZebraHouse(i + 1);
			add(houses[i]);
		}
	}

	private class ZebraHouse extends JPanel {
		Color defaultColor = UIManager.getColor("Panel.background");
		String[] categ = new String[] { "nat.", "color", "cig.", "pet", "drink" };
		JLabel[] labels = new JLabel[5];
		JPanel container;

		ZebraHouse(int num) {
			super(new BorderLayout());
			JLabel entete = new JLabel("", SwingConstants.CENTER);
			if (num >= 0)
				entete.setText("House" + num);
			else entete.setText("*");
			add(entete, BorderLayout.NORTH);
			this.container = new JPanel(new GridLayout(5, 1, 5, 20));
			for (int i = 0; i < 5; i++) {

				if (num < 0) {
					labels[i] = new ResizeFontLabel("");
					labels[i].setText(categ[i]);
				} else
					labels[i] = new JLabel("", SwingConstants.CENTER);
				container.add(labels[i]);
			}
			add(container, BorderLayout.CENTER);
		}

		void clear() {
			setBackground(defaultColor);
			container.setBackground(defaultColor);
			for (JLabel label : labels) {
				label.setForeground(Color.darkGray);
				label.setText("-");
			}
		}

		void set(int rank, String value) {
			labels[rank].setText(value);
			Color color = defaultColor;
			if (rank == 1) {
				switch (value) {
				case "red":
					color = Color.red;
					break;
				case "blue":
					color = Color.blue;
					;
					break;
				case "yellow":
					color = Color.yellow;
					break;
				case "green":
					color = Color.green;
					break;
				case "ivory":
					color = Color.white;
					break;
				}
				container.setBackground(color);
				Color foregroundColor;
				if (color == Color.red || color == Color.blue)
					foregroundColor = Color.white;
				else
					foregroundColor = Color.black;
				for (JLabel label : labels)
					label.setForeground(foregroundColor);
			}
		}

	}

	@Override
	public void display(ACQ_Query query) {
		// clear houses
		for (ZebraHouse house : houses)
			house.clear();
		for (int x = 0; x < 25; x++) {
			int val = query.getValue(x);
			if (val > 0) {
				houses[val - 1].set(x / 5, properties[x]);
			}
		}
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
}
