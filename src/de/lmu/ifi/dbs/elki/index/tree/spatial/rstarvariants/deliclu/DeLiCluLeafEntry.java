package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;

/**
 * Defines the requirements for a leaf entry in an DeLiClu-Tree node.
 * Additionally to a leaf entry in an R*-Tree two boolean flags that indicate whether this entry's node
 * contains handled or unhandled data objects.
 *
 * @author Elke Achtert 
 */
public class DeLiCluLeafEntry extends SpatialPointLeafEntry implements DeLiCluEntry {
  private static final long serialVersionUID = 1;

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
  public DeLiCluLeafEntry(DBID id, NumberVector<?,?> vector) {
    super(id, vector);
    this.hasHandled = false;
    this.hasUnhandled = true;
  }

  @Override
  public boolean hasHandled() {
    return hasHandled;
  }

  @Override
  public boolean hasUnhandled() {
    return hasUnhandled;
  }

  @Override
  public void setHasHandled(boolean hasHandled) {
    this.hasHandled = hasHandled;
  }

  @Override
  public void setHasUnhandled(boolean hasUnhandled) {
    this.hasUnhandled = hasUnhandled;
  }

  /**
   * Returns the id as a string representation of this entry.
   *
   * @return a string representation of this entry
   */
  @Override
  public String toString() {
    return super.toString() + "[" + hasHandled + "-" + hasUnhandled + "]";
  }
}
