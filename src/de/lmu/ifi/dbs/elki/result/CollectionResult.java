package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Simple 'collection' type of result.
 * For example, a list of NumberVectors.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 *
 * @param <O> data type
 */
public class CollectionResult<O> extends BasicResult implements IterableResult<O> {
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
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection represented
   * @param header Auxiliary information for result headers
   */
  public CollectionResult(String name, String shortname, Collection<O> col, Collection<String> header) {
    super(name, shortname);
    this.col = col;
    this.header = header;
  }
  
  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection represented
   */
  public CollectionResult(String name, String shortname, Collection<O> col) {
    this(name, shortname, col, new ArrayList<String>());
  }
  
  /**
   * Add header information
   * 
   * @param s Header information string
   */
  public void addHeader(String s) {
    header.add(s);
  }
  
  /**
   * Get header information
   * 
   * @return header information of the result
   */
  public Collection<String> getHeader() {
    return header;
  }

  /**
   * Implementation of the {@link IterableResult} interface, using the backing collection.
   */
  @Override
  public Iterator<O> iterator() {
    return col.iterator();
  }
  
  /**
   * Get the collection size.
   * 
   * @return Collection size
   */
  public int size() {
    return col.size();
  }
}