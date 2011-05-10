package experimentalcode.frankenb.algorithms.partitioning;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Partition representing a grid cell.
 * 
 * @author Erich Schubert
 */
public class DBIDGridPartition extends DBIDPartition {
  /**
   * Grid position
   */
  private final int[] position;

  /**
   * Constructor.
   * 
   * @param ids
   * @param position
   */
  public DBIDGridPartition(DBIDs ids, int[] position) {
    super(ids);
    this.position = position;
  }

  /**
   * Get the grid position in a particular dimension.
   * 
   * @param dimension Dimension
   * @return Grid position.
   */
  public int getPosition(int dimension) {
    return this.position[dimension - 1];
  }

  /**
   * Get the grid dimensionality.
   * 
   * @return Dimensionality.
   */
  public int getDimensionality() {
    return this.position.length;
  }

  /**
   * Get the position array.
   * 
   * @return Array
   */
  public int[] getPositionArray() {
    return this.position;
  }
}
