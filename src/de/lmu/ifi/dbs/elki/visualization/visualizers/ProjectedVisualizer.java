package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

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
   * @return an Element representing a 2-dimensional visualization.
   */
  public Element visualize(SVGPlot svgp, VisualizationProjection proj);
}