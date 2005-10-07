package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rtree.DeLiCluTree;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.List;

/**
 * DeLiCluTreeDatabase is a database implementation which is supported by a
 * DeLiCluTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluTreeDatabase<T extends RealVector> extends SpatialIndexDatabase<T> {

  /**
   * Empty constructor, creates a new DeliRTreeDatabase.
   */
  public DeLiCluTreeDatabase() {
    super();
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Returns the specific spatial index object for this database.
   *
   * @return the spatial index for this database
   */
  public SpatialIndex<T> createSpatialIndex(List<T> objects) {
    return new DeLiCluTree<T>(objects, fileName, pageSize, cacheSize);
  }

  /**
   * Returns the spatial index object with the specified parameters
   * for this database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public SpatialIndex<T> createSpatialIndex(int dimensionality) {
    return new DeLiCluTree<T>(dimensionality, fileName, pageSize, cacheSize);
  }

  /**
   * @see Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(DeLiCluTreeDatabase.class.getName());
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

  /**
   * Returns the index of this database.
   *
   * @return the index of this database
   */
  public DeLiCluTree<T> getIndex() {
    return (DeLiCluTree<T>) index;
  }

  /**
   * Marks the specified object as handled and returns the ids of the
   * objects's subtree.
   *
   * @param id the id of the object to be marked as handled
   * @return the id of the objects's parent node
   */
  public List<Entry> setHandled(Integer id) {
    T o = get(id);
    return getIndex().setHandled(o);
  }
}
