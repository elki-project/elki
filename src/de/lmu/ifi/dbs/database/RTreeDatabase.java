package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rtree.RTree;

public class RTreeDatabase extends SpatialIndexDatabase {
  /**
   * The name of the file for storing the RTree.
   */
  private String fileName;

  /**
   * The size of a page in bytes.
   */
  private int pageSize = 4000;

  /**
   * Tthe size of the cache.
   */
  private int cacheSize = 50;

  /**
   * If true, the RTree will have a flat directory
   */
  private boolean flatDirectory = false;

  /**
   * Empty constructor
   */
  public RTreeDatabase() {
  }

  /**
   * Creates a new RTreeDatabase with the specified parameters.
   *
   * @param fileName      the name of the file for storing the RTree, if this parameter
   *                      is null the RTree will be hold in main memory
   * @param pageSize      the size of a page in bytes
   * @param cacheSize     the size of the cache (must be >= 1)
   * @param flatDirectory if true, the RTree will have a flat directory (only one level)
   */
  public RTreeDatabase(String fileName, int pageSize, int cacheSize, boolean flatDirectory) {
    this.fileName = fileName;
    this.pageSize = pageSize;
    this.cacheSize = cacheSize;
    this.flatDirectory = flatDirectory;
  }

  /**
   * Returns the specific spatial index object for this database.
   *
   * @return the spatial index for this database
   */
  public SpatialIndex createSpatialIndex(final RealVector[] objects, final int[] ids) {
    return new RTree(objects, ids, fileName, pageSize, cacheSize, flatDirectory);
  }

  /**
   * Returns the spatial index object with the specified parameters
   * for this database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public SpatialIndex createSpatialIndex(int dimensionality) {
    return new RTree(dimensionality, fileName, pageSize, cacheSize, flatDirectory);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(RTreeDatabase.class.getName());
    description.append(" holds all the data in a RTree index structure.");
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    // TODO set parameters

    // TODO return remaining parameters
    return args;
  }


}
