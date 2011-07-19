package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.TypeFilterIterator;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Map to store context information for the visualizer. This can be any data
 * that should to be shared among plots, such as line colors, styles etc.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses ContextChangedEvent oneway - - «emit»
 * @apiviz.composedOf StyleLibrary
 * @apiviz.composedOf SelectionResult
 * @apiviz.composedOf ResultHierarchy
 * @apiviz.composedOf EventListenerList
 */
public class VisualizerContext extends AnyMap<String> implements DataStoreListener, ResultListener, Result {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

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
   * Factories to use
   */
  private Collection<VisFactory> factories;

  /**
   * Visualizers to hide by default
   */
  private Pattern hideVisualizers;

  /**
   * Identifier for the primary clustering to use.
   */
  public static final String CLUSTERING = "clustering";

  /**
   * Identifier for a fallback (default) clustering.
   */
  public static final String CLUSTERING_FALLBACK = "clustering-fallback";

  /**
   * Identifier for the visualizer list
   */
  public static final String VISUALIZER_LIST = "visualizers";

  /**
   * Identifier for the selection
   */
  public static final String SELECTION = "selection";

  /**
   * Constructor. We currently require a Database and a Result.
   * 
   * @param result Result
   * @param stylelib Style library
   * @param factories Visualizer Factories to use
   * @param hideVisualizers Pattern to hide visualizers
   */
  public VisualizerContext(HierarchicalResult result, StyleLibrary stylelib, Collection<VisFactory> factories, Pattern hideVisualizers) {
    super();
    this.result = result;
    this.stylelib = stylelib;
    this.factories = factories;
    this.hideVisualizers = hideVisualizers;

    List<Clustering<? extends Model>> clusterings = ResultUtil.getClusteringResults(result);
    if(clusterings.size() > 0) {
      this.put(CLUSTERING, clusterings.get(0));
    }
    List<SelectionResult> selections = ResultUtil.filterResults(result, SelectionResult.class);
    if(selections.size() > 0) {
      this.put(SELECTION, selections.get(0));
    }

    result.getHierarchy().add(result, this);

    // Add visualizers.
    processNewResult(result, result);

    // For proxying events.
    // FIXME: RELEASE4
    // this.database.addDataStoreListener(this);
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
   * Convenience method to get the clustering to use, and fall back to a default
   * "clustering".
   * 
   * @return Clustering to use
   */
  public Clustering<Model> getOrCreateDefaultClustering() {
    Clustering<Model> c = getGenerics(CLUSTERING, Clustering.class);
    if(c == null) {
      c = getGenerics(CLUSTERING_FALLBACK, Clustering.class);
    }
    if(c == null) {
      c = generateDefaultClustering();
    }
    return c;
  }

  /**
   * Generate a default (fallback) clustering.
   * 
   * @return generated clustering
   */
  private Clustering<Model> generateDefaultClustering() {
    // Cluster by labels
    ByLabelHierarchicalClustering split = new ByLabelHierarchicalClustering();
    Clustering<Model> c = split.run(ResultUtil.findDatabase(getResult()));
    // store.
    put(CLUSTERING_FALLBACK, c);
    return c;
  }

  // TODO: add ShowVisualizer,HideVisualizer with tool semantics.

  /**
   * Get the current selection.
   * 
   * @return selection
   */
  public DBIDSelection getSelection() {
    SelectionResult res = getGenerics(SELECTION, SelectionResult.class);
    if(res != null) {
      return res.getSelection();
    }
    return null;
  }

  /**
   * Set a new selection.
   * 
   * @param sel Selection
   */
  public void setSelection(DBIDSelection sel) {
    SelectionResult selres = getGenerics(SELECTION, SelectionResult.class);
    selres.setSelection(sel);
    getHierarchy().resultChanged(selres);
  }

  /**
   * Change a visualizers visibility.
   * 
   * When a Tool visualizer is made visible, other tools are hidden.
   * 
   * @param task Visualization task
   * @param visibility new visibility
   */
  public void setVisualizationVisibility(VisualizationTask task, boolean visibility) {
    // Hide other tools
    if(visibility && VisualizerUtil.isTool(task)) {
      for(VisualizationTask other : iterVisualizers()) {
        if(other != task && VisualizerUtil.isTool(other) && VisualizerUtil.isVisible(other)) {
          other.put(VisualizationTask.META_VISIBLE, false);
          getHierarchy().resultChanged(other);
        }
      }
    }
    task.put(VisualizationTask.META_VISIBLE, visibility);
    getHierarchy().resultChanged(task);
  }

  /**
   * Add a context change listener.
   * 
   * @param listener
   */
  public void addContextChangeListener(ContextChangeListener listener) {
    listenerList.add(ContextChangeListener.class, listener);
  }

  /**
   * Remove a context change listener.
   * 
   * @param listener
   */
  public void removeContextChangeListener(ContextChangeListener listener) {
    listenerList.remove(ContextChangeListener.class, listener);
  }

  /**
   * Trigger a context change event.
   * 
   * @param e Event
   */
  public void fireContextChange(ContextChangedEvent e) {
    for(ContextChangeListener listener : listenerList.getListeners(ContextChangeListener.class)) {
      listener.contextChanged(e);
    }
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
   * @param result Result
   */
  private void processNewResult(HierarchicalResult baseResult, Result newResult) {
    // Collect all visualizers.
    for(VisFactory f : factories) {
      if(hideVisualizers.matcher(f.getClass().getName()).find()) {
        continue;
      }
      try {
        f.processNewResult(baseResult, newResult);
      }
      catch(Throwable e) {
        logger.warning("AlgorithmAdapter " + f.getClass().getCanonicalName() + " failed:", e);
      }
    }
  }

  /**
   * Get an iterator over all visualizers.
   * 
   * @return Iterator
   */
  public IterableIterator<VisualizationTask> iterVisualizers() {
    return new VisualizerIterator();
  }

  /**
   * Iterator doing a depth-first traversal of the tree.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class VisualizerIterator implements IterableIterator<VisualizationTask> {
    /**
     * The results iterator.
     */
    private Iterator<? extends Result> resultiter = null;

    /**
     * Current results visualizers
     */
    private Iterator<VisualizationTask> resultvisiter = null;

    /**
     * The current result
     */
    private Result curResult = null;

    /**
     * The next item to return.
     */
    private VisualizationTask nextItem = null;

    /**
     * Constructor.
     */
    public VisualizerIterator() {
      super();
      this.resultiter = ResultUtil.filteredResults(getResult(), Result.class);
      updateNext();
    }

    /**
     * Update the iterator to point to the next element.
     */
    private void updateNext() {
      nextItem = null;
      // try within the current result
      if(resultvisiter != null && resultvisiter.hasNext()) {
        nextItem = resultvisiter.next();
        return;
      }
      if(resultiter != null && resultiter.hasNext()) {
        // advance to next result, retry.
        curResult = resultiter.next();
        final List<Result> children = getHierarchy().getChildren(curResult);
        resultvisiter = new TypeFilterIterator<Result, VisualizationTask>(VisualizationTask.class, children);
        updateNext();
        return;
      }
      // This means we have failed - we'll leave nextItem = null
    }

    @Override
    public boolean hasNext() {
      return (nextItem != null);
    }

    @Override
    public VisualizationTask next() {
      VisualizationTask vis = nextItem;
      updateNext();
      return vis;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removals are not supported.");
    }

    @Override
    public Iterator<VisualizationTask> iterator() {
      return this;
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
  public void resultAdded(Result child, @SuppressWarnings("unused") Result parent) {
    processNewResult(getResult(), child);
  }

  @SuppressWarnings("unused")
  @Override
  public void resultChanged(Result current) {
    // FIXME: need to do anything?
  }

  @SuppressWarnings("unused")
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