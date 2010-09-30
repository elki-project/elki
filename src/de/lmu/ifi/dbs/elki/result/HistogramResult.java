package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * Histogram result.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class (e.g. {@link de.lmu.ifi.dbs.elki.data.DoubleVector})
 */
public class HistogramResult<O extends DatabaseObject> extends CollectionResult<O> {
  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection
   */
  public HistogramResult(String name, String shortname, Collection<O> col) {
    super(name, shortname, col);
  }

  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection
   * @param header Header information
   */
  public HistogramResult(String name, String shortname, Collection<O> col, Collection<String> header) {
    super(name, shortname, col, header);
  }
}