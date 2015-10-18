package de.lmu.ifi.dbs.elki.visualization.gui.detail;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.VisualizationListener;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGEffects;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Manages a detail view.
 *
 * @author Erich Schubert
 *
 * @apiviz.has Visualization
 * @apiviz.has PlotItem
 * @apiviz.uses VisualizerContext
 * @apiviz.uses VisualizationTask
 */
public class DetailView extends VisualizationPlot implements ResultListener, VisualizationListener {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(DetailView.class);

  /**
   * Meta information on the visualizers contained.
   */
  private PlotItem item;

  /**
   * Ratio of this view.
   */
  double ratio = 1.0;

  /**
   * The visualizer context
   */
  VisualizerContext context;

  /**
   * Map from tasks to visualizations.
   */
  Map<VisualizationTask, Visualization> taskmap = new HashMap<>();

  /**
   * Map from visualizations to SVG layers.
   */
  Map<Visualization, Element> layermap = new HashMap<>();

  /**
   * The created width
   */
  private double width;

  /**
   * The created height
   */
  private double height;

  /**
   * Pending refresh, for lazy refreshing
   */
  AtomicReference<Runnable> pendingRefresh = new AtomicReference<>(null);

  /**
   * Constructor.
   *
   * @param vis Visualizations to use
   * @param ratio Plot ratio
   */
  public DetailView(VisualizerContext context, PlotItem vis, double ratio) {
    super();
    this.context = context;
    this.item = new PlotItem(vis); // Clone!
    this.ratio = ratio;

    this.item.sort();

    // TODO: only do this when there is an interactive visualizer?
    setDisableInteractions(true);
    addBackground(context);
    SVGEffects.addShadowFilter(this);
    SVGEffects.addLightGradient(this);

    initialize();
    context.addVisualizationListener(this);
    context.addResultListener(this);
    // FIXME: add datastore listener, too?
  }

  /**
   * Create a background node. Note: don't call this at arbitrary times - the
   * background may cover already drawn parts of the image!
   *
   * @param context
   */
  private void addBackground(VisualizerContext context) {
    // Make a background
    CSSClass cls = new CSSClass(this, "background");
    cls.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getBackgroundColor(StyleLibrary.PAGE));
    Element bg = this.svgElement(SVGConstants.SVG_RECT_TAG);
    SVGUtil.setAtt(bg, SVGConstants.SVG_X_ATTRIBUTE, "0");
    SVGUtil.setAtt(bg, SVGConstants.SVG_Y_ATTRIBUTE, "0");
    SVGUtil.setAtt(bg, SVGConstants.SVG_WIDTH_ATTRIBUTE, "100%");
    SVGUtil.setAtt(bg, SVGConstants.SVG_HEIGHT_ATTRIBUTE, "100%");
    SVGUtil.setAtt(bg, NO_EXPORT_ATTRIBUTE, NO_EXPORT_ATTRIBUTE);
    addCSSClassOrLogError(cls);
    SVGUtil.setCSSClass(bg, cls.getName());

    // Note that we rely on this being called before any other drawing routines.
    getRoot().appendChild(bg);
  }

  private void initialize() {
    // Try to keep the area approximately 1.0
    width = Math.sqrt(getRatio());
    height = 1.0 / width;

    ArrayList<Visualization> layers = new ArrayList<>();
    // TODO: center/arrange visualizations?
    for(Iterator<VisualizationTask> tit = item.tasks.iterator(); tit.hasNext();) {
      VisualizationTask task = tit.next();
      if(task.visible) {
        Visualization v = instantiateVisualization(task);
        if(v != null) {
          layers.add(v);
          taskmap.put(task, v);
          layermap.put(v, v.getLayer());
        }
      }
    }
    // Arrange
    for(Visualization layer : layers) {
      if(layer.getLayer() != null) {
        getRoot().appendChild(layer.getLayer());
      }
      else {
        LOG.warning("NULL layer seen.");
      }
    }

    double ratio = width / height;
    getRoot().setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    getRoot().setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, (20 / ratio) + "cm");
    getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + width + " " + height);

    updateStyleElement();
  }

  /**
   * Do a refresh (when visibilities have changed).
   */
  private synchronized void refresh() {
    pendingRefresh.set(null); // Clear
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Refresh in thread " + Thread.currentThread().getName());
    }
    boolean updateStyle = false;
    Iterator<Map.Entry<VisualizationTask, Visualization>> it = taskmap.entrySet().iterator();
    while(it.hasNext()) {
      Entry<VisualizationTask, Visualization> ent = it.next();
      VisualizationTask task = ent.getKey();
      Visualization vis = ent.getValue();
      if(vis == null) {
        vis = instantiateVisualization(task);
        ent.setValue(vis);
      }
      Element prevlayer = layermap.get(vis);
      Element layer = vis.getLayer();
      if(prevlayer == layer) { // Unchanged:
        // Current visibility ("not hidden")
        boolean isVisible = !SVGConstants.CSS_HIDDEN_VALUE.equals(layer.getAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY));
        if(task.visible != isVisible) {
          // scheduleUpdate(new AttributeModifier(
          layer.setAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY, //
              task.visible ? SVGConstants.CSS_VISIBLE_VALUE : SVGConstants.CSS_HIDDEN_VALUE);
        }
      }
      else {
        if(task.hasAnyFlags(VisualizationTask.FLAG_NO_EXPORT)) {
          layer.setAttribute(NO_EXPORT_ATTRIBUTE, NO_EXPORT_ATTRIBUTE);
        }
        if(prevlayer == null) {
          if(LOG.isDebuggingFine()) {
            LOG.debugFine("New layer: " + task);
          }
          // Insert new!
          // TODO: insert position!
          getRoot().appendChild(layer);
        }
        else {
          if(LOG.isDebuggingFine()) {
            LOG.debugFine("Updated layer: " + task);
          }
          // Replace
          final Node parent = prevlayer.getParentNode();
          if(parent != null) {
            parent.replaceChild(/* new! */layer, /* old */prevlayer);
          }
        }
        layermap.put(vis, layer);
        updateStyle = true;
      }
    }
    if(updateStyle) {
      updateStyleElement();
    }
  }

  /**
   * Instantiate a visualization.
   *
   * @param task Task to instantiate
   * @return Visualization
   */
  private Visualization instantiateVisualization(VisualizationTask task) {
    try {
      Visualization v = task.getFactory().makeVisualization(task, this, width, height, item.proj);
      if(task.hasAnyFlags(VisualizationTask.FLAG_NO_EXPORT)) {
        v.getLayer().setAttribute(NO_EXPORT_ATTRIBUTE, NO_EXPORT_ATTRIBUTE);
      }
      return v;
    }
    catch(Exception e) {
      if(LOG.isDebugging()) {
        LOG.warning("Visualizer " + task.getFactory().getClass().getName() + " failed.", e);
      }
      else {
        LOG.warning("Visualizer " + task.getFactory().getClass().getName() + " failed - enable debugging to see details: " + e.toString());
      }
    }
    return null;
  }

  /**
   * Cleanup function. To remove listeners.
   */
  public void destroy() {
    context.removeVisualizationListener(this);
    context.removeResultListener(this);
    for(Entry<VisualizationTask, Visualization> v : taskmap.entrySet()) {
      Visualization vis = v.getValue();
      if(vis != null) {
        vis.destroy();
      }
    }
    taskmap.clear();
  }

  @Override
  public void dispose() {
    destroy();
    super.dispose();
  }

  /**
   * Get the plot ratio.
   *
   * @return the current ratio
   */
  public double getRatio() {
    return ratio;
  }

  /**
   * Set the plot ratio
   *
   * @param ratio the new ratio to set
   */
  public void setRatio(double ratio) {
    // TODO: trigger refresh?
    this.ratio = ratio;
  }

  /**
   * Trigger a refresh.
   */
  private void lazyRefresh() {
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if(DetailView.this.pendingRefresh.compareAndSet(this, null)) {
          DetailView.this.refresh();
        }
      }
    };
    DetailView.this.pendingRefresh.set(pr);
    scheduleUpdate(pr);
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
  public void visualizationChanged(VisualizationItem current) {
    // Make sure we are affected:
    if(!(current instanceof VisualizationTask)) {
      return;
    }
    final VisualizationTask task = (VisualizationTask) current;
    // Get the layer
    Visualization vis = taskmap.get(task);
    if(vis == null) { // Unknown only.
      boolean include = false;
      Hierarchy.Iter<Object> it = context.getVisHierarchy().iterAncestors(current);
      for(; it.valid(); it.advance()) {
        if((item.proj != null && item.proj.getProjector() == it.get()) || taskmap.containsKey(it.get())) {
          include = true;
          break;
        }
      }
      if(!include) {
        return; // Attached to different projection.
      }
    }
    if(vis == null) { // New visualization
      taskmap.put(task, null);
      lazyRefresh();
    }
    else {
      Element prevlayer = layermap.get(vis);
      Element layer = vis.getLayer();
      if(prevlayer != layer) {
        lazyRefresh();
      }
      else {
        boolean isVisible = !SVGConstants.CSS_HIDDEN_VALUE.equals(layer.getAttribute(SVGConstants.CSS_VISIBILITY_PROPERTY));
        if(task.visible != isVisible) {
          lazyRefresh(); // Visibility has changed.
        }
      }
    }
  }

  @Override
  protected void redraw() {
    boolean active = false;
    while(!updateQueue.isEmpty()) {
      Visualization vis = updateQueue.pop();
      if(!active) {
        Element prev = layermap.get(vis);
        vis.incrementalRedraw();
        final boolean changed = prev != vis.getLayer();
        if(LOG.isDebuggingFine() && changed) {
          LOG.debugFine("Visualization " + vis + " changed.");
        }
        active |= changed;
      }
      else {
        vis.incrementalRedraw();
      }
    }
    if(active || true) {
      refresh();
    }
  }

  /**
   * Get the item visualized by this view.
   *
   * @return Plot item
   */
  public PlotItem getPlotItem() {
    return item;
  }
}
