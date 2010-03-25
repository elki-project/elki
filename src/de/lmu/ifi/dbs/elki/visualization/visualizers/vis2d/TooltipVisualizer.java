package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generates a SVG-Element containing Tooltips. Tooltips remain invisible until
 * their corresponding Marker is touched by the cursor and stay visible as long
 * as the cursor lingers on the marker.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Data type visualized.
 */
public class TooltipVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * OptionID for {@link #DIGITS_PARAM}.
   */
  public static final OptionID DIGITS_ID = OptionID.getOrCreateOptionID("tooltip.digits", "Number of digits to show (e.g. when visualizing outlier scores)");

  /**
   * Parameter for the gamma-correction.
   * 
   * <p>
   * Key: {@code -tooltip.digits}
   * </p>
   * 
   * <p>
   * Default value: 4
   * < /p>
   */
  private final IntParameter DIGITS_PARAM = new IntParameter(DIGITS_ID, new GreaterEqualConstraint(0), 4);
  
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
   * Number formatter used for visualization
   */
  NumberFormat nf = NumberFormat.getInstance(Locale.ROOT);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public TooltipVisualizer(Parameterization config) {
    super();
    super.metadata.put(Visualizer.META_NOTHUMB, true);
    if (config.grab(DIGITS_PARAM)) {
      int digits = DIGITS_PARAM.getValue();
      nf.setGroupingUsed(false);
      nf.setMaximumFractionDigits(digits);
    }
  }

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
   * @param name Visualizer name
   * @param context Visualization context
   * @param result the outlier score visualized
   */
  public void init(String name, VisualizerContext context, OutlierResult result) {
    super.init(name, context);
    super.setLevel(Visualizer.LEVEL_INTERACTIVE);
    this.anResult = result.getScores();
  }

  /**
   * Registers the Tooltip-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the Tooltip-CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {
    double fontsize = context.getStyleLibrary().getTextSize(StyleLibrary.PLOT);

    CSSClass tooltiphidden = new CSSClass(svgp, TOOLTIP_HIDDEN);
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.PLOT));
    tooltiphidden.setStatement(SVGConstants.CSS_DISPLAY_PROPERTY, SVGConstants.CSS_NONE_VALUE);

    CSSClass tooltipvisible = new CSSClass(svgp, TOOLTIP_VISIBLE);
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.PLOT));

    CSSClass tooltipsticky = new CSSClass(svgp, TOOLTIP_STICKY);
    tooltipsticky.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, fontsize);
    tooltipsticky.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.PLOT));

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
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
    }
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    Element layer = super.setupCanvas(svgp, proj, margin, width, height);
    setupCSS(svgp);

    double dotsize = 2 * context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT);
    double fontsize = 3 * context.getStyleLibrary().getTextSize(StyleLibrary.PLOT);

    EventListener hoverer = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        handleHoverevent(evt);
      }
    };

    Database<NV> database = context.getDatabase();
    for(int id : database) {
      Vector v = proj.projectDataToRenderSpace(database.get(id));
      // FIXME: Make number of digits configurable!
      Element tooltip = svgp.svgText(v.get(0) + dotsize, v.get(1) + fontsize * 0.07, nf.format(getValue(id).doubleValue()));
      SVGUtil.addCSSClass(tooltip, TOOLTIP_HIDDEN);

      // sensitive area.
      Element area = svgp.svgCircle(v.get(0), v.get(1), dotsize);
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