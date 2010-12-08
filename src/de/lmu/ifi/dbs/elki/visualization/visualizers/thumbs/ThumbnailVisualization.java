package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import java.io.File;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.ThumbnailThread;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ResizedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

/**
 * Thumbnail visualization.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Thumbnailer
 * @apiviz.uses ThumbnailThread
 */
public class ThumbnailVisualization<O extends DatabaseObject> implements Visualization, ThumbnailThread.Listener, ContextChangeListener, DataStoreListener<O> {
  /**
   * Constant to listen for data changes
   */
  public static final int ON_DATA = 1;

  /**
   * Constant to listen for selection changes
   */
  public static final int ON_SELECTION = 2;
  
  /**
   * Visualizer factory
   */
  protected final VisFactory<? extends O> visFactory;
  
  /**
   * Visualization task
   */
  protected final VisualizationTask task;

  /**
   * Context
   */
  protected final VisualizerContext<? extends O> context;

  /**
   * The thumbnail file.
   */
  protected File thumb = null;

  /**
   * Pending redraw
   */
  protected ThumbnailThread.Task pendingThumbnail = null;

  /**
   * Layer storage
   */
  protected Element layer;

  /**
   * Thumbnail resolution
   */
  protected int tresolution;

  /**
   * The latest pending redraw
   */
  private Runnable pendingRedraw = null;

  /**
   * The event mask. See {@link #ON_DATA}, {@link #ON_SELECTION}
   */
  private int mask;

  /**
   * Constructor.
   * 
   * @param visFactory Visualizer Factory to use
   * @param task Task to use
   * @param mask Event mask (for auto-updating)
   */
  public ThumbnailVisualization(VisFactory<? extends O> visFactory, VisualizationTask task, int mask) {
    super();
    this.visFactory = visFactory;
    this.task = task;
    this.context = task.getContext();
    Integer tres = task.getGenerics(VisualizationTask.THUMBNAIL_RESOLUTION, Integer.class);
    this.tresolution = tres;
    this.layer = task.getPlot().svgElement(SVGConstants.SVG_G_TAG);
    this.thumb = null;
    this.mask = mask;
    // Listen for database events only when needed.
    if((mask & ON_DATA) == ON_DATA) {
      context.addDataStoreListener(this);
    }
    // Always listen for context changes, in particular resize.
    context.addContextChangeListener(this);
  }

  @Override
  public void destroy() {
    if(pendingThumbnail != null) {
      ThumbnailThread.UNQUEUE(pendingThumbnail);
    }
    context.removeContextChangeListener(this);
    context.removeDataStoreListener(this);
  }

  @Override
  public Element getLayer() {
    if(thumb == null) {
      synchronizedRedraw();
    }
    return layer;
  }

  @Override
  public void contextChanged(ContextChangedEvent e) {
    if(testRedraw(e)) {
      refreshThumbnail();
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
    if((mask & ON_SELECTION) == ON_SELECTION && e instanceof SelectionChangedEvent) {
      return true;
    }
    return false;
  }
  
  @Override
  public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<O> e) {
    refreshThumbnail();
  }

  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  protected final synchronized void synchronizedRedraw() {
    // FIXME: only run once!
    pendingRedraw = new Runnable() {
      @Override
      public void run() {
        executePendingRedraw(this);
      }
    };
    task.getPlot().scheduleUpdate(pendingRedraw);
  }

  /**
   * Execute the pending redraw, if it is the latest one.
   * 
   * @param t the current pending redraw request.
   */
  protected final void executePendingRedraw(Runnable t) {
    if(t == pendingRedraw) {
      pendingRedraw = null;
      incrementalRedraw();
    }
  }

  /**
   * Redraw the visualization (maybe incremental).
   * 
   * Optional - by default, it will do a full redraw, which often is faster!
   */
  protected void incrementalRedraw() {
    final Element oldcontainer;
    if(layer.hasChildNodes()) {
      oldcontainer = layer;
      layer = (Element) layer.cloneNode(false);
    }
    else {
      oldcontainer = null;
    }
    redraw();
    if(oldcontainer != null && oldcontainer.getParentNode() != null) {
      oldcontainer.getParentNode().replaceChild(layer, oldcontainer);
    }
  }

  /**
   * Perform a full redraw.
   */
  protected void redraw() {
    if(thumb == null) {
      // LoggingUtil.warning("Generating new thumbnail " + this);
      layer.appendChild(SVGUtil.svgWaitIcon(task.getPlot().getDocument(), 0, 0, task.getWidth(), task.getHeight()));
      if(pendingThumbnail == null) {
        pendingThumbnail = ThumbnailThread.QUEUE(this);
      }
    }
    else {
      // LoggingUtil.warning("Injecting Thumbnail " + this);
      Element i = task.getPlot().svgElement(SVGConstants.SVG_IMAGE_TAG);
      SVGUtil.setAtt(i, SVGConstants.SVG_X_ATTRIBUTE, 0);
      SVGUtil.setAtt(i, SVGConstants.SVG_Y_ATTRIBUTE, 0);
      SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, task.getWidth());
      SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, task.getHeight());
      i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, thumb.toURI().toString());
      layer.appendChild(i);
    }
  }

  @Override
  public synchronized void doThumbnail(Thumbnailer t) {
    pendingThumbnail = null;
    try {
      SVGPlot plot = new SVGPlot();
      plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + task.getWidth() + " " + task.getHeight());
      
      // Work on a clone
      VisualizationTask clone = task.clone(plot);
      clone.put(VisualizationTask.THUMBNAIL, false);
      Visualization vis = visFactory.makeVisualization(clone);
      
      plot.getRoot().appendChild(vis.getLayer());
      plot.updateStyleElement();
      final int tw = (int) (task.getWidth() * tresolution);
      final int th = (int) (task.getHeight() * tresolution);
      thumb = t.thumbnail(plot, tw, th);
      // The visualization will not be used anymore.
      vis.destroy();
      synchronizedRedraw();
    }
    catch(Exception e) {
      // TODO: Replace with error image instead?
      LoggingUtil.exception("Error rendering thumbnail.", e);
    }
  }

  protected void refreshThumbnail() {
    // Discard an existing thumbnail
    thumb = null;
    synchronizedRedraw();
  }
}