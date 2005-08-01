package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.SpatialData;
import de.lmu.ifi.dbs.index.spatial.MBR;

/**
 * The class Data represents a data object in a RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class Data implements SpatialData {
  /**
   * The id of this data object.
   */
  private int id;

  /**
   * The values of this data object.
   */
  private double[] values;

  /**
   * The id of the parent of this data object.
   */
  private int parentID;

  /**
   * Creates a new Data object.
   *
   * @param id       the id of the data object
   * @param values   the values of the data object
   * @param parentID the id of the parent of the data object
   */
  public Data(int id, double[] values, int parentID) {
    this.id = id;
    this.values = values;
    this.parentID = parentID;
  }

  /**
   * Returns the values of this data object.
   *
   * @return the values of this data object
   */
  public double[] getValues() {
    return (double[]) values;
  }

  public double getValue(int dimension) {
    return values[dimension];
  }



  /**
   * Returns the unique object id if the data object.
   *
   * @return the unique object id if the data object
   */
  public int getObjectID() {
    return id;
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.SpatialObject#getParentID()
   */
  public int getParentID() {
    return parentID;
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.SpatialObject#getDimensionality()
   */
  public int getDimensionality() {
    return values.length;
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.SpatialObject#mbr()
   */
  public MBR mbr() {
    double[] v = (double[]) values.clone();
    return new MBR(v, v);
  }

  /**
   * @see Comparable#compareTo(Object)
   */
  public int compareTo(Object o) {
    Data other = (Data) o;
    return this.id - other.id;
  }
}
