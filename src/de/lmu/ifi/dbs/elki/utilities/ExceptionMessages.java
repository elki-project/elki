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
  /**
   * Message when a new label was discovered in a database, that did not exist before.
   */
  public static final String INCONSISTENT_STATE_NEW_LABEL = "inconsistent state of database - found new label";
  /**
   * Message when an empty clustering is encountered.
   */
  public static final String CLUSTERING_EMPTY = "Clustering doesn't contain any cluster.";
  /**
   * Message when a distance doesn't support undefined values.
   */
  public static final String UNSUPPORTED_UNDEFINED_DISTANCE = "Undefinded distance not supported!";
  /**
   * Generic "unsupported" message
   */
  public static final String UNSUPPORTED = "Unsupported.";
  /**
   * Generic "not yet supported" message
   */
  public static final String UNSUPPORTED_NOT_YET = "Not yet supported.";
  /**
   * "remove unsupported" message for iterators
   */
  public static final String UNSUPPORTED_REMOVE = "remove() unsupported";
  /**
   * File not found. 404.
   */
  public static final String FILE_NOT_FOUND = "File not found";
  /**
   * File already exists, will not overwrite.
   */
  public static final String FILE_EXISTS = "File already exists";
}
