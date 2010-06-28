package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * A projected visualizer needs a projection for visualization.
 * 
 * @author Erich Schubert
 */
public interface ProjectedVisualizer extends Visualizer {  
  /**
   * Returns an Element representing a visualization.
   * 
   * @param svgp plot to contain the returned Element.
   * @param proj projection to use
   * @param width Width of plot
   * @param height Height of plot
   * @return a materialized visualization
   */
  public Visualization visualize(SVGPlot svgp, VisualizationProjection proj, double width, double height);
  
  /**
   * Returns a thumbnail representation of the element.
   * 
   * @param svgp plot to contain the returned Element.
   * @param proj projection to use
   * @param width Width of plot
   * @param height Height of plot
   * @param tresolution Resolution of thumbnail
   * @return a materialized visualization
   */
  public Visualization makeThumbnail(SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution);
}