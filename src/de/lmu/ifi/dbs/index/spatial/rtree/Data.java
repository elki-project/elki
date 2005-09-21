package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialData;

/**
 * The class Data represents a data object in a spatial index.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class Data implements SpatialData {
  /**
   * The id of this data object.
   */
  private Integer id;

  /**
   * The values of this data object.
   */
  private double[] values;

  /**
   * The id of the parent of this data object.
   */
  private Integer parentID;

  /**
   * Creates a new Data object.
   *
   * @param id       the id of the data object
   * @param values   the values of the data object
   * @param parentID the id of the parent of the data object
   */
  public Data(Integer id, double[] values, Integer parentID) {
    this.id = id;
    this.values = values;
    this.parentID = parentID;
  }
  
  /**
   * Creates a new Data object.
   *
   * @param id       the id of the data object
   * @param values   the values of the data object
   * @param parentID the id of the parent of the data object
   */
  public Data(Integer id, Number[] values, Integer parentID)
  {
      this.id = id;
      this.values = new double[values.length];
      for(int i = 0; i < values.length; i++)
      {
          this.values[i] = values[i].doubleValue();
      }
      this.parentID = parentID;  
  }

  /**
   * Returns the values of this data object.
   *
   * @return the values of this data object
   */
  public double[] getValues() {
    return values;
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
   * Returns the id of the parent node of this data object.
   *
   * @return the id of the parent node of this data object
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
    double[] v = values.clone();
    return new MBR(v, v);
  }

  /**
   * @see Comparable#compareTo(Object)
   */
//  public int compareTo(Object o) {
//    Data other = (Data) o;
//    return this.id - other.id;
//  }

  /**
   * Returns a string representation of this data object.
   *
   * @return a string representation of this data object.
   */
  public String toString() {
    return ""+id;
  }
}
