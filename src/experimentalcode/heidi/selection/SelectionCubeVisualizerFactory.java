package experimentalcode.heidi.selection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Factory for visualizers to generate an SVG-Element containing a cube as marker
 * representing the selected range for each dimension
 * 
 * @author
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionCubeVisualizerFactory<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "SelectionCubeVisualizer";

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public SelectionCubeVisualizerFactory() {
    super(NAME);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   */
  @Override
  public void init(VisualizerContext<? extends NV> context) {
    super.init(context);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer#visualize(de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot, de.lmu.ifi.dbs.elki.visualization.VisualizationProjection, double, double)
   */
  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new SelectionCubeVisualizer<NV>(context, svgp, proj, width, height);
  }

  @Override
  public Visualization makeThumbnail(SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
  }
}
