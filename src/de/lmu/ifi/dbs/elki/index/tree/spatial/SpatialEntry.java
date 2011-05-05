package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Entry;

/**
 * Defines the requirements for an entry in a node of a Spatial Index.
 * 
 * @author Elke Achtert
 */
public interface SpatialEntry extends Entry, SpatialComparable {
  // Emtpy - just combining the two interfaces above.
}