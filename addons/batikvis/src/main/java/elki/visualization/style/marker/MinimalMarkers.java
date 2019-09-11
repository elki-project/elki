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
package elki.visualization.style.marker;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import elki.visualization.colors.ColorLibrary;
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
   * Constructor
   * 
   * @param style Style library to use
   */
  public MinimalMarkers(StyleLibrary style) {
    super();
    this.colors = style.getColorSet(StyleLibrary.MARKERPLOT);
    this.dotcolor = style.getColor(StyleLibrary.MARKERPLOT);
    this.greycolor = style.getColor(StyleLibrary.PLOTGREY);
  }

  /**
   * Use a given marker on the document.
   */
  @Override
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int stylenr, double size) {
    Element marker = plot.svgRect(x - size * .5, y - size * .5, size, size);
    final String col;
    if(stylenr == -1) {
      col = dotcolor;
    }
    else if(stylenr == -2) {
      col = greycolor;
    }
    else {
      col = colors.getColor(stylenr);
    }
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY + ":" + col);
    parent.appendChild(marker);
    return marker;
  }
}