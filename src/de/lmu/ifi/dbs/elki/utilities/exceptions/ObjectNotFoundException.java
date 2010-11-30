package de.lmu.ifi.dbs.elki.utilities.exceptions;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Exception thrown when the requested object was not found in the database.
 * 
 * @author Erich Schubert
 */
public class ObjectNotFoundException extends RuntimeException {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Object
   */
  public ObjectNotFoundException(DBID id) {
    super("Object "+id+" was not found in the database.");
  }
}
