package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.UnprojectedVisualizer;

/**
 * Generic thumbnail visualizer converting an existing visualization to a thumbnail.
 * 
 * @author Erich Schubert
 *
 * @param <O> object type
 */
public class UnprojectedThumbnail<O extends DatabaseObject> extends ThumbnailVisualization<O> {
  /**
   * Actual visualizer
   */
  protected UnprojectedVisualizer<O> vis;
  
  /**
   * Constructor.
   * 
   * @param vis Visualizer to call
   * @param context Visualization context
   * @param svgp Plot
   * @param width Width
   * @param height Height
   * @param tresolution Resolution of thumbnail
   * @param mask Redraw event mask
   */
  public UnprojectedThumbnail(UnprojectedVisualizer<O> vis, VisualizerContext<? extends O> context, SVGPlot svgp, double width, double height, int tresolution, int mask) {
    super(context, svgp, width, height, Visualizer.LEVEL_DATA, tresolution, mask);
    this.vis = vis;
  }

  @Override
  protected Visualization drawThumbnail(SVGPlot plot) {
    return vis.visualize(plot, width, height);
  }
}