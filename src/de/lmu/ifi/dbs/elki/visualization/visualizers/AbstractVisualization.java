package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Abstract base class for visualizations.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractVisualization<O extends DatabaseObject> extends AbstractLoggable implements Visualization {
  /**
   * The visualization level
   */
  private final Integer level;
  
  /**
   * Our context
   */
  protected VisualizerContext<? extends O> context;

  /**
   * The plot we are attached to
   */
  protected SVGPlot svgp;

  /**
   * Pending redraw
   */
  protected Runnable pendingRedraw = null;

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

  /**
   * Constructor.
   * 
   * @param context Context
   * @param svgp Plot
   * @param width Width
   * @param height Height
   * @param level Level
   */
  public AbstractVisualization(VisualizerContext<? extends O> context, SVGPlot svgp, double width, double height, Integer level) {
    super();
    this.context = context;
    this.svgp = svgp;
    this.width = width;
    this.height = height;
    this.level = level;
    this.layer = null;
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

  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  protected final void synchronizedRedraw() {
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if (pendingRedraw == this) {
          pendingRedraw = null;
          incrementalRedraw();
        }
      }
    };
    pendingRedraw = pr;
    svgp.scheduleUpdate(pr);
  }

  /**
   * Redraw the visualization (maybe incremental).
   * 
   * Optional - by default, it will do a full redraw, which often is faster!
   */
  protected void incrementalRedraw() {
    Element oldcontainer = null;
    if(layer.hasChildNodes()) {
      oldcontainer = layer;
      layer = (Element) layer.cloneNode(false);
    }
    redraw();
    if(oldcontainer != null && oldcontainer.getParentNode() != null) {
      oldcontainer.getParentNode().replaceChild(layer, oldcontainer);
    }
  }

  /**
   * Perform a full redraw.
   */
  protected abstract void redraw();
}