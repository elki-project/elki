package de.lmu.ifi.dbs.elki.utilities;

/**
 * Defines the requirements for an enumeratable object, i.e. an object which allows to
 * enumerate over its children (e.g. a node in a tree).
 *
 * @author Elke Achtert 
 */
public interface Enumeratable<E extends Enumeratable<E>> {
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
