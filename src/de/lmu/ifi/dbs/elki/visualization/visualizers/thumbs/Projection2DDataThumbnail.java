package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d.Projection2DVisualizer;

/**
 * Thumbnail that needs to be regenerated on database changes.
 * 
 * @author Erich Schubert
 *
 * @param <NV> Vector class.
 */
public class Projection2DDataThumbnail<NV extends NumberVector<NV,?>> extends Projection2DThumbnail<NV> implements DatabaseListener<NV> {
  /**
   * Constructor.
   * 
   * @param vis Visualizer to use
   * @param context Context
   * @param svgp Plot
   * @param proj Projection
   * @param width Width
   * @param height Height
   * @param tresolution Thumbnail resolution
   */
  public Projection2DDataThumbnail(Projection2DVisualizer<NV> vis, VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height, int tresolution) {
    super(vis, context, svgp, proj, width, height, tresolution);
    context.addDatabaseListener(this);
  }

  @Override
  public void destroy() {
    super.destroy();
    context.removeDatabaseListener(this);
  }

  @Override
  public void objectsChanged(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
    refreshThumbnail();
  }

  @Override
  public void objectsInserted(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
    refreshThumbnail();
  }

  @Override
  public void objectsRemoved(@SuppressWarnings("unused") DatabaseEvent<NV> e) {
    refreshThumbnail();
  }
}