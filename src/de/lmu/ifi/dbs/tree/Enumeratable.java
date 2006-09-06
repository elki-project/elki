package de.lmu.ifi.dbs.tree;

import java.util.Enumeration;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Enumeratable<E extends Enumeratable> {
  int numChildren();
  E getChild(int i);
}
