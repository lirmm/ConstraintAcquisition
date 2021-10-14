package fr.lirmm.coconut.acquisition.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * The Class VerticalFlowLayout.
 */
public class VerticalFlowLayout extends FlowLayout implements
		java.io.Serializable {

	/** The Constant TOP. */
	public static final int TOP = 0;

	/** The Constant MIDDLE. */
	public static final int MIDDLE = 1;

	/** The Constant BOTTOM. */
	public static final int BOTTOM = 2;

	/** The hgap. */
	int hgap;

	/** The vgap. */
	int vgap;

	/** The hfill. */
	boolean hfill;

	/** The vfill. */
	boolean vfill;

	/**
	 * Instantiates a new vertical flow layout.
	 */
	public VerticalFlowLayout() {
		this(TOP, 5, 5, true, false);
	}

	/**
	 * Instantiates a new vertical flow layout.
	 * 
	 * @param hfill the hfill
	 * @param vfill the vfill
	 */
	public VerticalFlowLayout(boolean hfill, boolean vfill) {
		this(TOP, 5, 5, hfill, vfill);
	}

	/**
	 * Instantiates a new vertical flow layout.
	 * 
	 * @param align the align
	 */
	public VerticalFlowLayout(int align) {
		this(align, 5, 5, true, false);
	}

	/**
	 * Instantiates a new vertical flow layout.
	 * 
	 * @param align the align
	 * @param hfill the hfill
	 * @param vfill the vfill
	 */
	public VerticalFlowLayout(int align, boolean hfill, boolean vfill) {
		this(align, 5, 5, hfill, vfill);
	}

	/**
	 * Instantiates a new vertical flow layout.
	 * 
	 * @param align the align
	 * @param hgap the hgap
	 * @param vgap the vgap
	 * @param hfill the hfill
	 * @param vfill the vfill
	 */
	public VerticalFlowLayout(int align, int hgap, int vgap, boolean hfill,
			boolean vfill) {
		setAlignment(align);
		this.hgap = hgap;
		this.vgap = vgap;
		this.hfill = hfill;
		this.vfill = vfill;
	}

	/* (non-Javadoc)
	 * @see java.awt.FlowLayout#getHgap()
	 */
	public int getHgap() {
		return hgap;
	}

	/* (non-Javadoc)
	 * @see java.awt.FlowLayout#setHgap(int)
	 */
	public void setHgap(int hgap) {
		super.setHgap(hgap);
		this.hgap = hgap;
	}

	/* (non-Javadoc)
	 * @see java.awt.FlowLayout#getVgap()
	 */
	public int getVgap() {
		return vgap;
	}

	/* (non-Javadoc)
	 * @see java.awt.FlowLayout#setVgap(int)
	 */
	public void setVgap(int vgap) {
		super.setVgap(vgap);
		this.vgap = vgap;
	}

	/* (non-Javadoc)
	 * @see java.awt.FlowLayout#preferredLayoutSize(java.awt.Container)
	 */
	public Dimension preferredLayoutSize(Container target) {
		Dimension tarsiz = new Dimension(0, 0);

		for (int i = 0; i < target.getComponentCount(); i++) {
			Component m = target.getComponent(i);
			if (m.isVisible()) {
				Dimension d = m.getPreferredSize();
				tarsiz.width = Math.max(tarsiz.width, d.width);
				if (i > 0) {
					tarsiz.height += vgap;
				}
				tarsiz.height += d.height;
			}
		}
		Insets insets = target.getInsets();
		tarsiz.width += insets.left + insets.right + hgap * 2;
		tarsiz.height += insets.top + insets.bottom + vgap * 2;
		return tarsiz;
	}

	/* (non-Javadoc)
	 * @see java.awt.FlowLayout#minimumLayoutSize(java.awt.Container)
	 */
	public Dimension minimumLayoutSize(Container target) {
		Dimension tarsiz = new Dimension(0, 0);

		for (int i = 0; i < target.getComponentCount(); i++) {
			Component m = target.getComponent(i);
			if (m.isVisible()) {
				Dimension d = m.getMinimumSize();
				tarsiz.width = Math.max(tarsiz.width, d.width);
				if (i > 0) {
					tarsiz.height += vgap;
				}
				tarsiz.height += d.height;
			}
		}
		Insets insets = target.getInsets();
		tarsiz.width += insets.left + insets.right + hgap * 2;
		tarsiz.height += insets.top + insets.bottom + vgap * 2;
		return tarsiz;
	}

	/**
	 * Sets the vertical fill.
	 * 
	 * @param vfill the new vertical fill
	 */
	public void setVerticalFill(boolean vfill) {
		this.vfill = vfill;
	}

	/**
	 * Gets the vertical fill.
	 * 
	 * @return the vertical fill
	 */
	public boolean getVerticalFill() {
		return vfill;
	}

	/**
	 * Sets the horizontal fill.
	 * 
	 * @param hfill the new horizontal fill
	 */
	public void setHorizontalFill(boolean hfill) {
		this.hfill = hfill;
	}

	/**
	 * Gets the horizontal fill.
	 * 
	 * @return the horizontal fill
	 */
	public boolean getHorizontalFill() {
		return hfill;
	}

	/**
	 * Placethem.
	 * 
	 * @param target the target
	 * @param x the x
	 * @param y the y
	 * @param width the width
	 * @param height the height
	 * @param first the first
	 * @param last the last
	 */
	private void placethem(Container target, int x, int y, int width,
			int height, int first, int last) {
		int align = getAlignment();
		// if ( align == this.TOP )
		// y = 0;
		Insets insets = target.getInsets();
		if (align == this.MIDDLE)
			y += height / 2;
		if (align == this.BOTTOM)
			y += height;

		for (int i = first; i < last; i++) {
			Component m = target.getComponent(i);
			Dimension md = m.getSize();
			if (m.isVisible()) {
				int px = x + (width - md.width) / 2;
				m.setLocation(px, y);
				y += vgap + md.height;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.FlowLayout#layoutContainer(java.awt.Container)
	 */
	public void layoutContainer(Container target) {
		Insets insets = target.getInsets();
		int maxheight = target.getSize().height
				- (insets.top + insets.bottom + vgap * 2);
		int maxwidth = target.getSize().width
				- (insets.left + insets.right + hgap * 2);
		int numcomp = target.getComponentCount();
		int x = insets.left + hgap;
		int y = 0;
		int colw = 0, start = 0;

		for (int i = 0; i < numcomp; i++) {
			Component m = target.getComponent(i);
			if (m.isVisible()) {
				Dimension d = m.getPreferredSize();
				// fit last component to remaining height
				if ((this.vfill) && (i == (numcomp - 1))) {
					d.height = Math.max((maxheight - y),
							m.getPreferredSize().height);
				}

				// fit componenent size to container width
				if (this.hfill) {
					m.setSize(maxwidth, d.height);
					d.width = maxwidth;
				} else {
					m.setSize(d.width, d.height);
				}

				if (y + d.height > maxheight) {
					placethem(target, x, insets.top + vgap, colw,
							maxheight - y, start, i);
					y = d.height;
					x += hgap + colw;
					colw = d.width;
					start = i;
				} else {
					if (y > 0)
						y += vgap;
					y += d.height;
					colw = Math.max(colw, d.width);
				}
			}
		}
		placethem(target, x, insets.top + vgap, colw, maxheight - y, start,
				numcomp);
	}
}
