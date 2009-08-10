package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PropertiesBasedColorLibrary;
import experimentalcode.remigius.VisualizationManager;

/**
 * Abstract superclass for Visualizers.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <O> the type of object this visualizer will process.
 */
public abstract class AbstractVisualizer<O extends DatabaseObject> extends AbstractParameterizable implements Visualizer {

  /**
   * Contains all objects to be processed.
   */
  protected Database<O> database;

  /**
   * Used to receive and publish any kind of information.
   */
  protected VisualizationManager<O> visManager;

  /**
   * Provides colors which can be used by elements of the visualization.
   */
  protected static final ColorLibrary COLORS = new PropertiesBasedColorLibrary();

  /**
   * Indicates on which level the visualization will be placed.
   */
  private int level;

  /**
   * Short characterization of this Visualizer.
   */
  private String name;

  /**
   * Initializes the Visualizer. <br>
   * Note that calling only this method might not be sufficient to be able to
   * run a visualizer.
   * 
   * @param db contains all objects to be processed.
   * @param v used to receive and publish different information.
   * @param name a short name characterizing this Visualizer
   */
  public void init(Database<O> db, VisualizationManager<O> v, int level, String name) {
    this.database = db;
    this.level = level;
    this.name = name;
    this.visManager = v;
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
