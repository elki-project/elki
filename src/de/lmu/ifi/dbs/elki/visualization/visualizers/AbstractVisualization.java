package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ResizedEvent;

/**
 * Abstract base class for visualizations.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.excludeSubtypes
 */
public abstract class AbstractVisualization<O extends DatabaseObject> implements Visualization, ContextChangeListener {
  /**
   * The visualization task we do.
   */
  protected final VisualizationTask task;

  /**
   * Our context
   */
  protected final VisualizerContext<? extends O> context;

  /**
   * The plot we are attached to
   */
  protected final SVGPlot svgp;

  /**
   * Pending redraw
   */
  protected Runnable pendingRedraw = null;

  /**
   * Layer storage
   */
  protected Element layer;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public AbstractVisualization(VisualizationTask task) {
    super();
    this.task = task;
    this.context = task.getContext();
    this.svgp = task.getPlot();
    this.layer = null;
  }

  @Override
  public void destroy() {
    context.removeContextChangeListener(this);
  }

  @Override
  public Element getLayer() {
    if(layer == null) {
      incrementalRedraw();
    }
    return layer;
  }

  /**
   * Get the width
   * 
   * @return the width
   */
  protected double getWidth() {
    return task.getWidth();
  }

  /**
   * Get the height
   * 
   * @return the height
   */
  protected double getHeight() {
    return task.getHeight();
  }

  @Override
  public void contextChanged(ContextChangedEvent e) {
    if(testRedraw(e)) {
      synchronizedRedraw();
    }
  }

  /**
   * Override this method to add additional redraw triggers!
   * 
   * @param e Event
   * @return Test result
   */
  protected boolean testRedraw(ContextChangedEvent e) {
    if(e instanceof ResizedEvent) {
      return true;
    }
    return false;
  }

  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  protected final void synchronizedRedraw() {
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if(AbstractVisualization.this.pendingRedraw == this) {
          AbstractVisualization.this.pendingRedraw = null;
          AbstractVisualization.this.incrementalRedraw();
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
    if(layer != null && layer.hasChildNodes()) {
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