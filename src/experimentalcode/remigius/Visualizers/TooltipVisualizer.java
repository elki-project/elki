package experimentalcode.remigius.Visualizers;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import experimentalcode.remigius.gui.listener.TooltipListener;

/**
 * Generates a SVG-Element containing ToolTips. ToolTips remain invisible until
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
   * Tooltip ID prefix
   */
  public static final String TOOLTIP_ID = "tooltip";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_HIDDEN = "tooltip_hidden";

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String TOOLTIP_VISIBLE = "tooltip_visible";

  /**
   * Contains the "outlierness-scores" to be displayed as ToolTips. If this
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
    init(Integer.MAX_VALUE, name, context);
    this.anResult = anResult;
  }

  /**
   * Registers the ToolTip-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the ToolTip-CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {
    CSSClass tooltiphidden = new CSSClass(svgp, TOOLTIP_HIDDEN);
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.3%");
    tooltiphidden.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, "\"Times New Roman\", serif");
    tooltiphidden.setStatement(SVGConstants.CSS_DISPLAY_PROPERTY, SVGConstants.CSS_NONE_VALUE);

    CSSClass tooltipvisible = new CSSClass(svgp, TOOLTIP_VISIBLE);
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.3%");
    tooltipvisible.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, "\"Times New Roman\", serif");

    try {
      svgp.getCSSClassManager().addClass(tooltiphidden);
      svgp.getCSSClassManager().addClass(tooltipvisible);
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

    TooltipListener hoverer = new TooltipListener(svgp);
    for(int id : database) {
      Element tooltip = SVGUtil.svgText(svgp.getDocument(), getProjected(id, 0) + 0.005, getProjected(id, 1) + 0.003, FormatUtil.NF2.format(getValue(id).doubleValue()));
      SVGUtil.addCSSClass(tooltip, TOOLTIP_HIDDEN);

      String dotID = DotVisualizer.MARKER + id;
      Element dot = svgp.getIdElement(dotID);
      if(dot != null) {

        EventTarget targ = (EventTarget) dot;
        targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
      }
      else {
        LoggingUtil.message("Attaching ToolTip to non-existing Object: " + dotID);
      }
      layer.appendChild(tooltip);
      svgp.putIdElement(TOOLTIP_ID + id, tooltip);
    }
    return layer;
  }
}
