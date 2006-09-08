package de.lmu.ifi.dbs.utilities;

/**
 * Defines the requirements for an enumeratable object, i.e. an object which allows to
 * enumerate over its children (e.g. a node in a tree).
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Enumeratable<E extends Enumeratable> {
  /**
   * Returns the number of children.
   * @return the number of children
   */
  int numChildren();

  /**
   * Returns the child at the specified index.
   * @param i the index of the child to be returned
   * @return  the child at the specified index
   */
  E getChild(int i);
}
