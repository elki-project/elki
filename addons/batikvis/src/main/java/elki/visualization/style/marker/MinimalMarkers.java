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
import elki.visualization.style.ColorInterpolation;
import elki.visualization.style.StyleLibrary;
import elki.visualization.svg.SVGPlot;
import elki.visualization.svg.SVGUtil;

/**
 * Simple marker library that just draws colored rectangles at the given
 * coordinates.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - ColorLibrary
 */
public class MinimalMarkers implements MarkerLibrary {
  /**
   * Color library
   */
  private ColorLibrary colors;

  /**
   * Color of "uncolored" dots
   */
  private String dotcolor = "black";

  /**
   * Color of "greyed out" dots
   */
  private String greycolor = "gray";

  /**
   * Color interpolation style
   */
  ColorInterpolation interpol = ColorInterpolation.RGB;

  /**
   * Constructor
   * 
   * @param style Style library to use
   */
  public MinimalMarkers(StyleLibrary style) {
    super();
    this.colors = style.getColorSet(StyleLibrary.MARKERPLOT);
    this.dotcolor = style.getColor(StyleLibrary.MARKERPLOT);
    this.greycolor = style.getColor(StyleLibrary.PLOTGRAY);
  }

  @Override
  public Element useMarker(SVGPlot plot, double x, double y, int stylenr, double size, double intensity) {
    String col = stylenr == -1 ? dotcolor : stylenr == -2 ? greycolor : colors.getColor(stylenr);
    if(intensity < 1) {
      col = interpol.interpolate(col, greycolor, intensity);
    }
    Element marker = plot.svgRect(x - size * .5, y - size * .5, size, size);
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY + ":" + col);
    return marker;
  }
}
