package experimentalcode.erich.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import experimentalcode.erich.visualization.SVGPlot;

public class PrettyMarkers extends MinimalMarkers {
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
   * @param prefix Prefix to use.
   */
  public PrettyMarkers(String prefix) {
    this.prefix = prefix;
  }
  
  /**
   * Constructor without arguments, will use {@link #DEFAULT_PREFIX} as prefix.
   */
  public PrettyMarkers() {
    this(DEFAULT_PREFIX);
  }

  /**
   * Draw an marker used in scatter plots. If you intend to use the markers multiple times,
   * you should consider using the {@link #useMarker} method instead, which exploits the SVG
   * features of symbol definition and use
   * 
   * @param document containing document
   * @param parent parent node
   * @param x position
   * @param y position
   * @param style marker style (enumerated)
   * @param size size
   */
  public static void plotMarker(Document document, Element parent, double x, double y, int style, double size) {
    assert(parent != null);
    // TODO: add more styles.
    String colorstr = getColor(style);

    switch(style % 8){
    case 0: {
      // + cross
      Element line1 = SVGUtil.svgElement(document, "line");
      SVGUtil.setAtt(line1,"x1", x);
      SVGUtil.setAtt(line1,"y1", y - size / 2);
      SVGUtil.setAtt(line1,"x2", x);
      SVGUtil.setAtt(line1,"y2", y + size / 2);
      SVGUtil.setAtt(line1,"style", "stroke:" + colorstr + "; stroke-width:" + SVGUtil.fmt(size / 6));
      parent.appendChild(line1);
      Element line2 = SVGUtil.svgElement(document, "line");
      SVGUtil.setAtt(line2,"x1", x - size / 2);
      SVGUtil.setAtt(line2,"y1", y);
      SVGUtil.setAtt(line2,"x2", x + size / 2);
      SVGUtil.setAtt(line2,"y2", y);
      SVGUtil.setAtt(line2,"style", "stroke:" + colorstr + "; stroke-width: " + SVGUtil.fmt(size / 6));
      parent.appendChild(line2);
      break;
    }
    case 1: {
      // X cross
      Element line1 = SVGUtil.svgElement(document, "line");
      SVGUtil.setAtt(line1,"x1", x - size / 2.828427);
      SVGUtil.setAtt(line1,"y1", y - size / 2.828427);
      SVGUtil.setAtt(line1,"x2", x + size / 2.828427);
      SVGUtil.setAtt(line1,"y2", y + size / 2.828427);
      SVGUtil.setAtt(line1,"style", "stroke:" + colorstr + "; stroke-width: " + SVGUtil.fmt(size / 6));
      parent.appendChild(line1);
      Element line2 = SVGUtil.svgElement(document, "line");
      SVGUtil.setAtt(line2,"x1", x - size / 2.828427);
      SVGUtil.setAtt(line2,"y1", y + size / 2.828427);
      SVGUtil.setAtt(line2,"x2", x + size / 2.828427);
      SVGUtil.setAtt(line2,"y2", y - size / 2.828427);
      SVGUtil.setAtt(line2,"style", "stroke:" + colorstr + "; stroke-width: " + SVGUtil.fmt(size / 6));
      parent.appendChild(line2);
      break;
    }
    case 2: {
      // O filled circle
      Element circ = SVGUtil.svgElement(document, "circle");
      SVGUtil.setAtt(circ,"cx", x);
      SVGUtil.setAtt(circ,"cy", y);
      SVGUtil.setAtt(circ,"r", size / 2);
      SVGUtil.setAtt(circ,"style", "fill:" + colorstr);
      parent.appendChild(circ);
      break;
    }
    case 3: {
      // [] filled rectangle
      Element rect = SVGUtil.svgElement(document, "rect");
      SVGUtil.setAtt(rect,"x",x - size / 2);
      SVGUtil.setAtt(rect,"y",y - size / 2);
      SVGUtil.setAtt(rect,"width",size);
      SVGUtil.setAtt(rect,"height",size);
      SVGUtil.setAtt(rect,"style","fill:" + colorstr);
      parent.appendChild(rect);
      break;
    }
    case 4: {
      // <> filled diamond
      Element rect = SVGUtil.svgElement(document, "rect");
      SVGUtil.setAtt(rect,"x",x - size / 2);
      SVGUtil.setAtt(rect,"y",y - size / 2);
      SVGUtil.setAtt(rect,"width",size);
      SVGUtil.setAtt(rect,"height",size);
      SVGUtil.setAtt(rect,"style","fill:" + colorstr);
      SVGUtil.setAtt(rect,"transform","rotate(45," + SVGUtil.fmt(x) + "," + SVGUtil.fmt(y) + ")");
      parent.appendChild(rect);
      break;
    }
    case 5: {
      // O hollow circle
      Element circ = SVGUtil.svgElement(document, "circle");
      SVGUtil.setAtt(circ,"cx",x);
      SVGUtil.setAtt(circ,"cy",y);
      SVGUtil.setAtt(circ,"r",size / 2);
      SVGUtil.setAtt(circ,"style","fill: none; stroke: " + colorstr + "; stroke-width: " + SVGUtil.fmt(size / 6));
      parent.appendChild(circ);
      break;
    }
    case 6: {
      // [] hollow rectangle
      Element rect = SVGUtil.svgElement(document, "rect");
      SVGUtil.setAtt(rect,"x",x - size / 2);
      SVGUtil.setAtt(rect,"y",y - size / 2);
      SVGUtil.setAtt(rect,"width",size);
      SVGUtil.setAtt(rect,"height",size);
      SVGUtil.setAtt(rect,"style","fill: none; stroke: " + colorstr + "; stroke-width: " + SVGUtil.fmt(size / 6));
      parent.appendChild(rect);
      break;
    }
    case 7: {
      // <> hollow diamond
      Element rect = SVGUtil.svgElement(document, "rect");
      SVGUtil.setAtt(rect,"x",x - size / 2);
      SVGUtil.setAtt(rect,"y",y - size / 2);
      SVGUtil.setAtt(rect,"width",size);
      SVGUtil.setAtt(rect,"height",size);
      SVGUtil.setAtt(rect,"style","fill: none; stroke: " + colorstr + "; stroke-width: " + SVGUtil.fmt(size / 6));
      SVGUtil.setAtt(rect,"transform","rotate(45," + SVGUtil.fmt(x) + "," + SVGUtil.fmt(y) + ")");
      parent.appendChild(rect);
      break;
    }
    }
  }

  @Override
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size) {
    String id = prefix + style;
    Element existing = plot.getIdElement(id);
    if (existing == null) {
      Element symbol = plot.svgElement("symbol");
      SVGUtil.setAtt(symbol,"id",id);
      SVGUtil.setAtt(symbol,"viewBox","-1 -1 2 2");
      plotMarker(plot.getDocument(), symbol, 0, 0, style, 2);
      plot.getDefs().appendChild(symbol);
      plot.putIdElement(id, symbol);
    }
    Element use = plot.svgElement("use");
    use.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, "#"+id);
    SVGUtil.setAtt(use,"x",x - size);
    SVGUtil.setAtt(use,"y",y - size);
    SVGUtil.setAtt(use,"width",size * 2);
    SVGUtil.setAtt(use,"height",size * 2);
    if (parent != null) {
      parent.appendChild(use);
    }
    return use;
  }
}
