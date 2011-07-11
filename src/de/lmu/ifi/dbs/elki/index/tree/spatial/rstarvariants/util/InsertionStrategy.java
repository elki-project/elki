package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.index.tree.Node;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for implementing insertion strategies, i.e. in which path of the
 * tree to insert the new element.
 * 
 * @author Erich Schubert
 */
public interface InsertionStrategy extends Parameterizable {
  /**
   * Find the child to insert into.
   * 
   * @param <N> Node type
   * @param <E> Entry type
   * @param node Node to use
   * @param mbr MBR of entry to insert
   * @return Entry to insert into
   */
  public <N extends Node<E>, E extends SpatialEntry> TreeIndexPathComponent<E> findInsertChild(N node, SpatialComparable mbr);
}
