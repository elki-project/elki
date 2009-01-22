package experimentalcode.erich.visualization;


import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import experimentalcode.erich.scales.LinearScale;
import experimentalcode.erich.visualization.svg.SVGUtil;

/**
 * Base class for SVG plots. Provides some basic functionality such as element
 * creation, axis plotting, markers and number formatting for SVG.
 * 
 * @author Erich Schubert
 * 
 */
public class SVGPlot {
  /**
   * SVG document we plot to.
   */
  private SVGDocument document;

  /**
   * Root element of the document.
   */
  private Element root;

  /**
   * Definitions element of the document.
   */
  private Element defs;

  /**
   * Create a new plotting document.
   */
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
    defs = svgElement(root, "defs");
  }

  /**
   * Flag for axis label position. First char: right-hand or left-hand side of
   * line. Second char: text alignment
   * 
   */
  private enum POSITION {
    RL, RC, RR, LL, LC, LR
  }

  /**
   * Plot an axis with appropriate scales
   * 
   * @param parent Containing element
   * @param scale axis scale information
   * @param x1 starting coordinate
   * @param y1 starting coordinate
   * @param x2 ending coordinate
   * @param y2 ending coordinate
   * @param labels control whether labels are printed.
   */
  public void drawAxis(Element parent, LinearScale scale, double x1, double y1, double x2, double y2, boolean labels) {
    Element line = svgElement(parent, "line");
    SVGUtil.setAtt(line,"x1", x1);
    SVGUtil.setAtt(line,"y1", -y1);
    SVGUtil.setAtt(line,"x2", x2);
    SVGUtil.setAtt(line,"y2", -y2);
    SVGUtil.setAtt(line,"style", "stroke:silver; stroke-width:0.2%;");

    double tx = x2 - x1;
    double ty = y2 - y1;
    // ticks are orthogonal
    double tw = ty * 0.01;
    double th = tx * 0.01;

    // choose where to print labels.
    POSITION pos = POSITION.LR;
    if(labels) {
      double angle = Math.atan2(-ty, tx);
      // System.err.println(tx + " " + (-ty) + " " + angle);
      if(angle < -2.3) {
        pos = POSITION.LL;
      }
      else if(angle < -0.7) {
        pos = POSITION.RR;
      }
      else if(angle < 0.5) {
        pos = POSITION.RC;
      }
      else if(angle < 1.5) {
        pos = POSITION.RL;
      }
      else if(angle < 2.3) {
        pos = POSITION.LR;
      }
      else {
        pos = POSITION.LC;
      }
    }
    // vertical text offset; align approximately with middle instead of
    // baseline.
    double textvoff = 0.007;

    // draw ticks on x axis
    for(double tick = scale.getMin(); tick <= scale.getMax(); tick += scale.getRes()) {
      Element tickline = svgElement(parent, "line");
      double x = x1 + tx * scale.getScaled(tick);
      double y = y1 + ty * scale.getScaled(tick);
      SVGUtil.setAtt(tickline,"x1", x - tw);
      SVGUtil.setAtt(tickline,"y1", -y - th);
      SVGUtil.setAtt(tickline,"x2", x + tw);
      SVGUtil.setAtt(tickline,"y2", -y + th);
      SVGUtil.setAtt(tickline,"style", "stroke:black; stroke-width:0.1%;");
      // draw labels
      if(labels) {
        Element text = svgElement(parent, "text");
        SVGUtil.setAtt(text,"style", "font-size: 0.2%");
        switch(pos){
        case LL:
        case LC:
        case LR:
          SVGUtil.setAtt(text,"x", x - tw * 2);
          SVGUtil.setAtt(text,"y", -y - th * 3 + textvoff);
          break;
        case RL:
        case RC:
        case RR:
          SVGUtil.setAtt(text,"x", x + tw * 2);
          SVGUtil.setAtt(text,"y", -y + th * 3 + textvoff);
        }
        switch(pos){
        case LL:
        case RL:
          SVGUtil.setAtt(text,"text-anchor", "start");
          break;
        case LC:
        case RC:
          SVGUtil.setAtt(text,"text-anchor", "middle");
          break;
        case LR:
        case RR:
          SVGUtil.setAtt(text,"text-anchor", "end");
          break;
        }
        text.setTextContent(scale.formatValue(tick));
      }
    }
  }

  /**
   * Create a SVG element in the SVG namespace. Non-static version.
   * 
   * @param parent parent node
   * @param name node name
   * @return
   */
  public Element svgElement(Element parent, String name) {
    return SVGUtil.svgElement(document, parent, name);
  }

  /**
   * Retrieve the SVG document.
   * 
   * @return resulting document.
   */
  public SVGDocument getDocument() {
    return document;
  }

  /**
   * Getter for definitions section
   * @return DOM element
   */
  public Element getRoot() {
    return root;
  }

  /**
   * Getter for definitions section
   * @return DOM element
   */
  public Element getDefs() {
    return defs;
  }
}
