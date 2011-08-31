package de.lmu.ifi.dbs.elki.visualization.gui.overview;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.CSSHoverClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;
import de.lmu.ifi.dbs.elki.visualization.projector.LayoutObject;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Generate an overview plot for a set of visualizations.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @apiviz.landmark
 * @apiviz.has VisualizerContext
 * @apiviz.composedOf PlotMap
 * @apiviz.has DetailViewSelectedEvent
 * @apiviz.uses DetailView
 * @apiviz.uses de.lmu.ifi.dbs.elki.visualization.projections.Projection
 */
public class OverviewPlot extends SVGPlot implements ResultListener {
  /**
   * Our logging class
   */
  private static final Logging logger = Logging.getLogger(OverviewPlot.class);

  /**
   * Visualizer context
   */
  private VisualizerContext context;

  /**
   * Result we work on. Currently unused, but kept for future requirements.
   */
  private Result result;

  /**
   * Map of coordinates to plots.
   */
  protected PlotMap plotmap;

  /**
   * Action listeners for this plot.
   */
  private java.util.Vector<ActionListener> actionListeners = new java.util.Vector<ActionListener>();

  /**
   * Constructor.
   * 
   * @param result Result to visualize
   * @param context Visualizer context
   */
  public OverviewPlot(Result result, VisualizerContext context) {
    super();
    this.result = result;
    this.context = context;
    // register context listener
    context.addResultListener(this);
  }

  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenwidth = 2000;

  /**
   * Screen size (used for thumbnail sizing)
   */
  public int screenheight = 2000;

  /**
   * React to mouse hover events
   */
  private EventListener hoverer;

  /**
   * Lookup
   */
  private HashMap<Pair<PlotItem, VisualizationTask>, Element> vistoelem;

  /**
   * Layer for plot thumbnail
   */
  private Element plotlayer;

  /**
   * Layer for hover elements
   */
  private Element hoverlayer;

  /**
   * The CSS class used on "selectable" rectangles.
   */
  private CSSClass selcss;

  /**
   * Screen ratio
   */
  private double ratio = 1.0;

  /**
   * Pending refresh, for lazy refreshing
   */
  Runnable pendingRefresh;

  /**
   * Reinitialize on refresh
   */
  private boolean reinitOnRefresh = true;

  /**
   * Recompute the layout of visualizations.
   */
  private void arrangeVisualizations() {
    plotmap = new PlotMap();

    ArrayList<Projector> projectors = ResultUtil.filterResults(result, Projector.class);
    for(Projector p : projectors) {
      double[] shape = p.getShape();
      Collection<LayoutObject> projs = p.arrange();
      for(LayoutObject l : projs) {
        double x = l.reqx + shape[0];
        double y = l.reqy + shape[1];
        final IterableIterator<VisualizationTask> vis = ResultUtil.filteredResults(p, VisualizationTask.class);
        for(VisualizationTask task : vis) {
          // VisualizationTask v = task.clone(this, context, l.proj, l.reqw, l.reqh);
          plotmap.addVis(x, y, l.reqw, l.reqh, l.proj, task);
        }
      }
    }
  }

  /**
   * Refresh the overview plot.
   */
  private void reinitialize() {
    setupHoverer();
    arrangeVisualizations();
    recalcViewbox();
    // TODO: cancel pending thumbnail requests!

    // Layers.
    if(plotlayer != null) {
      if(plotlayer.getParentNode() != null) {
        plotlayer.getParentNode().removeChild(plotlayer);
      }
      plotlayer = null;
    }
    if(hoverlayer != null) {
      if(hoverlayer.getParentNode() != null) {
        hoverlayer.getParentNode().removeChild(hoverlayer);
      }
      hoverlayer = null;
    }
    plotlayer = this.svgElement(SVGConstants.SVG_G_TAG);
    hoverlayer = this.svgElement(SVGConstants.SVG_G_TAG);
    vistoelem = new HashMap<Pair<PlotItem, VisualizationTask>, Element>();

    final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());

    // TODO: kill all children in document root except style, defs etc?
    for(PlotItem it : plotmap.values()) {
      boolean hasDetails = false;
      Element g = this.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(g, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + it.x + " " + it.y + ")");
      for(VisualizationTask task : it) {
        Element parent = this.svgElement(SVGConstants.SVG_G_TAG);
        g.appendChild(parent);
        makeThumbnail(thumbsize, it, task, parent);
        vistoelem.put(new Pair<PlotItem, VisualizationTask>(it, task), parent);

        if(VisualizerUtil.detailsEnabled(task)) {
          hasDetails = true;
        }
      }
      plotlayer.appendChild(g);
      if(hasDetails) {
        Element hover = this.svgRect(it.x, it.y, it.w, it.h);
        SVGUtil.addCSSClass(hover, selcss.getName());
        // link hoverer.
        EventTarget targ = (EventTarget) hover;
        targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
        targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new SelectPlotEvent(it.x, it.y), false);

        hoverlayer.appendChild(hover);
      }
    }
    getRoot().appendChild(plotlayer);
    getRoot().appendChild(hoverlayer);
    updateStyleElement();
  }

  /**
   * Produce thumbnail for a visualizer.
   * 
   * @param thumbsize Thumbnail size
   * @param it Plot item
   * @param task Task
   * @param parent Parent element to draw to
   */
  private void makeThumbnail(final int thumbsize, PlotItem it, VisualizationTask task, Element parent) {
    if(VisualizerUtil.isVisible(task) && VisualizerUtil.thumbnailEnabled(task)) {
      VisualizationTask thumbtask = task.clone(this, context, it.proj, it.w, it.h);
      thumbtask.put(VisualizationTask.THUMBNAIL, true);
      thumbtask.put(VisualizationTask.THUMBNAIL_RESOLUTION, thumbsize);
      Visualization vis = thumbtask.getFactory().makeVisualizationOrThumbnail(thumbtask);
      if(vis.getLayer() == null) {
        LoggingUtil.warning("Visualization returned empty layer: " + vis);
      }
      else {
        parent.appendChild(vis.getLayer());
      }
    }
  }

  /**
   * Do a refresh (when visibilities have changed).
   */
  synchronized void refresh() {
    logger.debug("Refresh");
    if(vistoelem == null || plotlayer == null || hoverlayer == null || reinitOnRefresh) {
      reinitialize();
      reinitOnRefresh = false;
    }
    else {
      boolean refreshcss = false;
      final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
      for(PlotItem it : plotmap.values()) {
        for(VisualizationTask task : it) {
          Element parent = vistoelem.get(new Pair<PlotItem, VisualizationTask>(it, task));
          if(parent == null) {
            LoggingUtil.warning("No container element produced by " + task);
            continue;
          }
          if(VisualizerUtil.thumbnailEnabled(task) && VisualizerUtil.isVisible(task)) {
            // unhide when hidden.
            if(parent.hasAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY)) {
              parent.removeAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY);
            }
            // if not yet rendered, add a thumbnail
            if(!parent.hasChildNodes()) {
              makeThumbnail(thumbsize, it, task, parent);
              refreshcss = true;
            }
          }
          else {
            // hide if there is anything to hide.
            if(parent != null && parent.hasChildNodes()) {
              parent.setAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_HIDDEN_VALUE);
            }
            // TODO: unqueue pending thumbnails
          }
        }
      }
      if (refreshcss) {
        updateStyleElement();
      }
    }
  }

  /**
   * Recompute the view box of the plot.
   */
  private void recalcViewbox() {
    // Recalculate bounding box.
    String vb = plotmap.minmaxx.getMin() + " " + plotmap.minmaxy.getMin() + " " + plotmap.getWidth() + " " + plotmap.getHeight();
    // Reset root bounding box.
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / plotmap.getWidth() * plotmap.getHeight()) + "cm");
    SVGUtil.setAtt(getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, vb);
  }

  /**
   * Setup the CSS hover effect.
   */
  private void setupHoverer() {
    // setup the hover CSS classes.
    selcss = new CSSClass(this, "s");
    selcss.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
    selcss.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_NONE_VALUE);
    selcss.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0");
    selcss.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);
    CSSClass hovcss = new CSSClass(this, "h");
    hovcss.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0.25");
    addCSSClassOrLogError(selcss);
    addCSSClassOrLogError(hovcss);
    // Hover listener.
    hoverer = new CSSHoverClass(hovcss.getName(), null, true);
  }

  /**
   * Event triggered when a plot was selected.
   * 
   * @param x X coordinate
   * @param y Y coordinate
   * @return sub plot
   */
  public DetailView makeDetailView(double x, double y) {
    PlotItem layers = plotmap.get(x, y);
    return new DetailView(context, layers, ratio);
  }

  /**
   * Adds an {@link ActionListener} to the plot.
   * 
   * @param actionListener the {@link ActionListener} to be added
   */
  public void addActionListener(ActionListener actionListener) {
    actionListeners.add(actionListener);
  }

  /**
   * When a subplot was selected, forward the event to listeners.
   * 
   * @param x X coordinate
   * @param y Y coordinate
   */
  protected void triggerSubplotSelectEvent(double x, double y) {
    // forward event to all listeners.
    for(ActionListener actionListener : actionListeners) {
      actionListener.actionPerformed(new DetailViewSelectedEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, x, y));
    }
  }

  /**
   * Event when a plot was selected.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public class SelectPlotEvent implements EventListener {
    /**
     * X coordinate of box.
     */
    double x;

    /**
     * Y coordinate of box.
     */
    double y;

    /**
     * Constructor.
     * 
     * @param x coordinate
     * @param y coordinate
     */
    public SelectPlotEvent(double x, double y) {
      super();
      this.x = x;
      this.y = y;
    }

    @Override
    public void handleEvent(@SuppressWarnings("unused") Event evt) {
      triggerSubplotSelectEvent(x, y);
    }
  }

  /**
   * Cancel the overview, i.e. stop the thumbnailer
   */
  @Override
  public void dispose() {
    context.removeResultListener(this);
    super.dispose();
  }

  /**
   * @return the ratio
   */
  public double getRatio() {
    return ratio;
  }

  /**
   * @param ratio the ratio to set
   */
  public void setRatio(double ratio) {
    this.ratio = ratio;
  }

  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  public final void lazyRefresh() {
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if(OverviewPlot.this.pendingRefresh == this) {
          OverviewPlot.this.pendingRefresh = null;
          OverviewPlot.this.refresh();
        }
      }
    };
    pendingRefresh = pr;
    scheduleUpdate(pr);
  }
  
  @SuppressWarnings("unused")
  @Override
  public void resultAdded(Result child, Result parent) {
    logger.debug("result added: "+child);
    if(child instanceof VisualizationTask) {
      reinitOnRefresh = true;
    }
    lazyRefresh();
  }

  @Override
  public void resultChanged(Result current) {
    logger.debug("result changed: "+current);
    lazyRefresh();
  }

  @SuppressWarnings("unused")
  @Override
  public void resultRemoved(Result child, Result parent) {
    logger.debug("result removed: "+child);
    lazyRefresh();
  }
}