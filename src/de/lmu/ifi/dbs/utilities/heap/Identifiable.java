package de.lmu.ifi.dbs.utilities.heap;

/**
 * Defines the requiremnts for objects that are identifiable, i.e. objects which have an unique id.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Identifiable extends Comparable<Identifiable> {
  /**
   * Returns the unique id of this object.
   *
   * @return the unique id of this object
   */
  Integer getID();
}
