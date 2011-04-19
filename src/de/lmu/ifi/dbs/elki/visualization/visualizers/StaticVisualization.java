package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

/**
 * Static visualization
 * 
 * @author Erich Schubert
 */
public class StaticVisualization extends AbstractVisualization {
  /**
   * Unchanging precomputed visualization.
   * 
   * @param task Task to visualize
   * @param element Element containing the resulting visualization
   */
  public StaticVisualization(VisualizationTask task, Element element) {
    super(task);
    this.layer = element;
  }

  @Override
  protected void incrementalRedraw() {
    // Do nothing - we keep our static layer
  }

  @Override
  protected void redraw() {
    // Do nothing - we keep our static layer
  }
}
