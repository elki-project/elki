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
package de.lmu.ifi.dbs.elki.visualization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.TrivialAllInOne;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.evaluation.AutomaticEvaluation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.*;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;

/**
 * Map to store context information for the visualizer. This can be any data
 * that should to be shared among plots, such as line colors, styles etc.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @opt nodefillcolor LemonChiffon
 * @composed - - - StyleLibrary
 * @composed - - - StylingPolicy
 * @composed - - - SelectionResult
 * @composed - - - ResultHierarchy
 * @composed - - - VisualizationTree
 * @composed - - - DataStoreListener
 * @composed - - - VisualizationProcessor
 */
public class VisualizerContext implements DataStoreListener, Result {
  /**
   * Logger.
   */
  private static final Logging LOG = Logging.getLogger(VisualizerContext.class);

  /**
   * Tree of visualizations.
   */
  private VisualizationTree vistree = new VisualizationTree();

  /**
   * The full result object
   */
  private ResultHierarchy hier;

  /**
   * The event listeners for this context.
   */
  private ArrayList<DataStoreListener> listenerList = new ArrayList<>();

  /**
   * Factories to use
   */
  private Collection<VisualizationProcessor> factories;

  /**
   * Selection result
   */
  private SelectionResult selection;

  /**
   * Styling policy
   */
  StylingPolicy stylepolicy;

  /**
   * Style library
   */
  StyleLibrary stylelibrary;

  /**
   * Starting point of the result tree, may be {@code null}.
   */
  private Result baseResult;

  /**
   * Constructor. We currently require a Database and a Result.
   *
   * @param hier Result hierarchy
   * @param start Starting result
   * @param stylelib Style library
   * @param factories Visualizer Factories to use
   */
  public VisualizerContext(ResultHierarchy hier, Result start, StyleLibrary stylelib, Collection<VisualizationProcessor> factories) {
    super();
    this.hier = hier;
    this.baseResult = start;
    this.factories = factories;

    // Ensure that various common results needed by visualizers are
    // automatically created
    final Database db = ResultUtil.findDatabase(hier);
    if(db == null) {
      LOG.warning("No database reachable from " + hier);
      return;
    }
    AutomaticEvaluation.ensureClusteringResult(db, db);
    this.selection = SelectionResult.ensureSelectionResult(db);
    for(Relation<?> rel : ResultUtil.getRelations(db)) {
      SamplingResult.getSamplingResult(rel);
      // FIXME: this is a really ugly workaround. :-(
      if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        @SuppressWarnings("unchecked")
        Relation<? extends NumberVector> vrel = (Relation<? extends NumberVector>) rel;
        ScalesResult.getScalesResult(vrel);
      }
    }
    makeStyleResult(stylelib);

    // Add visualizers.
    notifyFactories(db);

    // For proxying events.
    db.addDataStoreListener(this);
    // Add a result listener.
    // Don't expose these methods to avoid inappropriate use.
    addResultListener(new ResultListener() {
      @Override
      public void resultAdded(Result child, Result parent) {
        notifyFactories(child);
      }

      @Override
      public void resultChanged(Result current) {
        // FIXME: need to do anything?
      }

      @Override
      public void resultRemoved(Result child, Result parent) {
        // FIXME: implement
      }
    });
  }

  /**
   * Generate a new style result for the given style library.
   *
   * @param stylelib Style library
   */
  protected void makeStyleResult(StyleLibrary stylelib) {
    final Database db = ResultUtil.findDatabase(hier);
    stylelibrary = stylelib;
    List<Clustering<? extends Model>> clusterings = Clustering.getClusteringResults(db);
    if(!clusterings.isEmpty()) {
      stylepolicy = new ClusterStylingPolicy(clusterings.get(0), stylelib);
    }
    else {
      Clustering<Model> c = generateDefaultClustering();
      stylepolicy = new ClusterStylingPolicy(c, stylelib);
    }
  }

  /**
   * Get the hierarchy object
   *
   * @return hierarchy object
   */
  public ResultHierarchy getHierarchy() {
    return hier;
  }

  /**
   * Get the active styling policy
   *
   * @return Styling policy
   */
  public StylingPolicy getStylingPolicy() {
    return stylepolicy;
  }

  /**
   * Set the active styling policy
   *
   * @param policy new Styling policy
   */
  public void setStylingPolicy(StylingPolicy policy) {
    this.stylepolicy = policy;
    visChanged(policy);
  }

  /**
   * Get the style library
   *
   * @return Style library
   */
  public StyleLibrary getStyleLibrary() {
    return stylelibrary;
  }

  /**
   * Get the style library
   *
   * @param library Style library
   */
  public void setStyleLibrary(StyleLibrary library) {
    this.stylelibrary = library;
  }

  /**
   * Generate a default (fallback) clustering.
   *
   * @return generated clustering
   */
  private Clustering<Model> generateDefaultClustering() {
    final Database db = ResultUtil.findDatabase(hier);
    Clustering<Model> c = null;
    try {
      // Try to cluster by labels
      ByLabelHierarchicalClustering split = new ByLabelHierarchicalClustering();
      c = split.run(db);
    }
    catch(NoSupportedDataTypeException e) {
      // Put everything into one
      c = new TrivialAllInOne().run(db);
    }
    return c;
  }

  // TODO: add ShowVisualizer,HideVisualizer with tool semantics.

  // TODO: add ShowVisualizer,HideVisualizer with tool semantics.

  /**
   * Get the current selection result.
   *
   * @return selection result
   */
  public SelectionResult getSelectionResult() {
    return selection;
  }

  /**
   * Get the current selection.
   *
   * @return selection
   */
  public DBIDSelection getSelection() {
    return selection.getSelection();
  }

  /**
   * Set a new selection.
   *
   * @param sel Selection
   */
  public void setSelection(DBIDSelection sel) {
    selection.setSelection(sel);
    getHierarchy().resultChanged(selection);
  }

  /**
   * Adds a listener for the <code>DataStoreEvent</code> posted after the
   * content changes.
   *
   * @param l the listener to add
   * @see #removeDataStoreListener
   */
  public void addDataStoreListener(DataStoreListener l) {
    for(int i = 0; i < listenerList.size(); i++) {
      if(listenerList.get(i) == l) {
        return;
      }
    }
    listenerList.add(l);
  }

  /**
   * Removes a listener previously added with <code>addDataStoreListener</code>.
   *
   * @param l the listener to remove
   * @see #addDataStoreListener
   */
  public void removeDataStoreListener(DataStoreListener l) {
    listenerList.remove(l);
  }

  /**
   * Proxy datastore event to child listeners.
   */
  @Override
  public void contentChanged(DataStoreEvent e) {
    for(int i = 0; i < listenerList.size(); i++) {
      listenerList.get(i).contentChanged(e);
    }
  }

  /**
   * Register a result listener.
   *
   * @param listener Result listener.
   */
  public void addResultListener(ResultListener listener) {
    getHierarchy().addResultListener(listener);
  }

  /**
   * Remove a result listener.
   *
   * @param listener Result listener.
   */
  public void removeResultListener(ResultListener listener) {
    getHierarchy().removeResultListener(listener);
  }

  /**
   * Add a listener.
   *
   * @param listener Listener to add
   */
  public void addVisualizationListener(VisualizationListener listener) {
    vistree.addVisualizationListener(listener);
  }

  /**
   * Add a listener.
   *
   * @param listener Listener to remove
   */
  public void removeVisualizationListener(VisualizationListener listener) {
    vistree.removeVisualizationListener(listener);
  }

  @Override
  public String getLongName() {
    return "Visualizer context";
  }

  @Override
  public String getShortName() {
    return "vis-context";
  }

  /**
   * Starting point for visualization, may be {@code null}.
   *
   * @return Starting point in the result tree, may be {@code null}.
   */
  public Result getBaseResult() {
    return baseResult;
  }

  /**
   * Add (register) a visualization.
   *
   * @param parent Parent object
   * @param vis Visualization
   */
  public void addVis(Object parent, VisualizationItem vis) {
    vistree.add(parent, vis);
    notifyFactories(vis);
    visChanged(vis);
  }

  /**
   * A visualization item has changed.
   *
   * @param item Item that has changed
   */
  public void visChanged(VisualizationItem item) {
    vistree.visChanged(item);
  }

  /**
   * Notify factories of a change.
   *
   * @param item Item that has changed.
   */
  private void notifyFactories(Object item) {
    for(VisualizationProcessor f : factories) {
      try {
        f.processNewResult(this, item);
      }
      catch(Throwable e) {
        LOG.warning("VisFactory " + f.getClass().getCanonicalName() + " failed:", e);
      }
    }
  }

  public List<VisualizationTask> getVisTasks(VisualizationItem item) {
    List<VisualizationTask> out = new ArrayList<>();
    vistree.iterDescendants(item).filter(VisualizationTask.class).forEach(out::add);
    return out;
  }

  public VisualizationTree getVisHierarchy() {
    return vistree;
  }
}
