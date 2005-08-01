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
  private int pageSize;

  /**
   * Tthe size of the cache.
   */
  private int cacheSize;

  /**
   * If true, the RTree will have a flat directory
   */
  private boolean flatDirectory;

  /**
   * Creates a new RTreeDatabase with the specified parameters.
   *
   * @param fileName      the name of the file for storing the RTree,
   *                      if this parameter is null the RTree will be hold in
   *                      main memory
   * @param pageSize      the size of a page in bytes
   * @param cacheSize     the size of the cache (must be >= 1)
   * @param flatDirectory if true, the RTree will have a flat directory
   *                      (only one level)
   */
  public RTreeDatabase(String fileName, int pageSize,
                       int cacheSize, boolean flatDirectory) {
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
  public SpatialIndex createSpatialIndex(final RealVector[] objects,
                                         final int[] ids) {

    return new RTree(objects, ids, fileName, pageSize, cacheSize, flatDirectory);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(SpatialIndexDatabase.class.getName());
    description.append(" holds all the data in a RTree index structure.");
    return description.toString();
  }
}
