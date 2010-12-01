package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * Static visualization
 * 
 * @author Erich Schubert
 */
public class StaticVisualization extends AbstractVisualization<DatabaseObject> {
  /**
   * Unchanging precomputed visualization.
   * 
   * @param task Task to visualize
   * @param element Element containing the resulting visualization
   * @param level Level
   */
  public StaticVisualization(VisualizationTask task, Element element, Integer level) {
    super(task, level);
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
