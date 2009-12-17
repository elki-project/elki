package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.UnprojectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * Visualization that does not require extra information for rendering.
 * 
 * @author Erich Schubert
 */
class VisualizationUnprojectedInfo extends VisualizationInfo {
  /**
   * Visualization
   */
  private UnprojectedVisualizer vis;

  /**
   * Constructor
   * 
   * @param vis Visualization
   * @param width Width
   * @param height Height
   */
  public VisualizationUnprojectedInfo(UnprojectedVisualizer vis, double width, double height) {
    super(width, height);
    this.vis = vis;
  }

  @Override
  public Element build(SVGPlot plot) {
    synchronized(vis) {
      return vis.visualize(plot, width, height);
    }
  }

  @Override
  protected Visualizer getVisualization() {
    return vis;
  }
}