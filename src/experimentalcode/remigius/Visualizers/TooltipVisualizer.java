package experimentalcode.remigius.Visualizers;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Generates a SVG-Element containing Tooltips. Tooltips remain invisible until
 * their corresponding Marker is touched by the cursor and stay visible as long
 * as the cursor lingers on the marker. This implementation uses
 * {@link TooltipListener} to achieve this behavior.
 * 
 * @see TooltipListener
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 */
public class TooltipVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Tooltips";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_HIDDEN = "tooltip_hidden";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_VISIBLE = "tooltip_visible";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_STICKY = "tooltip_sticky";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_AREA = "tooltip_area";

  /**
   * Contains the "outlierness-scores" to be displayed as Tooltips. If this
   * result does not contain <b>all</b> IDs the database contains, behavior is
   * undefined.
   */
  private AnnotationResult<? extends Number> anResult;

  /**
   * Returns the outlierness-score for a given ID. If there is no corresponding
   * score, behavior is undefined.
   * 
   * @param id an ID which has to exist in both the database and the result.
   * @return the outlierness-score for a given ID.
   */
  private Number getValue(int id) {
    return anResult.getValueFor(id);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param database contains all objects to be processed.
   * @param anResult contains "outlierness-scores", corresponding to the
   *        database.
   */
  public void init(String name, VisualizerContext context, AnnotationResult<? extends Number> anResult) {
    super.init(name, context);
    super.setLevel(Visualizer.LEVEL_INTERACTIVE);
    this.anResult = anResult;
  }

  /**
   * Registers the Tooltip-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {
    CSSClass tooltiphidden = new CSSClass(svgp, TOOLTIP_HIDDEN);
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.3%");
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, "\"Times New Roman\", serif");
    tooltiphidden.setStatement(SVGConstants.CSS_DISPLAY_PROPERTY, SVGConstants.CSS_NONE_VALUE);

    CSSClass tooltipvisible = new CSSClass(svgp, TOOLTIP_VISIBLE);
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.3%");
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, "\"Times New Roman\", serif");

    CSSClass tooltipsticky = new CSSClass(svgp, TOOLTIP_STICKY);
    tooltipsticky.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.3%");
    tooltipsticky.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, "\"Times New Roman\", serif");

    // invisible but sensitive area for the tooltip activator
    CSSClass tooltiparea = new CSSClass(svgp, TOOLTIP_AREA);
    tooltiparea.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
    tooltiparea.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    tooltiparea.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0");
    tooltiparea.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);

    try {
      svgp.getCSSClassManager().addClass(tooltiphidden);
      svgp.getCSSClassManager().addClass(tooltipvisible);
      svgp.getCSSClassManager().addClass(tooltipsticky);
      svgp.getCSSClassManager().addClass(tooltiparea);
      // TODO: have the parent call updateStyleElement!
      svgp.updateStyleElement();
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
    }
  }

  @Override
  public Element visualize(SVGPlot svgp) {
    Element layer = super.visualize(svgp);
    setupCSS(svgp);

    EventListener hoverer = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        handleHoverevent(evt);
      }
    };

    for(int id : database) {
      Element tooltip = svgp.svgText(getProjected(id, 0) + 0.005, getProjected(id, 1) + 0.003, FormatUtil.NF2.format(getValue(id).doubleValue()));
      SVGUtil.addCSSClass(tooltip, TOOLTIP_HIDDEN);

      // sensitive area.
      Element area = svgp.svgCircle(getProjected(id, 0), getProjected(id, 1), 0.01);
      SVGUtil.addCSSClass(area, TOOLTIP_AREA);

      EventTarget targ = (EventTarget) area;
      targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
      targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
      targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);

      // NOTE: do not change the sequence in which these are inserted!
      layer.appendChild(area);
      layer.appendChild(tooltip);
    }
    return layer;
  }

  /**
   * Handle the hover events.
   * 
   * @param evt Event.
   */
  protected void handleHoverevent(Event evt) {
    if(evt.getTarget() instanceof Element) {
      Element e = (Element) evt.getTarget();
      Node next = e.getNextSibling();
      if(next != null && next instanceof Element) {
        toggleTooltip((Element) next, evt.getType());
      }
      else {
        logger.warning("Tooltip sibling not found.");
      }
    }
    else {
      logger.warning("Got event for non-Element?!?");
    }
  }

  /**
   * Toggle the Tooltip of an element.
   * 
   * @param elem Element
   * @param type Event type
   */
  protected void toggleTooltip(Element elem, String type) {
    String csscls = elem.getAttribute(SVGConstants.SVG_CLASS_ATTRIBUTE);
    if(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE.equals(type)) {
      if(TOOLTIP_HIDDEN.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_VISIBLE);
      }
    }
    else if(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE.equals(type)) {
      if(TOOLTIP_VISIBLE.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_HIDDEN);
      }
    }
    else if(SVGConstants.SVG_CLICK_EVENT_TYPE.equals(type)) {
      if(TOOLTIP_STICKY.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_HIDDEN);
      }
      if(TOOLTIP_HIDDEN.equals(csscls) || TOOLTIP_VISIBLE.equals(csscls)) {
        SVGUtil.setAtt(elem, SVGConstants.SVG_CLASS_ATTRIBUTE, TOOLTIP_STICKY);
      }
    }
  }
}
