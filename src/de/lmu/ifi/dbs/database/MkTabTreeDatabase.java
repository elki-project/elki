package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.mktab.MkTabTree;

import java.util.List;

/**
 * MkTabTreeDatabase is a database implementation which is supported by an
 * MkTabTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkTabTreeDatabase<O extends DatabaseObject, D extends Distance<D>> extends MkMaxTreeDatabase<O, D> {

  /**
   * Empty constructor, creates a new MkTabTreeDatabase.
   */
  public MkTabTreeDatabase() {
    super();
  }

  /**
   * Creates a MkTabTree object for this database.
   */
  public MetricalIndex<O, D> createMetricalIndex() {
    return new MkTabTree<O, D>(fileName, pageSize, cacheSize, getDistanceFunction(), k);
  }

  /**
   * Creates a MkTabTree object for this database.
   *
   * @param objects the objects to be indexed
   */
  public MetricalIndex<O, D> createMetricalIndex(List<O> objects) {
    return new MkTabTree<O, D>(fileName, pageSize, cacheSize, getDistanceFunction(), k, objects);
  }

  /**
   * @see Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(MkTabTreeDatabase.class.getName());
    description.append(" holds all the data in an MkTabTree-Tree index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }
}
