package de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.index.tree.spatial.SpatialLeafEntry;

/**
 * Defines the requirements for a leaf entry in an DeLiClu-Tree node.
 * Additionally to a leaf entry in an R*-Tree two boolean flags that indicate wether this entry's node
 * contains handled or unhandled data objects.
 *
 * @author Elke Achtert 
 */
public class DeLiCluLeafEntry extends SpatialLeafEntry implements DeLiCluEntry {
  /**
   * Indicates that the node (or its child nodes) which is represented by this entry
   * contains handled data objects.
   */
  private boolean hasHandled;

  /**
   * Indicates that the node (or its child nodes) which is represented by this entry
   * contains unhandled data objects.
   */
  private boolean hasUnhandled;

  /**
   * Empty constructor for serialization purposes.
   */
  public DeLiCluLeafEntry() {
	  // empty constructor
  }

  /**
   * Constructs a new LeafEntry object with the given parameters.
   *
   * @param id     the unique id of the underlying data object
   * @param values the values of the underlying data object
   */
  public DeLiCluLeafEntry(int id, double[] values) {
    super(id, values);
    this.hasHandled = false;
    this.hasUnhandled = true;
  }

  /**
   * @see DeLiCluEntry#hasHandled()
   */
  public boolean hasHandled() {
    return hasHandled;
  }

  /**
   * @see DeLiCluEntry#hasUnhandled()
   */
  public boolean hasUnhandled() {
    return hasUnhandled;
  }

  /**
   * @see DeLiCluEntry#setHasHandled(boolean)
   */
  public void setHasHandled(boolean hasHandled) {
    this.hasHandled = hasHandled;
  }

  /**
   * @see DeLiCluEntry#setHasUnhandled(boolean)
   */
  public void setHasUnhandled(boolean hasUnhandled) {
    this.hasUnhandled = hasUnhandled;
  }


  /**
   * Returns the id as a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  public String toString() {
    return super.toString() + "[" + hasHandled + "-" + hasUnhandled + "]";
  }
}
