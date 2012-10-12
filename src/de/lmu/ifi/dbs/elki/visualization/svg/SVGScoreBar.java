package de.lmu.ifi.dbs.elki.visualization.svg;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.text.NumberFormat;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Draw a score bar. Essentially like a progress bar, left-to-right, displaying
 * a relative score.
 * 
 * @author Sascha Goldhofer
 */
// TODO: refactor to get a progress bar?
public class SVGScoreBar {
  /**
   * Fill value
   */
  protected double fill = 0.0;

  /**
   * Total size
   */
  protected double size = 1.0;

  /**
   * Label (on the right)
   */
  protected String label = null;

  /**
   * Number format, set to print the actual score
   */
  private NumberFormat format = null;

  /**
   * Constructor.
   */
  public SVGScoreBar() {
    // Nothing to do here.
  }

  /**
   * Set the fill of the score bar.
   * 
   * @param fill Fill value
   * @param size Total size
   */
  public void setFill(double fill, double size) {
    this.fill = fill;
    this.size = size;
  }

  /**
   * Set label (right of the bar)
   * 
   * @param text Label text
   */
  public void addLabel(String text) {
    this.label = text;
  }

  /**
   * To show score values, set a number format
   * 
   * @param format Number format
   */
  public void showValues(NumberFormat format) {
    this.format = format;
  }

  /**
   * Build the actual element
   * 
   * @param svgp Plot to draw to
   * @param x X coordinate
   * @param y Y coordinate
   * @param width Width
   * @param height Height
   * @return new element
   */
  public Element build(SVGPlot svgp, double x, double y, double width, double height) {
    Element barchart = svgp.svgElement(SVGConstants.SVG_G_TAG);

    // TODO: use style library for colors!
    Element bar = svgp.svgRect(x, y, width, height);
    bar.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#a0a0a0");
    bar.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
    bar.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, String.valueOf(height * 0.01));
    barchart.appendChild(bar);

    if(fill >= 0 && fill <= size + 1) {
      double fpos = (fill / size) * (width - (0.04 * height));
      Element chart = svgp.svgRect(x + 0.02 * height, y + 0.02 * height, fpos, height - 0.04 * height);
      chart.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
      chart.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
      chart.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, String.valueOf(height * 0.01));
      barchart.appendChild(chart);
    }

    // Draw the values:
    if(format != null) {
      String num = Double.isNaN(fill) ? "NaN" : FormatUtil.format(fill, format);
      Element lbl = svgp.svgText(x + 0.05 * width, y + 0.75 * height, num);
      lbl.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: " + 0.75 * height + "; font-weight: bold");
      barchart.appendChild(lbl);
    }

    // Draw the label
    if(label != null) {
      Element lbl = svgp.svgText(x + 1.05 * width, y + 0.75 * height, label);
      lbl.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: " + 0.75 * height + "; font-weight: normal");
      barchart.appendChild(lbl);
    }
    return barchart;
  }
}