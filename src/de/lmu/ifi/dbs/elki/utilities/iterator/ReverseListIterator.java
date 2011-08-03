package de.lmu.ifi.dbs.elki.utilities.iterator;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Reverse iterator for lists.
 * 
 * @author Erich Schubert
 * 
 * @param <E> Element type
 */
public class ReverseListIterator<E> implements Iterator<E>, ListIterator<E> {
  /**
   * The actual iterator
   */
  final ListIterator<E> iter;

  /**
   * Constructor.
   * 
   * @param iter List iterator
   */
  public ReverseListIterator(ListIterator<E> iter) {
    this.iter = iter;
  }

  /**
   * Constructor.
   * 
   * @param list Existing list
   */
  public ReverseListIterator(List<E> list) {
    this.iter = list.listIterator(list.size());
  }

  @Override
  public boolean hasNext() {
    return iter.hasPrevious();
  }

  @Override
  public E next() {
    return iter.previous();
  }

  @Override
  public void remove() {
    iter.remove();
  }

  @Override
  public boolean hasPrevious() {
    return iter.hasNext();
  }

  @Override
  public E previous() {
    return iter.next();
  }

  @Override
  public int nextIndex() {
    return iter.previousIndex();
  }

  @Override
  public int previousIndex() {
    return iter.nextIndex();
  }

  @Override
  public void set(E e) {
    iter.set(e);
  }

  @Override
  public void add(E e) {
    iter.add(e);
  }
}