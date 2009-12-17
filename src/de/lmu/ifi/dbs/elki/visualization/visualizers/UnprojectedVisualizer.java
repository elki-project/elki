package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * An unprojected Visualizer can run stand-alone.
 * 
 * @author Erich Schubert
 *
 */
public interface UnprojectedVisualizer extends Visualizer {
  /**
   * Returns an Element representing a visualization.
   * 
   * @param svgp the SVGPlot which will act as owner for the returned Element.
   * @return an Element representing a 2-dimensional visualization.
   */
  public Element visualize(SVGPlot svgp, double width, double height);
}
