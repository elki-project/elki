package experimentalcode.erich.newdblayer.ids;

import java.util.Collection;
import java.util.Iterator;

/**
 * Merge the IDs of multiple layers into one.
 * 
 * @author Erich Schubert
 */
// TODO: include ID mapping?
public class MergedDBIDs implements DBIDs, Collection<DBID> {
  /**
   * Childs to merge
   */
  DBIDs childs[];
  
  /**
   * Constructor.
   * 
   * @param childs
   */
  public MergedDBIDs(DBIDs... childs) {
    super();
    this.childs = childs;
  }

  @Override
  public Collection<DBID> asCollection() {
    return this;
  }

  @Override
  public Iterator<DBID> iterator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int size() {
    int si = 0;
    for(DBIDs child : childs) {
      si += child.size();
    }
    return si;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Object[] toArray() {
    return toArray(new Object[size()]);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    final int si = size();
    T[] r = a;
    if(a.length < si) {
      r = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), si);
    }
    int i = 0;
    for (Iterator<DBID> iter = iterator(); iter.hasNext(); i++) {
      DBID id = iter.next();
      r[i] = (T) id;
    }
    // zero-terminate array
    if(r.length > si) {
      r[si] = null;
    }
    return r;
  }

  @Override
  public boolean contains(Object o) {
    for(DBIDs child : childs) {
      if(child.contains(o)) {
        return true;
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
  public void clear() {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean add(@SuppressWarnings("unused") DBID e) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean addAll(@SuppressWarnings("unused") Collection<? extends DBID> c) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean remove(@SuppressWarnings("unused") Object o) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean removeAll(@SuppressWarnings("unused") Collection<?> c) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }

  @Override
  public boolean retainAll(@SuppressWarnings("unused") Collection<?> c) {
    throw new UnsupportedOperationException(MergedDBIDs.class.getName() + " are unmodifiable!");
  }
}
