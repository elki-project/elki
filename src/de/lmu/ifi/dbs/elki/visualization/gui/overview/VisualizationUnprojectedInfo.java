package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.UnprojectedVisFactory;

/**
 * Visualization that does not require extra information for rendering.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has UnprojectedVisualizer
 */
@Deprecated
class VisualizationUnprojectedInfo extends VisualizationInfo {
  /**
   * Visualization
   */
  private UnprojectedVisFactory<DatabaseObject> vis;
  
  /**
   * The result to visualize
   */
  private AnyResult result;

  /**
   * Constructor
   * 
   * @param vis Visualization
   * @param width Width
   * @param height Height
   */
  @SuppressWarnings("unchecked")
  public VisualizationUnprojectedInfo(AnyResult result, UnprojectedVisFactory<?> vis, double width, double height) {
    super(width, height);
    this.result = result;
    this.vis = (UnprojectedVisFactory<DatabaseObject>) vis;
  }

  @Override
  public Visualization build(VisualizerContext<? extends DatabaseObject> context, SVGPlot plot, double width, double height) {
    VisualizationTask task = new VisualizationTask(context, result, null, plot, width, height);
    synchronized(vis) {
      return vis.makeVisualization(task);
    }
  }

  @Override
  public Visualization buildThumb(VisualizerContext<? extends DatabaseObject> context, SVGPlot plot, double width, double height, int tresolution) {
    VisualizationTask task = new VisualizationTask(context, result, null, plot, width, height);
    synchronized(vis) {
      return vis.makeVisualizationOrThumbnail(task);
    }
  }

  @Override
  public VisFactory<?> getVisualizer() {
    return vis;
  }
}