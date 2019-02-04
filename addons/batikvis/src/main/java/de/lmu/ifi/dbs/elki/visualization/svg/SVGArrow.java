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

import org.w3c.dom.Element;

/**
 * Static class for drawing simple arrows
 * 
 * @author Erich Schubert
 * @author Robert Rödler
 * @since 0.5.5
 * 
 * @composed - - - Direction
 * @assoc - - - SVGPath
 */
public final class SVGArrow {
  /**
   * Private constructor. Static methods only.
   */
  private SVGArrow() {
    // Do not use.
  }

  /**
   * Direction constants
   * 
   * @author Erich Schubert
   * @author Robert Rödler
   */
  public enum Direction {
    LEFT, DOWN, RIGHT, UP, // SWAPWITH, INSERT
  }

  /**
   * Draw an arrow at the given position.
   * 
   * Note: the arrow is an unstyled svg path. You need to apply style
   * afterwards.
   * 
   * @param svgp Plot to draw to
   * @param dir Direction to draw
   * @param x Center x coordinate
   * @param y Center y coordinate
   * @param size Arrow size
   * @return SVG Element
   */
  public static Element makeArrow(SVGPlot svgp, Direction dir, double x, double y, double size) {
    final double hs = size / 2.;

    switch(dir){
    case LEFT:
      return new SVGPath().drawTo(x + hs, y + hs).drawTo(x - hs, y).drawTo(x + hs, y - hs).drawTo(x + hs, y + hs).close().makeElement(svgp);
    case DOWN:
      return new SVGPath().drawTo(x - hs, y - hs).drawTo(x + hs, y - hs).drawTo(x, y + hs).drawTo(x - hs, y - hs).close().makeElement(svgp);
    case RIGHT:
      return new SVGPath().drawTo(x - hs, y - hs).drawTo(x + hs, y).drawTo(x - hs, y + hs).drawTo(x - hs, y - hs).close().makeElement(svgp);
    case UP:
      return new SVGPath().drawTo(x - hs, y + hs).drawTo(x, y - hs).drawTo(x + hs, y + hs).drawTo(x - hs, y + hs).close().makeElement(svgp);
    default:
      throw new IllegalArgumentException("Unexpected direction: " + dir);
    }
  }
}