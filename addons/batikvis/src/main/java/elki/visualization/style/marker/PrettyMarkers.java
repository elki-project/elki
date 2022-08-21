/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.visualization.style.marker;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.visualization.colors.ColorLibrary;
import elki.visualization.css.CSSClass;
import elki.visualization.style.ColorInterpolation;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGPath;
import elki.visualization.svg.SVGPlot;
import elki.visualization.svg.SVGUtil;

/**
 * Marker library achieving a larger number of styles by combining different
 * shapes with different colors. Uses object ID management by SVGPlot.
 * <p>
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
   * Color of "grayed out" dots
   */
  private String graycolor;

  /**
   * Color interpolation style
   */
  ColorInterpolation interpol = ColorInterpolation.RGB;

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
    this.graycolor = style.getColor(StyleLibrary.PLOTGRAY);
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
   * Draw an marker used in scatter plots.
   *
   * @param plot containing plot
   * @param x position
   * @param y position
   * @param style marker style (enumerated)
   * @param size size
   * @return container element
   */
  public Element plotMarker(SVGPlot plot, double x, double y, int style, double size, double intensity) {
    if(style == -1 || style == -2) {
      return plotSimple(plot, x, y, size, style == -1 ? dotcolor : graycolor);
    }
    // No dot allowed!
    String cssid = prefix + style + "_" + (int) (100 * size);
    String colorstr = colors.getColor(style);
    String icolorstr = intensity < 1 ? interpol.interpolate(colorstr, graycolor, intensity) : null;

    switch(style & 0x7){
    case 0: {
      // + cross
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        plot.addCSSClassOrLogError(c);
      }
      Element line1 = plot.svgLine(x, y - size * 0.45, x, y + size * 0.45);
      Element line2 = plot.svgLine(x - size * 0.45, y, x + size * 0.45, y);
      SVGUtil.setCSSClass(line1, cssid);
      SVGUtil.setCSSClass(line2, cssid);
      if(icolorstr != null) {
        final String s = SVGConstants.CSS_STROKE_PROPERTY + ":" + icolorstr;
        SVGUtil.setStyle(line1, s);
        SVGUtil.setStyle(line2, s);
      }
      Element e = plot.svgElement(SVGConstants.SVG_G_TAG);
      e.appendChild(line1);
      e.appendChild(line2);
      return e;
    }
    case 1: {
      // X cross
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        plot.addCSSClassOrLogError(c);
      }
      Element line1 = plot.svgLine(x - size * 0.35355, y - size * 0.35355, x + size * 0.35355, y + size * 0.35355);
      Element line2 = plot.svgLine(x - size * 0.35355, y + size * 0.35355, x + size * 0.35355, y - size * 0.35355);
      SVGUtil.setCSSClass(line1, cssid);
      SVGUtil.setCSSClass(line2, cssid);
      if(icolorstr != null) {
        final String s = SVGConstants.CSS_STROKE_PROPERTY + ":" + icolorstr;
        SVGUtil.setStyle(line1, s);
        SVGUtil.setStyle(line2, s);
      }
      Element e = plot.svgElement(SVGConstants.SVG_G_TAG);
      e.appendChild(line1);
      e.appendChild(line2);
      return e;
    }
    case 2: {
      // O hollow circle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        plot.addCSSClassOrLogError(c);
      }
      Element circ = plot.svgCircle(x, y, size * 0.45);
      SVGUtil.setCSSClass(circ, cssid);
      if(icolorstr != null) {
        SVGUtil.setStyle(circ, SVGConstants.CSS_STROKE_PROPERTY + ":" + icolorstr);
      }
      return circ;
    }
    case 3: {
      // [] hollow rectangle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = plot.svgRect(x - size * 0.42, y - size * 0.42, size * 0.84, size * 0.84);
      SVGUtil.setCSSClass(rect, cssid);
      if(icolorstr != null) {
        SVGUtil.setStyle(rect, SVGConstants.CSS_STROKE_PROPERTY + ":" + icolorstr);
      }
      return rect;
    }
    case 4: {
      // <> hollow diamond
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_STROKE_PROPERTY, colorstr);
        c.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, size / 6);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_NONE_VALUE);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = new SVGPath().moveTo(x, y - size * 0.6).moveTo(x + size * 0.6, y) //
          .moveTo(x, y + size * 0.6).moveTo(x - size * 0.6, y).close().makeElement(plot);
      SVGUtil.setCSSClass(rect, cssid);
      if(icolorstr != null) {
        SVGUtil.setStyle(rect, SVGConstants.CSS_STROKE_PROPERTY + ":" + icolorstr);
      }
      return rect;
    }
    case 5: {
      // O filled circle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, colorstr);
        plot.addCSSClassOrLogError(c);
      }
      Element circ = plot.svgCircle(x, y, size * .5);
      SVGUtil.setCSSClass(circ, cssid);
      if(icolorstr != null) {
        SVGUtil.setStyle(circ, SVGConstants.CSS_FILL_PROPERTY + ":" + icolorstr);
      }
      return circ;
    }
    case 6: {
      // [] filled rectangle
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, colorstr);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = plot.svgRect(x - size * 0.45, y - size * 0.45, size * 0.90, size * 0.90);
      SVGUtil.setCSSClass(rect, cssid);
      if(icolorstr != null) {
        SVGUtil.setStyle(rect, SVGConstants.CSS_FILL_PROPERTY + ":" + icolorstr);
      }
      return rect;
    }
    case 7: {
      // <> filled diamond
      if(!plot.getCSSClassManager().contains(cssid)) {
        CSSClass c = new CSSClass(this, cssid);
        c.setStatement(SVGConstants.CSS_FILL_PROPERTY, colorstr);
        plot.addCSSClassOrLogError(c);
      }
      Element rect = new SVGPath().moveTo(x, y - size * 0.6).moveTo(x + size * 0.6, y) //
          .moveTo(x, y + size * 0.6).moveTo(x - size * 0.6, y).close().makeElement(plot);
      SVGUtil.setCSSClass(rect, cssid);
      if(icolorstr != null) {
        SVGUtil.setStyle(rect, SVGConstants.CSS_FILL_PROPERTY + ":" + icolorstr);
      }
      return rect;
    }
    }
    throw new IllegalStateException("Supposedly unreachable.");
  }

  /**
   * Plot a replacement marker when no color is set; usually black
   *
   * @param plot Plot to draw to
   * @param x X position
   * @param y Y position
   * @param size Size
   * @param color Color string
   */
  protected Element plotSimple(SVGPlot plot, double x, double y, double size, String color) {
    Element marker = plot.svgCircle(x, y, size * .5);
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY + ":" + color);
    return marker;
  }

  @Override
  public Element useMarker(SVGPlot plot, double x, double y, int style, double size, double intensity) {
    // Note: we used to use <symbol> and <use>, but Batik performance was poor
    return plotMarker(plot, x, y, style, size, intensity);
  }
}
