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
   * @param col Collection
   */
  public HistogramResult(Collection<O> col) {
    super(col);
  }

  /**
   * Constructor
   * 
   * @param col Collection
   * @param header Header information
   */
  public HistogramResult(Collection<O> col, Collection<String> header) {
    super(col, header);
  }

  @Override
  public String getName() {
    return "histogram";
  }
}
