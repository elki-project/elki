package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.ProjectedVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualization info that needs projection information.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ProjectedVisualizer
 */
@Deprecated
class VisualizationProjectedInfo<P extends Projection> extends VisualizationInfo {
  /**
   * Projection to use in this visualization.
   */
  P proj;

  /**
   * Visualizer to use.
   */
  ProjectedVisFactory<DatabaseObject> vis;

  /**
   * The result to visualize
   */
  private AnyResult result;

  /**
   * Constructor.
   * 
   * @param vis Visualizer to use
   * @param proj Projection to use
   * @param width Width
   * @param height Height
   */
  @SuppressWarnings("unchecked")
  public VisualizationProjectedInfo(AnyResult result, ProjectedVisFactory<?> vis, P proj, double width, double height) {
    super(width, height);
    this.result = result;
    this.vis = (ProjectedVisFactory<DatabaseObject>) vis;
    this.proj = proj;
  }

  @Override
  public Visualization build(VisualizerContext<? extends DatabaseObject> context, SVGPlot plot, double width, double height) {
    VisualizationTask task = new VisualizationTask(context, result, proj, plot, width, height);
    synchronized(vis) {
      return vis.makeVisualization(task);
    }
  }

  @Override
  public Visualization buildThumb(VisualizerContext<? extends DatabaseObject> context, SVGPlot plot, double width, double height, int tresolution) {
    VisualizationTask task = new VisualizationTask(context, result, proj, plot, width, height);
    task.put(VisualizationTask.THUMBNAIL, true);
    task.put(VisualizationTask.THUMBNAIL_RESOLUTION, tresolution);
    synchronized(vis) {
      return vis.makeVisualizationOrThumbnail(task);
    }
  }

  @Override
  public VisFactory<?> getVisualizer() {
    return vis;
  }
}