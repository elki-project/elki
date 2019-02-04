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

/**
 * Class containing some popular SVG effects.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public final class SVGEffects {
  /**
   * Private constructor. Static methods only.
   */
  private SVGEffects() {
    // Do not use.
  }

  /**
   * ID for the drop shadow effect
   */
  public static final String SHADOW_ID = "shadow-effect";

  /**
   * ID for the light gradient fill
   */
  public static final String LIGHT_GRADIENT_ID = "light-gradient";
  
  /**
   * Static method to prepare a SVG document for drop shadow effects.
   * 
   * Invoke this from an appropriate update thread!
   * 
   * @param svgp Plot to prepare
   */
  public static void addShadowFilter(SVGPlot svgp) {
    Element shadow = svgp.getIdElement(SHADOW_ID);
    if(shadow == null) {
      shadow = svgp.svgElement(SVGConstants.SVG_FILTER_TAG);
      shadow.setAttribute(SVGConstants.SVG_ID_ATTRIBUTE, SHADOW_ID);
      shadow.setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "140%");
      shadow.setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, "140%");

      Element offset = svgp.svgElement(SVGConstants.SVG_FE_OFFSET_TAG);
      offset.setAttribute(SVGConstants.SVG_IN_ATTRIBUTE, SVGConstants.SVG_SOURCE_ALPHA_VALUE);
      offset.setAttribute(SVGConstants.SVG_RESULT_ATTRIBUTE, "off");
      offset.setAttribute(SVGConstants.SVG_DX_ATTRIBUTE, "0.1");
      offset.setAttribute(SVGConstants.SVG_DY_ATTRIBUTE, "0.1");
      shadow.appendChild(offset);

      Element gauss = svgp.svgElement(SVGConstants.SVG_FE_GAUSSIAN_BLUR_TAG);
      gauss.setAttribute(SVGConstants.SVG_IN_ATTRIBUTE, "off");
      gauss.setAttribute(SVGConstants.SVG_RESULT_ATTRIBUTE, "blur");
      gauss.setAttribute(SVGConstants.SVG_STD_DEVIATION_ATTRIBUTE, "0.1");
      shadow.appendChild(gauss);

      Element blend = svgp.svgElement(SVGConstants.SVG_FE_BLEND_TAG);
      blend.setAttribute(SVGConstants.SVG_IN_ATTRIBUTE, SVGConstants.SVG_SOURCE_GRAPHIC_VALUE);
      blend.setAttribute(SVGConstants.SVG_IN2_ATTRIBUTE, "blur");
      blend.setAttribute(SVGConstants.SVG_MODE_ATTRIBUTE, SVGConstants.SVG_NORMAL_VALUE);
      shadow.appendChild(blend);

      svgp.getDefs().appendChild(shadow);
      svgp.putIdElement(SHADOW_ID, shadow);
    }
  }

  /**
   * Static method to prepare a SVG document for light gradient effects.
   * 
   * Invoke this from an appropriate update thread!
   * 
   * @param svgp Plot to prepare
   */
  public static void addLightGradient(SVGPlot svgp) {
    Element gradient = svgp.getIdElement(LIGHT_GRADIENT_ID);
    if(gradient == null) {
      gradient = svgp.svgElement(SVGConstants.SVG_LINEAR_GRADIENT_TAG);
      gradient.setAttribute(SVGConstants.SVG_ID_ATTRIBUTE, LIGHT_GRADIENT_ID);
      gradient.setAttribute(SVGConstants.SVG_X1_ATTRIBUTE, "0");
      gradient.setAttribute(SVGConstants.SVG_Y1_ATTRIBUTE, "0");
      gradient.setAttribute(SVGConstants.SVG_X2_ATTRIBUTE, "0");
      gradient.setAttribute(SVGConstants.SVG_Y2_ATTRIBUTE, "1");

      Element stop0 = svgp.svgElement(SVGConstants.SVG_STOP_TAG);
      stop0.setAttribute(SVGConstants.SVG_STOP_COLOR_ATTRIBUTE, "white");
      stop0.setAttribute(SVGConstants.SVG_STOP_OPACITY_ATTRIBUTE, "1");
      stop0.setAttribute(SVGConstants.SVG_OFFSET_ATTRIBUTE, "0");
      gradient.appendChild(stop0);

      Element stop04 = svgp.svgElement(SVGConstants.SVG_STOP_TAG);
      stop04.setAttribute(SVGConstants.SVG_STOP_COLOR_ATTRIBUTE, "white");
      stop04.setAttribute(SVGConstants.SVG_STOP_OPACITY_ATTRIBUTE, "0");
      stop04.setAttribute(SVGConstants.SVG_OFFSET_ATTRIBUTE, ".4");
      gradient.appendChild(stop04);
      
      Element stop06 = svgp.svgElement(SVGConstants.SVG_STOP_TAG);
      stop06.setAttribute(SVGConstants.SVG_STOP_COLOR_ATTRIBUTE, "black");
      stop06.setAttribute(SVGConstants.SVG_STOP_OPACITY_ATTRIBUTE, "0");
      stop06.setAttribute(SVGConstants.SVG_OFFSET_ATTRIBUTE, ".6");
      gradient.appendChild(stop06);

      Element stop1 = svgp.svgElement(SVGConstants.SVG_STOP_TAG);
      stop1.setAttribute(SVGConstants.SVG_STOP_COLOR_ATTRIBUTE, "black");
      stop1.setAttribute(SVGConstants.SVG_STOP_OPACITY_ATTRIBUTE, ".5");
      stop1.setAttribute(SVGConstants.SVG_OFFSET_ATTRIBUTE, "1");
      gradient.appendChild(stop1);

      svgp.getDefs().appendChild(gradient);
      svgp.putIdElement(LIGHT_GRADIENT_ID, gradient);
    }
  }

  /**
   * Checkmark path, sized approx. 15x15
   */
  public static final String SVG_CHECKMARK_PATH = "M0 6.458 L2.047 4.426 5.442 7.721 12.795 0 15 2.117 5.66 11.922 Z";

  /**
   * Creates a 15x15 big checkmark
   * 
   * @param svgp Plot to create the element for
   * @return Element
   */
  public static Element makeCheckmark(SVGPlot svgp) {
    Element checkmark = svgp.svgElement(SVGConstants.SVG_PATH_TAG);
    checkmark.setAttribute(SVGConstants.SVG_D_ATTRIBUTE, SVG_CHECKMARK_PATH);
    checkmark.setAttribute(SVGConstants.SVG_FILL_ATTRIBUTE, SVGConstants.CSS_BLACK_VALUE);
    checkmark.setAttribute(SVGConstants.SVG_STROKE_ATTRIBUTE, SVGConstants.CSS_NONE_VALUE);
    return checkmark;
  }
}