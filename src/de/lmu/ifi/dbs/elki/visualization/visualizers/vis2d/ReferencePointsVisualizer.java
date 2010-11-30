package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
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
 * @apiviz.has ReferencePointsVisualization oneway - - produces
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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ReferencePointsVisualizer() {
    super(NAME);
    super.metadata.put(Visualizer.META_GROUP, Visualizer.GROUP_METADATA);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param colResult contains all reference points.
   */
  public void init(VisualizerContext<? extends NV> context, CollectionResult<NV> colResult) {
    super.init(context);
    this.result = colResult;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new ReferencePointsVisualization(context, svgp, proj, width, height);
  }

  /**
   * The actual visualization instance, for a single projection
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.result.ReferencePointsResult oneway - - visualizes
   */
  // TODO: add a result listener for the reference points.
  protected class ReferencePointsVisualization extends Projection2DVisualization<NV> {
    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     */
    public ReferencePointsVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_DATA);
      incrementalRedraw();
    }

    @Override
    public void redraw() {
      setupCSS(svgp);
      Iterator<NV> iter = result.iterator();

      final double dotsize = context.getStyleLibrary().getSize(StyleLibrary.REFERENCE_POINTS);
      while(iter.hasNext()) {
        NV v = iter.next();
        double[] projected = proj.fastProjectDataToRenderSpace(v);
        Element dot = svgp.svgCircle(projected[0], projected[1], dotsize);
        SVGUtil.addCSSClass(dot, REFPOINT);
        layer.appendChild(dot);
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
      svgp.addCSSClassOrLogError(refpoint);
    }
  }
}