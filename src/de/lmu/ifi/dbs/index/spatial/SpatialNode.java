package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.index.Node;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

/**
 * Defines the requirements for an object that can be used as a node in a Spatial Index.
 * A spatial node can be a spatial directory node or a spatial leaf node.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialNode<N extends SpatialNode<N,E>, E extends SpatialEntry> extends Node<N,E>, SpatialObject {
  /**
   * Returns the dimensionality of this spatial object.
   *
   * @return the dimensionality of this spatial object
   */
  int getDimensionality();

  /**
   * Computes and returns the MBR of this spatial object.
   *
   * @return the MBR of this spatial object
   */
  HyperBoundingBox mbr();
}
