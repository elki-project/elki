package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.List;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseEvent;
import de.lmu.ifi.dbs.elki.database.DatabaseListener;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.AnyMap;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.lines.DashedLineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.lines.LineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.PrettyMarkers;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Map to store context information for the visualizer. This can be any data
 * that should to be shared among plots, such as line colors, styles etc.
 * 
 * @author Erich Schubert
 */
public class VisualizerContext<O extends DatabaseObject> extends AnyMap<String> implements DatabaseListener<O> {
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
  private Result result;

  /**
   * The event listeners for this parameter.
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
   * Constructor. We currently require a Database and a Result.
   * 
   * @param database Database
   * @param result Result
   */
  public VisualizerContext(Database<O> database, Result result) {
    super();
    this.database = database;
    this.result = result;
    
    List<Clustering<? extends Model>> clusterings = ResultUtil.getClusteringResults(result);
    if (clusterings.size() > 0) {
      this.put(CLUSTERING, clusterings.get(0));
    }
    
    this.database.addDatabaseListener(this);
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
  public Result getResult() {
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
   * Add a database change listener.
   * 
   * @param listener
   */
  public void addDatabaseListener(DatabaseListener<?> listener) {
    listenerList.add(DatabaseListener.class, listener);
  }

  /**
   * Remove a database change listener.
   * 
   * @param listener
   */
  public void removeDatabaseListener(DatabaseListener<?> listener) {
    listenerList.remove(DatabaseListener.class, listener);
  }

  /**
   * Proxy database change event to child listeners
   */
  @SuppressWarnings("unchecked")
  @Override
  public void objectsChanged(DatabaseEvent<O> e) {
    for(DatabaseListener<?> listener : listenerList.getListeners(DatabaseListener.class)) {
      ((DatabaseListener<O>)listener).objectsChanged(e);
    }
  }

  /**
   * Proxy database change event to child listeners
   */
  @SuppressWarnings("unchecked")
  @Override
  public void objectsInserted(DatabaseEvent<O> e) {
    for(DatabaseListener<?> listener : listenerList.getListeners(DatabaseListener.class)) {
      ((DatabaseListener<O>)listener).objectsInserted(e);
    }
  }

  /**
   * Proxy database change event to child listeners
   */
  @SuppressWarnings("unchecked")
  @Override
  public void objectsRemoved(DatabaseEvent<O> e) {
    for(DatabaseListener<?> listener : listenerList.getListeners(DatabaseListener.class)) {
      ((DatabaseListener<O>)listener).objectsRemoved(e);
    }
  }
}