package experimentalcode.remigius.Visualizers;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PropertiesBasedColorLibrary;
import experimentalcode.remigius.VisualizationManager;

public abstract class AbstractVisualizer<O extends DatabaseObject> extends AbstractParameterizable implements Visualizer<O> {

  protected Database<O> database;
  protected VisualizationManager<O> visManager;
  protected static final ColorLibrary COLORS = new PropertiesBasedColorLibrary();
  
  private int level;
  private String name;
  
  public void init(Database<O> db, VisualizationManager<O> v, String name){
    init(db, v, 0, name);
  }
  
  public void init(Database<O> db, VisualizationManager<O> v, int level, String name){
    this.database = db;
    this.visManager = v;
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
