package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.io.File;

import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Class representing a single visualization on the screen.
 * 
 * @author Erich Schubert
 */
public class VisualizationInfo {
  /**
   * Thumbnail reference.
   */
  protected File thumbnail = null;
  
  /**
   * Width
   */
  protected double width;
  
  /**
   * Height
   */
  protected double height;
  
  /**
   * Visualizer Factory
   */
  protected VisFactory<?> vis;
  
  /**
   * The result to visualize
   */
  private AnyResult result;

  /**
   * Projection (optional)
   */
  protected Projection proj;
  
  /**
   * Constructor.
   * 
   * @param vis Visualizer factory
   * @param result Result
   * @param proj Projection to use (may be 0)
   * @param width Width
   * @param height Height
   */
  public VisualizationInfo(VisFactory<?> vis, AnyResult result, Projection proj, double width, double height) {
    super();
    this.vis = vis;
    this.result = result;
    this.proj = proj;
    this.width = width;
    this.height = height;
  }

  /**
   * Build (render) the visualization into an SVG tree.
   * 
   * @param plot SVG plot context (factory)
   * @param width Canvas width
   * @param height Canvas height
   * @return SVG subtree
   */
  public Visualization build(VisualizerContext<?> context, SVGPlot plot, double width, double height) {
    VisualizationTask task = new VisualizationTask(context, result, proj, plot, width, height);
    synchronized(vis) {
      return vis.makeVisualization(task);
    }
  }

  /**
   * Build (render) the visualization into an SVG tree in thumbnail mode.
   * 
   * @param plot SVG plot context (factory)
   * @param width Canvas width
   * @param height Canvas height
   * @param tresolution Thumbnail resolution
   * @return SVG subtree
   */
  public Visualization buildThumb(VisualizerContext<?> context, SVGPlot plot, double width, double height, int tresolution) {
    VisualizationTask task = new VisualizationTask(context, result, proj, plot, width, height);
    task.put(VisualizationTask.THUMBNAIL, true);
    task.put(VisualizationTask.THUMBNAIL_RESOLUTION, tresolution);
    synchronized(vis) {
      return vis.makeVisualizationOrThumbnail(task);
    }
  }

  /**
   * Get the visualizer responsible for this visualization.
   * 
   * @return the actual visualizer involved.
   */
  public VisFactory<?> getVisualizer() {
    return vis;
  }

  /**
   * Test whether a thumbnail is needed for this visualization.
   * 
   * @return Whether or not to generate a thumbnail.
   */
  public boolean thumbnailEnabled() {
    Boolean nothumb = getVisualizer().getMetadata().get(VisFactory.META_NOTHUMB, Boolean.class);
    if (nothumb != null && nothumb) {
      return false;
    }
    return true;
  }
  
  /**
   * Test whether a detail view is available.
   * 
   * @return Whether or not a detail view is available.
   */
  public boolean hasDetails() {
    return true;
  }
  
  /**
   * Test whether the visualization is set to be visible.
   * 
   * @return Whether or not to show this visualization.
   */
  public boolean isVisible() {
    return VisualizerUtil.isVisible(getVisualizer());
  }
  
  /**
   * Get the width
   * 
   * @return the width
   */
  protected double getWidth() {
    return width;
  }

  /**
   * Get the height
   * 
   * @return the height
   */
  protected double getHeight() {
    return height;
  }
}