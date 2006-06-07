package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.persistent.Page;

import java.util.Enumeration;

/**
 * This interface defines the common requirements nodes in an index structure.
 * A node has to extend the page interface for persistent storage and
 * has to provide an enumeration over its children.
 *
 * @see de.lmu.ifi.dbs.persistent.Page
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Node extends Page {
  /**
   * Returns an enumeration of the children paths of this node.
   *
   * @param parentPath the path to this node
   * @return an enumeration of the children paths of this node
   */
  Enumeration<IndexPath> children(IndexPath parentPath);
}
