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
package de.lmu.ifi.dbs.elki.visualization.gui.overview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.VisualizationListener;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.RenderFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.CSSHoverClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGEffects;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Generate an overview plot for a set of visualizations.
 *
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * @since 0.3
 *
 * @opt nodefillcolor LemonChiffon
 * @has - - - VisualizerContext
 * @composed - - - RectangleArranger
 * @composed - - - LayerMap
 * @has - - - DetailViewSelectedEvent
 * @assoc - - - DetailView
 */
public class OverviewPlot implements ResultListener, VisualizationListener {
  /**
   * Our logging class
   */
  private static final Logging LOG = Logging.getLogger(OverviewPlot.class);

  /**
   * Event when the overview plot started refreshing.
   */
  public static final String OVERVIEW_REFRESHING = "Overview refreshing";

  /**
   * Event when the overview plot was refreshed.
   */
  public static final String OVERVIEW_REFRESHED = "Overview refreshed";

  /**
   * Draw red borders around items.
   */
  private static final boolean DEBUG_LAYOUT = false;

  /**
   * Visualizer context
   */
  private VisualizerContext context;

  /**
   * The SVG plot object.
   */
  private VisualizationPlot plot;

  /**
   * Map of coordinates to plots.
   */
  protected RectangleArranger<PlotItem> plotmap;

  /**
   * Action listeners for this plot.
   */
  private ArrayList<ActionListener> actionListeners = new ArrayList<>();

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
  private LayerMap vistoelem = new LayerMap();

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
  AtomicReference<Runnable> pendingRefresh = new AtomicReference<>(null);

  /**
   * Reinitialize on refresh
   */
  private boolean reinitOnRefresh = false;

  /**
   * Constructor.
   *
   * @param context Visualizer context
   * @param single Single view mode
   */
  public OverviewPlot(VisualizerContext context, boolean single) {
    super();
    this.context = context;
    this.single = single;

    // Important:
    // You still need to call: initialize(ratio);
  }

  /**
   * Recompute the layout of visualizations.
   *
   * @param width Initial width
   * @param height Initial height
   * @return Arrangement
   */
  private RectangleArranger<PlotItem> arrangeVisualizations(double width, double height) {
    if(!(width > 0. && height > 0.)) {
      LOG.warning("No size information during arrange()", new Throwable());
      return new RectangleArranger<>(1., 1.);
    }
    RectangleArranger<PlotItem> plotmap = new RectangleArranger<>(width, height);

    Hierarchy<Object> vistree = context.getVisHierarchy();
    for(It<Projector> iter2 = vistree.iterAll().filter(Projector.class); iter2.valid(); iter2.advance()) {
      Collection<PlotItem> projs = iter2.get().arrange(context);
      for(PlotItem it : projs) {
        if(it.w <= 0.0 || it.h <= 0.0) {
          LOG.warning("Plot item with improper size information: " + it);
          continue;
        }
        plotmap.put(it.w, it.h, it);
      }
    }

    nextTask: for(It<VisualizationTask> iter2 = vistree.iterAll().filter(VisualizationTask.class); iter2.valid(); iter2.advance()) {
      VisualizationTask task = iter2.get();
      if(!task.isVisible()) {
        continue;
      }
      if(vistree.iterParents(task).filter(Projector.class).valid()) {
        continue nextTask;
      }
      if(task.getRequestedWidth() <= 0.0 || task.getRequestedHeight() <= 0.0) {
        LOG.warning("Task with improper size information: " + task);
        continue;
      }
      PlotItem it = new PlotItem(task.getRequestedWidth(), task.getRequestedHeight(), null);
      it.tasks.add(task);
      plotmap.put(it.w, it.h, it);
    }
    return plotmap;
  }

  /**
   * Initialize the plot.
   *
   * @param ratio Initial ratio
   */
  public void initialize(double ratio) {
    if(!(ratio > 0 && ratio < Double.POSITIVE_INFINITY)) {
      LOG.warning("Invalid ratio: " + ratio, new Throwable());
      ratio = 1.4;
    }
    this.ratio = ratio;
    if(plot != null) {
      LOG.warning("Already initialized.");
      lazyRefresh();
      return;
    }
    reinitialize();
    // register context listener
    context.addResultListener(this);
    context.addVisualizationListener(this);
  }

  /**
   * Refresh the overview plot.
   */
  private synchronized void reinitialize() {
    if(plot == null) {
      initializePlot();
    }
    else {
      final ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, OVERVIEW_REFRESHING);
      for(ActionListener actionListener : actionListeners) {
        actionListener.actionPerformed(ev);
      }
    }

    // Detach existing elements:
    for(Pair<Element, Visualization> pair : vistoelem.values()) {
      SVGUtil.removeFromParent(pair.first);
    }
    plotmap = arrangeVisualizations(ratio, 1.0);

    recalcViewbox();
    final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
    // TODO: cancel pending thumbnail requests!

    // Replace the layer map
    LayerMap oldlayers = vistoelem;
    vistoelem = new LayerMap();

    // Redo main layers
    SVGUtil.removeFromParent(plotlayer);
    SVGUtil.removeFromParent(hoverlayer);
    plotlayer = plot.svgElement(SVGConstants.SVG_G_TAG);
    hoverlayer = plot.svgElement(SVGConstants.SVG_G_TAG);
    hoverlayer.setAttribute(SVGPlot.NO_EXPORT_ATTRIBUTE, SVGPlot.NO_EXPORT_ATTRIBUTE);

    // Redo the layout
    for(Entry<PlotItem, double[]> e : plotmap.entrySet()) {
      final double basex = e.getValue()[0];
      final double basey = e.getValue()[1];
      for(Iterator<PlotItem> iter = e.getKey().itemIterator(); iter.hasNext();) {
        PlotItem it = iter.next();

        boolean hasDetails = false;
        // Container element for main plot item
        Element g = plot.svgElement(SVGConstants.SVG_G_TAG);
        SVGUtil.setAtt(g, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, "translate(" + (basex + it.x) + " " + (basey + it.y) + ")");
        plotlayer.appendChild(g);
        vistoelem.put(it, null, g, null);
        // Add the actual tasks:
        for(VisualizationTask task : it.tasks) {
          if(!visibleInOverview(task)) {
            continue;
          }
          hasDetails |= !task.has(RenderFlag.NO_DETAIL);
          Pair<Element, Visualization> pair = oldlayers.remove(it, task);
          if(pair == null) {
            pair = new Pair<>(plot.svgElement(SVGConstants.SVG_G_TAG), null);
          }
          if(pair.second == null) {
            pair.second = embedOrThumbnail(thumbsize, it, task, pair.first);
          }
          g.appendChild(pair.first);
          vistoelem.put(it, task, pair);
        }
        // When needed, add a hover effect
        if(hasDetails && !single) {
          Element hover = plot.svgRect(basex + it.x, basey + it.y, it.w, it.h);
          SVGUtil.addCSSClass(hover, selcss.getName());
          // link hoverer.
          EventTarget targ = (EventTarget) hover;
          targ.addEventListener(SVGConstants.SVG_MOUSEOVER_EVENT_TYPE, hoverer, false);
          targ.addEventListener(SVGConstants.SVG_MOUSEOUT_EVENT_TYPE, hoverer, false);
          targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, hoverer, false);
          targ.addEventListener(SVGConstants.SVG_CLICK_EVENT_TYPE, (evt) -> triggerSubplotSelectEvent(it), false);

          hoverlayer.appendChild(hover);
        }
      }
    }
    for(Pair<Element, Visualization> pair : oldlayers.values()) {
      if(pair.second != null) {
        pair.second.destroy();
      }
    }
    plot.getRoot().appendChild(plotlayer);
    plot.getRoot().appendChild(hoverlayer);
    plot.updateStyleElement();

    // Notify listeners.
    final ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, OVERVIEW_REFRESHED);
    for(ActionListener actionListener : actionListeners) {
      actionListener.actionPerformed(ev);
    }
  }

  /**
   * Initialize the SVG plot.
   */
  private void initializePlot() {
    plot = new VisualizationPlot();
    { // Add a background element:
      CSSClass cls = new CSSClass(this, "background");
      final String bgcol = context.getStyleLibrary().getBackgroundColor(StyleLibrary.PAGE);
      cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, bgcol);
      plot.addCSSClassOrLogError(cls);
      Element background = plot.svgElement(SVGConstants.SVG_RECT_TAG);
      background.setAttribute(SVGConstants.SVG_X_ATTRIBUTE, "0");
      background.setAttribute(SVGConstants.SVG_Y_ATTRIBUTE, "0");
      background.setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "100%");
      background.setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, "100%");
      SVGUtil.setCSSClass(background, cls.getName());
      // Don't export a white background:
      if("white".equals(bgcol)) {
        background.setAttribute(SVGPlot.NO_EXPORT_ATTRIBUTE, SVGPlot.NO_EXPORT_ATTRIBUTE);
      }
      plot.getRoot().appendChild(background);
    }
    { // setup the hover CSS classes.
      selcss = new CSSClass(this, "s");
      if(DEBUG_LAYOUT) {
        selcss.setStatement(SVGConstants.CSS_STROKE_PROPERTY, SVGConstants.CSS_RED_VALUE);
        selcss.setStatement(SVGConstants.CSS_STROKE_WIDTH_PROPERTY, .00001 * StyleLibrary.SCALE);
        selcss.setStatement(SVGConstants.CSS_STROKE_OPACITY_PROPERTY, "0.5");
      }
      selcss.setStatement(SVGConstants.CSS_FILL_PROPERTY, SVGConstants.CSS_RED_VALUE);
      selcss.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0");
      selcss.setStatement(SVGConstants.CSS_CURSOR_PROPERTY, SVGConstants.CSS_POINTER_VALUE);
      plot.addCSSClassOrLogError(selcss);
      CSSClass hovcss = new CSSClass(this, "h");
      hovcss.setStatement(SVGConstants.CSS_FILL_OPACITY_PROPERTY, "0.25");
      plot.addCSSClassOrLogError(hovcss);
      // Hover listener.
      hoverer = new CSSHoverClass(hovcss.getName(), null, true);
    }

    // Disable Batik default interactions (zoom, rotate, etc.)
    if(single) {
      plot.setDisableInteractions(true);
    }
    SVGEffects.addShadowFilter(plot);
    SVGEffects.addLightGradient(plot);
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
    final Visualization vis;
    if(!single) {
      vis = task.getFactory().makeVisualizationOrThumbnail(context, task, plot, it.w, it.h, it.proj, thumbsize);
    }
    else {
      vis = task.getFactory().makeVisualization(context, task, plot, it.w, it.h, it.proj);
    }
    if(vis == null || vis.getLayer() == null) {
      LOG.warning("Visualization returned empty layer: " + vis);
      return vis;
    }
    if(task.has(RenderFlag.NO_EXPORT)) {
      vis.getLayer().setAttribute(SVGPlot.NO_EXPORT_ATTRIBUTE, SVGPlot.NO_EXPORT_ATTRIBUTE);
    }
    parent.appendChild(vis.getLayer());
    return vis;
  }

  /**
   * Do a refresh (when visibilities have changed).
   */
  synchronized void refresh() {
    if(reinitOnRefresh) {
      LOG.debug("Reinitialize in thread " + Thread.currentThread().getName());
      reinitialize();
      reinitOnRefresh = false;
      return;
    }
    synchronized(plot) {
      boolean refreshcss = false;
      if(plotmap == null) {
        throw new IllegalStateException("Plotmap is null");
      }
      final int thumbsize = (int) Math.max(screenwidth / plotmap.getWidth(), screenheight / plotmap.getHeight());
      for(PlotItem pi : plotmap.keySet()) {
        for(Iterator<PlotItem> iter = pi.itemIterator(); iter.hasNext();) {
          PlotItem it = iter.next();

          for(Iterator<VisualizationTask> tit = it.tasks.iterator(); tit.hasNext();) {
            VisualizationTask task = tit.next();
            Pair<Element, Visualization> pair = vistoelem.get(it, task);
            // New task?
            if(pair == null) {
              if(visibleInOverview(task)) {
                Element elem = plot.svgElement(SVGConstants.SVG_G_TAG);
                pair = new Pair<>(elem, embedOrThumbnail(thumbsize, it, task, elem));
                vistoelem.get(it, null).first.appendChild(elem);
                vistoelem.put(it, task, pair);
                refreshcss = true;
              }
            }
            else if(pair.first != null) {
              if(visibleInOverview(task)) {
                // unhide if hidden.
                if(pair.first.hasAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY)) {
                  pair.first.removeAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY);
                }
              }
              else {
                // hide if there is anything to hide.
                if(pair.first.hasChildNodes()) {
                  pair.first.setAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY, SVGConstants.CSS_HIDDEN_VALUE);
                }
              }
              // TODO: unqueue pending thumbnails
            }
          }
        }
      }
      if(refreshcss) {
        plot.updateStyleElement();
      }
    }
  }

  /**
   * Test whether a task should be displayed in the overview plot.
   *
   * @param task Task to display
   * @return visibility
   */
  protected boolean visibleInOverview(VisualizationTask task) {
    return task.isVisible() && !task.has(single ? RenderFlag.NO_EMBED : RenderFlag.NO_THUMBNAIL);
  }

  /**
   * Recompute the view box of the plot.
   */
  private void recalcViewbox() {
    final Element root = plot.getRoot();
    // Reset plot attributes
    SVGUtil.setAtt(root, SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    SVGUtil.setAtt(root, SVGConstants.SVG_HEIGHT_ATTRIBUTE, SVGUtil.fmt(20 * plotmap.getHeight() / plotmap.getWidth()) + "cm");
    String vb = "0 0 " + SVGUtil.fmt(plotmap.getWidth()) + " " + SVGUtil.fmt(plotmap.getHeight());
    SVGUtil.setAtt(root, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, vb);
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
   * Destroy this overview plot.
   */
  public void destroy() {
    context.removeVisualizationListener(this);
    context.removeResultListener(this);
    plot.dispose();
  }

  /**
   * Get the SVGPlot object.
   *
   * @return SVG plot
   */
  public SVGPlot getPlot() {
    return plot;
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
    if(ratio != this.ratio) {
      this.ratio = ratio;
      reinitOnRefresh = true;
      lazyRefresh();
    }
  }

  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  public final void lazyRefresh() {
    if(plot == null) {
      LOG.warning("'lazyRefresh' called before initialized!");
      return;
    }
    LOG.debug("Scheduling refresh.");
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if(OverviewPlot.this.pendingRefresh.compareAndSet(this, null)) {
          OverviewPlot.this.refresh();
        }
      }
    };
    OverviewPlot.this.pendingRefresh.set(pr);
    plot.scheduleUpdate(pr);
  }

  @Override
  public void resultAdded(Result child, Result parent) {
    lazyRefresh();
  }

  @Override
  public void resultChanged(Result current) {
    lazyRefresh();
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    lazyRefresh();
  }

  @Override
  public void visualizationChanged(VisualizationItem child) {
    if(!context.getVisHierarchy().iterParents(child).filter(Projector.class).valid()) {
      reinitOnRefresh = true;
    }
    lazyRefresh();
  }
}
