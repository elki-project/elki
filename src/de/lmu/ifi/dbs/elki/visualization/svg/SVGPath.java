package de.lmu.ifi.dbs.elki.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Element used for building an SVG path using a string buffer.
 * 
 * @author Erich Schubert
 */
// TODO: add elliptical arc commands
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
   * Draw a line given a series of coordinates.
   * 
   * Helper function that will use "move" for the first point, "lineto" for the remaining.
   * 
   * @param x new coordinates
   * @param y new coordinates
   * @return path object, for compact syntax.
   */
  public SVGPath drawTo(double x, double y) {
    if (lastaction == null) {
      moveTo(x,y);
    } else {
      lineTo(x,y);
    }
    return this;
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