package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer;
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
public class ProjectedThumbnail<NV extends NumberVector<NV, ?>, P extends Projection> extends ThumbnailVisualization<NV> {
  /**
   * The current projection
   */
  protected P proj;
  
  /**
   * Actual visualizer
   */
  protected ProjectedVisualizer<P> vis;
  
  /**
   * Constructor.
   * 
   * @param context Visualization context
   * @param svgp Plot
   * @param proj Projection
   * @param width Width
   * @param height Height
   * @param tresolution Thumbnail Resolution
   * @param mask Event mask
   */
  public ProjectedThumbnail(ProjectedVisualizer<P> vis, VisualizerContext<? extends NV> context, SVGPlot svgp, P proj, double width, double height, int tresolution, int mask) {
    super(context, svgp, width, height, Visualizer.LEVEL_DATA, tresolution, mask);
    this.vis = vis;
    this.proj = proj;
  }

  @Override
  protected Visualization drawThumbnail(SVGPlot plot) {
    return vis.visualize(plot, proj, width, height);
  }
}
