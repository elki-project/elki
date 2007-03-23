package de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.utilities.HyperBoundingBox;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialDirectoryEntry;

/**
 * Defines the requirements for a directory entry in an DeLiClu-Tree node.
 * Additionally to a directory entry in an R*-Tree two boolean flags that indicate wether this entry's node
 * contains handled or unhandled data objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DeLiCluDirectoryEntry extends SpatialDirectoryEntry implements DeLiCluEntry {
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
  }

  /**
   * Constructs a new DeLiCluDirectoryEntry object with the given parameters.
   *
   * @param id  the unique id of the underlying spatial node
   * @param mbr the minmum bounding rectangle of the underlying spatial node
   * @param hasHandled indicates if this entry has handled nodes
   * @param hasUnhandled indicates if this entry has unhandled nodes
   */
  public DeLiCluDirectoryEntry(int id, HyperBoundingBox mbr, boolean hasHandled, boolean hasUnhandled) {
    super(id, mbr);
    this.hasHandled = hasHandled;
    this.hasUnhandled = hasUnhandled;
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
