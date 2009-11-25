package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelHierarchicalClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.AnyMap;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PropertiesBasedColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.linestyles.DashedLineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.linestyles.LineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.PrettyMarkers;

/**
 * Map to store context information for the visualizer. This can be any data
 * that should to be shared among plots, such as line colors, styles etc.
 * 
 * @author Erich Schubert
 * 
 */
public class VisualizerContext extends AnyMap<String> {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The database
   */
  private Database<?> database;

  /**
   * The full result object
   */
  private Result result;

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
  public VisualizerContext(Database<?> database, Result result) {
    super();
    this.database = database;
    this.result = result;
    
    List<Clustering<?>> clusterings = ResultUtil.getClusteringResults(result);
    if (clusterings.size() > 0) {
      this.put(CLUSTERING, clusterings.get(0));
    }
  }

  /**
   * Get the database itself
   * 
   * @param <O> Database object type
   * @return Database
   */
  @SuppressWarnings("unchecked")
  public <O extends DatabaseObject> Database<O> getDatabase() {
    // TODO: can we get some increase type safety here maybe?
    return (Database<O>) database;
  }

  /**
   * Get the full result object
   */
  public Result getResult() {
    return result;
  }

  /**
   * Convenience method to get the current color library, or use a default.
   * 
   * @return Color library
   */
  public ColorLibrary getColorLibrary() {
    ColorLibrary lib = get(COLOR_LIBRARY, ColorLibrary.class);
    if(lib == null) {
      lib = new PropertiesBasedColorLibrary();
      put(COLOR_LIBRARY, lib);
    }
    return lib;
  }

  /**
   * Convenience method to get the current marker
   * 
   * @return Marker library
   */
  public MarkerLibrary getMarkerLibrary() {
    MarkerLibrary lib = get(MARKER_LIBRARY, MarkerLibrary.class);
    if(lib == null) {
      lib = new PrettyMarkers(getColorLibrary());
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
      lib = new DashedLineStyleLibrary(getColorLibrary());
      put(LINESTYLE_LIBRARY, lib);
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
    ByLabelHierarchicalClustering<DatabaseObject> split = new ByLabelHierarchicalClustering<DatabaseObject>();
    Clustering<Model> c = split.run(getDatabase());
    // store.
    put(CLUSTERING_FALLBACK, c);
    return c;
  }
}
