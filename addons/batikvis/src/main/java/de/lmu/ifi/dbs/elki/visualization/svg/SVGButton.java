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

import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;

/**
 * Class to draw a button as SVG.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class SVGButton {
  /**
   * Default button color
   */
  public static final String DEFAULT_BUTTON_COLOR = SVGConstants.CSS_LIGHTGRAY_VALUE;

  /**
   * Default text color
   */
  public static final String DEFAULT_TEXT_COLOR = SVGConstants.CSS_BLACK_VALUE;

  /**
   * X position
   */
  private double x;

  /**
   * Y position
   */
  private double y;

  /**
   * Width
   */
  private double w;

  /**
   * Height
   */
  private double h;

  /**
   * Corner rounding factor. NaN = no rounding
   */
  private double r = Double.NaN;

  /**
   * Class for the buttons main CSS
   */
  private CSSClass butcss;

  /**
   * Button title, optional
   */
  private String title = null;

  /**
   * Title styling
   */
  private CSSClass titlecss = null;

  /**
   * Constructor.
   * 
   * @param x Position X
   * @param y Position Y
   * @param w Width
   * @param h Height
   * @param r Rounded radius
   */
  public SVGButton(double x, double y, double w, double h, double r) {
    super();
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.r = r;
    this.butcss = new CSSClass(this, "button");
    butcss.setStatement(SVGConstants.CSS_FILL_PROPERTY, DEFAULT_BUTTON_COLOR);
    butcss.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_BLACK_VALUE);
    butcss.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, ".01");
  }

  /**
   * Produce the actual SVG elements for the button.
   * 
   * @param svgp Plot to draw to
   * @return Button wrapper element
   */
  public Element render(SVGPlot svgp) {
    Element tag = svgp.svgElement(SVGConstants.SVG_G_TAG);
    Element button = svgp.svgRect(x, y, w, h);
    if(!Double.isNaN(r)) {
      SVGUtil.setAtt(button, SVGConstants.SVG_RX_ATTRIBUTE, r);
      SVGUtil.setAtt(button, SVGConstants.SVG_RY_ATTRIBUTE, r);
    }
    SVGUtil.setAtt(button, SVGConstants.SVG_STYLE_ATTRIBUTE, butcss.inlineCSS());
    tag.appendChild(button);
    // Add light effect:
    if (svgp.getIdElement(SVGEffects.LIGHT_GRADIENT_ID) != null) {
      Element light = svgp.svgRect(x, y, w, h);      
      if(!Double.isNaN(r)) {
        SVGUtil.setAtt(light, SVGConstants.SVG_RX_ATTRIBUTE, r);
        SVGUtil.setAtt(light, SVGConstants.SVG_RY_ATTRIBUTE, r);
      }
      SVGUtil.setAtt(light, SVGConstants.SVG_STYLE_ATTRIBUTE, "fill:url(#"+SVGEffects.LIGHT_GRADIENT_ID+");fill-opacity:.5");
      tag.appendChild(light);
    }

    // Add shadow effect:
    if(svgp.getIdElement(SVGEffects.SHADOW_ID) != null) {
      //Element shadow = svgp.svgRect(x + (w * .05), y + (h * .05), w, h);
      //SVGUtil.setAtt(button, SVGConstants.SVG_STYLE_ATTRIBUTE, SVGConstants.CSS_FILL_PROPERTY + ":" + SVGConstants.CSS_BLACK_VALUE);
      button.setAttribute(SVGConstants.SVG_FILTER_ATTRIBUTE, "url(#" + SVGEffects.SHADOW_ID + ")");
      //tag.appendChild(shadow);
    }

    if(title != null) {
      Element label = svgp.svgText(x + w * .5, y + h * .7, title);
      label.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, titlecss.inlineCSS());
      tag.appendChild(label);
    }
    return tag;
  }

  /**
   * Set the button title
   * 
   * @param title Button title
   * @param textcolor Color
   */
  public void setTitle(String title, String textcolor) {
    this.title = title;
    if(titlecss == null) {
      titlecss = new CSSClass(this, "text");
      titlecss.setStatement(SVGConstants.CSS_TEXT_ANCHOR_PROPERTY, SVGConstants.CSS_MIDDLE_VALUE);
      titlecss.setStatement(SVGConstants.CSS_FILL_PROPERTY, textcolor);
      titlecss.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, .6 * h);
    }
  }
}