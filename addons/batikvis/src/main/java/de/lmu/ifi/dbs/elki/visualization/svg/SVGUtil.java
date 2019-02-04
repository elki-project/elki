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

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.text.html.StyleSheet;

import org.apache.batik.dom.events.DOMMouseEvent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGLocatable;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGPoint;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.jafama.DoubleWrapper;
import net.jafama.FastMath;

/**
 * Utility class for SVG processing.
 *
 * Much of the classes are to allow easier attribute setting (conversion to
 * string) and Namespace handling
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @navassoc - create - Element
 */
public final class SVGUtil {
  /**
   * Private constructor. Static methods only.
   */
  private SVGUtil() {
    // Do not use.
  }

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
   * Throbber path.
   */
  final public static String THROBBER_PATH = "M.5,.25 a.25,.25 0 0 1 .1766,.42635 l-.0589 -.0589 a-.1766 -.1766 0 0 0 -.1178,-.2835 z";

  /**
   * Throbber style.
   */
  final public static String THROBBER_STYLE = "fill: #3d7fe6; opacity: .2";

  /**
   * SVG color names conversion.
   */
  final private static Object2IntOpenHashMap<String> SVG_COLOR_NAMES;

  /**
   * Key not found value. Not a reasonable color, fully transparent!
   */
  final private static int NO_VALUE = 0x00123456;

  static {
    // Build a reasonably sized hashmap. Use 0
    SVG_COLOR_NAMES = new Object2IntOpenHashMap<>(90);
    SVG_COLOR_NAMES.defaultReturnValue(NO_VALUE);
    // List taken from SVG specification:
    // http://www.w3.org/TR/SVG/types.html#ColorKeywords
    SVG_COLOR_NAMES.put("aliceblue", 0xFFF0F8FF);
    SVG_COLOR_NAMES.put("antiquewhite", 0xFFFAEBD7);
    SVG_COLOR_NAMES.put("aqua", 0xFF00FFFF);
    SVG_COLOR_NAMES.put("aquamarine", 0xFF7FFFD4);
    SVG_COLOR_NAMES.put("azure", 0xFFF0FFFF);
    SVG_COLOR_NAMES.put("beige", 0xFFF5F5DC);
    SVG_COLOR_NAMES.put("bisque", 0xFFFFE4C4);
    SVG_COLOR_NAMES.put("black", 0xFF000000);
    SVG_COLOR_NAMES.put("blanchedalmond", 0xFFFFEBCD);
    SVG_COLOR_NAMES.put("blue", 0xFF0000FF);
    SVG_COLOR_NAMES.put("blueviolet", 0xFF8A2BE2);
    SVG_COLOR_NAMES.put("brown", 0xFFA52A2A);
    SVG_COLOR_NAMES.put("burlywood", 0xFFDEB887);
    SVG_COLOR_NAMES.put("cadetblue", 0xFF5F9EA0);
    SVG_COLOR_NAMES.put("chartreuse", 0xFF7FFF00);
    SVG_COLOR_NAMES.put("chocolate", 0xFFD2691E);
    SVG_COLOR_NAMES.put("coral", 0xFFFF7F50);
    SVG_COLOR_NAMES.put("cornflowerblue", 0xFF6495ED);
    SVG_COLOR_NAMES.put("cornsilk", 0xFFFFF8DC);
    SVG_COLOR_NAMES.put("crimson", 0xFFDC143C);
    SVG_COLOR_NAMES.put("cyan", 0xFF00FFFF);
    SVG_COLOR_NAMES.put("darkblue", 0xFF00008B);
    SVG_COLOR_NAMES.put("darkcyan", 0xFF008B8B);
    SVG_COLOR_NAMES.put("darkgoldenrod", 0xFFB8860B);
    SVG_COLOR_NAMES.put("darkgray", 0xFFA9A9A9);
    SVG_COLOR_NAMES.put("darkgreen", 0xFF006400);
    SVG_COLOR_NAMES.put("darkgrey", 0xFFA9A9A9);
    SVG_COLOR_NAMES.put("darkkhaki", 0xFFBDB76B);
    SVG_COLOR_NAMES.put("darkmagenta", 0xFF8B008B);
    SVG_COLOR_NAMES.put("darkolivegreen", 0xFF556B2F);
    SVG_COLOR_NAMES.put("darkorange", 0xFFFF8C00);
    SVG_COLOR_NAMES.put("darkorchid", 0xFF9932CC);
    SVG_COLOR_NAMES.put("darkred", 0xFF8B0000);
    SVG_COLOR_NAMES.put("darksalmon", 0xFFE9967A);
    SVG_COLOR_NAMES.put("darkseagreen", 0xFF8FBC8F);
    SVG_COLOR_NAMES.put("darkslateblue", 0xFF483D8B);
    SVG_COLOR_NAMES.put("darkslategray", 0xFF2F4F4F);
    SVG_COLOR_NAMES.put("darkslategrey", 0xFF2F4F4F);
    SVG_COLOR_NAMES.put("darkturquoise", 0xFF00CED1);
    SVG_COLOR_NAMES.put("darkviolet", 0xFF9400D3);
    SVG_COLOR_NAMES.put("deeppink", 0xFFFF1493);
    SVG_COLOR_NAMES.put("deepskyblue", 0xFF00BFFF);
    SVG_COLOR_NAMES.put("dimgray", 0xFF696969);
    SVG_COLOR_NAMES.put("dimgrey", 0xFF696969);
    SVG_COLOR_NAMES.put("dodgerblue", 0xFF1E90FF);
    SVG_COLOR_NAMES.put("firebrick", 0xFFB22222);
    SVG_COLOR_NAMES.put("floralwhite", 0xFFFFFAF0);
    SVG_COLOR_NAMES.put("forestgreen", 0xFF228B22);
    SVG_COLOR_NAMES.put("fuchsia", 0xFFFF00FF);
    SVG_COLOR_NAMES.put("gainsboro", 0xFFDCDCDC);
    SVG_COLOR_NAMES.put("ghostwhite", 0xFFF8F8FF);
    SVG_COLOR_NAMES.put("gold", 0xFFFFD700);
    SVG_COLOR_NAMES.put("goldenrod", 0xFFDAA520);
    SVG_COLOR_NAMES.put("gray", 0xFF808080);
    SVG_COLOR_NAMES.put("grey", 0xFF808080);
    SVG_COLOR_NAMES.put("green", 0xFF008000);
    SVG_COLOR_NAMES.put("greenyellow", 0xFFADFF2F);
    SVG_COLOR_NAMES.put("honeydew", 0xFFF0FFF0);
    SVG_COLOR_NAMES.put("hotpink", 0xFFFF69B4);
    SVG_COLOR_NAMES.put("indianred", 0xFFCD5C5C);
    SVG_COLOR_NAMES.put("indigo", 0xFF4B0082);
    SVG_COLOR_NAMES.put("ivory", 0xFFFFFFF0);
    SVG_COLOR_NAMES.put("khaki", 0xFFF0E68C);
    SVG_COLOR_NAMES.put("lavender", 0xFFE6E6FA);
    SVG_COLOR_NAMES.put("lavenderblush", 0xFFFFF0F5);
    SVG_COLOR_NAMES.put("lawngreen", 0xFF7CFC00);
    SVG_COLOR_NAMES.put("lemonchiffon", 0xFFFFFACD);
    SVG_COLOR_NAMES.put("lightblue", 0xFFADD8E6);
    SVG_COLOR_NAMES.put("lightcoral", 0xFFF08080);
    SVG_COLOR_NAMES.put("lightcyan", 0xFFE0FFFF);
    SVG_COLOR_NAMES.put("lightgoldenrodyellow", 0xFFFAFAD2);
    SVG_COLOR_NAMES.put("lightgray", 0xFFD3D3D3);
    SVG_COLOR_NAMES.put("lightgreen", 0xFF90EE90);
    SVG_COLOR_NAMES.put("lightgrey", 0xFFD3D3D3);
    SVG_COLOR_NAMES.put("lightpink", 0xFFFFB6C1);
    SVG_COLOR_NAMES.put("lightsalmon", 0xFFFFA07A);
    SVG_COLOR_NAMES.put("lightseagreen", 0xFF20B2AA);
    SVG_COLOR_NAMES.put("lightskyblue", 0xFF87CEFA);
    SVG_COLOR_NAMES.put("lightslategray", 0xFF778899);
    SVG_COLOR_NAMES.put("lightslategrey", 0xFF778899);
    SVG_COLOR_NAMES.put("lightsteelblue", 0xFFB0C4DE);
    SVG_COLOR_NAMES.put("lightyellow", 0xFFFFFFE0);
    SVG_COLOR_NAMES.put("lime", 0xFF00FF00);
    SVG_COLOR_NAMES.put("limegreen", 0xFF32CD32);
    SVG_COLOR_NAMES.put("linen", 0xFFFAF0E6);
    SVG_COLOR_NAMES.put("magenta", 0xFFFF00FF);
    SVG_COLOR_NAMES.put("maroon", 0xFF800000);
    SVG_COLOR_NAMES.put("mediumaquamarine", 0xFF66CDAA);
    SVG_COLOR_NAMES.put("mediumblue", 0xFF0000CD);
    SVG_COLOR_NAMES.put("mediumorchid", 0xFFBA55D3);
    SVG_COLOR_NAMES.put("mediumpurple", 0xFF9370DB);
    SVG_COLOR_NAMES.put("mediumseagreen", 0xFF3CB371);
    SVG_COLOR_NAMES.put("mediumslateblue", 0xFF7B68EE);
    SVG_COLOR_NAMES.put("mediumspringgreen", 0xFF00FA9A);
    SVG_COLOR_NAMES.put("mediumturquoise", 0xFF48D1CC);
    SVG_COLOR_NAMES.put("mediumvioletred", 0xFFC71585);
    SVG_COLOR_NAMES.put("midnightblue", 0xFF191970);
    SVG_COLOR_NAMES.put("mintcream", 0xFFF5FFFA);
    SVG_COLOR_NAMES.put("mistyrose", 0xFFFFE4E1);
    SVG_COLOR_NAMES.put("moccasin", 0xFFFFE4B5);
    SVG_COLOR_NAMES.put("navajowhite", 0xFFFFDEAD);
    SVG_COLOR_NAMES.put("navy", 0xFF000080);
    SVG_COLOR_NAMES.put("oldlace", 0xFFFDF5E6);
    SVG_COLOR_NAMES.put("olive", 0xFF808000);
    SVG_COLOR_NAMES.put("olivedrab", 0xFF6B8E23);
    SVG_COLOR_NAMES.put("orange", 0xFFFFA500);
    SVG_COLOR_NAMES.put("orangered", 0xFFFF4500);
    SVG_COLOR_NAMES.put("orchid", 0xFFDA70D6);
    SVG_COLOR_NAMES.put("palegoldenrod", 0xFFEEE8AA);
    SVG_COLOR_NAMES.put("palegreen", 0xFF98FB98);
    SVG_COLOR_NAMES.put("paleturquoise", 0xFFAFEEEE);
    SVG_COLOR_NAMES.put("palevioletred", 0xFFDB7093);
    SVG_COLOR_NAMES.put("papayawhip", 0xFFFFEFD5);
    SVG_COLOR_NAMES.put("peachpuff", 0xFFFFDAB9);
    SVG_COLOR_NAMES.put("peru", 0xFFCD853F);
    SVG_COLOR_NAMES.put("pink", 0xFFFFC0CB);
    SVG_COLOR_NAMES.put("plum", 0xFFDDA0DD);
    SVG_COLOR_NAMES.put("powderblue", 0xFFB0E0E6);
    SVG_COLOR_NAMES.put("purple", 0xFF800080);
    SVG_COLOR_NAMES.put("red", 0xFFFF0000);
    SVG_COLOR_NAMES.put("rosybrown", 0xFFBC8F8F);
    SVG_COLOR_NAMES.put("royalblue", 0xFF4169E1);
    SVG_COLOR_NAMES.put("saddlebrown", 0xFF8B4513);
    SVG_COLOR_NAMES.put("salmon", 0xFFFA8072);
    SVG_COLOR_NAMES.put("sandybrown", 0xFFF4A460);
    SVG_COLOR_NAMES.put("seagreen", 0xFF2E8B57);
    SVG_COLOR_NAMES.put("seashell", 0xFFFFF5EE);
    SVG_COLOR_NAMES.put("sienna", 0xFFA0522D);
    SVG_COLOR_NAMES.put("silver", 0xFFC0C0C0);
    SVG_COLOR_NAMES.put("skyblue", 0xFF87CEEB);
    SVG_COLOR_NAMES.put("slateblue", 0xFF6A5ACD);
    SVG_COLOR_NAMES.put("slategray", 0xFF708090);
    SVG_COLOR_NAMES.put("slategrey", 0xFF708090);
    SVG_COLOR_NAMES.put("snow", 0xFFFFFAFA);
    SVG_COLOR_NAMES.put("springgreen", 0xFF00FF7F);
    SVG_COLOR_NAMES.put("steelblue", 0xFF4682B4);
    SVG_COLOR_NAMES.put("tan", 0xFFD2B48C);
    SVG_COLOR_NAMES.put("teal", 0xFF008080);
    SVG_COLOR_NAMES.put("thistle", 0xFFD8BFD8);
    SVG_COLOR_NAMES.put("tomato", 0xFFFF6347);
    SVG_COLOR_NAMES.put("turquoise", 0xFF40E0D0);
    SVG_COLOR_NAMES.put("violet", 0xFFEE82EE);
    SVG_COLOR_NAMES.put("wheat", 0xFFF5DEB3);
    SVG_COLOR_NAMES.put("white", 0xFFFFFFFF);
    SVG_COLOR_NAMES.put("whitesmoke", 0xFFF5F5F5);
    SVG_COLOR_NAMES.put("yellow", 0xFFFFFF00);
    SVG_COLOR_NAMES.put("yellowgreen", 0xFF9ACD32);
    // Nonstandard:
    SVG_COLOR_NAMES.put("transparent", 0xFFFFFFFF);
  }

  /**
   * CSS Stylesheet from Javax, to parse color values.
   */
  private static final StyleSheet colorLookupStylesheet = new StyleSheet();

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
   * Set the CSS class of an Element. See also {@link #addCSSClass} and
   * {@link #removeCSSClass}.
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
    if(classes.length == 1) {
      if(cssclass.equals(classes[0])) {
        e.removeAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE);
      }
    }
    else if(classes.length == 2) {
      if(cssclass.equals(classes[0])) {
        if(cssclass.equals(classes[1])) {
          e.removeAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE);
        }
        else {
          e.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, classes[1]);
        }
      }
      else if(cssclass.equals(classes[1])) {
        e.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, classes[0]);
      }
    }
    else {
      StringBuilder joined = new StringBuilder();
      for(String c : classes) {
        if(!c.equals(cssclass)) {
          if(joined.length() > 0) {
            joined.append(' ');
          }
          joined.append(c);
        }
      }
      e.setAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE, joined.toString());
    }
  }

  /**
   * Make a new CSS style element for the given Document.
   *
   * @param document document (factory)
   * @return new CSS style element.
   */
  public static Element makeStyleElement(Document document) {
    Element style = SVGUtil.svgElement(document, SVGConstants.SVG_STYLE_TAG);
    style.setAttribute(SVGConstants.SVG_TYPE_ATTRIBUTE, SVGConstants.CSS_MIME_TYPE);
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
   * Draw a simple "please wait" icon (in-progress) as placeholder for running
   * renderings.
   *
   * @param document Document.
   * @param x Left
   * @param y Top
   * @param w Width
   * @param h Height
   * @return New element (currently a {@link SVGConstants#SVG_PATH_TAG})
   */
  public static Element svgWaitIcon(Document document, double x, double y, double w, double h) {
    Element g = SVGUtil.svgElement(document, SVGConstants.SVG_G_TAG);
    setAtt(g, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + x + " " + y + ") scale(" + w + " " + h + ")");
    Element thro = SVGUtil.svgElement(document, SVGConstants.SVG_PATH_TAG);
    setAtt(thro, SVGConstants.SVG_D_ATTRIBUTE, THROBBER_PATH);
    setStyle(thro, THROBBER_STYLE);
    Element anim = SVGUtil.svgElement(document, SVGConstants.SVG_ANIMATE_TRANSFORM_TAG);
    setAtt(anim, SVGConstants.SVG_ATTRIBUTE_NAME_ATTRIBUTE, SVGConstants.SVG_TRANSFORM_ATTRIBUTE);
    setAtt(anim, SVGConstants.SVG_ATTRIBUTE_TYPE_ATTRIBUTE, "XML");
    setAtt(anim, SVGConstants.SVG_TYPE_ATTRIBUTE, SVGConstants.SVG_ROTATE_ATTRIBUTE);
    setAtt(anim, SVGConstants.SVG_FROM_ATTRIBUTE, "0 .5 .5");
    setAtt(anim, SVGConstants.SVG_TO_ATTRIBUTE, "360 .5 .5");
    setAtt(anim, SVGConstants.SVG_BEGIN_ATTRIBUTE, fmt(Math.random() * 2) + "s");
    setAtt(anim, SVGConstants.SVG_DUR_ATTRIBUTE, "2s");
    setAtt(anim, SVGConstants.SVG_REPEAT_COUNT_ATTRIBUTE, "indefinite");
    setAtt(anim, SVGConstants.SVG_FILL_ATTRIBUTE, "freeze");
    thro.appendChild(anim);
    g.appendChild(thro);
    return g;
  }

  /**
   * Convert a color name from SVG syntax to an AWT color object.
   *
   * @param str Color name
   * @return Color value
   */
  public static Color stringToColor(String str) {
    int icol = SVG_COLOR_NAMES.getInt(str.toLowerCase());
    if(icol != NO_VALUE) {
      return new Color(icol, false);
    }
    return colorLookupStylesheet.stringToColor(str);
  }

  /**
   * Convert a color name from an AWT color object to CSS syntax
   *
   * Note: currently only RGB (from ARGB order) are supported.
   *
   * @param col Color value
   * @return Color string
   */
  public static String colorToString(Color col) {
    return colorToString(col.getRGB());
  }

  /**
   * Convert a color name from an integer RGB color to CSS syntax
   *
   * Note: currently only RGB (from ARGB order) are supported. The alpha channel
   * will be ignored.
   *
   * @param col Color value
   * @return Color string
   */
  public static String colorToString(int col) {
    final char[] buf = new char[] { '#', 'X', 'X', 'X', 'X', 'X', 'X' };
    for(int i = 6; i > 0; i--) {
      final int v = (col & 0xF);
      buf[i] = (char) ((v < 10) ? ('0' + v) : ('a' + v - 10));
      col >>>= 4;
    }
    return new String(buf);
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
    double offx = (scale * owidth - swidth) * .5 + lmargin;
    double offy = (scale * oheight - sheight) * .5 + tmargin;
    return "scale(" + fmt(1 / scale) + ") translate(" + fmt(offx) + " " + fmt(offy) + ")";
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

  /**
   * Convert the coordinates of an DOM Event from screen into element
   * coordinates.
   *
   * @param doc Document context
   * @param tag Element containing the coordinate system
   * @param evt Event to interpret
   * @return coordinates
   */
  public static SVGPoint elementCoordinatesFromEvent(Document doc, Element tag, Event evt) {
    try {
      DOMMouseEvent gnme = (DOMMouseEvent) evt;
      SVGMatrix mat = ((SVGLocatable) tag).getScreenCTM();
      SVGMatrix imat = mat.inverse();
      SVGPoint cPt = ((SVGDocument) doc).getRootElement().createSVGPoint();
      cPt.setX(gnme.getClientX());
      cPt.setY(gnme.getClientY());
      return cPt.matrixTransform(imat);
    }
    catch(Exception e) {
      LoggingUtil.warning("Error getting coordinates from SVG event.", e);
      return null;
    }
  }

  /**
   * Remove last child of an element, when present
   *
   * @param tag Parent
   */
  public static void removeLastChild(Element tag) {
    final Node last = tag.getLastChild();
    if(last != null) {
      tag.removeChild(last);
    }
  }

  /**
   * Remove an element from its parent, if defined.
   *
   * @param elem Element to remove
   */
  public static void removeFromParent(Element elem) {
    if(elem != null && elem.getParentNode() != null) {
      elem.getParentNode().removeChild(elem);
    }
  }

  /**
   * Create a circle segment.
   *
   * @param svgp Plot to draw to
   * @param centerx Center X position
   * @param centery Center Y position
   * @param angleStart Starting angle
   * @param angleDelta Angle delta
   * @param innerRadius inner radius
   * @param outerRadius outer radius
   * @return SVG element representing this circle segment
   */
  public static Element svgCircleSegment(SVGPlot svgp, double centerx, double centery, double angleStart, double angleDelta, double innerRadius, double outerRadius) {
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    double sin1st = FastMath.sinAndCos(angleStart, tmp);
    double cos1st = tmp.value;

    double sin2nd = FastMath.sinAndCos(angleStart + angleDelta, tmp);
    double cos2nd = tmp.value; // Note: tmp is modified!

    double inner1stx = centerx + (innerRadius * sin1st);
    double inner1sty = centery - (innerRadius * cos1st);
    double outer1stx = centerx + (outerRadius * sin1st);
    double outer1sty = centery - (outerRadius * cos1st);

    double inner2ndx = centerx + (innerRadius * sin2nd);
    double inner2ndy = centery - (innerRadius * cos2nd);
    double outer2ndx = centerx + (outerRadius * sin2nd);
    double outer2ndy = centery - (outerRadius * cos2nd);

    double largeArc = angleDelta >= Math.PI ? 1 : 0;
    SVGPath path = new SVGPath(inner1stx, inner1sty).lineTo(outer1stx, outer1sty) //
        .ellipticalArc(outerRadius, outerRadius, 0, largeArc, 1, outer2ndx, outer2ndy) //
        .lineTo(inner2ndx, inner2ndy);
    if(innerRadius > 0) {
      path.ellipticalArc(innerRadius, innerRadius, 0, largeArc, 0, inner1stx, inner1sty);
    }

    return path.makeElement(svgp);
  }
}
