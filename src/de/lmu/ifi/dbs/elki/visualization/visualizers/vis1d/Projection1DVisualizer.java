package de.lmu.ifi.dbs.elki.visualization.visualizers.vis1d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection1D;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.ProjectedThumbnail;

/**
 * Produces visualizations of 1-dimensional projections.
 * 
 * @author Remigius Wojdanowski
 * @author Erich Schubert
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public abstract class Projection1DVisualizer<NV extends NumberVector<NV, ?>> extends AbstractVisualizer<NV> implements ProjectedVisualizer<Projection1D> {
  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   * @param level the visualizer level 
   */
  public Projection1DVisualizer(String name, int level) {
    super(name, level);
  }

  /**
   * Constructor.
   * 
   * @param name a short name characterizing this Visualizer
   */
  protected Projection1DVisualizer(String name) {
    super(name);
  }

  // Default operation to render thumbnails
  @Override
  public Visualization makeThumbnail(SVGPlot svgp, Projection1D proj, double width, double height, int tresolution) {
    return new ProjectedThumbnail<NV, Projection1D>(this, context, svgp, proj, width, height, tresolution, 0);
  }
}