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
    Element rect = SVGUtil.svgElement(document, SVGConstants.SVG_CIRCLE_TAG);
    SVGUtil.setAtt(rect, SVGConstants.SVG_CX_ATTRIBUTE, cx);
    SVGUtil.setAtt(rect, SVGConstants.SVG_CY_ATTRIBUTE, cy);
    SVGUtil.setAtt(rect, SVGConstants.SVG_R_ATTRIBUTE, r);
    return rect;
  }
}
