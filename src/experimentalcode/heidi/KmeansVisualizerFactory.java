package experimentalcode.heidi;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ThumbnailVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Factory for visualizers to generate an SVG-Element containing a marker for the mean 
 * in a KMeans-Clustering
 * 
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class KmeansVisualizerFactory<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "KMeans";

  /**
   * Clustering to visualize.
   */
  protected Clustering<MeanModel<NV>> clustering = null;
  
  /**
   * Constructor
   */
  public KmeansVisualizerFactory() {
    super(NAME);
  }

  @Override
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height) {
    return new KmeansVisualizer<NV>(context, clustering, svgp, proj, width, height);
  }

  /**
   * Initializes this Visualizer.
   * 
   * @param context Visualization context
   * @param clustering Clustering to visualize
   */
  public void init(VisualizerContext<? extends NV> context, Clustering<MeanModel<NV>> clustering) {
    super.init(context);
    super.setLevel(Visualizer.LEVEL_BACKGROUND + 1);
    this.clustering = clustering;
  }
  @Override
  public Visualization makeThumbnail(SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV>(this, context, svgp, proj, width, height, tresolution, ThumbnailVisualization.ON_DATA | ThumbnailVisualization.ON_SELECTION);
  }
}
