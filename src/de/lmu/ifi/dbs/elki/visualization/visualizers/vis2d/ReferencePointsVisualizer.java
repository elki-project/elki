package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
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
  protected CollectionResult<NV> result;

  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String REFPOINT = "refpoint";

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Reference Points";

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param colResult contains all reference points.
   */
  public void init(VisualizerContext<? extends NV> context, CollectionResult<NV> colResult) {
    super.init(NAME, context);
    this.result = colResult;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new ReferencePointsVisualization(context, svgp, proj, width, height);
  }

  /**
   * The actual visualization instance, for a single projection
   * 
   * @author Erich Schubert
   */
  // TODO: add a result listener for the reference points.
  protected class ReferencePointsVisualization extends Projection2DVisualization<NV> {
    /**
     * Container element.
     */
    private Element container;

    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     */
    public ReferencePointsVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height) {
      super(context, svgp, proj, width, height);
      double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
      this.container = super.setupCanvas(svgp, proj, margin, width, height);
      this.layer = new VisualizationLayer(Visualizer.LEVEL_DATA, this.container);

      redraw();
    }

    @Override
    public void redraw() {
      // Implementation note: replacing the container element is faster than
      // removing all markers and adding new ones - i.e. a "bluk" operation
      // instead of incremental changes
      Element oldcontainer = null;
      if(container.hasChildNodes()) {
        oldcontainer = container;
        container = (Element) container.cloneNode(false);
      }

      setupCSS(svgp);
      Iterator<NV> iter = result.iterator();

      final double dotsize = context.getStyleLibrary().getSize(StyleLibrary.REFERENCE_POINTS);
      while(iter.hasNext()) {
        NV v = iter.next();
        double[] projected = proj.fastProjectDataToRenderSpace(v);
        Element dot = svgp.svgCircle(projected[0], projected[1], dotsize);
        SVGUtil.addCSSClass(dot, REFPOINT);
        container.appendChild(dot);
      }
      if(oldcontainer != null && oldcontainer.getParentNode() != null) {
        oldcontainer.getParentNode().replaceChild(container, oldcontainer);
      }
    }

    /**
     * Registers the Reference-Point-CSS-Class at a SVGPlot.
     * 
     * @param svgp the SVGPlot to register the -CSS-Class.
     */
    private void setupCSS(SVGPlot svgp) {
      CSSClass refpoint = new CSSClass(svgp, REFPOINT);
      refpoint.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.REFERENCE_POINTS));

      try {
        svgp.getCSSClassManager().addClass(refpoint);
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception("Equally-named CSSClass with different owner already exists", e);
      }
    }
  }
}