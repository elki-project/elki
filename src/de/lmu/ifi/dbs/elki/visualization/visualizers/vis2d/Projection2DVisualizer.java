package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs.Projection2DThumbnail;

/**
 * Produces visualizations of 2-dimensional projections. <br>
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public abstract class Projection2DVisualizer<NV extends NumberVector<NV, ?>> extends AbstractVisualizer<NV> implements ProjectedVisualizer {
  // Default operation to render thumbnails
  @Override
  public Visualization makeThumbnail(SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution) {
    return new Projection2DThumbnail<NV>(this, context, svgp, proj, width, height, tresolution);
  }
}