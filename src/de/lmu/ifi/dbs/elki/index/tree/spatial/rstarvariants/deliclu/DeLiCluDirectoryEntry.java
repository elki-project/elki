package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

/**
 * Defines the requirements for a directory entry in an DeLiClu-Tree node.
 * Additionally to a directory entry in an R*-Tree two boolean flags that indicate whether this entry's node
 * contains handled or unhandled data objects.
 *
 * @author Elke Achtert 
 */
public class DeLiCluDirectoryEntry extends SpatialDirectoryEntry implements DeLiCluEntry {
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
  public DeLiCluDirectoryEntry() {
	  // empty constructor
  }

  /**
   * Constructs a new DeLiCluDirectoryEntry object with the given parameters.
   *
   * @param id  the unique id of the underlying spatial node
   * @param mbr the minimum bounding rectangle of the underlying spatial node
   * @param hasHandled indicates if this entry has handled nodes
   * @param hasUnhandled indicates if this entry has unhandled nodes
   */
  public DeLiCluDirectoryEntry(int id, HyperBoundingBox mbr, boolean hasHandled, boolean hasUnhandled) {
    super(id, mbr);
    this.hasHandled = hasHandled;
    this.hasUnhandled = hasUnhandled;
  }

  public boolean hasHandled() {
    return hasHandled;
  }

  public boolean hasUnhandled() {
    return hasUnhandled;
  }

  public void setHasHandled(boolean hasHandled) {
    this.hasHandled = hasHandled;
  }

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
