package experimentalcode.erich;

import java.text.NumberFormat;
import java.util.BitSet;
import java.util.Locale;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import experimentalcode.erich.scales.LinearScale;

public class SVGPlot {
  // format for serializing numbers into SVG
  public static final NumberFormat FMT = NumberFormat.getInstance(Locale.ROOT);
  static {
    FMT.setMaximumFractionDigits(8);
  }
  
  protected SVGDocument document;
  protected Element root;
  protected Element defs;
  
  BitSet definedMarkers = new BitSet(); 

  public SVGPlot() {
    super();
    // Get a DOMImplementation.
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    DocumentType dt = domImpl.createDocumentType("svg", SVGConstants.SVG_PUBLIC_ID, SVGConstants.SVG_SYSTEM_ID);
    // Workaround: sometimes DocumentType doesn't work right, which
    // causes problems with
    // serialization...
    if(dt.getName() == null) {
      dt = null;
    }
  
    document = (SVGDocument) domImpl.createDocument(SVGConstants.SVG_NAMESPACE_URI, "svg", dt);
  
    root = document.getDocumentElement();
    // setup common SVG namespaces
    root.setAttribute("xmlns", SVGConstants.SVG_NAMESPACE_URI);    
    root.setAttributeNS(SVGConstants.XMLNS_NAMESPACE_URI, SVGConstants.XMLNS_PREFIX + ":" + SVGConstants.XLINK_PREFIX, SVGConstants.XLINK_NAMESPACE_URI);
    // create element for SVG definitions
    defs = svgElement(document, root, "defs");  
  }
  
  public void drawAxis(Element parent, LinearScale scale, double w, double h) {
    Element line = svgElement(document, parent, "line");
    line.setAttribute("x1", FMT.format(0.0));
    line.setAttribute("y1", FMT.format(1.0));
    line.setAttribute("x2", FMT.format(0.0 + w));
    line.setAttribute("y2", FMT.format(1.0 - h));
    line.setAttribute("style", "stroke:silver; stroke-width:0.2%;");
  
    double tx = w;
    double ty = h;
    // ticks are orthogonal
    double tw = ty * 0.01;
    double th = tx * 0.01;
    // anchor
    String anchor = (w < h/2) ? "end" : "middle";
    // vertical text offset; align approximately with middle instead of baseline.
    double textvoff = 0.007;
    
    // draw ticks on x axis
    for(double tick = scale.getMin(); tick <= scale.getMax(); tick += scale.getRes()) {
      Element tickline = svgElement(document, parent, "line");
      double x = tx * scale.getScaled(tick);
      double y = ty * scale.getScaled(tick);
      tickline.setAttribute("x1", FMT.format(x - tw));
      tickline.setAttribute("y1", FMT.format(1 - y - th));
      tickline.setAttribute("x2", FMT.format(x + tw));
      tickline.setAttribute("y2", FMT.format(1 - y + th));
      tickline.setAttribute("style", "stroke:black; stroke-width:0.1%;");
      Element text = svgElement(document, parent, "text");
      text.setAttribute("x", FMT.format(x - tw * 2));
      text.setAttribute("y", FMT.format(1 - y + th * 3 + textvoff ));
      text.setAttribute("style", "font-size: 0.2%");
      text.setAttribute("text-anchor", anchor);
      text.setTextContent(scale.formatValue(tick));
    }
  }

  public void drawAxis(Element parent, LinearScale scale, double x1, double y1, double x2, double y2) {
    Element line = svgElement(document, parent, "line");
    line.setAttribute("x1", FMT.format(x1));
    line.setAttribute("y1", FMT.format(- y1));
    line.setAttribute("x2", FMT.format(x2));
    line.setAttribute("y2", FMT.format(- y2));
    line.setAttribute("style", "stroke:silver; stroke-width:0.2%;");
  
    double tx = x2-x1;
    double ty = y2-y1;
    // ticks are orthogonal
    double tw = ty * 0.01;
    double th = tx * 0.01;
    // anchor
    String anchor = (tx < ty/2) ? "end" : "middle";
    // vertical text offset; align approximately with middle instead of baseline.
    double textvoff = 0.007;
    
    // draw ticks on x axis
    for(double tick = scale.getMin(); tick <= scale.getMax(); tick += scale.getRes()) {
      Element tickline = svgElement(document, parent, "line");
      double x = x1 + tx * scale.getScaled(tick);
      double y = y1 + ty * scale.getScaled(tick);
      tickline.setAttribute("x1", FMT.format(x - tw));
      tickline.setAttribute("y1", FMT.format(- y - th));
      tickline.setAttribute("x2", FMT.format(x + tw));
      tickline.setAttribute("y2", FMT.format(- y + th));
      tickline.setAttribute("style", "stroke:black; stroke-width:0.1%;");
      Element text = svgElement(document, parent, "text");
      text.setAttribute("x", FMT.format(x - tw * 2));
      text.setAttribute("y", FMT.format(- y + th * 3 + textvoff ));
      text.setAttribute("style", "font-size: 0.2%");
      text.setAttribute("text-anchor", anchor);
      text.setTextContent(scale.formatValue(tick));
    }
  }

  public static Element svgElement(Document document, Element parent, String name) {
    Element neu = document.createElementNS(SVGConstants.SVG_NAMESPACE_URI, name);
    if (parent != null) {
      parent.appendChild(neu);
    }
    return neu;
  }

  public static void plotMarker(Document document, Element parent, double x, double y, int style, double size) {
    // TODO: add more styles.
    String[] colors = { "red", "blue", "green", "orange", "cyan", "magenta", "yellow" };
    String colorstr = colors[style % colors.length];
  
    switch(style % 8){
    case 0: {
      // + cross
      Element line1 = svgElement(document, parent, "line");
      line1.setAttribute("x1", FMT.format(x));
      line1.setAttribute("y1", FMT.format(y - size / 2));
      line1.setAttribute("x2", FMT.format(x));
      line1.setAttribute("y2", FMT.format(y + size / 2));
      line1.setAttribute("style", "stroke:" + colorstr + "; stroke-width:" + FMT.format(size / 6));
      Element line2 = svgElement(document, parent, "line");
      line2.setAttribute("x1", FMT.format(x - size / 2));
      line2.setAttribute("y1", FMT.format(y));
      line2.setAttribute("x2", FMT.format(x + size / 2));
      line2.setAttribute("y2", FMT.format(y));
      line2.setAttribute("style", "stroke:" + colorstr + "; stroke-width: " + FMT.format(size / 6));
      break;
    }
    case 1: {
      // X cross
      Element line1 = svgElement(document, parent, "line");
      line1.setAttribute("x1", FMT.format(x - size / 2));
      line1.setAttribute("y1", FMT.format(y - size / 2));
      line1.setAttribute("x2", FMT.format(x + size / 2));
      line1.setAttribute("y2", FMT.format(y + size / 2));
      line1.setAttribute("style", "stroke:" + colorstr + "; stroke-width: " + FMT.format(size / 6));
      Element line2 = svgElement(document, parent, "line");
      line2.setAttribute("x1", FMT.format(x - size / 2));
      line2.setAttribute("y1", FMT.format(y + size / 2));
      line2.setAttribute("x2", FMT.format(x + size / 2));
      line2.setAttribute("y2", FMT.format(y - size / 2));
      line2.setAttribute("style", "stroke:" + colorstr + "; stroke-width: " + FMT.format(size / 6));
      break;
    }
    case 2: {
      // O filled circle
      Element circ = svgElement(document, parent, "circle");
      circ.setAttribute("cx", FMT.format(x));
      circ.setAttribute("cy", FMT.format(y));
      circ.setAttribute("r", FMT.format(size / 2));
      circ.setAttribute("style", "fill:" + colorstr);
      break;
    }
    case 3: {
      // [] filled rectangle
      Element rect = svgElement(document, parent, "rect");
      rect.setAttribute("x", FMT.format(x - size / 2));
      rect.setAttribute("y", FMT.format(y - size / 2));
      rect.setAttribute("width", FMT.format(size));
      rect.setAttribute("height", FMT.format(size));
      rect.setAttribute("style", "fill:" + colorstr);
      break;
    }
    case 4: {
      // <> filled diamond
      Element rect = svgElement(document, parent, "rect");
      rect.setAttribute("x", FMT.format(x - size / 2));
      rect.setAttribute("y", FMT.format(y - size / 2));
      rect.setAttribute("width", FMT.format(size));
      rect.setAttribute("height", FMT.format(size));
      rect.setAttribute("style", "fill:" + colorstr);
      rect.setAttribute("transform", "rotate(45," + FMT.format(x) + "," + FMT.format(y) + ")");
      break;
    }
    case 5: {
      // O hollow circle
      Element circ = svgElement(document, parent, "circle");
      circ.setAttribute("cx", FMT.format(x));
      circ.setAttribute("cy", FMT.format(y));
      circ.setAttribute("r", FMT.format(size / 2));
      circ.setAttribute("style", "fill: none; stroke: " + colorstr + "; stroke-width: " + FMT.format(size / 6));
      break;
    }
    case 6: {
      // [] hollow rectangle
      Element rect = svgElement(document, parent, "rect");
      rect.setAttribute("x", FMT.format(x - size / 2));
      rect.setAttribute("y", FMT.format(y - size / 2));
      rect.setAttribute("width", FMT.format(size));
      rect.setAttribute("height", FMT.format(size));
      rect.setAttribute("style", "fill: none; stroke: " + colorstr + "; stroke-width: " + FMT.format(size / 6));
      break;
    }
    case 7: {
      // <> hollow diamond
      Element rect = svgElement(document, parent, "rect");
      rect.setAttribute("x", FMT.format(x - size / 2));
      rect.setAttribute("y", FMT.format(y - size / 2));
      rect.setAttribute("width", FMT.format(size));
      rect.setAttribute("height", FMT.format(size));
      rect.setAttribute("style", "fill: none; stroke: " + colorstr + "; stroke-width: " + FMT.format(size / 6));
      rect.setAttribute("transform", "rotate(45," + FMT.format(x) + "," + FMT.format(y) + ")");
      break;
    }
    }
  }

  public void useMarker(Element parent, double x, double y, int style, double size) {
    if (!definedMarkers.get(style)) {
      Element symbol = svgElement(document, defs, "symbol");
      symbol.setAttribute("id", "s" + style);
      symbol.setAttribute("viewBox", "-1 -1 2 2");
      plotMarker(document, symbol, 0, 0, style, 2);
      definedMarkers.set(style);
    }
    Element use = svgElement(document, parent, "use");
    use.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, "#s" + style);
    use.setAttribute("x", FMT.format(x - size));
    use.setAttribute("y", FMT.format(y - size));
    use.setAttribute("width", FMT.format(size*2));
    use.setAttribute("height", FMT.format(size*2));
  }

  public SVGDocument getDocument() {
    return document;
  }
}
