package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Static visualization
 * 
 * @author Erich Schubert
 */
public class StaticVisualization extends AbstractVisualization<DatabaseObject> {
  /**
   * Constructor for a static visualization.
   * 
   * @param context Context
   * @param plot Plot
   * @param level Level
   * @param element Element
   * @param width Width
   * @param height Height
   */
  public StaticVisualization(VisualizerContext<? extends DatabaseObject> context, SVGPlot plot, Integer level, Element element, double width, double height) {
    super(context, plot, width, height, level);
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
