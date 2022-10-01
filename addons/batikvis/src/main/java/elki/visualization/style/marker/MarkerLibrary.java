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

import org.w3c.dom.Element;

import elki.visualization.svg.SVGPlot;

/**
 * A marker library is a class that can generate and draw various styles of
 * markers. Different uses might require different marker libraries (e.g., full
 * screen, thumbnail, print)
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - create - Element
 */
public interface MarkerLibrary {
  /**
   * Insert a marker at the given coordinates.
   * 
   * @param plot Plot to draw on
   * @param x coordinate
   * @param y coordinate
   * @param style style (enumerated)
   * @param size size
   * @return Element node generated.
   */
  default Element useMarker(SVGPlot plot, double x, double y, int style, double size) {
    return useMarker(plot, x, y, style, size, 1.0);
  }

  /**
   * Insert a soft marker at the given coordinates.
   * 
   * @param plot Plot to draw on
   * @param x coordinate
   * @param y coordinate
   * @param style style (enumerated)
   * @param size size
   * @param intensity intensity of the color
   * @return Element node generated.
   */
  Element useMarker(SVGPlot plot, double x, double y, int style, double size, double intensity);
}
