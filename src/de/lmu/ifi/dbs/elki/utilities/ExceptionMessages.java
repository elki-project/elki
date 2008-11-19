package de.lmu.ifi.dbs.elki.utilities;

/**
 * Interface to collect exception messages that are used in several cases.
 * 
 * @author Arthur Zimek
 */
public interface ExceptionMessages {
  
  /**
   * Messages in case a database is unexpectedly empty.
   */
  public static final String DATABASE_EMPTY = "database empty: must contain elements";
}
