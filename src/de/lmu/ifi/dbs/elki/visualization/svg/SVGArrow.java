package de.lmu.ifi.dbs.elki.visualization.svg;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import org.w3c.dom.Element;

/**
 * Static class for drawing simple arrows
 * 
 * @author Erich Schubert
 * @author Robert Rödler
 * 
 * @apiviz.uses SVGPath
 */
public final class SVGArrow {
  /**
   * Direction constants
   * 
   * @author Erich Schubert
   * @author Robert Rödler
   * 
   * @apiviz.exclude
   */
  public static enum Direction {
    LEFT, DOWN, RIGHT, UP, // SWAPWITH, INSERT
  }

  /**
   * Constant for "up"
   */
  public static final Direction UP = Direction.UP;

  /**
   * Constant for "down"
   */
  public static final Direction DOWN = Direction.DOWN;

  /**
   * Constant for "right"
   */
  public static final Direction RIGHT = Direction.RIGHT;

  /**
   * Constant for "left"
   */
  public static final Direction LEFT = Direction.LEFT;

  /**
   * Draw an arrow at the given position.
   * 
   * Note: the arrow is an unstyled svg path. You need to apply style afterwards.
   * 
   * @param svgp Plot to draw to
   * @param dir Direction to draw
   * @param x Center x coordinate
   * @param y Center y coordinate
   * @param size Arrow size
   * @return SVG Element
   */
  public static Element makeArrow(SVGPlot svgp, Direction dir, double x, double y, double size) {
    final SVGPath path = new SVGPath();
    final double hs = size / 2.;

    switch(dir){
    case LEFT:
      path.drawTo(x + hs, y + hs);
      path.drawTo(x - hs, y);
      path.drawTo(x + hs, y - hs);
      path.drawTo(x + hs, y + hs);
      break;
    case DOWN:
      path.drawTo(x - hs, y - hs);
      path.drawTo(x + hs, y - hs);
      path.drawTo(x, y + hs);
      path.drawTo(x - hs, y - hs);
      break;
    case RIGHT:
      path.drawTo(x - hs, y - hs);
      path.drawTo(x + hs, y);
      path.drawTo(x - hs, y + hs);
      path.drawTo(x - hs, y - hs);
      break;
    case UP:
      path.drawTo(x - hs, y + hs);
      path.drawTo(x, y - hs);
      path.drawTo(x + hs, y + hs);
      path.drawTo(x - hs, y + hs);
      break;
    }
    path.close();
    return path.makeElement(svgp);
  }
}