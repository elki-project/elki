package experimentalcode.erich.newdblayer.storage.memory;

import java.util.Iterator;

import experimentalcode.erich.newdblayer.DBID;

/**
 * Static (no modifications allowed) set of Database Object IDs.
 * 
 * @author Erich Schubert
 * 
 */
public class ArrayStaticDBIDs extends StaticDBIDs {
  /**
   * The actual storage.
   */
  protected int[] ids;

  /**
   * Constructor
   * 
   * @param ids Array of ids.
   */
  public ArrayStaticDBIDs(int[] ids) {
    super();
    this.ids = ids;
  }

  @Override
  public Iterator<DBID> iterator() {
    return new Itr();
  }
  
  /**
   * Iterator class.
   * 
   * @author Erich Schubert
   */
  protected class Itr implements Iterator<DBID> {
    int off = 0;

    @Override
    public boolean hasNext() {
      return off < ids.length;
    }

    @Override
    public DBID next() {
      DBID ret = new DBID(ids[off]);
      off++;
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("ArrayStaticDBIDs is read-only.");
    }
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
}
