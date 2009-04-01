package experimentalcode.erich.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;

import experimentalcode.erich.scales.LinearScale;

public class SVGAxis {

  /**
   * Flag for axis label position. First char: right-hand or left-hand side of
   * line. Second char: text alignment
   * 
   */
  private enum ALIGNMENT {
    LL, RL, LC, RC, LR, RR
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
   * @param righthanded control whether to print labels on the right hand side
   *        or left hand side
   */
  public static void drawAxis(SVGDocument document, Element parent, LinearScale scale, double x1, double y1, double x2, double y2, boolean labels, boolean righthanded) {
    assert (parent != null);
    Element line = SVGUtil.svgElement(document, SVGConstants.SVG_LINE_TAG);
    SVGUtil.setAtt(line, SVGConstants.SVG_X1_ATTRIBUTE, x1);
    SVGUtil.setAtt(line, SVGConstants.SVG_Y1_ATTRIBUTE, y1);
    SVGUtil.setAtt(line, SVGConstants.SVG_X2_ATTRIBUTE , x2);
    SVGUtil.setAtt(line, SVGConstants.SVG_Y2_ATTRIBUTE, y2);
    SVGUtil.setAtt(line, SVGConstants.SVG_STYLE_ATTRIBUTE, "stroke:silver; stroke-width:0.2%;");
    parent.appendChild(line);
  
    double tx = x2 - x1;
    double ty = y2 - y1;
    // ticks are orthogonal
    double tw = ty * 0.01;
    double th = - tx * 0.01;
  
    // choose where to print labels.
    ALIGNMENT pos = ALIGNMENT.LL;
    if(labels) {
      double angle = Math.atan2(ty, tx);
      //System.err.println(tx + " " + (-ty) + " " + angle);
      if(angle > 2.6) { // pi .. 2.6 = 180 .. 150
        pos = righthanded ? ALIGNMENT.RC : ALIGNMENT.LC;
      }
      else if(angle > 0.5) { // 2.3 .. 0.7 = 130 .. 40
        pos = righthanded ? ALIGNMENT.RR : ALIGNMENT.LL;
      }
      else if(angle > -0.5) { // 0.5 .. -0.5 = 30 .. -30
        pos = righthanded ? ALIGNMENT.RC : ALIGNMENT.LC;
      }
      else if(angle > -2.6) { // -0.5 .. -2.6 = -30 .. -150
        pos = righthanded ? ALIGNMENT.RL : ALIGNMENT.LR;
      }
      else { // -2.6 .. -pi = -150 .. -180
        pos = righthanded ? ALIGNMENT.RC : ALIGNMENT.LC;
      }
    }
    // vertical text offset; align approximately with middle instead of
    // baseline.
    double textvoff = 0.007;
  
    // draw ticks on x axis
    for(double tick = scale.getMin(); tick <= scale.getMax(); tick += scale.getRes()) {
      Element tickline = SVGUtil.svgElement(document, SVGConstants.SVG_LINE_TAG);
      double x = x1 + tx * scale.getScaled(tick);
      double y = y1 + ty * scale.getScaled(tick);
      SVGUtil.setAtt(tickline, SVGConstants.SVG_X1_ATTRIBUTE, x - tw);
      SVGUtil.setAtt(tickline, SVGConstants.SVG_Y1_ATTRIBUTE, y - th);
      SVGUtil.setAtt(tickline, SVGConstants.SVG_X2_ATTRIBUTE, x + tw);
      SVGUtil.setAtt(tickline, SVGConstants.SVG_Y2_ATTRIBUTE, y + th);
      SVGUtil.setAtt(tickline, SVGConstants.SVG_STYLE_ATTRIBUTE, "stroke:black; stroke-width:0.1%;");
      parent.appendChild(tickline);
      // draw labels
      if(labels) {
        Element text = SVGUtil.svgElement(document, SVGConstants.SVG_TEXT_TAG);
        SVGUtil.setAtt(text, SVGConstants.SVG_STYLE_ATTRIBUTE, "font-size: 0.2%");
        switch(pos){
        case LL:
        case LC:
        case LR:
          SVGUtil.setAtt(text, SVGConstants.SVG_X_ATTRIBUTE, x + tw * 1.5);
          SVGUtil.setAtt(text, SVGConstants.SVG_Y_ATTRIBUTE, y + th * 1.5 + textvoff);
          break;
        case RL:
        case RC:
        case RR:
          SVGUtil.setAtt(text, SVGConstants.SVG_X_ATTRIBUTE, x - tw * 1.5);
          SVGUtil.setAtt(text, SVGConstants.SVG_Y_ATTRIBUTE, y - th * 1.5 + textvoff);
        }
        switch(pos){
        case LL:
        case RL:
          SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_START_VALUE);
          break;
        case LC:
        case RC:
          SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_MIDDLE_VALUE);
          break;
        case LR:
        case RR:
          SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, SVGConstants.SVG_END_VALUE);
          break;
        }
        text.setTextContent(scale.formatValue(tick));
        parent.appendChild(text);
      }
    }
  }

}
