package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Node;

/**
 * Defines the requirements for an object that can be used as a node in a
 * Spatial Index. A spatial node can be a spatial directory node or a spatial
 * leaf node.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.composedOf SpatialEntry
 * 
 * @param <N> Self reference
 * @param <E> Entry type
 */
public interface SpatialNode<N extends SpatialNode<N, E>, E extends SpatialEntry> extends Node<E>, SpatialComparable {
  // No additional methods.
}
