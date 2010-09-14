package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * Visualization info that needs projection information.
 * 
 * @author Erich Schubert
 */
class VisualizationProjectedInfo<P extends Projection> extends VisualizationInfo {
  /**
   * Projection to use in this visualization.
   */
  P proj;

  /**
   * Visualizer to use.
   */
  ProjectedVisualizer<P> vis;

  /**
   * Constructor.
   * 
   * @param vis Visualizer to use
   * @param proj Projection to use
   * @param width Width
   * @param height Height
   */
  public VisualizationProjectedInfo(ProjectedVisualizer<P> vis, P proj, double width, double height) {
    super(width, height);
    this.vis = vis;
    this.proj = proj;
  }

  @Override
  public Visualization build(SVGPlot plot, double width, double height) {
    synchronized(vis) {
      return vis.visualize(plot, proj, width, height);
    }
  }

  @Override
  public Visualization buildThumb(SVGPlot plot, double width, double height, int tresolution) {
    synchronized(vis) {
      return vis.makeThumbnail(plot, proj, width, height, tresolution);
    }
  }

  @Override
  public Visualizer getVisualizer() {
    return vis;
  }
}