package experimentalcode.erich.newdblayer;

import java.util.Collection;
import java.util.Iterator;

/**
 * Static (no modifications allowed) set of Database Object IDs.
 * 
 * @author Erich Schubert
 * 
 */
public class StaticDBIDs implements DBIDs, Collection<DBID> {
  /**
   * The actual storage.
   */
  private int[] ids;

  @Override
  public Collection<DBID> asCollection() {
    return this;
  }

  @Override
  public Iterator<DBID> iterator() {
    return null;
  }

  @Override
  public int size() {
    return ids.length;
  }
  
  /*
   * "Contains" operations
   */
  @Override
  public boolean contains(Object o) {
    if(o instanceof DBID) {
      int oid = ((DBID) o).getIntegerID();
      for(int i = 0; i < ids.length; i++) {
        if(ids[i] == oid) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    Iterator<?> e = c.iterator();
    while(e.hasNext()) {
      if(!contains(e.next())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    return ids.length == 0;
  }

  /*
   * To Array operations
   */
  @Override
  public Object[] toArray() {
    return toArray(new Object[ids.length]);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    T[] r = a;
    if(a.length < ids.length) {
      r = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), ids.length);
    }
    for(int i = 0; i < ids.length; i++) {
      r[i] = (T) new DBID(ids[i]);
    }
    // zero-terminate array
    if(r.length > ids.length) {
      r[ids.length] = null;
    }
    return r;
  }

  /*
   * Unsupported operations (modifications are not allowed)
   */
  @Override
  public boolean add(@SuppressWarnings("unused") DBID e) {
    throw new UnsupportedOperationException(StaticDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean addAll(@SuppressWarnings("unused") Collection<? extends DBID> c) {
    throw new UnsupportedOperationException(StaticDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean remove(@SuppressWarnings("unused") Object o) {
    throw new UnsupportedOperationException(StaticDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean removeAll(@SuppressWarnings("unused") Collection<?> c) {
    throw new UnsupportedOperationException(StaticDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean retainAll(@SuppressWarnings("unused") Collection<?> c) {
    throw new UnsupportedOperationException(StaticDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException(StaticDBIDs.class.getName() + " are unmodifiable!");
  }
}
