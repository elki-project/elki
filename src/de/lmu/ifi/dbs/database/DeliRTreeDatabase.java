package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rtree.DeLiCluTree;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

/**
 * DeliRTreeDatabase is a database implementation which is supported by a
 * DeliRTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeliRTreeDatabase extends SpatialIndexDatabase {

  /**
   * Empty constructor, creates a new DeliRTreeDatabase.
   */
  public DeliRTreeDatabase() {
    super();
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Returns the specific spatial index object for this database.
   *
   * @return the spatial index for this database
   */
  public SpatialIndex createSpatialIndex(final FeatureVector[] objects) {
    return new DeLiCluTree(objects, fileName, pageSize, cacheSize);
  }

  /**
   * Returns the spatial index object with the specified parameters
   * for this database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public SpatialIndex createSpatialIndex(int dimensionality) {
    return new DeLiCluTree(dimensionality, fileName, pageSize, cacheSize);
  }

  /**
   * @see Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(DeliRTreeDatabase.class.getName());
    description.append(" holds all the data in a DeliRTree index structure.\n");
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
