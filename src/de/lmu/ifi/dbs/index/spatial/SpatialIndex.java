package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.index.Index;

import java.util.List;

/**
 * Defines the requirements for a spatial index that can be used to efficiently store data.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialIndex<O extends RealVector> extends Index<O> {
  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   *
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  List<DirectoryEntry> getLeaves();

  /**
   * Returns the spatial node with the specified ID.
   *
   * @param nodeID the id of the node to be returned
   * @return the spatial node with the specified ID
   */
  SpatialNode getNode(int nodeID);

  /**
   * Returns the entry that denotes the root.
   * @return the entry that denotes the root
   */
  DirectoryEntry getRootEntry();
}
