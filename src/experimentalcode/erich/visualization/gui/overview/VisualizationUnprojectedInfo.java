package experimentalcode.erich.visualization.gui.overview;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.erich.visualization.visualizers.UnprojectedVisualizer;

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
   */
  public VisualizationUnprojectedInfo(UnprojectedVisualizer vis) {
    this.vis = vis;
  }

  @Override
  public Element build(SVGPlot plot) {
    synchronized(vis) {
      return vis.visualize(plot);
    }
  }
}