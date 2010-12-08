package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.TypeFilterIterator;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.lines.DashedLineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.lines.LineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.PrettyMarkers;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.VisualizationChangedEvent;

/**
 * Map to store context information for the visualizer. This can be any data
 * that should to be shared among plots, such as line colors, styles etc.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses ContextChangedEvent oneway - - «emit»
 * @apiviz.has MarkerLibrary
 * @apiviz.has LineStyleLibrary
 * @apiviz.has StyleLibrary
 * @apiviz.has SelectionResult
 * @apiviz.uses Result oneway - - handles
 */
public class VisualizerContext<O extends DatabaseObject> extends AnyMap<String> implements DataStoreListener<O>, ResultListener {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The database
   */
  private Database<O> database;

  /**
   * The full result object
   */
  private HierarchicalResult result;

  /**
   * The event listeners for this context.
   */
  private EventListenerList listenerList = new EventListenerList();

  /**
   * Identifier for the main color library to use.
   */
  public static final String COLOR_LIBRARY = "colorlibrary";

  /**
   * Identifier for the main marker library to use.
   */
  public static final String MARKER_LIBRARY = "markerlibrary";

  /**
   * Identifier for the main line library to use.
   */
  public static final String LINESTYLE_LIBRARY = "linelibrary";

  /**
   * Identifier for the main style library to use.
   */
  public static final String STYLE_LIBRARY = "stylelibrary";

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
   * Visualizers to hide by default.
   */
  public static final String HIDE_PATTERN = "hide-vis";

  /**
   * Constructor. We currently require a Database and a Result.
   * 
   * @param database Database
   * @param result Result
   */
  public VisualizerContext(Database<O> database, HierarchicalResult result) {
    super();
    this.database = database;
    this.result = result;

    List<Clustering<? extends Model>> clusterings = ResultUtil.getClusteringResults(result);
    if(clusterings.size() > 0) {
      this.put(CLUSTERING, clusterings.get(0));
    }
    List<SelectionResult> selections = ResultUtil.filterResults(result, SelectionResult.class);
    if(selections.size() > 0) {
      this.put(SELECTION, selections.get(0));
    }
    this.database.addDataStoreListener(this);
    // this.result.addResultListener(this);
  }

  /**
   * Get the database itself
   * 
   * @return Database
   */
  public Database<O> getDatabase() {
    return database;
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
   * Convenience method to get the current marker
   * 
   * @return Marker library
   */
  public MarkerLibrary getMarkerLibrary() {
    MarkerLibrary lib = get(MARKER_LIBRARY, MarkerLibrary.class);
    if(lib == null) {
      lib = new PrettyMarkers(getStyleLibrary());
      put(MARKER_LIBRARY, lib);
    }
    return lib;
  }

  /**
   * Convenience method to get the current line style library, or use a default.
   * 
   * @return Line style library
   */
  public LineStyleLibrary getLineStyleLibrary() {
    LineStyleLibrary lib = get(LINESTYLE_LIBRARY, LineStyleLibrary.class);
    if(lib == null) {
      lib = new DashedLineStyleLibrary(getStyleLibrary());
      put(LINESTYLE_LIBRARY, lib);
    }
    return lib;
  }

  /**
   * Get the style library
   * 
   * @return style library
   */
  public StyleLibrary getStyleLibrary() {
    StyleLibrary lib = get(STYLE_LIBRARY, StyleLibrary.class);
    if(lib == null) {
      lib = new PropertiesBasedStyleLibrary();
      put(STYLE_LIBRARY, lib);
    }
    return lib;
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
    ByLabelHierarchicalClustering<O> split = new ByLabelHierarchicalClustering<O>();
    Clustering<Model> c = split.run(getDatabase());
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
    this.fireContextChange(new SelectionChangedEvent(this));
  }

  /**
   * Change a visualizers visibility.
   * 
   * When a Tool visualizer is made visible, other tools are hidden.
   * 
   * @param v Visualizer
   * @param visibility new visibility
   */
  public void setVisualizationVisibility(VisualizationTask task, boolean visibility) {
    // Hide other tools
    if(visibility && VisualizerUtil.isTool(task)) {
      for(VisualizationTask other : iterVisualizers()) {
        if(other != task && VisualizerUtil.isTool(other) && VisualizerUtil.isVisible(other)) {
          other.put(VisualizationTask.META_VISIBLE, false);
          fireContextChange(new VisualizationChangedEvent(this, other));
        }
      }
    }
    task.put(VisualizationTask.META_VISIBLE, visibility);
    fireContextChange(new VisualizationChangedEvent(this, task));
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
  public void addDataStoreListener(DataStoreListener<?> l) {
    listenerList.add(DataStoreListener.class, l);
  }

  /**
   * Removes a listener previously added with <code>addDataStoreListener</code>.
   * 
   * @param l the listener to remove
   * @see #addDataStoreListener
   */
  public void removeDataStoreListener(DataStoreListener<?> l) {
    listenerList.remove(DataStoreListener.class, l);
  }

  /**
   * Proxy datastore event to child listeners.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void contentChanged(DataStoreEvent<O> e) {
    for(DataStoreListener<?> listener : listenerList.getListeners(DataStoreListener.class)) {
      ((DataStoreListener<O>) listener).contentChanged(e);
    }
  }

  /**
   * Proxy result change event to child listeners
   */
  @Override
  public void resultAdded(Result r, Result parent) {
    for(ResultListener listener : listenerList.getListeners(ResultListener.class)) {
      listener.resultAdded(r, parent);
    }
  }

  /**
   * Proxy result change event to child listeners
   */
  @Override
  public void resultRemoved(Result r, Result parent) {
    for(ResultListener listener : listenerList.getListeners(ResultListener.class)) {
      listener.resultRemoved(r, parent);
    }
  }

  /**
   * Attach a visualization to a result.
   * 
   * @param result Result to add the visualization to
   * @param vis Visualization to add
   */
  public void addVisualizer(Result result, VisualizationTask task) {
    if(result == null) {
      LoggingUtil.warning("Visualizer added to null result: " + task, new Throwable());
      return;
    }
    // TODO: solve this in a better way
    if(VisualizerUtil.isTool(task) && VisualizerUtil.isVisible(task)) {
      task.put(VisualizationTask.META_VISIBLE, false);
    }
    // Hide visualizers that match a regexp.
    Pattern hidepatt = get(VisualizerContext.HIDE_PATTERN, Pattern.class);
    if(hidepatt != null) {
      if(hidepatt.matcher(task.getFactory().getClass().getName()).find()) {
        task.put(VisualizationTask.META_VISIBLE, false);
      }
    }
    getResult().getHierarchy().add(result, task);
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
   * Get the visualizers for a particular result.
   * 
   * @param r Result
   * @return Visualizers
   */
  public Iterator<VisualizationTask> getVisualizers(Result r) {
    final List<Result> children = getResult().getHierarchy().getChildren(r);
    return new TypeFilterIterator<Result, VisualizationTask>(VisualizationTask.class, children);
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
        final List<Result> children = getResult().getHierarchy().getChildren(curResult);
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
}