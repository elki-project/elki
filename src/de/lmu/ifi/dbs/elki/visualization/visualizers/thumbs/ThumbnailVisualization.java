package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import java.io.File;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.ThumbnailThread;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ResizedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

/**
 * Thumbnail visualization.
 * 
 * @author Erich Schubert
 */
public abstract class ThumbnailVisualization<O extends DatabaseObject> implements Visualization, ThumbnailThread.Listener, ContextChangeListener, DatabaseListener<O> {
  /**
   * Constant to listen for data changes
   */
  public static final int ON_DATA = 1;

  /**
   * Constant to listen for selection changes
   */
  public static final int ON_SELECTION = 2;

  /**
   * Context
   */
  protected VisualizerContext<? extends O> context;

  /**
   * The visualization level
   */
  private final Integer level;

  /**
   * The plot we are attached to
   */
  protected SVGPlot svgp;

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
   * Width
   */
  protected double width;

  /**
   * Height
   */
  protected double height;
  
  /**
   * Thumbnail resolution
   */
  protected int tresolution;

  /**
   * The event mask. See {@link #ON_DATA}, {@link #ON_SELECTION}
   */
  private int mask;

  /**
   * Constructor.
   * 
   * @param context context
   * @param svgp Plot
   * @param width Width
   * @param height Height
   * @param level Level
   * @param tresolution Resolution of thumbnail
   * @param mask Mask of event to redraw on
   */
  public ThumbnailVisualization(VisualizerContext<? extends O> context, SVGPlot svgp, double width, double height, Integer level, int tresolution, int mask) {
    super();
    this.context = context;
    this.svgp = svgp;
    this.width = width;
    this.height = height;
    this.level = level;
    this.tresolution = tresolution;
    this.layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.thumb = null;
    this.mask = mask;
    // Listen for database events only when needed.
    if ((mask & ON_DATA) == ON_DATA) {
      context.addDatabaseListener(this);
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
    context.removeDatabaseListener(this);
  }

  @Override
  public Element getLayer() {
    if(thumb == null) {
      synchronizedRedraw();
    }
    return layer;
  }

  @Override
  public Integer getLevel() {
    return level;
  }

  @Override
  public void contextChanged(ContextChangedEvent e) {
    if (testRedraw(e)) {
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
    if (e instanceof ResizedEvent) {
      return true;
    }
    if ((mask & ON_SELECTION) == ON_SELECTION && e instanceof SelectionChangedEvent) {
      return true;
    }
    return false;
  }

  @Override
  public void objectsChanged(@SuppressWarnings("unused") DatabaseEvent<O> e) {
    refreshThumbnail();
  }

  @Override
  public void objectsInserted(@SuppressWarnings("unused") DatabaseEvent<O> e) {
    refreshThumbnail();
  }

  @Override
  public void objectsRemoved(@SuppressWarnings("unused") DatabaseEvent<O> e) {
    refreshThumbnail();
  }
  
  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  protected final void synchronizedRedraw() {
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        incrementalRedraw();
      }
    };
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
  protected void redraw() {
    if(thumb == null) {
      //LoggingUtil.warning("Generating new thumbnail " + this);
      layer.appendChild(SVGUtil.svgWaitIcon(svgp.getDocument(), 0, 0, width, height));
      if(pendingThumbnail == null) {
        pendingThumbnail = ThumbnailThread.QUEUE(this);
      }
    }
    else {
      //LoggingUtil.warning("Injecting Thumbnail " + this);
      Element i = svgp.svgElement(SVGConstants.SVG_IMAGE_TAG);
      SVGUtil.setAtt(i, SVGConstants.SVG_X_ATTRIBUTE, 0);
      SVGUtil.setAtt(i, SVGConstants.SVG_Y_ATTRIBUTE, 0);
      SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, width);
      SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, height);
      i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, thumb.toURI().toString());
      layer.appendChild(i);
    }
  }

  @Override
  public synchronized void doThumbnail(Thumbnailer t) {
    pendingThumbnail = null;
    try {
      SVGPlot plot = new SVGPlot();
      plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + width + " " + height);
      Visualization vis = drawThumbnail(plot);
      plot.getRoot().appendChild(vis.getLayer());
      plot.updateStyleElement();
      //final double ratio = width / height;
      final int tw = (int)(width * tresolution);
      final int th = (int)(height * tresolution);
      /*if (ratio >= 1.0) {
        tw = (int) (ratio * tresolution);
        th = tresolution;
      } else {
        tw = tresolution;
        th = (int) (tresolution / ratio);
      }*/
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

  protected abstract Visualization drawThumbnail(SVGPlot plot);

  protected void refreshThumbnail() {
    // Discard an existing thumbnail
    thumb = null;
    synchronizedRedraw();
  }
}