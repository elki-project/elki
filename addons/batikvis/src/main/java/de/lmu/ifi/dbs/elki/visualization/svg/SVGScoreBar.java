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

import java.text.NumberFormat;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

/**
 * Draw a score bar. Essentially like a progress bar, left-to-right, displaying
 * a relative score.
 * 
 * @author Sascha Goldhofer
 * @since 0.5.0
 */
// TODO: refactor to get a progress bar?
public class SVGScoreBar {
  /**
   * Value, minimum and maximum values
   */
  protected double val, min = 0., max = 1.;

  /**
   * Reversed flag.
   */
  protected boolean reversed = false;

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
   * @param val Value
   * @param min Minimum value
   * @param max Maximum value
   */
  public void setFill(double val, double min, double max) {
    this.val = val;
    this.min = min;
    this.max = max;
  }

  /**
   * Set the reversed flag.
   * 
   * @param reversed Reversed flag.
   */
  public void setReversed(boolean reversed) {
    this.reversed = reversed;
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

    if(val >= min && val <= max && min < max) {
      final double frame = 0.02 * height;
      double fpos = (val - min) / (max - min) * (width - 2 * frame);
      Element chart;
      if(reversed) {
        chart = svgp.svgRect(x + frame + fpos, y + frame, width - fpos - 2 * frame, height - 2 * frame);
      }
      else {
        chart = svgp.svgRect(x + frame, y + frame, fpos, height - 2 * frame);
      }
      chart.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, "#d4e4f1");
      chart.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, "#a0a0a0");
      chart.setAttribute(SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, String.valueOf(height * 0.01));
      barchart.appendChild(chart);
    }

    // Draw the values:
    if(format != null) {
      String num = Double.isNaN(val) ? "NaN" : format.format(val);
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