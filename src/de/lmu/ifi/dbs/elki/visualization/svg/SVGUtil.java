package de.lmu.ifi.dbs.elki.visualization.svg;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.text.html.StyleSheet;

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
    FMT.setMaximumFractionDigits(10);
    FMT.setGroupingUsed(false);
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
   * SVG color names conversion.
   */
  final private static HashMap<String, Integer> SVG_COLOR_NAMES = new HashMap<String,Integer>();
  
  static {
    // List taken from SVG specification: http://www.w3.org/TR/SVG/types.html#ColorKeywords
    SVG_COLOR_NAMES.put("aliceblue",0xFFF0F8FF);
    SVG_COLOR_NAMES.put("antiquewhite",0xFFFAEBD7);
    SVG_COLOR_NAMES.put("aqua",0xFF00FFFF);
    SVG_COLOR_NAMES.put("aquamarine",0xFF7FFFD4);
    SVG_COLOR_NAMES.put("azure",0xFFF0FFFF);
    SVG_COLOR_NAMES.put("beige",0xFFF5F5DC);
    SVG_COLOR_NAMES.put("bisque",0xFFFFE4C4);
    SVG_COLOR_NAMES.put("black",0xFF000000);
    SVG_COLOR_NAMES.put("blanchedalmond",0xFFFFEBCD);
    SVG_COLOR_NAMES.put("blue",0xFF0000FF);
    SVG_COLOR_NAMES.put("blueviolet",0xFF8A2BE2);
    SVG_COLOR_NAMES.put("brown",0xFFA52A2A);
    SVG_COLOR_NAMES.put("burlywood",0xFFDEB887);
    SVG_COLOR_NAMES.put("cadetblue",0xFF5F9EA0);
    SVG_COLOR_NAMES.put("chartreuse",0xFF7FFF00);
    SVG_COLOR_NAMES.put("chocolate",0xFFD2691E);
    SVG_COLOR_NAMES.put("coral",0xFFFF7F50);
    SVG_COLOR_NAMES.put("cornflowerblue",0xFF6495ED);
    SVG_COLOR_NAMES.put("cornsilk",0xFFFFF8DC);
    SVG_COLOR_NAMES.put("crimson",0xFFDC143C);
    SVG_COLOR_NAMES.put("cyan",0xFF00FFFF);
    SVG_COLOR_NAMES.put("darkblue",0xFF00008B);
    SVG_COLOR_NAMES.put("darkcyan",0xFF008B8B);
    SVG_COLOR_NAMES.put("darkgoldenrod",0xFFB8860B);
    SVG_COLOR_NAMES.put("darkgray",0xFFA9A9A9);
    SVG_COLOR_NAMES.put("darkgreen",0xFF006400);
    SVG_COLOR_NAMES.put("darkgrey",0xFFA9A9A9);
    SVG_COLOR_NAMES.put("darkkhaki",0xFFBDB76B);
    SVG_COLOR_NAMES.put("darkmagenta",0xFF8B008B);
    SVG_COLOR_NAMES.put("darkolivegreen",0xFF556B2F);
    SVG_COLOR_NAMES.put("darkorange",0xFFFF8C00);
    SVG_COLOR_NAMES.put("darkorchid",0xFF9932CC);
    SVG_COLOR_NAMES.put("darkred",0xFF8B0000);
    SVG_COLOR_NAMES.put("darksalmon",0xFFE9967A);
    SVG_COLOR_NAMES.put("darkseagreen",0xFF8FBC8F);
    SVG_COLOR_NAMES.put("darkslateblue",0xFF483D8B);
    SVG_COLOR_NAMES.put("darkslategray",0xFF2F4F4F);
    SVG_COLOR_NAMES.put("darkslategrey",0xFF2F4F4F);
    SVG_COLOR_NAMES.put("darkturquoise",0xFF00CED1);
    SVG_COLOR_NAMES.put("darkviolet",0xFF9400D3);
    SVG_COLOR_NAMES.put("deeppink",0xFFFF1493);
    SVG_COLOR_NAMES.put("deepskyblue",0xFF00BFFF);
    SVG_COLOR_NAMES.put("dimgray",0xFF696969);
    SVG_COLOR_NAMES.put("dimgrey",0xFF696969);
    SVG_COLOR_NAMES.put("dodgerblue",0xFF1E90FF);
    SVG_COLOR_NAMES.put("firebrick",0xFFB22222);
    SVG_COLOR_NAMES.put("floralwhite",0xFFFFFAF0);
    SVG_COLOR_NAMES.put("forestgreen",0xFF228B22);
    SVG_COLOR_NAMES.put("fuchsia",0xFFFF00FF);
    SVG_COLOR_NAMES.put("gainsboro",0xFFDCDCDC);
    SVG_COLOR_NAMES.put("ghostwhite",0xFFF8F8FF);
    SVG_COLOR_NAMES.put("gold",0xFFFFD700);
    SVG_COLOR_NAMES.put("goldenrod",0xFFDAA520);
    SVG_COLOR_NAMES.put("gray",0xFF808080);
    SVG_COLOR_NAMES.put("grey",0xFF808080);
    SVG_COLOR_NAMES.put("green",0xFF008000);
    SVG_COLOR_NAMES.put("greenyellow",0xFFADFF2F);
    SVG_COLOR_NAMES.put("honeydew",0xFFF0FFF0);
    SVG_COLOR_NAMES.put("hotpink",0xFFFF69B4);
    SVG_COLOR_NAMES.put("indianred",0xFFCD5C5C);
    SVG_COLOR_NAMES.put("indigo",0xFF4B0082);
    SVG_COLOR_NAMES.put("ivory",0xFFFFFFF0);
    SVG_COLOR_NAMES.put("khaki",0xFFF0E68C);
    SVG_COLOR_NAMES.put("lavender",0xFFE6E6FA);
    SVG_COLOR_NAMES.put("lavenderblush",0xFFFFF0F5);
    SVG_COLOR_NAMES.put("lawngreen",0xFF7CFC00);
    SVG_COLOR_NAMES.put("lemonchiffon",0xFFFFFACD);
    SVG_COLOR_NAMES.put("lightblue",0xFFADD8E6);
    SVG_COLOR_NAMES.put("lightcoral",0xFFF08080);
    SVG_COLOR_NAMES.put("lightcyan",0xFFE0FFFF);
    SVG_COLOR_NAMES.put("lightgoldenrodyellow",0xFFFAFAD2);
    SVG_COLOR_NAMES.put("lightgray",0xFFD3D3D3);
    SVG_COLOR_NAMES.put("lightgreen",0xFF90EE90);
    SVG_COLOR_NAMES.put("lightgrey",0xFFD3D3D3);
    SVG_COLOR_NAMES.put("lightpink",0xFFFFB6C1);
    SVG_COLOR_NAMES.put("lightsalmon",0xFFFFA07A);
    SVG_COLOR_NAMES.put("lightseagreen",0xFF20B2AA);
    SVG_COLOR_NAMES.put("lightskyblue",0xFF87CEFA);
    SVG_COLOR_NAMES.put("lightslategray",0xFF778899);
    SVG_COLOR_NAMES.put("lightslategrey",0xFF778899);
    SVG_COLOR_NAMES.put("lightsteelblue",0xFFB0C4DE);
    SVG_COLOR_NAMES.put("lightyellow",0xFFFFFFE0);
    SVG_COLOR_NAMES.put("lime",0xFF00FF00);
    SVG_COLOR_NAMES.put("limegreen",0xFF32CD32);
    SVG_COLOR_NAMES.put("linen",0xFFFAF0E6);
    SVG_COLOR_NAMES.put("magenta",0xFFFF00FF);
    SVG_COLOR_NAMES.put("maroon",0xFF800000);
    SVG_COLOR_NAMES.put("mediumaquamarine",0xFF66CDAA);
    SVG_COLOR_NAMES.put("mediumblue",0xFF0000CD);
    SVG_COLOR_NAMES.put("mediumorchid",0xFFBA55D3);
    SVG_COLOR_NAMES.put("mediumpurple",0xFF9370DB);
    SVG_COLOR_NAMES.put("mediumseagreen",0xFF3CB371);
    SVG_COLOR_NAMES.put("mediumslateblue",0xFF7B68EE);
    SVG_COLOR_NAMES.put("mediumspringgreen",0xFF00FA9A);
    SVG_COLOR_NAMES.put("mediumturquoise",0xFF48D1CC);
    SVG_COLOR_NAMES.put("mediumvioletred",0xFFC71585);
    SVG_COLOR_NAMES.put("midnightblue",0xFF191970);
    SVG_COLOR_NAMES.put("mintcream",0xFFF5FFFA);
    SVG_COLOR_NAMES.put("mistyrose",0xFFFFE4E1);
    SVG_COLOR_NAMES.put("moccasin",0xFFFFE4B5);
    SVG_COLOR_NAMES.put("navajowhite",0xFFFFDEAD);
    SVG_COLOR_NAMES.put("navy",0xFF000080);
    SVG_COLOR_NAMES.put("oldlace",0xFFFDF5E6);
    SVG_COLOR_NAMES.put("olive",0xFF808000);
    SVG_COLOR_NAMES.put("olivedrab",0xFF6B8E23);
    SVG_COLOR_NAMES.put("orange",0xFFFFA500);
    SVG_COLOR_NAMES.put("orangered",0xFFFF4500);
    SVG_COLOR_NAMES.put("orchid",0xFFDA70D6);
    SVG_COLOR_NAMES.put("palegoldenrod",0xFFEEE8AA);
    SVG_COLOR_NAMES.put("palegreen",0xFF98FB98);
    SVG_COLOR_NAMES.put("paleturquoise",0xFFAFEEEE);
    SVG_COLOR_NAMES.put("palevioletred",0xFFDB7093);
    SVG_COLOR_NAMES.put("papayawhip",0xFFFFEFD5);
    SVG_COLOR_NAMES.put("peachpuff",0xFFFFDAB9);
    SVG_COLOR_NAMES.put("peru",0xFFCD853F);
    SVG_COLOR_NAMES.put("pink",0xFFFFC0CB);
    SVG_COLOR_NAMES.put("plum",0xFFDDA0DD);
    SVG_COLOR_NAMES.put("powderblue",0xFFB0E0E6);
    SVG_COLOR_NAMES.put("purple",0xFF800080);
    SVG_COLOR_NAMES.put("red",0xFFFF0000);
    SVG_COLOR_NAMES.put("rosybrown",0xFFBC8F8F);
    SVG_COLOR_NAMES.put("royalblue",0xFF4169E1);
    SVG_COLOR_NAMES.put("saddlebrown",0xFF8B4513);
    SVG_COLOR_NAMES.put("salmon",0xFFFA8072);
    SVG_COLOR_NAMES.put("sandybrown",0xFFF4A460);
    SVG_COLOR_NAMES.put("seagreen",0xFF2E8B57);
    SVG_COLOR_NAMES.put("seashell",0xFFFFF5EE);
    SVG_COLOR_NAMES.put("sienna",0xFFA0522D);
    SVG_COLOR_NAMES.put("silver",0xFFC0C0C0);
    SVG_COLOR_NAMES.put("skyblue",0xFF87CEEB);
    SVG_COLOR_NAMES.put("slateblue",0xFF6A5ACD);
    SVG_COLOR_NAMES.put("slategray",0xFF708090);
    SVG_COLOR_NAMES.put("slategrey",0xFF708090);
    SVG_COLOR_NAMES.put("snow",0xFFFFFAFA);
    SVG_COLOR_NAMES.put("springgreen",0xFF00FF7F);
    SVG_COLOR_NAMES.put("steelblue",0xFF4682B4);
    SVG_COLOR_NAMES.put("tan",0xFFD2B48C);
    SVG_COLOR_NAMES.put("teal",0xFF008080);
    SVG_COLOR_NAMES.put("thistle",0xFFD8BFD8);
    SVG_COLOR_NAMES.put("tomato",0xFFFF6347);
    SVG_COLOR_NAMES.put("turquoise",0xFF40E0D0);
    SVG_COLOR_NAMES.put("violet",0xFFEE82EE);
    SVG_COLOR_NAMES.put("wheat",0xFFF5DEB3);
    SVG_COLOR_NAMES.put("white",0xFFFFFFFF);
    SVG_COLOR_NAMES.put("whitesmoke",0xFFF5F5F5);
    SVG_COLOR_NAMES.put("yellow",0xFFFFFF00);
    SVG_COLOR_NAMES.put("yellowgreen",0xFF9ACD32);
  }
  /**
   * CSS Stylesheet from Javax, to parse color values.
   */
  private final static StyleSheet colorLookupStylesheet = new StyleSheet();
  
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
  
  /**
   * Convert a color name from SVG syntax to an AWT color object.
   * 
   * @param str Color name
   * @return Color value
   */
  public static Color stringToColor(String str) {
    Integer icol = SVG_COLOR_NAMES.get(str.toLowerCase());
    if (icol != null) {
      return new Color(icol, false);
    }
    return colorLookupStylesheet.stringToColor(str);
  }
  
  /**
   * Make a transform string to add margins
   * 
   * @param owidth Width of outer (embedding) canvas
   * @param oheight Height of outer (embedding) canvas
   * @param iwidth Width of inner (embedded) canvas
   * @param iheight Height of inner (embedded) canvas
   * @param lmargin Left margin (in inner canvas' units)
   * @param tmargin Top margin (in inner canvas' units)
   * @param rmargin Right margin (in inner canvas' units)
   * @param bmargin Bottom margin (in inner canvas' units)
   * @return Transform string
   */
  public static String makeMarginTransform(double owidth, double oheight, double iwidth, double iheight, double lmargin, double tmargin, double rmargin, double bmargin) {
    double swidth = iwidth + lmargin + rmargin;
    double sheight = iheight + tmargin + bmargin;
    double scale = Math.max(swidth / owidth, sheight / oheight);
    double offx = (scale * owidth - swidth) / 2 + lmargin;
    double offy = (scale * oheight - sheight) / 2 + tmargin;
    return "scale("+fmt(1 / scale)+") translate("+fmt(offx)+" "+fmt(offy)+")";
  }

  /**
   * Make a transform string to add margins
   * 
   * @param owidth Width of outer (embedding) canvas
   * @param oheight Height of outer (embedding) canvas
   * @param iwidth Width of inner (embedded) canvas
   * @param iheight Height of inner (embedded) canvas
   * @param xmargin Left and right margin (in inner canvas' units)
   * @param ymargin Top and bottom margin (in inner canvas' units)
   * @return Transform string
   */
  public static String makeMarginTransform(double owidth, double oheight, double iwidth, double iheight, double xmargin, double ymargin) {
    return makeMarginTransform(owidth, oheight, iwidth, iheight, xmargin, ymargin, xmargin, ymargin);
  }

  /**
   * Make a transform string to add margins
   * 
   * @param owidth Width of outer (embedding) canvas
   * @param oheight Height of outer (embedding) canvas
   * @param iwidth Width of inner (embedded) canvas
   * @param iheight Height of inner (embedded) canvas
   * @param margin Margin (in inner canvas' units)
   * @return Transform string
   */
  public static String makeMarginTransform(double owidth, double oheight, double iwidth, double iheight, double margin) {
    return makeMarginTransform(owidth, oheight, iwidth, iheight, margin, margin, margin, margin);
  }
}