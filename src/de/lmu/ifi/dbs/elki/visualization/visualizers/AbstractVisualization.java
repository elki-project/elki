package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;

/**
 * Static, single-layer visualization (with no interactivity)
 * 
 * @author Erich Schubert
 */
public abstract class AbstractVisualization extends AbstractLoggable implements Visualization {
  /**
   * The visualization level
   */
  private final Integer level;
  
  /**
   * Layer storage
   */
  protected Element layer;
  
  /**
   * Width
   */
  protected double width;
  
  /**
   * Height
   */
  protected double height;  

  public AbstractVisualization(double width, double height, Integer level) {
    super();
    this.width = width;
    this.height = height;
    this.level = level;
  }

  @Override
  public void destroy() {
    // Nothing to do here.
  }

  @Override
  public Element getLayer() {
    return layer;
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

  @Override
  public Integer getLevel() {
    return level;
  }
}