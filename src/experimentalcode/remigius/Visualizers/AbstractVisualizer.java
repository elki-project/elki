package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
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
   * Indicates when to execute this Visualizer.
   * @see #getLevel()
   */
  private int level;

  /**
   * Short characterization of this Visualizer.
   * @see #getName()
   */
  protected String name;

  /**
   * Initializes this Visualizer.
   * 
   * @param db contains all objects to be processed.
   * @param level indicates when to execute this Visualizer.
   * @param name a short name characterizing this Visualizer
   */
  public void init(Database<O> db, int level, String name) {
    this.database = db;
    this.level = level;
    this.name = name;
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public String getName() {
    return name;
  }
}
