package de.lmu.ifi.dbs.elki.visualization;

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
import java.util.Collection;
import java.util.List;

import javax.swing.event.EventListenerList;

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
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;

/**
 * Map to store context information for the visualizer. This can be any data
 * that should to be shared among plots, such as line colors, styles etc.
 *
 * @author Erich Schubert
 *
 * @apiviz.landmark
 * @apiviz.composedOf StyleLibrary
 * @apiviz.composedOf SelectionResult
 * @apiviz.composedOf ResultHierarchy
 * @apiviz.composedOf EventListenerList
 * @apiviz.composedOf StyleResult
 * @apiviz.composedOf ProjectorFactory
 * @apiviz.composedOf VisFactory
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
  private EventListenerList listenerList = new EventListenerList();

  /**
   * Factories to use
   */
  private Collection<VisualizationProcessor> factories;

  /**
   * Selection result
   */
  private SelectionResult selection;

  /**
   * Styling result
   */
  private StyleResult styleresult;

  /**
   * Starting point of the result tree, may be {@code null}.
   */
  private Result baseResult;

  /**
   * Relation currently visualized.
   */
  private Relation<?> relation;

  /**
   * Constructor. We currently require a Database and a Result.
   *
   * @param hier Result hierarchy
   * @param start Starting result
   * @param factories Visualizer Factories to use
   */
  public VisualizerContext(ResultHierarchy hier, Result start, Relation<?> relation, StyleLibrary stylelib, Collection<VisualizationProcessor> factories) {
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
    ResultUtil.ensureClusteringResult(db, db);
    this.selection = ResultUtil.ensureSelectionResult(db);
    for(Relation<?> rel : ResultUtil.getRelations(db)) {
      ResultUtil.getSamplingResult(rel);
      // FIXME: this is a really ugly workaround. :-(
      if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        @SuppressWarnings("unchecked")
        Relation<? extends NumberVector> vrel = (Relation<? extends NumberVector>) rel;
        ResultUtil.getScalesResult(vrel);
      }
    }
    makeStyleResult(stylelib);

    // result.getHierarchy().add(result, this);

    // Add visualizers.
    notifyFactories(db);

    // For proxying events.
    db.addDataStoreListener(this);
    // Add a result listener. Don't expose these methods to avoid inappropriate
    // use.
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
    styleresult = new StyleResult();
    styleresult.setStyleLibrary(stylelib);
    List<Clustering<? extends Model>> clusterings = ResultUtil.getClusteringResults(db);
    if(clusterings.size() > 0) {
      styleresult.setStylingPolicy(new ClusterStylingPolicy(clusterings.get(0), stylelib));
    }
    else {
      Clustering<Model> c = generateDefaultClustering();
      styleresult.setStylingPolicy(new ClusterStylingPolicy(c, stylelib));
    }
    hier.add(db, styleresult);
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
   * Get the style result.
   *
   * @return Style result
   */
  public StyleResult getStyleResult() {
    return styleresult;
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
   * Current relation.
   */
  public Relation<?> getRelation() {
    return relation;
  }

  /**
   * Set the current relation.
   *
   * @param rel Relation
   */
  public void setRelation(Relation<?> rel) {
    this.relation = rel;
    getHierarchy().resultChanged(this);
  }

  /**
   * Adds a listener for the <code>DataStoreEvent</code> posted after the
   * content changes.
   *
   * @param l the listener to add
   * @see #removeDataStoreListener
   */
  public void addDataStoreListener(DataStoreListener l) {
    listenerList.add(DataStoreListener.class, l);
  }

  /**
   * Removes a listener previously added with <code>addDataStoreListener</code>.
   *
   * @param l the listener to remove
   * @see #addDataStoreListener
   */
  public void removeDataStoreListener(DataStoreListener l) {
    listenerList.remove(DataStoreListener.class, l);
  }

  /**
   * Proxy datastore event to child listeners.
   */
  @Override
  public void contentChanged(DataStoreEvent e) {
    for(DataStoreListener listener : listenerList.getListeners(DataStoreListener.class)) {
      listener.contentChanged(e);
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
    listenerList.add(VisualizationListener.class, listener);
  }

  /**
   * Add a listener.
   *
   * @param listener Listener to remove
   */
  public void removeVisualizationListener(VisualizationListener listener) {
    listenerList.remove(VisualizationListener.class, listener);
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
    visChanged(vis);
  }

  /**
   * A visualization item has changed.
   *
   * @param item Item that has changed
   */
  public void visChanged(VisualizationItem item) {
    notifyFactories(item);
    for(VisualizationListener listener : listenerList.getListeners(VisualizationListener.class)) {
      listener.visualizationChanged(item);
    }
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
    for(Hierarchy.Iter<?> iter = vistree.iterDescendants(item); iter.valid(); iter.advance()) {
      Object o = iter.get();
      if(o instanceof VisualizationTask) {
        out.add((VisualizationTask) o);
      }
    }
    return out;
  }

  public VisualizationTree getVisHierarchy() {
    return vistree;
  }
}
