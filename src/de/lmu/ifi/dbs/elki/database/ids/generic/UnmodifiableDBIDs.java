package de.lmu.ifi.dbs.elki.database.ids.generic;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.collections.iterators.UnmodifiableIterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Unmodifiable wrapper for DBIDs.
 * 
 * @author Erich Schubert
 */
public class UnmodifiableDBIDs implements DBIDs {
  /**
   * The DBIDs we wrap.
   */
  final private DBIDs inner;
  
  /**
   * Constructor.
   * 
   * @param inner Inner DBID collection.
   */
  public UnmodifiableDBIDs(DBIDs inner) {
    super();
    this.inner = inner;
  }

  @Override
  public Collection<DBID> asCollection() {
    return Collections.unmodifiableCollection(inner.asCollection());
  }

  @Override
  public boolean contains(Object o) {
    return inner.contains(o);
  }

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<DBID> iterator() {
    return UnmodifiableIterator.decorate(inner.iterator());
  }

  @Override
  public int size() {
    return inner.size();
  }
}
