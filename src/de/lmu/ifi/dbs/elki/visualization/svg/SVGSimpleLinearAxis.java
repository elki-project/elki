package de.lmu.ifi.dbs.elki.visualization.svg;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;


/**
 * Class to draw a simple axis with tick marks on the plot.
 * 
 * @author Erich Schubert
 *
 */
public class SVGSimpleLinearAxis {

  /**
   * Flag for axis label position. First char: right-hand or left-hand side of
   * line. Second char: text alignment
   * 
   */
  private enum ALIGNMENT {
    LL, RL, LC, RC, LR, RR
  }

  /**
   * CSS class name for the axes
   */
  private final static String CSS_AXIS = "axis";

  /**
   * CSS class name for the axes
   */
  private final static String CSS_AXIS_TICK = "axis-tick";

  /**
   * CSS class name for the axes
   */
  private final static String CSS_AXIS_LABEL = "axis-label";

  /**
   * Register CSS classes with a {@link CSSClassManager}
   * 
   * @param owner Owner of the CSS classes
   * @param manager Manager to register the classes with
   * @throws CSSNamingConflict when a name clash occurs
   */
  public static void setupCSSClasses(Object owner, CSSClassManager manager) throws CSSNamingConflict {
    if (!manager.contains(CSS_AXIS)) {
      CSSClass axis = new CSSClass(owner, CSS_AXIS);
      axis.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_SILVER_VALUE);
      axis.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, "0.2%");
      manager.addClass(axis);
    }
    if (!manager.contains(CSS_AXIS_TICK)) {
      CSSClass tick = new CSSClass(owner, CSS_AXIS_TICK);
      tick.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_SILVER_VALUE);
      tick.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, "0.1%");
      manager.addClass(tick);
    }
    if (!manager.contains(CSS_AXIS_LABEL)) {
      CSSClass label = new CSSClass(owner, CSS_AXIS_LABEL);
      label.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.2%");
      manager.addClass(label);
    }
  }

  /**
   * Plot an axis with appropriate scales
   * 
   * @param plot Plot object
   * @param parent Containing element
   * @param scale axis scale information
   * @param x1 starting coordinate
   * @param y1 starting coordinate
   * @param x2 ending coordinate
   * @param y2 ending coordinate
   * @param labels control whether labels are printed.
   * @param righthanded control whether to print labels on the right hand side
   *        or left hand side
   * @throws CSSNamingConflict when a conflict occurs in CSS
   */
  public static void drawAxis(SVGPlot plot, Element parent, LinearScale scale, double x1, double y1, double x2, double y2, boolean labels, boolean righthanded) throws CSSNamingConflict {
    assert (parent != null);
    Element line = plot.svgLine(x1, y1, x2, y2);
    SVGUtil.setAtt(line, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_AXIS);
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
      double x = x1 + tx * scale.getScaled(tick);
      double y = y1 + ty * scale.getScaled(tick);
      Element tickline = plot.svgLine(x - tw, y - th, x + tw, y + th);
      SVGUtil.setAtt(tickline, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_AXIS_TICK);
      parent.appendChild(tickline);
      // draw labels
      if(labels) {
        Element text = plot.svgElement(SVGConstants.SVG_TEXT_TAG);
        SVGUtil.setAtt(text, SVGConstants.SVG_CLASS_ATTRIBUTE, CSS_AXIS_LABEL);
        SVGUtil.setAtt(text, SVGConstants.SVG_TEXT_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_LEGIBILITY_VALUE);
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
    setupCSSClasses(plot, plot.getCSSClassManager());
  }

}
