package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.UnprojectedVisualizer;

/**
 * Visualization that does not require extra information for rendering.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has UnprojectedVisualizer
 */
class VisualizationUnprojectedInfo extends VisualizationInfo {
  /**
   * Visualization
   */
  private UnprojectedVisualizer<?> vis;

  /**
   * Constructor
   * 
   * @param vis Visualization
   * @param width Width
   * @param height Height
   */
  public VisualizationUnprojectedInfo(UnprojectedVisualizer<?> vis, double width, double height) {
    super(width, height);
    this.vis = vis;
  }

  @Override
  public Visualization build(SVGPlot plot, double width, double height) {
    synchronized(vis) {
      return vis.visualize(plot, width, height);
    }
  }

  @Override
  public Visualization buildThumb(SVGPlot plot, double width, double height, int tresolution) {
    synchronized(vis) {
      return vis.makeThumbnail(plot, width, height, tresolution);
    }
  }

  @Override
  public Visualizer getVisualizer() {
    return vis;
  }
}