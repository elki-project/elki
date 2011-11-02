package de.lmu.ifi.dbs.elki.visualization.svg;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Element used for building an SVG path using a string buffer.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Element oneway - - «create»
 */
public class SVGPath {
  /**
   * String buffer for building the path.
   */
  private StringBuffer buf = new StringBuffer();

  /**
   * The last action we did, to not add unnecessary commands
   */
  private String lastaction = null;

  /**
   * The absolute "smooth cubic to" SVG path command (missing from
   * SVGConstants).
   */
  public static final String PATH_SMOOTH_CUBIC_TO = "S";

  /**
   * The lower case version (relative) line to command.
   */
  public final static String PATH_LINE_TO_RELATIVE = SVGConstants.PATH_LINE_TO.toLowerCase();

  /**
   * The lower case version (relative) move command.
   */
  public final static String PATH_MOVE_RELATIVE = SVGConstants.PATH_MOVE.toLowerCase();

  /**
   * The lower case version (relative) horizontal line to command.
   */
  public final static String PATH_HORIZONTAL_LINE_TO_RELATIVE = SVGConstants.PATH_HORIZONTAL_LINE_TO.toLowerCase();

  /**
   * The lower case version (relative) vertical line to command.
   */
  public final static String PATH_VERTICAL_LINE_TO_RELATIVE = SVGConstants.PATH_VERTICAL_LINE_TO.toLowerCase();

  /**
   * The lower case version (relative) cubic line to command.
   */
  public final static String PATH_CUBIC_TO_RELATIVE = SVGConstants.PATH_CUBIC_TO.toLowerCase();

  /**
   * The lower case version (relative) smooth cubic to command.
   */
  public final static String PATH_SMOOTH_CUBIC_TO_RELATIVE = PATH_SMOOTH_CUBIC_TO.toLowerCase();

  /**
   * The lower case version (relative) quadratic interpolation to command.
   */
  public final static String PATH_QUAD_TO_RELATIVE = SVGConstants.PATH_QUAD_TO.toLowerCase();

  /**
   * The lower case version (relative) smooth quadratic interpolation to
   * command.
   */
  public final static String PATH_SMOOTH_QUAD_TO_RELATIVE = SVGConstants.PATH_SMOOTH_QUAD_TO.toLowerCase();

  /**
   * The lower case version (relative) path arc command.
   */
  public final static String PATH_ARC_RELATIVE = SVGConstants.PATH_ARC.toLowerCase();

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
    this.moveTo(xy);
  }

  /**
   * Constructor from a vector collection (e.g. a polygon)
   * 
   * @param vectors vectors
   */
  public SVGPath(Iterable<Vector> vectors) {
    this();
    for(Vector vec : vectors) {
      this.drawTo(vec.get(0), vec.get(1));
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
    if(!isStarted()) {
      moveTo(x, y);
    }
    else {
      lineTo(x, y);
    }
    return this;
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
    if(!isStarted()) {
      moveTo(xy);
    }
    else {
      lineTo(xy);
    }
    return this;
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
  public SVGPath drawTo(Vector xy) {
    if(!isStarted()) {
      moveTo(xy);
    }
    else {
      lineTo(xy);
    }
    return this;
  }

  /**
   * Test whether the path drawing has already started.
   * 
   * @return Path freshness
   */
  public boolean isStarted() {
    return lastaction != null;
  }

  /**
   * Draw a line to the given coordinates.
   * 
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath lineTo(double x, double y) {
    append(SVGConstants.PATH_LINE_TO, x, y);
    return this;
  }

  /**
   * Draw a line to the given coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath lineTo(double[] xy) {
    append(SVGConstants.PATH_LINE_TO, xy[0], xy[1]);
    return this;
  }

  /**
   * Draw a line to the given coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath lineTo(Vector xy) {
    append(SVGConstants.PATH_LINE_TO, xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Draw a line to the given relative coordinates.
   * 
   * @param x relative coordinates
   * @param y relative coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeLineTo(double x, double y) {
    append(PATH_LINE_TO_RELATIVE, x, y);
    return this;
  }

  /**
   * Draw a line to the given relative coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeLineTo(double[] xy) {
    append(PATH_LINE_TO_RELATIVE, xy[0], xy[1]);
    return this;
  }

  /**
   * Draw a line to the given relative coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeLineTo(Vector xy) {
    append(PATH_LINE_TO_RELATIVE, xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Draw a horizontal line to the given x coordinate.
   * 
   * @param x new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath horizontalLineTo(double x) {
    append(SVGConstants.PATH_HORIZONTAL_LINE_TO, x);
    return this;
  }

  /**
   * Draw a horizontal line to the given relative x coordinate.
   * 
   * @param x new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeHorizontalLineTo(double x) {
    append(PATH_HORIZONTAL_LINE_TO_RELATIVE, x);
    return this;
  }

  /**
   * Draw a vertical line to the given y coordinate.
   * 
   * @param y new coordinate
   * @return path object, for compact syntax.
   */
  public SVGPath verticalLineTo(double y) {
    append(SVGConstants.PATH_VERTICAL_LINE_TO, y);
    return this;
  }

  /**
   * Draw a vertical line to the given relative y coordinate.
   * 
   * @param y new coordinate
   * @return path object, for compact syntax.
   */
  public SVGPath relativeVerticalLineTo(double y) {
    append(PATH_VERTICAL_LINE_TO_RELATIVE, y);
    return this;
  }

  /**
   * Move to the given coordinates.
   * 
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath moveTo(double x, double y) {
    append(SVGConstants.PATH_MOVE, x, y);
    return this;
  }

  /**
   * Move to the given coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath moveTo(double[] xy) {
    append(SVGConstants.PATH_MOVE, xy[0], xy[1]);
    return this;
  }

  /**
   * Move to the given coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath moveTo(Vector xy) {
    append(SVGConstants.PATH_MOVE, xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Move to the given relative coordinates.
   * 
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeMoveTo(double x, double y) {
    append(PATH_MOVE_RELATIVE, x, y);
    return this;
  }

  /**
   * Move to the given relative coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeMoveTo(double[] xy) {
    append(PATH_MOVE_RELATIVE, xy[0], xy[1]);
    return this;
  }

  /**
   * Move to the given relative coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeMoveTo(Vector xy) {
    append(PATH_MOVE_RELATIVE, xy.get(0), xy.get(1));
    return this;
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
    append(SVGConstants.PATH_CUBIC_TO, c1x, c1y, c2x, c2y, x, y);
    return this;
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
    append(SVGConstants.PATH_CUBIC_TO, c1xy[0], c1xy[1], c2xy[0], c2xy[1], xy[0], xy[1]);
    return this;
  }

  /**
   * Cubic Bezier line to the given coordinates.
   * 
   * @param c1xy first control point
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath cubicTo(Vector c1xy, Vector c2xy, Vector xy) {
    append(SVGConstants.PATH_CUBIC_TO, c1xy.get(0), c1xy.get(1), c2xy.get(0), c2xy.get(1), xy.get(0), xy.get(1));
    return this;
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
    append(PATH_CUBIC_TO_RELATIVE, c1x, c1y, c2x, c2y, x, y);
    return this;
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
    append(PATH_CUBIC_TO_RELATIVE, c1xy[0], c1xy[1], c2xy[0], c2xy[1], xy[0], xy[1]);
    return this;
  }

  /**
   * Cubic Bezier line to the given relative coordinates.
   * 
   * @param c1xy first control point
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeCubicTo(Vector c1xy, Vector c2xy, Vector xy) {
    append(PATH_CUBIC_TO_RELATIVE, c1xy.get(0), c1xy.get(1), c2xy.get(0), c2xy.get(1), xy.get(0), xy.get(1));
    return this;
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
    append(PATH_SMOOTH_CUBIC_TO, c2x, c2y, x, y);
    return this;
  }

  /**
   * Smooth Cubic Bezier line to the given coordinates.
   * 
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothCubicTo(double[] c2xy, double[] xy) {
    append(PATH_SMOOTH_CUBIC_TO, c2xy[0], c2xy[1], xy[0], xy[1]);
    return this;
  }

  /**
   * Smooth Cubic Bezier line to the given coordinates.
   * 
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothCubicTo(Vector c2xy, Vector xy) {
    append(PATH_SMOOTH_CUBIC_TO, c2xy.get(0), c2xy.get(1), xy.get(0), xy.get(1));
    return this;
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
    append(PATH_SMOOTH_CUBIC_TO_RELATIVE, c2x, c2y, x, y);
    return this;
  }

  /**
   * Smooth Cubic Bezier line to the given relative coordinates.
   * 
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothCubicTo(double[] c2xy, double[] xy) {
    append(PATH_SMOOTH_CUBIC_TO_RELATIVE, c2xy[0], c2xy[1], xy[0], xy[1]);
    return this;
  }

  /**
   * Smooth Cubic Bezier line to the given relative coordinates.
   * 
   * @param c2xy second control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothCubicTo(Vector c2xy, Vector xy) {
    append(PATH_SMOOTH_CUBIC_TO_RELATIVE, c2xy.get(0), c2xy.get(1), xy.get(0), xy.get(1));
    return this;
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
    append(SVGConstants.PATH_QUAD_TO, c1x, c1y, x, y);
    return this;
  }

  /**
   * Quadratic Bezier line to the given coordinates.
   * 
   * @param c1xy first control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath quadTo(double[] c1xy, double[] xy) {
    append(SVGConstants.PATH_QUAD_TO, c1xy[0], c1xy[1], xy[0], xy[1]);
    return this;
  }

  /**
   * Quadratic Bezier line to the given coordinates.
   * 
   * @param c1xy first control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath quadTo(Vector c1xy, Vector xy) {
    append(SVGConstants.PATH_QUAD_TO, c1xy.get(0), c1xy.get(1), xy.get(0), xy.get(1));
    return this;
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
    append(PATH_QUAD_TO_RELATIVE, c1x, c1y, x, y);
    return this;
  }

  /**
   * Quadratic Bezier line to the given relative coordinates.
   * 
   * @param c1xy first control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeQuadTo(double[] c1xy, double[] xy) {
    append(PATH_QUAD_TO_RELATIVE, c1xy[0], c1xy[1], xy[0], xy[1]);
    return this;
  }

  /**
   * Quadratic Bezier line to the given relative coordinates.
   * 
   * @param c1xy first control point
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeQuadTo(Vector c1xy, Vector xy) {
    append(PATH_QUAD_TO_RELATIVE, c1xy.get(0), c1xy.get(1), xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Smooth quadratic Bezier line to the given coordinates.
   * 
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothQuadTo(double x, double y) {
    append(SVGConstants.PATH_SMOOTH_QUAD_TO, x, y);
    return this;
  }

  /**
   * Smooth quadratic Bezier line to the given coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothQuadTo(double[] xy) {
    append(SVGConstants.PATH_SMOOTH_QUAD_TO, xy[0], xy[1]);
    return this;
  }

  /**
   * Smooth quadratic Bezier line to the given coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath smoothQuadTo(Vector xy) {
    append(SVGConstants.PATH_SMOOTH_QUAD_TO, xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Smooth quadratic Bezier line to the given relative coordinates.
   * 
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothQuadTo(double x, double y) {
    append(PATH_SMOOTH_QUAD_TO_RELATIVE, x, y);
    return this;
  }

  /**
   * Smooth quadratic Bezier line to the given relative coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothQuadTo(double[] xy) {
    append(PATH_SMOOTH_QUAD_TO_RELATIVE, xy[0], xy[1]);
    return this;
  }

  /**
   * Smooth quadratic Bezier line to the given relative coordinates.
   * 
   * @param xy new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath relativeSmoothQuadTo(Vector xy) {
    append(PATH_SMOOTH_QUAD_TO_RELATIVE, xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Elliptical arc curve to the given coordinates.
   * 
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle >= 180¬∞
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param x new coordinates
   * @param y new coordinates
   */
  public SVGPath ellipticalArc(double rx, double ry, double ar, double la, double sp, double x, double y) {
    append(SVGConstants.PATH_ARC, rx, ry, ar, la, sp, x, y);
    return this;
  }

  /**
   * Elliptical arc curve to the given coordinates.
   * 
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle >= 180¬∞
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath ellipticalArc(double rx, double ry, double ar, double la, double sp, double[] xy) {
    append(SVGConstants.PATH_ARC, rx, ry, ar, la, sp, xy[0], xy[1]);
    return this;
  }

  /**
   * Elliptical arc curve to the given coordinates.
   * 
   * @param rxy radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle >= 180¬∞
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath ellipticalArc(Vector rxy, double ar, double la, double sp, Vector xy) {
    append(SVGConstants.PATH_ARC, rxy.get(0), rxy.get(1), ar, la, sp, xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Elliptical arc curve to the given relative coordinates.
   * 
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle >= 180¬∞
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param x new coordinates
   * @param y new coordinates
   */
  public SVGPath relativeEllipticalArc(double rx, double ry, double ar, double la, double sp, double x, double y) {
    append(PATH_ARC_RELATIVE, rx, ry, ar, la, sp, x, y);
    return this;
  }

  /**
   * Elliptical arc curve to the given relative coordinates.
   * 
   * @param rx x radius
   * @param ry y radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle >= 180¬∞
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath relativeEllipticalArc(double rx, double ry, double ar, double la, double sp, double[] xy) {
    append(PATH_ARC_RELATIVE, rx, ry, ar, la, sp, xy[0], xy[1]);
    return this;
  }

  /**
   * Elliptical arc curve to the given relative coordinates.
   * 
   * @param rxy radius
   * @param ar x-axis-rotation
   * @param la large arc flag, if angle >= 180¬∞
   * @param sp sweep flag, if arc will be drawn in positive-angle direction
   * @param xy new coordinates
   */
  public SVGPath relativeEllipticalArc(Vector rxy, double ar, double la, double sp, Vector xy) {
    append(PATH_ARC_RELATIVE, rxy.get(0), rxy.get(1), ar, la, sp, xy.get(0), xy.get(1));
    return this;
  }

  /**
   * Append an action to the current path.
   * 
   * @param action Current action
   * @param ds coordinates.
   */
  private void append(String action, double... ds) {
    if(lastaction != action) {
      buf.append(action);
      lastaction = action;
    }
    for(double d : ds) {
      buf.append(SVGUtil.FMT.format(d));
      buf.append(" ");
    }
  }

  /**
   * Close the path.
   * 
   * @return path object, for compact syntax.
   */
  public SVGPath close() {
    if(lastaction != SVGConstants.PATH_CLOSE) {
      buf.append(SVGConstants.PATH_CLOSE);
      lastaction = SVGConstants.PATH_CLOSE;
    }
    return this;
  }

  /**
   * Turn the path buffer into an SVG element.
   * 
   * @param document Document context (= element factory)
   * @return SVG Element
   */
  public Element makeElement(Document document) {
    Element elem = SVGUtil.svgElement(document, SVGConstants.SVG_PATH_TAG);
    elem.setAttribute(SVGConstants.SVG_D_ATTRIBUTE, buf.toString());
    return elem;
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
   * Return the SVG serialization of the path.
   */
  @Override
  public String toString() {
    return buf.toString();
  }
}