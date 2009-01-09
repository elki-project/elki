package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;
import java.util.Iterator;

/**
 * Simple 'collection' type of result.
 * For example, a list of RealVectors.
 * 
 * @author Erich Schubert
 *
 * @param <O> data type
 */
public class CollectionResult<O> implements IterableResult<O> {
  /**
   * The collection represented.
   */
  private Collection<O> col;
  
  /**
   * Constructor
   * 
   * @param col Collection represented
   */
  public CollectionResult(Collection<O> col) {
    this.col = col;
  }

  /**
   * Implementation of the {@link IterableResult} interface, using the backing collection.
   */
  @Override
  public Iterator<O> iter() {
    return col.iterator();
  }
}