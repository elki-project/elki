package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.AnyMap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PropertiesBasedColorLibrary;

/**
 * Abstract superclass for Visualizers.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <O> the type of object this Visualizer will process.
 */
public abstract class AbstractVisualizer<O extends DatabaseObject> extends AbstractParameterizable implements Visualizer {
  /**
   * Contains all objects to be processed.
   */
  protected Database<O> database;

  /**
   * Provides colors which can be used by elements of the visualization.
   */
  protected static final ColorLibrary COLORS = new PropertiesBasedColorLibrary();
  
  /**
   * Meta data storage
   */
  protected AnyMap<String> metadata;

  /**
   * Initializes this Visualizer.
   * 
   * @param db contains all objects to be processed.
   * @param level indicates when to execute this Visualizer.
   * @param name a short name characterizing this Visualizer
   */
  public void init(Database<O> db, int level, String name) {
    this.database = db;
    this.metadata = new AnyMap<String>();
    this.metadata.put(Visualizer.META_LEVEL, level);
    this.metadata.put(Visualizer.META_NAME, name);
  }

  @Override
  public AnyMap<String> getMetadata() {
    return metadata;
  }
}
