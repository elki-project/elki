package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
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
public class ProjectedThumbnail<NV extends NumberVector<NV, ?>> extends ThumbnailVisualization<NV> {
  /**
   * The current projection
   */
  protected VisualizationProjection proj;
  
  /**
   * Actual visualizer
   */
  protected ProjectedVisualizer vis;
  
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
  public ProjectedThumbnail(ProjectedVisualizer vis, VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution, int mask) {
    super(context, svgp, width, height, Visualizer.LEVEL_DATA, tresolution, mask);
    this.vis = vis;
    this.proj = proj;
  }

  @Override
  protected Visualization drawThumbnail(SVGPlot plot) {
    return vis.visualize(plot, proj, width, height);
  }
}
