/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import net.jafama.FastMath;

/**
 * Class to draw a simple axis with tick marks on the plot.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @composed - - - Alignment
 * @composed - - - LabelStyle
 * @assoc - - - CSSClass
 * @assoc - - - CSSClassManager
 * @assoc - - - LinearScale
 * @assoc - - - StyleLibrary
 * @navassoc - create - Element
 */
public final class SVGSimpleLinearAxis {
  /**
   * Private constructor. Static methods only.
   */
  private SVGSimpleLinearAxis() {
    // Do not use.
  }

  /**
   * Flag for axis label position. First char: right-hand or left-hand side of
   * line. Second char: text alignment
   */
  private enum Alignment {
    LL, RL, LC, RC, LR, RR
  }

  /**
   * Labeling style: left-handed, right-handed, no ticks, labels at ends
   */
  public enum LabelStyle {
    LEFTHAND, RIGHTHAND, NOLABELS, NOTHING, ENDLABEL
  }

  /**
   * CSS class name for the axes
   */
  private static final String CSS_AXIS = "axis";

  /**
   * CSS class name for the axes
   */
  private static final String CSS_AXIS_TICK = "axis-tick";

  /**
   * CSS class name for the axes
   */
  private static final String CSS_AXIS_LABEL = "axis-label";

  /**
   * Register CSS classes with a {@link CSSClassManager}
   * 
   * @param owner Owner of the CSS classes
   * @param manager Manager to register the classes with
   * @throws CSSNamingConflict when a name clash occurs
   */
  private static void setupCSSClasses(Object owner, CSSClassManager manager, StyleLibrary style) throws CSSNamingConflict {
    if(!manager.contains(CSS_AXIS)) {
      CSSClass axis = new CSSClass(owner, CSS_AXIS);
      axis.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.AXIS));
      axis.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.AXIS));
      manager.addClass(axis);
    }
    if(!manager.contains(CSS_AXIS_TICK)) {
      CSSClass tick = new CSSClass(owner, CSS_AXIS_TICK);
      tick.setStatement(SVGConstants.CSS_STROKE_PROPERTY, style.getColor(StyleLibrary.AXIS_TICK));
      tick.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, style.getLineWidth(StyleLibrary.AXIS_TICK));
      manager.addClass(tick);
    }
    if(!manager.contains(CSS_AXIS_LABEL)) {
      CSSClass label = new CSSClass(owner, CSS_AXIS_LABEL);
      label.setStatement(SVGConstants.CSS_FILL_PROPERTY, style.getTextColor(StyleLibrary.AXIS_LABEL));
      label.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, style.getFontFamily(StyleLibrary.AXIS_LABEL));
      label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, style.getTextSize(StyleLibrary.AXIS_LABEL));
      manager.addClass(label);
    }
  }

  /**
   * Plot an axis with appropriate scales
   * 
   * @param plot Plot object
   * @param parent Containing element
   * @param scale axis scale information
   * @param x1 starting coordinate
   * @param y1 starting coordinate
   * @param x2 ending coordinate
   * @param y2 ending coordinate
   * @param labelstyle Style for placing the labels
   * @param style Style library
   * @throws CSSNamingConflict when a conflict occurs in CSS
   */
  public static void drawAxis(SVGPlot plot, Element parent, LinearScale scale, double x1, double y1, double x2, double y2, LabelStyle labelstyle, StyleLibrary style) throws CSSNamingConflict {
    assert (parent != null);
    Element line = plot.svgLine(x1, y1, x2, y2);
    SVGUtil.setCSSClass(line, CSS_AXIS);
    parent.appendChild(line);

    final double tx = x2 - x1;
    final double ty = y2 - y1;
    // ticks are orthogonal
    final double tw = ty * 0.01;
    final double th = -tx * 0.01;

    // choose where to print labels.
    final boolean labels, ticks;
    switch(labelstyle){
    case LEFTHAND:
    case RIGHTHAND:
      labels = true;
      ticks = true;
      break;
    case NOLABELS:
      labels = false;
      ticks = true;
      break;
    case ENDLABEL: // end labels are handle specially
    case NOTHING:
    default:
      labels = false;
      ticks = false;
    }
    Alignment pos = Alignment.LL;
    if(labels) {
      double angle = FastMath.atan2(ty, tx);
      // System.err.println(tx + " " + (-ty) + " " + angle);
      if(angle > 2.6) { // pi .. 2.6 = 180 .. 150
        pos = labelstyle == LabelStyle.RIGHTHAND ? Alignment.RC : Alignment.LC;
      }
      else if(angle > 0.5) { // 2.3 .. 0.7 = 130 .. 40
        pos = labelstyle == LabelStyle.RIGHTHAND ? Alignment.RR : Alignment.LL;
      }
      else if(angle > -0.5) { // 0.5 .. -0.5 = 30 .. -30
        pos = labelstyle == LabelStyle.RIGHTHAND ? Alignment.RC : Alignment.LC;
      }
      else if(angle > -2.6) { // -0.5 .. -2.6 = -30 .. -150
        pos = labelstyle == LabelStyle.RIGHTHAND ? Alignment.RL : Alignment.LR;
      }
      else { // -2.6 .. -pi = -150 .. -180
        pos = labelstyle == LabelStyle.RIGHTHAND ? Alignment.RC : Alignment.LC;
      }
    }
    // vertical text offset; align approximately with middle instead of
    // baseline.
    double textvoff = style.getTextSize(StyleLibrary.AXIS_LABEL) * .35;

    // draw ticks on x axis
    if(ticks || labels) {
      int sw = 1;
      { // Compute how many ticks to draw
        int numticks = (int) ((scale.getMax() - scale.getMin()) / scale.getRes());
        double tlen = FastMath.sqrt(tx * tx + ty * ty);
        double minl = 10 * style.getLineWidth(StyleLibrary.AXIS_TICK);
        // Try proper divisors first.
        if(sw * tlen / numticks < minl) {
          for(int i = 2; i <= (numticks >> 1); i++) {
            if(numticks % i == 0 && i * tlen / numticks >= minl) {
              sw = i;
              break;
            }
          }
        }
        // Otherwise, also allow non-divisors.
        if(sw * tlen / numticks < minl) {
          sw = (int) Math.floor(minl * numticks / tlen);
        }
      }
      for(double tick = scale.getMin(); tick <= scale.getMax() + scale.getRes() / 10; tick += sw * scale.getRes()) {
        double x = x1 + tx * scale.getScaled(tick);
        double y = y1 + ty * scale.getScaled(tick);
        if(ticks) {
          // This is correct. Vectors: (vec - tvec) to (vec + tvec)
          Element tickline = plot.svgLine(x - tw, y - th, x + tw, y + th);
          SVGUtil.setAtt(tickline, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_AXIS_TICK);
          parent.appendChild(tickline);
        }
        // draw labels
        if(labels) {
          double tex = x;
          double tey = y;
          switch(pos){
          case LL:
          case LC:
          case LR:
            tex = x + tw * 2.5;
            tey = y + th * 2.5 + textvoff;
            break;
          case RL:
          case RC:
          case RR:
            tex = x - tw * 2.5;
            tey = y - th * 2.5 + textvoff;
          }
          Element text = plot.svgText(tex, tey, scale.formatValue(tick));
          text.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_AXIS_LABEL);
          switch(pos){
          case LL:
          case RL:
            text.setAttribute(SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_START_VALUE);
            break;
          case LC:
          case RC:
            text.setAttribute(SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
            break;
          case LR:
          case RR:
            text.setAttribute(SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_END_VALUE);
            break;
          }
          parent.appendChild(text);
        }
      }
    }
    if(labelstyle == LabelStyle.ENDLABEL) {
      {
        Element text = plot.svgText(x1 - tx * 0.02, y1 - ty * 0.02 + textvoff, scale.formatValue(scale.getMin()));
        text.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_AXIS_LABEL);
        text.setAttribute(SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
        parent.appendChild(text);
      }
      {
        Element text = plot.svgText(x2 + tx * 0.02, y2 + ty * 0.02 + textvoff, scale.formatValue(scale.getMax()));
        text.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_AXIS_LABEL);
        text.setAttribute(SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
        parent.appendChild(text);
      }
    }
    setupCSSClasses(plot, plot.getCSSClassManager(), style);
  }
}