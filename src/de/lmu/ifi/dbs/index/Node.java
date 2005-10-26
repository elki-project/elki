package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.persistent.Page;

import java.util.Enumeration;

/**
 * This interface is used to enumerate over the nodes in an index structure.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Node extends Page {
  /**
   * Returns an enumeration of the children of this node.
   *
   * @return an enumeration of the children of this node
   */
  Enumeration<Identifier> children();
}
