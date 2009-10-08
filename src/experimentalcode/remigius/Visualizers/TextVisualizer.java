package experimentalcode.remigius.Visualizers;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.UpdatableSVGPlot;
import experimentalcode.remigius.gui.listener.ToolTipListener;

/**
 * Generates a SVG-Element containing ToolTips. ToolTips remain invisible
 * until their corresponding Marker is touched by the cursor and stay visible as
 * long as the cursor lingers on the marker. This implementation uses
 * {@link ToolTipListener} to achieve this behavior.
 * 
 * @see ToolTipListener
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV>
 */
public class TextVisualizer<NV extends NumberVector<NV, ?>> extends PlanarVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  public static final String NAME = "Tooltips";

  /**
   * Contains the "outlierness-scores" to be displayed as ToolTips. If this
   * result does not contain <b>all</b> IDs the database contains,
   * behavior is undefined.
   */
  private AnnotationResult<Double> anResult;

  @Override
  public int getLevel() {
    return Integer.MAX_VALUE;
  }

  /**
   * Returns the outlierness-score for a given ID. If there is no corresponding
   * score, behavior is undefined.
   * 
   * @param id an ID which has to exist in both the database and the result.
   * @return the outlierness-score for a given ID.
   */
  private Double getValue(int id) {

    return anResult.getValueFor(id);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param database contains all objects to be processed.
   * @param anResult contains "outlierness-scores", corresponding to the database.
   * 
   * TODO: Refactor from AnnotationResult<Double> to AnnotationResult<Number>
   */
  public void init(String name, Database<NV> database, AnnotationResult<Double> anResult) {
    init(database, name);
    this.anResult = anResult;
  }

  /**
   * Registers the ToolTip-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the ToolTip-CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {

    CSSClass tooltip = new CSSClass(svgp, ShapeLibrary.TOOLTIP);
    tooltip.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, "0.1%");

    try {
      svgp.getCSSClassManager().addClass(tooltip);
      svgp.updateStyleElement();
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
    }
  }

  @Override
  public Element visualize(SVGPlot svgp) {
    setupCSS(svgp);
    Element layer = ShapeLibrary.createG(svgp.getDocument());
    
    if (svgp instanceof UpdatableSVGPlot){
      ToolTipListener hoverer = new ToolTipListener((UpdatableSVGPlot)svgp);
      for(int id : database.getIDs()) {
        Element tooltip = ShapeLibrary.createToolTip(svgp.getDocument(), getPositioned(database.get(id), dimx), (1 - getPositioned(database.get(id), dimy)), getValue(id));
        
        String dotID = ShapeLibrary.createID(ShapeLibrary.MARKER, id);
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
        svgp.putIdElement(ShapeLibrary.createID(ShapeLibrary.TOOLTIP, id), tooltip);
      }
    } else {
      LoggingUtil.message("This SVGPlot doesn't contain an UpdateRunner.");
    }
    return layer;
  }
}
