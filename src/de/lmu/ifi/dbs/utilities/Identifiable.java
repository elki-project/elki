package de.lmu.ifi.dbs.utilities;

/**
 * Defines the requiremnts for objects that are identifiable, i.e. objects which have an unique id.
 *
 * @author Elke Achtert 
 */
public interface Identifiable<O> extends Comparable<Identifiable<O>> {
  /**
   * Returns the unique id of this object.
   *
   * @return the unique id of this object
   */
  Integer getID();
}
