package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ThumbnailVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Generic thumbnail visualizer converting an existing visualization to a thumbnail.
 * 
 * @author Erich Schubert
 *
 * @param <NV>
 */
public class Projection2DThumbnail<NV extends NumberVector<NV, ?>> extends ThumbnailVisualization<NV> {
  /**
   * The current projection
   */
  protected VisualizationProjection proj;
  
  /**
   * Actual visualizer
   */
  protected Projection2DVisualizer<NV> vis;
  
  /**
   * Constructor.
   * 
   * @param context Visualization context
   * @param svgp Plot
   * @param proj Projection
   * @param width Width
   * @param height Height
   * @param tresolution Thumbnail Resolution
   */
  public Projection2DThumbnail(Projection2DVisualizer<NV> vis, VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution) {
    super(context, svgp, width, height, Visualizer.LEVEL_DATA, tresolution);
    this.vis = vis;
    this.proj = proj;
  }

  @Override
  protected Visualization drawThumbnail(SVGPlot plot) {
    return vis.visualize(plot, proj, width, height);
  }
}
