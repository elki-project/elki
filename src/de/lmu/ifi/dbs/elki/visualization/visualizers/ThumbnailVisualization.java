package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.io.File;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.ThumbnailThread;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.Thumbnailer;

/**
 * Thumbnail visualization.
 * 
 * @author Erich Schubert
 */
public abstract class ThumbnailVisualization<O extends DatabaseObject> implements Visualization, ThumbnailThread.Listener {
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
   * Constructor.
   * 
   * @param context context
   * @param svgp Plot
   * @param width Width
   * @param height Height
   * @param level Level
   * @param tresolution Resolution of thumbnail
   */
  public ThumbnailVisualization(VisualizerContext<? extends O> context, SVGPlot svgp, double width, double height, Integer level, int tresolution) {
    super();
    this.context = context;
    this.svgp = svgp;
    this.width = width;
    this.height = height;
    this.level = level;
    this.tresolution = tresolution;
    this.layer = svgp.svgElement(SVGConstants.SVG_G_TAG);
    this.thumb = null;
  }

  @Override
  public void destroy() {
    if(pendingThumbnail != null) {
      ThumbnailThread.UNQUEUE(pendingThumbnail);
    }
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
      final double ratio = width / height;
      final int tw;
      final int th;
      if (ratio >= 1.0) {
        tw = (int) (ratio * tresolution);
        th = tresolution;
      } else {
        tw = tresolution;
        th = (int) (tresolution / ratio);
      }
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
}