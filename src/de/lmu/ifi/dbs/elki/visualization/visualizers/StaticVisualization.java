package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

/**
 * Static visualization
 * 
 * @author Erich Schubert
 */
public class StaticVisualization extends AbstractVisualization {
  /**
   * Constructor for a static visualization.
   * 
   * @param level Level
   * @param element Element
   * @param width Width
   * @param height Height
   */
  public StaticVisualization(Integer level, Element element, double width, double height) {
    super(width, height, level);
    this.layer = element;
  }
}
