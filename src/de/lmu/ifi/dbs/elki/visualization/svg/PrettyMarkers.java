package de.lmu.ifi.dbs.elki.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;

/**
 * Marker library achieving a larger number of styles by combining different
 * shapes with different colors. Uses object ID management by SVGPlot.
 * 
 * @author Erich Schubert
 */
public class PrettyMarkers implements MarkerLibrary {
  /**
   * Color library
   */
  private ColorLibrary colors;

  /**
   * Default prefix to use.
   */
  private final static String DEFAULT_PREFIX = "s";

  /**
   * Prefix for the IDs generated.
   */
  private String prefix;

  /**
   * Constructor
   * 
   * @param prefix prefix to use.
   * @param style style library to use
   */
  public PrettyMarkers(String prefix, StyleLibrary style) {
    this.prefix = prefix;
    this.colors = style.getColorSet(StyleLibrary.PLOT);
  }

  /**
   * Constructor without prefix argument, will use {@link #DEFAULT_PREFIX} as
   * prefix.
   * 
   * @param style Style library to use
   */
  public PrettyMarkers(StyleLibrary style) {
    this(DEFAULT_PREFIX, style);
  }

  /**
   * Constructor without a a {@link ColorLibrary}, will use a default
   * {@link PropertiesBasedStyleLibrary} as style library.
   * 
   * @param prefix prefix to use.
   */
  public PrettyMarkers(String prefix) {
    this(prefix, new PropertiesBasedStyleLibrary());
  }

  /**
   * Constructor without arguments, will use {@link #DEFAULT_PREFIX} as prefix
   * and a default {@link PropertiesBasedStyleLibrary} as style library.
   */
  public PrettyMarkers() {
    this(DEFAULT_PREFIX, new PropertiesBasedStyleLibrary());
  }

  /**
   * Draw an marker used in scatter plots. If you intend to use the markers
   * multiple times, you should consider using the {@link #useMarker} method
   * instead, which exploits the SVG features of symbol definition and use
   * 
   * @param plot containing plot
   * @param parent parent node
   * @param x position
   * @param y position
   * @param style marker style (enumerated)
   * @param size size
   */
  public void plotMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    assert (parent != null);
    // TODO: add more styles.
    String colorstr = colors.getColor(style);
    String strokestyle = SVGConstants.CSS_STROKE_PROPERTY + ":" + colorstr + ";" + SVGConstants.CSS_STROKE_WIDTH_PROPERTY + ":" + SVGUtil.fmt(size / 6);

    switch(style % 8){
    case 0: {
      // + cross
      Element line1 = plot.svgLine(x, y - size / 2, x, y + size / 2);
      SVGUtil.setStyle(line1, strokestyle);
      parent.appendChild(line1);
      Element line2 = plot.svgLine(x - size / 2, y, x + size / 2, y);
      SVGUtil.setStyle(line2, strokestyle);
      parent.appendChild(line2);
      break;
    }
    case 1: {
      // X cross
      Element line1 = plot.svgLine(x - size / 2.828427, y - size / 2.828427, x + size / 2.828427, y + size / 2.828427);
      SVGUtil.setStyle(line1, strokestyle);
      parent.appendChild(line1);
      Element line2 = plot.svgLine(x - size / 2.828427, y + size / 2.828427, x + size / 2.828427, y - size / 2.828427);
      SVGUtil.setStyle(line2, strokestyle);
      parent.appendChild(line2);
      break;
    }
    case 2: {
      // O filled circle
      Element circ = plot.svgCircle(x, y, size / 2);
      SVGUtil.setStyle(circ, SVGConstants.CSS_FILL_PROPERTY + ":" + colorstr);
      parent.appendChild(circ);
      break;
    }
    case 3: {
      // [] filled rectangle
      Element rect = plot.svgRect(x - size / 2, y - size / 2, size, size);
      SVGUtil.setStyle(rect, "fill:" + colorstr);
      parent.appendChild(rect);
      break;
    }
    case 4: {
      // <> filled diamond
      Element rect = plot.svgRect(x - size / 2, y - size / 2, size, size);
      SVGUtil.setStyle(rect, "fill:" + colorstr);
      SVGUtil.setAtt(rect, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(45," + SVGUtil.fmt(x) + "," + SVGUtil.fmt(y) + ")");
      parent.appendChild(rect);
      break;
    }
    case 5: {
      // O hollow circle
      Element circ = plot.svgCircle(x, y, size / 2);
      SVGUtil.setStyle(circ, "fill: none;" + strokestyle);
      parent.appendChild(circ);
      break;
    }
    case 6: {
      // [] hollow rectangle
      Element rect = plot.svgRect(x - size / 2, y - size / 2, size, size);
      SVGUtil.setStyle(rect, "fill: none;" + strokestyle);
      parent.appendChild(rect);
      break;
    }
    case 7: {
      // <> hollow diamond
      Element rect = plot.svgRect(x - size / 2, y - size / 2, size, size);
      SVGUtil.setStyle(rect, "fill: none;" + strokestyle);
      SVGUtil.setAtt(rect, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "rotate(45," + SVGUtil.fmt(x) + "," + SVGUtil.fmt(y) + ")");
      parent.appendChild(rect);
      break;
    }
    }
  }

  @Override
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    String id = prefix + style + "_" + size;
    Element existing = plot.getIdElement(id);
    if(existing == null) {
      Element symbol = plot.svgElement(SVGConstants.SVG_SYMBOL_TAG);
      SVGUtil.setAtt(symbol, SVGConstants.SVG_ID_ATTRIBUTE, id);
      plotMarker(plot, symbol, 2 * size, 2 * size, style, 2 * size);
      plot.getDefs().appendChild(symbol);
      plot.putIdElement(id, symbol);
    }
    Element use = plot.svgElement(SVGConstants.SVG_USE_TAG);
    use.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, "#" + id);
    SVGUtil.setAtt(use, SVGConstants.SVG_X_ATTRIBUTE, x - 2 * size);
    SVGUtil.setAtt(use, SVGConstants.SVG_Y_ATTRIBUTE, y - 2 * size);
    if(parent != null) {
      parent.appendChild(use);
    }
    return use;
  }

  @Override
  public void setStyleLibrary(StyleLibrary style) {
    this.colors = style.getColorSet(StyleLibrary.PLOT);
  }
}
