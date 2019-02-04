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

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * A marker library is a class that can generate and draw various styles of
 * markers. Different uses might require different marker libraries (e.g. full
 * screen, thumbnail, print)
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - create - Element
 */
public interface MarkerLibrary {
  /**
   * Insert a marker at the given coordinates. Markers will be defined in the
   * defs part of the document, and then SVG-"use"d at the given coordinates.
   * This supposedly is more efficient and significantly reduces file size.
   * Symbols will be named "s0", "s1" etc.; these names must not be used by
   * other elements in the SVG document!
   * 
   * @param plot Plot to draw on
   * @param parent parent node
   * @param x coordinate
   * @param y coordinate
   * @param style style (enumerated)
   * @param size size
   * @return Element node generated.
   */
  Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size);
}