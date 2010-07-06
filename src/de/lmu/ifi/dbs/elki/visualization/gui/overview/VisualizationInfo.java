package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.io.File;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Class representing a single visualization on the screen.
 * 
 * @author Erich Schubert
 */
public abstract class VisualizationInfo {
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
   * Constructor.
   * 
   * @param width Width
   * @param height Height
   */
  public VisualizationInfo(double width, double height) {
    super();
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
  public abstract Visualization build(SVGPlot plot, double width, double height);

  /**
   * Build (render) the visualization into an SVG tree in thumbnail mode.
   * 
   * @param plot SVG plot context (factory)
   * @param width Canvas width
   * @param height Canvas height
   * @param tresolution Thumbnail resolution
   * @return SVG subtree
   */
  public abstract Visualization buildThumb(SVGPlot plot, double width, double height, int tresolution);

  /**
   * Get the visualizer responsible for this visualization.
   * 
   * @return the actual visualizer involved.
   */
  public abstract Visualizer getVisualizer();

  /**
   * Test whether a thumbnail is needed for this visualization.
   * 
   * @return Whether or not to generate a thumbnail.
   */
  public boolean thumbnailEnabled() {
    Boolean nothumb = getVisualizer().getMetadata().get(Visualizer.META_NOTHUMB, Boolean.class);
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