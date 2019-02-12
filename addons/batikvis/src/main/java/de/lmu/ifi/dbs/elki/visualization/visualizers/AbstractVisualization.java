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
package de.lmu.ifi.dbs.elki.visualization.visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.SamplingResult;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.VisualizationListener;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask.UpdateFlag;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;

/**
 * Abstract base class for visualizations.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 */
public abstract class AbstractVisualization implements Visualization, ResultListener, VisualizationListener, DataStoreListener {
  /**
   * The visualization task we do.
   */
  protected final VisualizationTask task;

  /**
   * Our context
   */
  protected final VisualizerContext context;

  /**
   * The plot we are attached to
   */
  protected final VisualizationPlot svgp;

  /**
   * Layer storage
   */
  protected Element layer;

  /**
   * Width
   */
  private double width;

  /**
   * Height
   */
  private double height;

  /**
   * Constructor.
   *
   * @param context Visualizer context
   * @param task Visualization task
   * @param plot Plot to draw to
   * @param width Embedding width
   * @param height Embedding height
   */
  public AbstractVisualization(VisualizerContext context, VisualizationTask task, VisualizationPlot plot, double width, double height) {
    super();
    this.task = task;
    this.context = context;
    this.svgp = plot;
    this.width = width;
    this.height = height;
    this.layer = null;
    // Note: we do not auto-add listeners, as we don't know what kind of
    // listeners a visualizer needs, and the visualizer might need to do some
    // initialization first
  }

  /**
   * Add the listeners according to the mask.
   */
  protected void addListeners() {
    // Listen for result changes, including the one we monitor
    context.addResultListener(this);
    context.addVisualizationListener(this);
    // Listen for database events only when needed.
    if(task.has(UpdateFlag.ON_DATA)) {
      context.addDataStoreListener(this);
    }
  }

  @Override
  public void destroy() {
    // Always unregister listeners, as this is easy to forget otherwise
    // TODO: remove destroy() overrides that are redundant?
    context.removeResultListener(this);
    context.removeVisualizationListener(this);
    context.removeDataStoreListener((DataStoreListener) this);
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

  /**
   * Redraw the visualization (maybe incremental).
   *
   * Optional - by default, it will do a full redraw, which often is faster!
   */
  @Override
  public void incrementalRedraw() {
    if(layer != null) {
      while(layer.hasChildNodes()) {
        layer.removeChild(layer.getLastChild());
      }
    }
    fullRedraw();
  }

  @Override
  public abstract void fullRedraw();

  @Override
  public void resultAdded(Result child, Result parent) {
    // Ignore by default
  }

  @Override
  public void resultChanged(Result current) {
    // Default is to redraw when the result we are attached to changed.
    if(task.getResult() == current //
        || (task.has(UpdateFlag.ON_SELECTION) && current instanceof SelectionResult) //
        || (task.has(UpdateFlag.ON_SAMPLE) && current instanceof SamplingResult)) {
      svgp.requestRedraw(task, this);
      return;
    }
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    // Ignore by default.
    // TODO: auto-remove if parent result is removed?
  }

  @Override
  public void visualizationChanged(VisualizationItem item) {
    if(task == item || task.getResult() == item //
        || (task.has(UpdateFlag.ON_STYLEPOLICY) && item instanceof StylingPolicy)) {
      svgp.requestRedraw(task, this);
      return;
    }
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    svgp.requestRedraw(task, this);
  }
}
