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
package de.lmu.ifi.dbs.elki.visualization.style.marker;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Marker library achieving a larger number of styles by combining different
 * shapes with different colors. Uses object ID management by SVGPlot.
 * 
 * TODO: Add more styles
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @composed - - - ColorLibrary
 */
public class PrettyMarkers implements MarkerLibrary {
  /**
   * Color library
   */
  private ColorLibrary colors;

  /**
   * Default prefix to use.
   */
  private static final String DEFAULT_PREFIX = "s";

  /**
   * Prefix for the IDs generated.
   */
  private String prefix;

  /**
   * Color of "uncolored" dots
   */
  private String dotcolor;

  /**
   * Color of "greyed out" dots
   */
  private String greycolor;

  /**
   * Constructor
   *
   * @param prefix prefix to use.
   * @param style style library to use
   */
  public PrettyMarkers(String prefix, StyleLibrary style) {
    this.prefix = prefix;
    this.colors = style.getColorSet(StyleLibrary.MARKERPLOT);
    this.dotcolor = style.getColor(StyleLibrary.MARKERPLOT);
    this.greycolor = style.getColor(StyleLibrary.PLOTGREY);
  }

  /**
   * Constructor without prefix argument, will use {@link #DEFAULT_PREFIX} as
   * prefix.
   *
   * @param style Style library to use
   */
  public PrettyMarkers(StyleLibrary style) {
    this(DEFAULT_PREFIX, style);
  }

  /**
   * Draw an marker used in scatter plots. If you intend to use the markers
   * multiple times, you should consider using the {@link #useMarker} method
   * instead, which exploits the SVG features of symbol definition and use
   *
   * @param plot containing plot
   * @param parent parent node
   * @param x position
   * @param y position
   * @param style marker style (enumerated)
   * @param size size
   */
  public void plotMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    assert (parent != null);
    if(style == -1) {
      plotUncolored(plot, parent, x, y, size);
      return;
    }
    if(style == -2) {
      plotGray(plot, parent, x, y, size);
      return;
    }
    // No dot allowed!
    String cssid = prefix + style + "_" + (int) (100 * size);

    switch(style & 0x7){
    case 0: {
      // + cross
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        plot.addCSSClassOrLogError(c);
      }
      Element line1 = plot.svgLine(x, y - size / 2.2, x, y + size / 2.2);
      SVGUtil.setCSSClass(line1, cssid);
      parent.appendChild(line1);
      Element line2 = plot.svgLine(x - size / 2.2, y, x + size / 2.2, y);
      SVGUtil.setCSSClass(line2, cssid);
      parent.appendChild(line2);
      break;
    }
    case 1: {
      // X cross
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        plot.addCSSClassOrLogError(c);
      }
      Element line1 = plot.svgLine(x - size / 2.828427, y - size / 2.828427, x + size / 2.828427, y + size / 2.828427);
      SVGUtil.setCSSClass(line1, cssid);
      parent.appendChild(line1);
      Element line2 = plot.svgLine(x - size / 2.828427, y + size / 2.828427, x + size / 2.828427, y - size / 2.828427);
      SVGUtil.setCSSClass(line2, cssid);
      parent.appendChild(line2);
      break;
    }
    case 2: {
      // O hollow circle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        plot.addCSSClassOrLogError(c);
      }
      Element circ = plot.svgCircle(x, y, size / 2.2);
      SVGUtil.setCSSClass(circ, cssid);
      parent.appendChild(circ);
      break;
    }
    case 3: {
      // [] hollow rectangle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = plot.svgRect(x - size / 2.4, y - size / 2.4, size / 1.2, size / 1.2);
      SVGUtil.setCSSClass(rect, cssid);
      parent.appendChild(rect);
      break;
    }
    case 4: {
      // <> hollow diamond
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = plot.svgRect(x - size / 2.7, y - size / 2.7, size / 1.35, size / 1.35);
      SVGUtil.setCSSClass(rect, cssid);
      SVGUtil.setAtt(rect, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(45," + SVGUtil.fmt(x) + "," + SVGUtil.fmt(y) + ")");
      parent.appendChild(rect);
      break;
    }
    case 5: {
      // O filled circle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, colorstr);
        plot.addCSSClassOrLogError(c);
      }
      Element circ = plot.svgCircle(x, y, size * .5);
      SVGUtil.setCSSClass(circ, cssid);
      parent.appendChild(circ);
      break;
    }
    case 6: {
      // [] filled rectangle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, colorstr);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = plot.svgRect(x - size / 2.2, y - size / 2.2, size / 1.1, size / 1.1);
      SVGUtil.setCSSClass(rect, cssid);
      parent.appendChild(rect);
      break;
    }
    case 7: {
      // <> filled diamond
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        String colorstr = colors.getColor(style);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, colorstr);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = plot.svgRect(x - size / 2.5, y - size / 2.5, size / 1.25, size / 1.25);
      SVGUtil.setCSSClass(rect, cssid);
      SVGUtil.setAtt(rect, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(45," + SVGUtil.fmt(x) + "," + SVGUtil.fmt(y) + ")");
      parent.appendChild(rect);
      break;
    }
    }
  }

  /**
   * Plot a replacement marker when an object is to be plotted as "disabled",
   * usually gray.
   *
   * @param plot Plot to draw to
   * @param parent Parent element
   * @param x X position
   * @param y Y position
   * @param size Size
   */
  protected void plotGray(SVGPlot plot, Element parent, double x, double y, double size) {
    Element marker = plot.svgCircle(x, y, size * .5);
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY + ":" + greycolor);
    parent.appendChild(marker);
  }

  /**
   * Plot a replacement marker when no color is set; usually black
   *
   * @param plot Plot to draw to
   * @param parent Parent element
   * @param x X position
   * @param y Y position
   * @param size Size
   */
  protected void plotUncolored(SVGPlot plot, Element parent, double x, double y, double size) {
    Element marker = plot.svgCircle(x, y, size * .5);
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY + ":" + dotcolor);
    parent.appendChild(marker);
  }

  @Override
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    // Note: we used to use <symbol> and <use>, but Batik performance was much
    // worse.
    Element use = plot.svgElement(SVGConstants.SVG_G_TAG);
    plotMarker(plot, use, x, y, style, size);
    if(parent != null) {
      parent.appendChild(use);
    }
    return use;
  }
}
