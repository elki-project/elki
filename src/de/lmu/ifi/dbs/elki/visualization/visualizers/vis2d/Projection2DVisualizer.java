package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.has Projection2DVisualization oneway - - produces
 * @apiviz.has ProjectedThumbnail
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public abstract class Projection2DVisualizer<NV extends NumberVector<NV, ?>> extends AbstractVisualizer<NV> implements ProjectedVisualizer<Projection2D> {
  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   * @param level the visualizer level 
   */
  public Projection2DVisualizer(String name, int level) {
    super(name, level);
  }

  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   */
  protected Projection2DVisualizer(String name) {
    super(name);
  }
  
  // Default operation to render thumbnails
  @Override
  public Visualization makeThumbnail(SVGPlot svgp, Projection2D proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV, Projection2D>(this, context, svgp, proj, width, height, tresolution, 0);
  }
}