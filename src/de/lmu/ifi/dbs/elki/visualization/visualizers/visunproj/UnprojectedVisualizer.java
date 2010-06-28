package de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * An unprojected Visualizer can run stand-alone.
 * 
 * @author Erich Schubert
 *
 */
public interface UnprojectedVisualizer<O extends DatabaseObject> extends Visualizer {
  /**
   * Returns an Element representing a visualization.
   * 
   * @param svgp the SVGPlot which will act as owner for the returned Element.
   * @param width Width of plot
   * @param height Height of plot
   * @return an Element representing a 2-dimensional visualization.
   */
  public Visualization visualize(SVGPlot svgp, double width, double height);

  /**
   * Returns a thumbnail representation of the element.
   * 
   * @param svgp plot to contain the returned Element.
   * @param width Width of plot
   * @param height Height of plot
   * @param tresolution Resolution of thumbnail
   * @return a materialized visualization
   */
  public Visualization makeThumbnail(SVGPlot svgp, double width, double height, int tresolution);
}
