package de.lmu.ifi.dbs.elki.visualization.svg;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility class for SVG processing.
 * 
 * Much of the classes are to allow easier attribute setting (conversion to
 * string) and Namespace handling
 * 
 * @author Erich Schubert
 */
public final class SVGUtil {
  /**
   * Formatter to output numbers in a valid SVG number format.
   */
  public static final NumberFormat FMT = NumberFormat.getInstance(Locale.ROOT);

  static {
    FMT.setMaximumFractionDigits(8);
  }

  /**
   * Hourglass object.
   */
  final public static String HOURGLASS_PATH = "M.35 .2 L.65 .2 L.65 .3 L.35 .7 L.35 .8 L.65 .8 L.65 .7 L.35 .3 Z";
  
  /**
   * Hourglass style.
   */
  final public static String HOURGLASS_STYLE = "stroke: black; stroke-width: .01; fill: grey; opacity: .2";

  /**
   * Format a double according to the SVG specs.
   * 
   * @param x number to format
   * @return String representation
   */
  public static String fmt(double x) {
    return FMT.format(x);
  }

  /**
   * Create a SVG element in appropriate namespace
   * 
   * @param document containing document
   * @param name node name
   * @return new SVG element.
   */
  public static Element svgElement(Document document, String name) {
    return document.createElementNS(SVGConstants.SVG_NAMESPACE_URI, name);
  }

  /**
   * Set a SVG attribute
   * 
   * @param el element
   * @param name attribute name
   * @param d double value
   */
  public static void setAtt(Element el, String name, double d) {
    el.setAttribute(name, fmt(d));
  }

  /**
   * Set a SVG attribute
   * 
   * @param el element
   * @param name attribute name
   * @param d integer value
   */
  public static void setAtt(Element el, String name, int d) {
    el.setAttribute(name, Integer.toString(d));
  }

  /**
   * Set a SVG attribute
   * 
   * @param el element
   * @param name attribute name
   * @param d string value
   */
  public static void setAtt(Element el, String name, String d) {
    el.setAttribute(name, d);
  }

  /**
   * Set a SVG style attribute
   * 
   * @param el element
   * @param d style value
   */
  public static void setStyle(Element el, String d) {
    el.setAttribute(SVGConstants.SVG_STYLE_ATTRIBUTE, d);
  }

  /**
   * Set the CSS class of an Element. See also {@link #addCSSClass} and {@link #removeCSSClass}.
   * 
   * @param e Element
   * @param cssclass class to set.
   */
  public static void setCSSClass(Element e, String cssclass) {
    setAtt(e, SVGConstants.SVG_CLASS_ATTRIBUTE, cssclass);
  }

  /**
   * Add a CSS class to an Element.
   * 
   * @param e Element
   * @param cssclass class to add.
   */
  public static void addCSSClass(Element e, String cssclass) {
    String oldval = e.getAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE);
    if(oldval == null || oldval.length() == 0) {
      setAtt(e, SVGConstants.SVG_CLASS_ATTRIBUTE, cssclass);
      return;
    }
    String[] classes = oldval.split(" ");
    for(String c : classes) {
      if(c.equals(cssclass)) {
        return;
      }
    }
    setAtt(e, SVGConstants.SVG_CLASS_ATTRIBUTE, oldval + " " + cssclass);
  }

  /**
   * Remove a CSS class from an Element.
   * 
   * @param e Element
   * @param cssclass class to remove.
   */
  public static void removeCSSClass(Element e, String cssclass) {
    String oldval = e.getAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE);
    if(oldval == null) {
      return;
    }
    String[] classes = oldval.split(" ");
    String joined = "";
    for(String c : classes) {
      if(!c.equals(cssclass)) {
        if(joined.length() > 0) {
          joined = joined + " " + c;
        }
        else {
          joined = c;
        }
      }
    }
    SVGUtil.setAtt(e, SVGConstants.SVG_CLASS_ATTRIBUTE, joined);
  }

  /**
   * Make a new CSS style element for the given Document.
   * 
   * @param document document (factory)
   * @return new CSS style element.
   */
  public static Element makeStyleElement(Document document) {
    Element style = SVGUtil.svgElement(document, SVGConstants.SVG_STYLE_TAG);
    SVGUtil.setAtt(style, SVGConstants.SVG_TYPE_ATTRIBUTE, SVGConstants.CSS_MIME_TYPE);
    return style;
  }

  /**
   * Create a SVG rectangle element.
   * 
   * @param document document to create in (factory)
   * @param x X coordinate
   * @param y Y coordinate
   * @param w Width
   * @param h Height
   * @return new element
   */
  public static Element svgRect(Document document, double x, double y, double w, double h) {
    Element rect = SVGUtil.svgElement(document, SVGConstants.SVG_RECT_TAG);
    SVGUtil.setAtt(rect, SVGConstants.SVG_X_ATTRIBUTE, x);
    SVGUtil.setAtt(rect, SVGConstants.SVG_Y_ATTRIBUTE, y);
    SVGUtil.setAtt(rect, SVGConstants.SVG_WIDTH_ATTRIBUTE, w);
    SVGUtil.setAtt(rect, SVGConstants.SVG_HEIGHT_ATTRIBUTE, h);
    return rect;
  }

  /**
   * Create a SVG circle element.
   * 
   * @param document document to create in (factory)
   * @param cx center X
   * @param cy center Y
   * @param r radius
   * @return new element
   */
  public static Element svgCircle(Document document, double cx, double cy, double r) {
    Element circ = SVGUtil.svgElement(document, SVGConstants.SVG_CIRCLE_TAG);
    SVGUtil.setAtt(circ, SVGConstants.SVG_CX_ATTRIBUTE, cx);
    SVGUtil.setAtt(circ, SVGConstants.SVG_CY_ATTRIBUTE, cy);
    SVGUtil.setAtt(circ, SVGConstants.SVG_R_ATTRIBUTE, r);
    return circ;
  }
  
  /**
   * Create a SVG line element. Do not confuse this with path elements.
   * 
   * @param document document to create in (factory)
   * @param x1 first point x
   * @param y1 first point y
   * @param x2 second point x
   * @param y2 second point y
   * @return new element
   */
  public static Element svgLine(Document document, double x1, double y1, double x2, double y2) {
    Element line = SVGUtil.svgElement(document, SVGConstants.SVG_LINE_TAG);
    SVGUtil.setAtt(line, SVGConstants.SVG_X1_ATTRIBUTE, x1);
    SVGUtil.setAtt(line, SVGConstants.SVG_Y1_ATTRIBUTE, y1);
    SVGUtil.setAtt(line, SVGConstants.SVG_X2_ATTRIBUTE, x2);
    SVGUtil.setAtt(line, SVGConstants.SVG_Y2_ATTRIBUTE, y2);
    return line;
  }
  
  /**
   * Create a SVG text element.
   * 
   * @param document document to create in (factory)
   * @param x first point x
   * @param y first point y
   * @param text Content of text element.
   * @return New text element.
   */
  public static Element svgText(Document document, double x, double y, String text) {
    Element elem = SVGUtil.svgElement(document, SVGConstants.SVG_TEXT_TAG);
    SVGUtil.setAtt(elem, SVGConstants.SVG_X_ATTRIBUTE, x);
    SVGUtil.setAtt(elem, SVGConstants.SVG_Y_ATTRIBUTE, y);
    elem.setTextContent(text);
    return elem;
  }
  
  /**
   * Draw a simple "please wait" icon (in-progress) as placeholder for running renderings.
   * 
   * @param document Document.
   * @param x Left
   * @param y Top
   * @param w Width
   * @param h Height
   * @return New element (currently a {@link SVGConstants#SVG_PATH_TAG})
   */
  public static Element svgWaitIcon(Document document, double x, double y, double w, double h) {
    Element elem = SVGUtil.svgElement(document, SVGConstants.SVG_PATH_TAG);
    setAtt(elem, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate("+x+" "+y+") scale("+w+" "+h+")");
    setAtt(elem, SVGConstants.SVG_D_ATTRIBUTE, HOURGLASS_PATH);
    setStyle(elem, HOURGLASS_STYLE);
    return elem;
  }
}
