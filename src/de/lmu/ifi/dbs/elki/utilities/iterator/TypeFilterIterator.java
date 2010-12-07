package de.lmu.ifi.dbs.elki.utilities.iterator;

import java.util.Iterator;

/**
 * Iterator that filters results by type.
 * 
 * @author Erich Schubert
 *
 * @param <IN> Input datatype
 * @param <OUT> Output datatype
 */
public class TypeFilterIterator<IN, OUT extends IN> extends AbstractFilteredIterator<IN, OUT> {
  /**
   * Class restriction
   */
  private Class<? super OUT> filterClass;

  /**
   * Parent iterator
   */
  private Iterator<IN> parent;

  /**
   * Constructor.
   * 
   * @param filterClass Filter
   * @param parent Parent collection
   */
  public TypeFilterIterator(Class<? super OUT> filterClass, Iterable<IN> parent) {
    super();
    this.filterClass = filterClass;
    this.parent = parent.iterator();
  }

  /**
   * Constructor.
   * 
   * @param filterClass Filter
   * @param parent Parent iterator
   */
  public TypeFilterIterator(Class<? super OUT> filterClass, Iterator<IN> parent) {
    super();
    this.filterClass = filterClass;
    this.parent = parent;
  }

  @Override
  protected Iterator<IN> getParentIterator() {
    return parent;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected OUT testFilter(IN nextobj) {
    try {
      return (OUT) filterClass.cast(nextobj);
    }
    catch(ClassCastException e) {
      return null;
    }
  }
}
