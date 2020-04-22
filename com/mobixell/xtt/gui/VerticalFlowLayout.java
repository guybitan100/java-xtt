package com.mobixell.xtt.gui;

import java.lang.*;
import java.awt.*;
/*

  $Header: /cvs/CorePlatform/Repository/xtt/com/mobixell/xtt/gui/VerticalFlowLayout.java,v 1.2 2006/07/21 17:04:31 cvsbuild Exp $

  Organisation:         Berner Fachhochschule / University of applied sciences
                        Biel School of Engineering and Architecture
                        CH-2501 Biel
                        Switzerland

  Copyright:            GNU General Public License

  Project:              SceneBuilder J3D

  Date:                 $Date: 2006/07/21 17:04:31 $

*/

/**
 * This class is similar to FlowLayout, except, its not horizontal, its vertical.
 * This is a nice Helperclass to provide a good lucking Layout in a Panel, where you don't know exactly the Number of components.
 *
 * @version $Revision: 1.2 $
 * @author $Author: cvsbuild $
 */
public class VerticalFlowLayout extends FlowLayout implements java.io.Serializable {

    public static final String tantau_sccsid = "@(#)$Id: VerticalFlowLayout.java,v 1.2 2006/07/21 17:04:31 cvsbuild Exp $";
  private int horizGap;
  private int vertGap;
  private boolean horizFill;
  private boolean vertFill;

  public static final int TOP   = 0;
  public static final int MIDDLE = 1;
  public static final int BOTTOM = 2;

  /**
   * Constructs a new VerticalFlowLayout with a middle alignment, and
   * the fill to edge flag set.
   * See The Documentation of FlowLAyout for Information
   */
  public VerticalFlowLayout() {
    this(TOP, 5, 5, true, false);
  }

  /**
   * Constructs a new VerticalFlowLayout with a middle alignment.
   * See The Documentation of FlowLAyout for Information
   */
  public VerticalFlowLayout(boolean horizFill, boolean vertFill){
    this(TOP, 5, 5, horizFill, vertFill);
  }

  /**
   * Constructs a new VerticalFlowLayout with a middle alignment.
   * See The Documentation of FlowLAyout for Information
   */
  public VerticalFlowLayout(int align) {
    this(align, 5, 5, true, false);
  }

  /**
   * Constructs a new VerticalFlowLayout.
   * See The Documentation of FlowLAyout for Information
   */
  public VerticalFlowLayout(int align, boolean horizFill, boolean vertFill) {
    this(align, 5, 5, horizFill, vertFill);
  }

  /**
   * Constructs a new VerticalFlowLayout.
   * See The Documentation of FlowLAyout for Information
   */
  public VerticalFlowLayout(int align, int horizGap, int vertGap, boolean horizFill, boolean vertFill) {
    setAlignment(align);
    this.horizGap = horizGap;
    this.vertGap = vertGap;
    this.horizFill = horizFill;
    this.vertFill = vertFill;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public int getVgap() {
    return vertGap;
  }
  /**
   * Gets the horizontal gap between components.
   */
  public int getHgap() {
    return horizGap;
  }
  /**
   * See The Documentation of FlowLAyout for Information
   */
  public void setHgap(int horizGap) {
    super.setHgap(horizGap);
    this.horizGap = horizGap;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public void setVgap(int vertGap) {
    super.setVgap(vertGap);
    this.vertGap = vertGap;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public boolean getHorizontalFill() {
    return horizFill;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public boolean getVerticalFill() {
    return vertFill;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public void setVerticalFill(boolean vertFill) {
    this.vertFill = vertFill;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public void setHorizontalFill(boolean horizFill) {
    this.horizFill = horizFill;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public Dimension minimumLayoutSize(Container target) {
    Dimension tarsiz = new Dimension(0, 0);

    for (int i = 0 ; i < target.getComponentCount(); i++) {
      Component m = target.getComponent(i);
      if (m.isVisible()) {
          Dimension d = m.getMinimumSize();
          tarsiz.width = Math.max(tarsiz.width, d.width);
          if (i > 0) {
            tarsiz.height += vertGap;
          }
          tarsiz.height += d.height;
      }
    }
    Insets insets = target.getInsets();
    tarsiz.width += insets.left + insets.right + horizGap*2;
    tarsiz.height += insets.top + insets.bottom + vertGap*2;
    return tarsiz;
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public Dimension preferredLayoutSize(Container target) {
    Dimension tarsiz = new Dimension(0, 0);

    for (int i = 0 ; i < target.getComponentCount(); i++) {
      Component m = target.getComponent(i);
      //if (m.isVisible()) {
        Dimension d = m.getPreferredSize();
        tarsiz.width = Math.max(tarsiz.width, d.width);
        if (i > 0) {
          tarsiz.height += vertGap;
        }
        tarsiz.height += d.height;
      //}
    }
    Insets insets = target.getInsets();
    tarsiz.width += insets.left + insets.right + horizGap*2;
    tarsiz.height += insets.top + insets.bottom + vertGap*2;
    return tarsiz;
  }

  private void placethem(Container target, int x, int y, int width, int height,
                                       int first, int last) {
    int align = getAlignment();
    Insets insets = target.getInsets();
    if ( align == this.MIDDLE )
      y += height  / 2;
    if ( align == this.BOTTOM )
      y += height;

    for (int i = first ; i < last ; i++) {
      Component m = target.getComponent(i);
        Dimension md = m.getSize();
      //if (m.isVisible()) {
        int px = x + (width-md.width)/2;
        m.setLocation(px, y);
        y += vertGap + md.height;
      //}
    }
  }

  /**
   * See The Documentation of FlowLAyout for Information
   */
  public void layoutContainer(Container target) {
    Insets insets = target.getInsets();
    int maxheight = target.getSize().height - (insets.top + insets.bottom + vertGap*2);
    int maxwidth = target.getSize().width - (insets.left + insets.right + horizGap*2);
    int numcomp = target.getComponentCount();
    int x = insets.left + horizGap;
    int y = 0  ;
    int colw = 0, start = 0;

    for (int i = 0 ; i < numcomp ; i++) {
      Component m = target.getComponent(i);
      if (m.isVisible()) {
        Dimension d = m.getPreferredSize();
        // fit last component to remaining height
        if ((this.vertFill) && (i == (numcomp-1))) {
          d.height = Math.max((maxheight - y), m.getPreferredSize().height);
        }

        // fit componenent size to container width
        if ( this.horizFill ) {
          m.setSize(maxwidth, d.height);
          d.width = maxwidth;
        }
        else {
          m.setSize(d.width, d.height);
        }

        if ( y  + d.height > maxheight ) {
          placethem(target, x, insets.top + vertGap, colw, maxheight-y, start, i);
          y = d.height;
          x += horizGap + colw;
          colw = d.width;
          start = i;
        }
        else {
          if ( y > 0 )
            y += vertGap;
           y += d.height;
           colw = Math.max(colw, d.width);
        }
      }
    }
    placethem(target, x, insets.top + vertGap, colw, maxheight - y, start, numcomp);
  }
}
