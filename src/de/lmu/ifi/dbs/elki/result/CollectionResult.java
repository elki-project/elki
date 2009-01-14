package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
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
   * Meta information (printed into the header)
   */
  private Collection<String> header;
  
  /**
   * Constructor
   * 
   * @param col Collection represented
   */
  public CollectionResult(Collection<O> col, Collection<String> header) {
    this.col = col;
    this.header = header;
  }
  
  /**
   * Constructor
   * 
   * @param col Collection represented
   */
  public CollectionResult(Collection<O> col) {
    this(col, new ArrayList<String>());
  }
  
  /**
   * Add header information
   * 
   * @param header Header information string
   */
  public void addHeader(String s) {
    header.add(s);
  }
  
  /**
   * Get header information
   */
  public Collection<String> getHeader() {
    return header;
  }

  /**
   * Implementation of the {@link IterableResult} interface, using the backing collection.
   */
  @Override
  public Iterator<O> iter() {
    return col.iterator();
  }
}