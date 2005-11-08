package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.MTree;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.List;

/**
 * MTreeDatabase is a database implementation which is supported by a
 * MTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTreeDatabase<O extends MetricalObject, D extends Distance> extends MetricalIndexDatabase<O, D> {
  /**
   * Empty constructor, creates a new MTreeDatabase.
   */
  public MTreeDatabase() {
    super();
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Creates a metrical index object for this database.
   */
  public MetricalIndex<O, D> createMetricalIndex() {
    return new MTree<O, D>(fileName, pageSize, cacheSize, getDistanceFunction());
  }

  /**
   * Creates a metrical index object for this database.
   *
   * @param objects the objects to be indexed
   */
  public MetricalIndex<O, D> createMetricalIndex(List<O> objects) {
    return new MTree<O, D>(fileName, pageSize, cacheSize, getDistanceFunction(), objects);
  }

  /**
   * @see Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(MTreeDatabase.class.getName());
    description.append(" holds all the data in a MTree index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * Sets the values for the parameters filename, pagesize, cachesize and flat
   * if specified. If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    return super.setParameters(args);
  }


}
