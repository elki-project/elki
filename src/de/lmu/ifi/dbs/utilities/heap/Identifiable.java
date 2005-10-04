package de.lmu.ifi.dbs.utilities.heap;

/**
 * TODO: comment
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
