package de.lmu.ifi.dbs.elki.visualization.style.marker;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Simple marker library that just draws colored rectangles at the given
 * coordinates.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf ColorLibrary
 */
public class MinimalMarkers implements MarkerLibrary {
  /**
   * Color library
   */
  private ColorLibrary colors;

  /**
   * Constructor
   * 
   * @param style Style library to use
   */
  public MinimalMarkers(StyleLibrary style) {
    super();
    this.colors = style.getColorSet(StyleLibrary.PLOT);
  }

  /**
   * Use a given marker on the document.
   */
  @Override
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int stylenr, double size) {
    Element marker = plot.svgRect(x - size / 2, y - size / 2, size, size);
    SVGUtil.setStyle(marker, SVGConstants.CSS_FILL_PROPERTY + ":" + colors.getColor(stylenr));
    parent.appendChild(marker);
    return marker;
  }
}