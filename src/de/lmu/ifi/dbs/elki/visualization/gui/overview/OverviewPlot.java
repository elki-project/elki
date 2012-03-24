package de.lmu.ifi.dbs.elki.visualization.gui.overview;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.CSSHoverClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGEffects;
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
 * @apiviz.composedOf RectangleArranger
 * @apiviz.has DetailViewSelectedEvent
 * @apiviz.uses DetailView
 * @apiviz.uses Projector
 */
// FIXME: there still is a synchronization issue, that causes the initialization
// to be run twice in parallel.
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
   * Map of coordinates to plots.
   */
  protected RectangleArranger<PlotItem> plotmap;

  /**
   * Action listeners for this plot.
   */
  private java.util.Vector<ActionListener> actionListeners = new java.util.Vector<ActionListener>();

  /**
   * Single view mode
   */
  private boolean single;

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
  private final HashMap<Pair<PlotItem, VisualizationTask>, Pair<Element, Visualization>> vistoelem = new HashMap<Pair<PlotItem, VisualizationTask>, Pair<Element, Visualization>>();

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
  Runnable pendingRefresh = null;

  /**
   * Reinitialize on refresh
   */
  private boolean reinitOnRefresh = true;

  /**
   * Constructor.
   * 
   * @param result Result to visualize
   * @param context Visualizer context
   * @param single Single view mode
   */
  public OverviewPlot(HierarchicalResult result, VisualizerContext context, boolean single) {
    super();
    this.context = context;
    this.single = single;

    // Add a background element:
    {
      CSSClass cls = new CSSClass(this, "background");
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getBackgroundColor(StyleLibrary.PAGE));
      addCSSClassOrLogError(cls);
      Element background = this.svgElement(SVGConstants.SVG_RECT_TAG);
      background.setAttribute(SVGConstants.SVG_X_ATTRIBUTE, "0");
      background.setAttribute(SVGConstants.SVG_Y_ATTRIBUTE, "0");
      background.setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "100%");
      background.setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, "100%");
      SVGUtil.setCSSClass(background, cls.getName());
      getRoot().appendChild(background);
    }

    if(single) {
      setDisableInteractions(true);
    }
    SVGEffects.addShadowFilter(this);
    SVGEffects.addLightGradient(this);

    // register context listener
    context.addResultListener(this);
  }

  /**
   * Recompute the layout of visualizations.
   */
  private void arrangeVisualizations() {
    RectangleArranger<PlotItem> plotmap = new RectangleArranger<PlotItem>(ratio);

    ArrayList<Projector> projectors = ResultUtil.filterResults(context.getResult(), Projector.class);
    // Rectangle layout
    for(Projector p : projectors) {
      Collection<PlotItem> projs = p.arrange();
      for(PlotItem it : projs) {
        if(it.w <= 0.0 || it.h <= 0.0) {
          logger.warning("Plot item with improper size information: " + it);
        }
        else {
          plotmap.put(it.w, it.h, it);
        }
      }
    }

    ResultHierarchy hier = context.getHierarchy();
    ArrayList<VisualizationTask> tasks = ResultUtil.filterResults(context.getResult(), VisualizationTask.class);
    nextTask: for(VisualizationTask task : tasks) {
      for(Result parent : hier.getParents(task)) {
        if(parent instanceof Projector) {
          continue nextTask;
        }
      }
      if(task.getWidth() <= 0.0 || task.getHeight() <= 0.0) {
        logger.warning("Task with improper size information: " + task);
      }
      else {
        PlotItem it = new PlotItem(task.getWidth(), task.getHeight(), null);
        it.tasks.add(task);
        plotmap.put(it.w, it.h, it);
      }
    }
    this.plotmap = plotmap;
  }

  /**
   * Refresh the overview plot.
   */
  private void reinitialize() {
    setupHoverer();
    arrangeVisualizations();
    recalcViewbox();
    // TODO: cancel pending thumbnail requests!

    // Clear vistoelem
    for(Pair<Element, Visualization> pair : vistoelem.values()) {
      if(pair.second != null) {
        pair.second.destroy();
      }
    }
    vistoelem.clear();

    // Layers.
    tryRemove(plotlayer);
    tryRemove(hoverlayer);
    plotlayer = this.svgElement(SVGConstants.SVG_G_TAG);
    hoverlayer = this.svgElement(SVGConstants.SVG_G_TAG);

    final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());

    // TODO: kill all children in document root except style, defs etc?
    for(Entry<PlotItem, double[]> e : plotmap.entrySet()) {
      final double basex = e.getValue()[0];
      final double basey = e.getValue()[1];
      for(Iterator<PlotItem> iter = e.getKey().itemIterator(); iter.hasNext();) {
        PlotItem it = iter.next();
        boolean hasDetails = false;
        Element g = this.svgElement(SVGConstants.SVG_G_TAG);
        SVGUtil.setAtt(g, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + (basex + it.x) + " " + (basey + it.y) + ")");
        for(VisualizationTask task : it.tasks) {
          Element parent = this.svgElement(SVGConstants.SVG_G_TAG);
          g.appendChild(parent);
          Visualization v = visibleInOverview(task) ? embedOrThumbnail(thumbsize, it, task, parent) : null;
          vistoelem.put(new Pair<PlotItem, VisualizationTask>(it, task), new Pair<Element, Visualization>(parent, v));
          if(VisualizerUtil.detailsEnabled(task)) {
            hasDetails = true;
            // TODO: not updatable
          }
        }
        plotlayer.appendChild(g);
        if(hasDetails && !single) {
          Element hover = this.svgRect(basex + it.x, basey + it.y, it.w, it.h);
          SVGUtil.addCSSClass(hover, selcss.getName());
          // link hoverer.
          EventTarget targ = (EventTarget) hover;
          targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
          targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
          targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
          targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, new SelectPlotEvent(it), false);

          hoverlayer.appendChild(hover);
        }
      }
    }
    getRoot().appendChild(plotlayer);
    getRoot().appendChild(hoverlayer);
    updateStyleElement();
  }

  /**
   * Remove an element from the plot, if defined and possible.
   * 
   * @param elem Element to remove
   * @return {@code null}
   */
  private Element tryRemove(Element elem) {
    if(elem != null) {
      if(elem.getParentNode() != null) {
        elem.getParentNode().removeChild(elem);
      }
    }
    return null;
  }

  /**
   * Produce thumbnail for a visualizer.
   * 
   * @param thumbsize Thumbnail size
   * @param it Plot item
   * @param task Task
   * @param parent Parent element to draw to
   */
  private Visualization embedOrThumbnail(final int thumbsize, PlotItem it, VisualizationTask task, Element parent) {
    if(single) {
      VisualizationTask thumbtask = task.clone(this, context, it.proj, it.w, it.h);
      final Visualization vis = thumbtask.getFactory().makeVisualization(thumbtask);
      if(vis.getLayer() == null) {
        LoggingUtil.warning("Visualization returned empty layer: " + vis);
      }
      else {
        parent.appendChild(vis.getLayer());
      }
      return vis;
    }
    else {
      VisualizationTask thumbtask = task.clone(this, context, it.proj, it.w, it.h);
      thumbtask.put(VisualizationTask.THUMBNAIL, true);
      thumbtask.put(VisualizationTask.THUMBNAIL_RESOLUTION, thumbsize);
      final Visualization vis = thumbtask.getFactory().makeVisualizationOrThumbnail(thumbtask);
      if(vis.getLayer() == null) {
        LoggingUtil.warning("Visualization returned empty layer: " + vis);
      }
      else {
        parent.appendChild(vis.getLayer());
      }
      return vis;
    }
  }

  /**
   * Do a refresh (when visibilities have changed).
   */
  synchronized void refresh() {
    pendingRefresh = null;
    if(reinitOnRefresh) {
      logger.debug("Reinitialize");
      reinitialize();
      reinitOnRefresh = false;
    }
    else {
      logger.debug("Incremental refresh");
      boolean refreshcss = false;
      final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
      for(Entry<PlotItem, double[]> ent : plotmap.entrySet()) {
        for(Iterator<PlotItem> iter = ent.getKey().itemIterator(); iter.hasNext();) {
          PlotItem it = iter.next();

          for(Iterator<VisualizationTask> tit = it.tasks.iterator(); tit.hasNext();) {
            VisualizationTask task = tit.next();
            Pair<Element, Visualization> pair = vistoelem.get(new Pair<PlotItem, VisualizationTask>(it, task));
            if(pair == null) {
              LoggingUtil.warning("No container element found for " + it + " " + task);
              continue;
            }
            if(visibleInOverview(task)) {
              // unhide when hidden.
              if(pair.first.hasAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY)) {
                pair.first.removeAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY);
              }
              // if not yet rendered, add a thumbnail
              if(!pair.first.hasChildNodes()) {
                pair.second = embedOrThumbnail(thumbsize, it, task, pair.first);
                refreshcss = true;
              }
            }
            else {
              // hide if there is anything to hide.
              if(pair.first != null && pair.first.hasChildNodes()) {
                pair.first.setAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_HIDDEN_VALUE);
              }
              // TODO: unqueue pending thumbnails
            }
          }
        }
      }
      if(refreshcss) {
        updateStyleElement();
      }
    }
  }

  /**
   * Test whether a task should be displayed in the overview plot.
   * 
   * @param task Task to display
   * @return
   */
  protected boolean visibleInOverview(VisualizationTask task) {
    if(!VisualizerUtil.isVisible(task)) {
      return false;
    }
    if(single) {
      Boolean nothumb = task.getGenerics(VisualizationTask.META_NOEMBED, Boolean.class);
      return (nothumb == null) || !nothumb;
    }
    else {
      return VisualizerUtil.thumbnailEnabled(task);
    }
  }

  /**
   * Recompute the view box of the plot.
   */
  private void recalcViewbox() {
    // Recalculate bounding box.
    String vb = "0 0 " + plotmap.getWidth() + " " + plotmap.getHeight();
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
   * @param it Plot item selected
   * @return sub plot
   */
  public DetailView makeDetailView(PlotItem it) {
    return new DetailView(context, it, ratio);
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
   * @param it PlotItem selected
   */
  protected void triggerSubplotSelectEvent(PlotItem it) {
    // forward event to all listeners.
    for(ActionListener actionListener : actionListeners) {
      actionListener.actionPerformed(new DetailViewSelectedEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, it));
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
     * Plot item clicked
     */
    PlotItem it;

    /**
     * Constructor.
     * 
     * @param it Item that was clicked
     */
    public SelectPlotEvent(PlotItem it) {
      super();
      this.it = it;
    }

    @Override
    public void handleEvent(Event evt) {
      triggerSubplotSelectEvent(it);
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
    logger.debug("Scheduling refresh.");
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if(OverviewPlot.this.pendingRefresh == this) {
          OverviewPlot.this.refresh();
        }
      }
    };
    pendingRefresh = pr;
    scheduleUpdate(pr);
  }

  @Override
  public void resultAdded(Result child, Result parent) {
    logger.debug("result added: " + child);
    if(child instanceof VisualizationTask) {
      reinitOnRefresh = true;
    }
    lazyRefresh();
  }

  @Override
  public void resultChanged(Result current) {
    logger.debug("result changed: " + current);
    lazyRefresh();
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    logger.debug("result removed: " + child);
    lazyRefresh();
  }
}