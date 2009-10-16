package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
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
   * Identifier for the full database
   */
  public static final String DATABASE = "database";

  /**
   * Identifier for the full result object
   */
  public static final String RESULT = "result";

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
   * Constructor. We currently require a Database and a Result.
   * 
   * @param db Database
   * @param r Result
   */
  public VisualizerContext(Database<?> db, Result r) {
    super();
    this.put(DATABASE, db);
    this.put(RESULT, r);
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
    return get(DATABASE, Database.class);
  }
  
  /**
   * Get the full result object
   */
  public Result getResult() {
    return get(RESULT, Result.class);
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
}
