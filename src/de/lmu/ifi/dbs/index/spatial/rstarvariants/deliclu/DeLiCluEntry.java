package de.lmu.ifi.dbs.index.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.index.spatial.SpatialEntry;

/**
 * Defines the requirements for an entry in an DeLiClu-Tree node.
 * Additionally to an entry in an R*-Tree two boolean flags that indicate wether this entry's node
 * contains handled or unhandled data objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface DeLiCluEntry extends SpatialEntry {
  /**
   * Returns true, if the node (or its child nodes) which is represented by this entry
   * contains handled data objects.
   *
   * @return true, if the node (or its child nodes) which is represented by this entry
   *         contains handled data objects,
   *         false otherwise.
   */
  public boolean hasHandled();

  /**
   * Returns true, if the node (or its child nodes) which is represented by this entry
   * contains unhandled data objects.
   *
   * @return true, if the node (or its child nodes) which is represented by this entry
   *         contains unhandled data objects,
   *         false otherwise.
   */
  public boolean hasUnhandled();

  /**
   * Sets the flag to marks the node (or its child nodes) which is represented by this entry
   * to contain handled data objects.
   *
   * @param hasHandled the flag to be set
   */
  public void setHasHandled(boolean hasHandled);

  /**
   * Sets the flag to marks the node (or its child nodes) which is represented by this entry
   * to contain unhandled data objects.
   *
   * @param hasUnhandled the flag to be set
   */
  public void setHasUnhandled(boolean hasUnhandled);

}
