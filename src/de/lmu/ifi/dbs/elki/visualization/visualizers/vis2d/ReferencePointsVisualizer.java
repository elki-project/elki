package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generates a SVG-Element visualizing reference points.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class ReferencePointsVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * Serves reference points.
   */
  private CollectionResult<NV> colResult;

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String REFPOINT = "refpoint";

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "ReferencePoints";

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param colResult contains all reference points.
   */
  public void init(VisualizerContext context, CollectionResult<NV> colResult) {
    super.init(NAME, context);
    this.colResult = colResult;
  }

  /**
   * Registers the Reference-Point-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the -CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {
    CSSClass refpoint = new CSSClass(svgp, REFPOINT);
    refpoint.setStatement(SVGConstants.CSS_FILL_PROPERTY, "red");

    try {
      svgp.getCSSClassManager().addClass(refpoint);
    }
    catch(CSSNamingConflict e) {
      LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
    }
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    Element layer = super.setupCanvas(svgp, proj, width, height);
    setupCSS(svgp);
    Iterator<NV> iter = colResult.iterator();

    while(iter.hasNext()) {
      NV v = iter.next();
      Vector projected = proj.projectDataToRenderSpace(v);
      Element dot = SVGUtil.svgCircle(svgp.getDocument(), projected.get(0), projected.get(1), context.getStyleLibrary().getLineWidth(StyleLibrary.PLOT));
      SVGUtil.addCSSClass(dot, REFPOINT);
      layer.appendChild(dot);
    }
    return layer;
  }
}