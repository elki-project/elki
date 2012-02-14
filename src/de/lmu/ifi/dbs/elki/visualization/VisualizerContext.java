package de.lmu.ifi.dbs.elki.visualization;

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

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.TrivialAllInOne;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.visualization.projector.ProjectorFactory;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Map to store context information for the visualizer. This can be any data
 * that should to be shared among plots, such as line colors, styles etc.
 * 
 * @author Erich Schubert
 * 
 *         TODO: remove this class
 * 
 * @apiviz.landmark
 * @apiviz.uses ContextChangedEvent oneway - - «emit»
 * @apiviz.composedOf StyleLibrary
 * @apiviz.composedOf SelectionResult
 * @apiviz.composedOf ResultHierarchy
 * @apiviz.composedOf EventListenerList
 */
public class VisualizerContext implements DataStoreListener, ResultListener, Result {
  /**
   * Logger.
   */
  private static final Logging logger = Logging.getLogger(VisualizerContext.class);

  /**
   * The full result object
   */
  private HierarchicalResult result;

  /**
   * The event listeners for this context.
   */
  private EventListenerList listenerList = new EventListenerList();

  /**
   * The style library of this context
   */
  private StyleLibrary stylelib;

  /**
   * Projectors to use
   */
  private Collection<ProjectorFactory> projectors;

  /**
   * Factories to use
   */
  private Collection<VisFactory> factories;

  /**
   * Visualizers to hide by default
   */
  private Pattern hideVisualizers;

  /**
   * Selection result
   */
  private SelectionResult selection;

  /**
   * Styling result
   */
  private StyleResult styleresult;

  /**
   * Constructor. We currently require a Database and a Result.
   * 
   * @param result Result
   * @param stylelib Style library
   * @param projectors Projectors to use
   * @param factories Visualizer Factories to use
   * @param hideVisualizers Pattern to hide visualizers
   */
  public VisualizerContext(HierarchicalResult result, StyleLibrary stylelib, Collection<ProjectorFactory> projectors, Collection<VisFactory> factories, Pattern hideVisualizers) {
    super();
    this.result = result;
    this.stylelib = stylelib;
    this.projectors = projectors;
    this.factories = factories;

    this.hideVisualizers = hideVisualizers;

    List<SelectionResult> selections = ResultUtil.filterResults(result, SelectionResult.class);
    if(selections.size() > 0) {
      this.selection = selections.get(0);
    }

    result.getHierarchy().add(result, this);

    // Ensure a sampling result exists already, as this can cause reentrance
    // errors (visualizers being added twice)!
    // FIXME: avoid these errors properly - e.g. do not create a sampling result
    // in getSamplingResult at all!
    for(Relation<?> rel : ResultUtil.getRelations(result)) {
      ResultUtil.getSamplingResult(rel);
    }

    // Add visualizers.
    processNewResult(result, result);

    // For proxying events.
    ResultUtil.findDatabase(result).addDataStoreListener(this);
    // Add ourselves as RL
    addResultListener(this);
  }

  /**
   * Get the full result object
   * 
   * @return result object
   */
  public HierarchicalResult getResult() {
    return result;
  }

  /**
   * Get the hierarchy object
   * 
   * @return hierarchy object
   */
  public ResultHierarchy getHierarchy() {
    return result.getHierarchy();
  }

  /**
   * Get the style library
   * 
   * @return style library
   */
  public StyleLibrary getStyleLibrary() {
    if(stylelib == null) {
      stylelib = new PropertiesBasedStyleLibrary();
    }
    return stylelib;
  }

  /**
   * Get the style result.
   * 
   * @return Style result
   */
  public StyleResult getStyleResult() {
    if(styleresult == null) {
      styleresult = new StyleResult();
      List<Clustering<? extends Model>> clusterings = ResultUtil.getClusteringResults(result);
      if(clusterings.size() > 0) {
        styleresult.setStylingPolicy(new ClusterStylingPolicy(clusterings.get(0)));
        return styleresult;
      }
      Clustering<Model> c = generateDefaultClustering();
      styleresult.setStylingPolicy(new ClusterStylingPolicy(c));
      return styleresult;
    }
    return styleresult;
  }

  /**
   * Generate a default (fallback) clustering.
   * 
   * @return generated clustering
   */
  private Clustering<Model> generateDefaultClustering() {
    final Database db = ResultUtil.findDatabase(getResult());
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
   * Change a visualizers visibility.
   * 
   * When a Tool visualizer is made visible, other tools are hidden.
   * 
   * @param task Visualization task
   * @param visibility new visibility
   * 
   * @deprecated Use {@link VisualizerUtil#setVisible}
   */
  @Deprecated
  public void setVisualizationVisibility(VisualizationTask task, boolean visibility) {
    VisualizerUtil.setVisible(this, task, visibility);
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
   * Process a particular result.
   * 
   * @param baseResult Base Result
   * @param newResult Newly added Result
   */
  private void processNewResult(HierarchicalResult baseResult, Result newResult) {
    for(ProjectorFactory p : projectors) {
      if(hideVisualizers != null && hideVisualizers.matcher(p.getClass().getName()).find()) {
        continue;
      }
      try {
        p.processNewResult(baseResult, newResult);
      }
      catch(Throwable e) {
        logger.warning("ProjectorFactory " + p.getClass().getCanonicalName() + " failed:", e);
      }
    }
    // Collect all visualizers.
    for(VisFactory f : factories) {
      if(hideVisualizers != null && hideVisualizers.matcher(f.getClass().getName()).find()) {
        continue;
      }
      try {
        f.processNewResult(baseResult, newResult);
      }
      catch(Throwable e) {
        logger.warning("VisFactory " + f.getClass().getCanonicalName() + " failed:", e);
      }
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

  @Override
  public void resultAdded(Result child, Result parent) {
    processNewResult(getResult(), child);
  }

  @Override
  public void resultChanged(Result current) {
    // FIXME: need to do anything?
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    // FIXME: implement
  }

  @Override
  public String getLongName() {
    return "Visualizer context";
  }

  @Override
  public String getShortName() {
    return "vis-context";
  }
}