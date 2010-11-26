package de.lmu.ifi.dbs.elki.database.ids;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.utilities.datastructures.EmptyIterator;

/**
 * Empty DBID collection.
 * 
 * @author Erich Schubert
 */
class EmptyDBIDs extends AbstractList<DBID> implements ArrayStaticDBIDs {
  @Override
  public Collection<DBID> asCollection() {
    return new ArrayList<DBID>(0);
  }

  @Override
  public boolean contains(@SuppressWarnings("unused") Object o) {
    return false;
  }

  @Override
  public Iterator<DBID> iterator() {
    return EmptyIterator.STATIC();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public DBID get(@SuppressWarnings("unused") int i) {
    throw new ArrayIndexOutOfBoundsException();
  }
}