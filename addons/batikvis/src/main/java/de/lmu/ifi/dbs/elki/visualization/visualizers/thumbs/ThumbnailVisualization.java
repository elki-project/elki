/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

import java.awt.image.BufferedImage;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.VisualizationListener;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.ThumbnailRegistryEntry;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Thumbnail visualization.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - ThumbnailThread
 */
public class ThumbnailVisualization extends AbstractVisualization implements ThumbnailThread.Listener, DataStoreListener, VisualizationListener {
  /**
   * Visualizer factory
   */
  protected final VisFactory visFactory;

  /**
   * The thumbnail id.
   */
  protected int thumbid = -1;

  /**
   * Pending redraw
   */
  protected ThumbnailThread.Task pendingThumbnail = null;

  /**
   * Thumbnail resolution
   */
  protected int tresolution;

  /**
   * Our thumbnail (keep a reference to prevent garbage collection!)
   */
  private BufferedImage thumb;

  /**
   * Plot the thumbnail is in.
   */
  private SVGPlot plot;

  /**
   * Projection.
   */
  private Projection proj;

  /**
   * Constructor.
   *
   * @param context Visualizer context
   * @param visFactory Visualizer Factory to use
   * @param task Task to use
   * @param plot Plot to draw to
   * @param width Embedding width
   * @param height Embedding height
   * @param proj Projection
   * @param thumbsize Thumbnail size
   */
  public ThumbnailVisualization(VisualizerContext context, VisFactory visFactory, VisualizationTask task, VisualizationPlot plot, double width, double height, Projection proj, int thumbsize) {
    super(context, task, plot, width, height);
    this.visFactory = visFactory;
    this.plot = plot;
    this.proj = proj;
    this.tresolution = thumbsize;
    this.layer = plot.svgElement(SVGConstants.SVG_G_TAG);
    this.thumbid = -1;
    this.thumb = null;
    addListeners();
  }

  @Override
  public void destroy() {
    if(pendingThumbnail != null) {
      ThumbnailThread.unqueue(pendingThumbnail);
    }
    // TODO: remove image from registry?
    super.destroy();
  }

  @Override
  public Element getLayer() {
    if(thumbid < 0) {
      svgp.requestRedraw(this.task, this);
    }
    return layer;
  }

  /**
   * Perform a full redraw.
   */
  @Override
  public void fullRedraw() {
    if(!(getWidth() > 0 && getHeight() > 0)) {
      LoggingUtil.warning("Thumbnail of zero size requested: " + visFactory);
      return;
    }
    if(thumbid < 0) {
      // LoggingUtil.warning("Generating new thumbnail " + this);
      layer.appendChild(SVGUtil.svgWaitIcon(plot.getDocument(), 0, 0, getWidth(), getHeight()));
      if(pendingThumbnail == null) {
        pendingThumbnail = ThumbnailThread.queue(this);
      }
      return;
    }
    // LoggingUtil.warning("Injecting Thumbnail " + this);
    Element i = plot.svgElement(SVGConstants.SVG_IMAGE_TAG);
    SVGUtil.setAtt(i, SVGConstants.SVG_X_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_Y_ATTRIBUTE, 0);
    SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, getWidth());
    SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, getHeight());
    i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, ThumbnailRegistryEntry.INTERNAL_PROTOCOL + ":" + thumbid);
    layer.appendChild(i);
  }

  @Override
  public synchronized void doThumbnail() {
    pendingThumbnail = null;
    try {
      VisualizationPlot plot = new VisualizationPlot();
      plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + getWidth() + " " + getHeight());

      // Work on a clone
      Visualization vis = visFactory.makeVisualization(context, task, plot, getWidth(), getHeight(), proj);

      plot.getRoot().appendChild(vis.getLayer());
      plot.updateStyleElement();
      final int tw = (int) (getWidth() * tresolution);
      final int th = (int) (getHeight() * tresolution);
      thumb = plot.makeAWTImage(tw, th);
      thumbid = ThumbnailRegistryEntry.registerImage(thumb);
      // The visualization will not be used anymore.
      vis.destroy();
      svgp.requestRedraw(this.task, this);
    }
    catch(Exception e) {
      final Logging logger = Logging.getLogger(task.getFactory().getClass());
      if(logger != null && logger.isDebugging()) {
        logger.exception("Thumbnail for " + task.getFactory() + " failed.", e);
      }
      else {
        LoggingUtil.warning("Thumbnail for " + task.getFactory() + " failed - enable debugging to see details.");
      }
      // TODO: hide the failed image?
    }
  }

  private void refreshThumbnail() {
    // Discard an existing thumbnail
    thumbid = -1;
    thumb = null;
    // TODO: also purge from ThumbnailRegistryEntry?
    svgp.requestRedraw(this.task, this);
  }

  @Override
  public void resultChanged(Result current) {
    // Default is to redraw when the result we are attached to changed.
    if(task == current || task.getResult() == current) {
      refreshThumbnail();
      return;
    }
    if(task.has(UpdateFlag.ON_SELECTION) && current instanceof SelectionResult) {
      refreshThumbnail();
      return;
    }
    if(task.has(UpdateFlag.ON_SAMPLE) && current instanceof SamplingResult) {
      refreshThumbnail();
      return;
    }
  }

  @Override
  public void visualizationChanged(VisualizationItem item) {
    if(task == item || (task.has(UpdateFlag.ON_STYLEPOLICY) && item instanceof StylingPolicy)) {
      refreshThumbnail();
      return;
    }
  }
}
