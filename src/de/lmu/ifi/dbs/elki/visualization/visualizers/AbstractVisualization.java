package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Arrays;
import java.util.Collection;

/**
 * Static, single-layer visualization (with no interactivity)
 * 
 * @author Erich Schubert
 */
public abstract class AbstractVisualization implements Visualization {
  /**
   * Layer storage
   */
  protected VisualizationLayer layer;
  
  /**
   * Width
   */
  // FIXME: keep?
  protected double width;
  
  /**
   * Height
   */
  // FIXME: keep?
  protected double height;  

  public AbstractVisualization(double width, double height) {
    super();
    this.width = width;
    this.height = height;
  }

  @Override
  public void destroy() {
    // Nothing to do here.
  }

  @Override
  public Collection<VisualizationLayer> getLayers() {
    return Arrays.asList(new VisualizationLayer[]{ layer });
  }

  @Override
  public void setVisible(@SuppressWarnings("unused") boolean vis) {
    // TODO: do we need to take care of this here,
    // or is it sufficient if the caller hides the layers?
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