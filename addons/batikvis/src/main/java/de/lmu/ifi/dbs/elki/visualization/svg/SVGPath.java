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

import de.lmu.ifi.dbs.elki.data.spatial.Polygon;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayListIter;

/**
 * Element used for building an SVG path using a string buffer.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @navassoc - create - Element
 */
public class SVGPath {
  /**
   * String buffer for building the path.
   */
  private StringBuilder buf = new StringBuilder(200);

  /**
   * The last action we did, to not add unnecessary commands
   */
  private char lastaction = 0;

  /**
   * Close path command.
   */
  public static final char PATH_CLOSE = 'Z';

  /**
   * Absolute line to command.
   */
  public static final char PATH_LINE_TO = 'L';

  /**
   * Relative line to command.
   */
  public static final char PATH_LINE_TO_RELATIVE = 'l';

  /**
   * Absolute move command.
   */
  public static final char PATH_MOVE = 'M';

  /**
   * Relative move command.
   */
  public static final char PATH_MOVE_RELATIVE = 'm';

  /**
   * Absolute horizontal line to command.
   */
  public static final char PATH_HORIZONTAL_LINE_TO = 'H';

  /**
   * Relative horizontal line to command.
   */
  public static final char PATH_HORIZONTAL_LINE_TO_RELATIVE = 'h';

  /**
   * Absolute vertical line to command.
   */
  public static final char PATH_VERTICAL_LINE_TO = 'V';

  /**
   * Relative vertical line to command.
   */
  public static final char PATH_VERTICAL_LINE_TO_RELATIVE = 'v';

  /**
   * Absolute cubic line to command.
   */
  public static final char PATH_CUBIC_TO = 'C';

  /**
   * Relative cubic line to command.
   */
  public static final char PATH_CUBIC_TO_RELATIVE = 'c';

  /**
   * Absolute "smooth cubic to" SVG path command.
   */
  public static final char PATH_SMOOTH_CUBIC_TO = 'S';

  /**
   * Relative smooth cubic to command.
   */
  public static final char PATH_SMOOTH_CUBIC_TO_RELATIVE = 's';

  /**
   * Absolute quadratic interpolation to command.
   */
  public static final char PATH_QUAD_TO = 'Q';

  /**
   * Relative quadratic interpolation to command.
   */
  public static final char PATH_QUAD_TO_RELATIVE = 'q';

  /**
   * Absolute smooth quadratic interpolation to
   * command.
   */
  public static final char PATH_SMOOTH_QUAD_TO = 'T';

  /**
   * Relative smooth quadratic interpolation to
   * command.
   */
  public static final char PATH_SMOOTH_QUAD_TO_RELATIVE = 't';

  /**
   * Absolute path arc command.
   */
  public static final char PATH_ARC = 'A';

  /**
   * Relative path arc command.
   */
  public static final char PATH_ARC_RELATIVE = 'a';

  /**
   * Empty path constructor.
   */
  public SVGPath() {
    // Nothing to do.
  }

  /**
   * Constructor with initial point.
   *
   * @param x initial coordinates
   * @param y initial coordinates
   */
  public SVGPath(double x, double y) {
    this();
    this.moveTo(x, y);
  }

  /**
   * Constructor with initial point.
   *
   * @param xy initial coordinates
   */
  public SVGPath(double[] xy) {
    this();
    this.moveTo(xy[0], xy[1]);
  }

  /**
   * Constructor from a double[] collection (e.g. a polygon)
   *
   * @param vectors vectors
   */
  public SVGPath(Polygon vectors) {
    this();
    for(ArrayListIter<double[]> it = vectors.iter(); it.valid(); it.advance()) {
      this.drawTo(it.get());
    }
    this.close();
  }

  /**
   * Draw a line given a series of coordinates.
   *
   * Helper function that will use "move" for the first point, "lineto" for the
   * remaining.
   *
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath drawTo(double x, double y) {
    return !isStarted() ? moveTo(x, y) : lineTo(x, y);
  }

  /**
   * Draw a line given a series of coordinates.
   *
   * Helper function that will use "move" for the first point, "lineto" for the
   * remaining.
   *
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath drawTo(double[] xy) {
    return !isStarted() ? moveTo(xy[0], xy[1]) : lineTo(xy[0], xy[1]);
  }

  /**
   * Test whether the path drawing has already started.
   *
   * @return Path freshness
   */
  public boolean isStarted() {
    return lastaction != 0;
  }

  /**
   * Draw a line to the given coordinates.
   *
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath lineTo(double x, double y) {
    return append(PATH_LINE_TO).append(x).append(y);
  }

  /**
   * Draw a line to the given coordinates.
   *
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath lineTo(double[] xy) {
    return lineTo(xy[0], xy[1]);
  }

  /**
   * Draw a line to the given relative coordinates.
   *
   * @param x relative coordinates
   * @param y relative coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeLineTo(double x, double y) {
    return append(PATH_LINE_TO_RELATIVE).append(x).append(y);
  }

  /**
   * Draw a line to the given relative coordinates.
   *
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeLineTo(double[] xy) {
    return relativeLineTo(xy[0], xy[1]);
  }

  /**
   * Draw a horizontal line to the given x coordinate.
   *
   * @param x new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath horizontalLineTo(double x) {
    return append(PATH_HORIZONTAL_LINE_TO).append(x);
  }

  /**
   * Draw a horizontal line to the given relative x coordinate.
   *
   * @param x new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeHorizontalLineTo(double x) {
    return append(PATH_HORIZONTAL_LINE_TO_RELATIVE).append(x);
  }

  /**
   * Draw a vertical line to the given y coordinate.
   *
   * @param y new coordinate
   * @return path object, for compact syntax.
   */
  public SVGPath verticalLineTo(double y) {
    return append(PATH_VERTICAL_LINE_TO).append(y);
  }

  /**
   * Draw a vertical line to the given relative y coordinate.
   *
   * @param y new coordinate
   * @return path object, for compact syntax.
   */
  public SVGPath relativeVerticalLineTo(double y) {
    return append(PATH_VERTICAL_LINE_TO_RELATIVE).append(y);
  }

  /**
   * Move to the given coordinates.
   *
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath moveTo(double x, double y) {
    return append(PATH_MOVE).append(x).append(y);
  }

  /**
   * Move to the given coordinates.
   *
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath moveTo(double[] xy) {
    return moveTo(xy[0], xy[1]);
  }

  /**
   * Move to the given relative coordinates.
   *
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeMoveTo(double x, double y) {
    return append(PATH_MOVE_RELATIVE).append(x).append(y);
  }

  /**
   * Move to the given relative coordinates.
   *
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeMoveTo(double[] xy) {
    return relativeMoveTo(xy[0], xy[1]);
  }

  /**
   * Cubic Bezier line to the given coordinates.
   *
   * @param c1x first control point x
   * @param c1y first control point y
   * @param c2x second control point x
   * @param c2y second control point y
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath cubicTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
    return append(PATH_CUBIC_TO).append(c1x).append(c1y).append(c2x).append(c2y).append(x).append(y);
  }

  /**
   * Cubic Bezier line to the given coordinates.
   *
   * @param c1xy first control point
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath cubicTo(double[] c1xy, double[] c2xy, double[] xy) {
    return append(PATH_CUBIC_TO).append(c1xy[0]).append(c1xy[1]).append(c2xy[0]).append(c2xy[1]).append(xy[0]).append(xy[1]);
  }

  /**
   * Cubic Bezier line to the given relative coordinates.
   *
   * @param c1x first control point x
   * @param c1y first control point y
   * @param c2x second control point x
   * @param c2y second control point y
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeCubicTo(double c1x, double c1y, double c2x, double c2y, double x, double y) {
    return append(PATH_CUBIC_TO_RELATIVE).append(c1x).append(c1y).append(c2x).append(c2y).append(x).append(y);
  }

  /**
   * Cubic Bezier line to the given relative coordinates.
   *
   * @param c1xy first control point
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeCubicTo(double[] c1xy, double[] c2xy, double[] xy) {
    return append(PATH_CUBIC_TO_RELATIVE).append(c1xy[0]).append(c1xy[1]).append(c2xy[0]).append(c2xy[1]).append(xy[0]).append(xy[1]);
  }

  /**
   * Smooth Cubic Bezier line to the given coordinates.
   *
   * @param c2x second control point x
   * @param c2y second control point y
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothCubicTo(double c2x, double c2y, double x, double y) {
    return append(PATH_SMOOTH_CUBIC_TO).append(c2x).append(c2y).append(x).append(y);
  }

  /**
   * Smooth Cubic Bezier line to the given coordinates.
   *
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothCubicTo(double[] c2xy, double[] xy) {
    return append(PATH_SMOOTH_CUBIC_TO).append(c2xy[0]).append(c2xy[1]).append(xy[0]).append(xy[1]);
  }

  /**
   * Smooth Cubic Bezier line to the given relative coordinates.
   *
   * @param c2x second control point x
   * @param c2y second control point y
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothCubicTo(double c2x, double c2y, double x, double y) {
    return append(PATH_SMOOTH_CUBIC_TO_RELATIVE).append(c2x).append(c2y).append(x).append(y);
  }

  /**
   * Smooth Cubic Bezier line to the given relative coordinates.
   *
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothCubicTo(double[] c2xy, double[] xy) {
    return append(PATH_SMOOTH_CUBIC_TO_RELATIVE).append(c2xy[0]).append(c2xy[1]).append(xy[0]).append(xy[1]);
  }

  /**
   * Quadratic Bezier line to the given coordinates.
   *
   * @param c1x first control point x
   * @param c1y first control point y
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath quadTo(double c1x, double c1y, double x, double y) {
    return append(PATH_QUAD_TO).append(c1x).append(c1y).append(x).append(y);
  }

  /**
   * Quadratic Bezier line to the given coordinates.
   *
   * @param c1xy first control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath quadTo(double[] c1xy, double[] xy) {
    return append(PATH_QUAD_TO).append(c1xy[0]).append(c1xy[1]).append(xy[0]).append(xy[1]);
  }

  /**
   * Quadratic Bezier line to the given relative coordinates.
   *
   * @param c1x first control point x
   * @param c1y first control point y
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeQuadTo(double c1x, double c1y, double x, double y) {
    return append(PATH_QUAD_TO_RELATIVE).append(c1x).append(c1y).append(x).append(y);
  }

  /**
   * Quadratic Bezier line to the given relative coordinates.
   *
   * @param c1xy first control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeQuadTo(double[] c1xy, double[] xy) {
    return append(PATH_QUAD_TO_RELATIVE).append(c1xy[0]).append(c1xy[1]).append(xy[0]).append(xy[1]);
  }

  /**
   * Smooth quadratic Bezier line to the given coordinates.
   *
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothQuadTo(double x, double y) {
    return append(PATH_SMOOTH_QUAD_TO).append(x).append(y);
  }

  /**
   * Smooth quadratic Bezier line to the given coordinates.
   *
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothQuadTo(double[] xy) {
    return append(PATH_SMOOTH_QUAD_TO).append(xy[0]).append(xy[1]);
  }

  /**
   * Smooth quadratic Bezier line to the given relative coordinates.
   *
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothQuadTo(double x, double y) {
    return append(PATH_SMOOTH_QUAD_TO_RELATIVE).append(x).append(y);
  }

  /**
   * Smooth quadratic Bezier line to the given relative coordinates.
   *
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothQuadTo(double[] xy) {
    return append(PATH_SMOOTH_QUAD_TO_RELATIVE).append(xy[0]).append(xy[1]);
  }

  /**
   * Elliptical arc curve to the given coordinates.
   *
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle &gt;= 180 deg
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param x new coordinates
   * @param y new coordinates
   */
  public SVGPath ellipticalArc(double rx, double ry, double ar, double la, double sp, double x, double y) {
    return append(PATH_ARC).append(rx).append(ry).append(ar).append(la).append(sp).append(x).append(y);
  }

  /**
   * Elliptical arc curve to the given coordinates.
   *
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle &gt;= 180 deg
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath ellipticalArc(double rx, double ry, double ar, double la, double sp, double[] xy) {
    return append(PATH_ARC).append(rx).append(ry).append(ar).append(la).append(sp).append(xy[0]).append(xy[1]);
  }

  /**
   * Elliptical arc curve to the given coordinates.
   *
   * @param rxy radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle &gt;= 180 deg
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath ellipticalArc(double[] rxy, double ar, double la, double sp, double[] xy) {
    return append(PATH_ARC).append(rxy[0]).append(rxy[1]).append(ar).append(la).append(sp).append(xy[0]).append(xy[1]);
  }

  /**
   * Elliptical arc curve to the given relative coordinates.
   *
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle &gt;= 180 deg
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param x new coordinates
   * @param y new coordinates
   */
  public SVGPath relativeEllipticalArc(double rx, double ry, double ar, double la, double sp, double x, double y) {
    return append(PATH_ARC_RELATIVE).append(rx).append(ry).append(ar).append(la).append(sp).append(x).append(y);
  }

  /**
   * Elliptical arc curve to the given relative coordinates.
   *
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle &gt;= 180 deg
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath relativeEllipticalArc(double rx, double ry, double ar, double la, double sp, double[] xy) {
    return append(PATH_ARC_RELATIVE).append(rx).append(ry).append(ar).append(la).append(sp).append(xy[0]).append(xy[1]);
  }

  /**
   * Elliptical arc curve to the given relative coordinates.
   *
   * @param rxy radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle &gt;= 180 deg
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath relativeEllipticalArc(double[] rxy, double ar, double la, double sp, double[] xy) {
    return append(PATH_ARC_RELATIVE).append(rxy[0]).append(rxy[1]).append(ar).append(la).append(sp).append(xy[0]).append(xy[1]);
  }

  /**
   * Append an action to the current path.
   *
   * @param action Current action
   */
  private SVGPath append(char action) {
    assert lastaction != 0 || action == PATH_MOVE : "Paths must begin with a move to the initial position!";
    if(lastaction != action) {
      buf.append(action);
      lastaction = action;
    }
    return this;
  }

  /**
   * Append a value to the current path.
   *
   * @param x coordinate.
   */
  private SVGPath append(double x) {
    if(!Double.isFinite(x)) {
      throw new IllegalArgumentException("Cannot draw an infinite/NaN position.");
    }
    if(x >= 0) {
      final int l = buf.length();
      if(l > 0) {
        char c = buf.charAt(l - 1);
        assert c != 'e' && c != 'E' : "Invalid exponential in path";
        if(c >= '0' && c <= '9')
          buf.append(' ');
      }
    }
    buf.append(SVGUtil.FMT.format(x));
    return this;
  }

  /**
   * Close the path.
   *
   * @return path object, for compact syntax.
   */
  public SVGPath close() {
    assert lastaction != 0 : "Paths must begin with a move to the initial position!";
    if(lastaction != PATH_CLOSE) {
      buf.append(' ').append(PATH_CLOSE);
      lastaction = PATH_CLOSE;
    }
    return this;
  }

  /**
   * Turn the path buffer into an SVG element.
   *
   * @param plot Plot context (= element factory)
   * @return SVG Element
   */
  public Element makeElement(SVGPlot plot) {
    Element elem = plot.svgElement(SVGConstants.SVG_PATH_TAG);
    elem.setAttribute(SVGConstants.SVG_D_ATTRIBUTE, buf.toString());
    return elem;
  }

  /**
   * Turn the path buffer into an SVG element.
   * O
   * 
   * @param plot Plot context (= element factory)
   * @param cssclass CSS class
   * @return SVG Element
   */
  public Element makeElement(SVGPlot plot, String cssclass) {
    Element elem = plot.svgElement(SVGConstants.SVG_PATH_TAG, cssclass);
    elem.setAttribute(SVGConstants.SVG_D_ATTRIBUTE, buf.toString());
    return elem;
  }

  /**
   * Return the SVG serialization of the path.
   */
  @Override
  public String toString() {
    return buf.toString();
  }
}
