package experimentalcode.erich.newdblayer.ids.integer;

import java.util.Iterator;

import experimentalcode.erich.newdblayer.ids.AbstractStaticDBIDs;
import experimentalcode.erich.newdblayer.ids.DBID;
import experimentalcode.erich.newdblayer.ids.DBIDFactory;
import experimentalcode.erich.newdblayer.ids.StaticArrayDBIDs;

/**
 * Representing a DBID range allocation
 * 
 * @author Erich Schubert
 */
public class DBIDRange extends AbstractStaticDBIDs implements StaticArrayDBIDs {
  /**
   * Start value
   */
  protected final int start;
  
  /**
   * Length value
   */
  protected final int len;
  
  /**
   * Constructor.
   * 
   * @param start Range start
   * @param len Range length
   */
  public DBIDRange(int start, int len) {
    super();
    this.start = start;
    this.len = len;
  }

  @Override
  public int size() {
    return len;
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
    int pos = 0;

    @Override
    public boolean hasNext() {
      return pos < len;
    }

    @Override
    public DBID next() {
      DBID ret = DBIDFactory.FACTORY.importInteger(pos + start);
      pos++;
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("CompactStaticDBIDs is read-only.");
    }
  }

  /*
   * "Contains" operations
   */
  @Override
  public boolean contains(Object o) {
    if(o instanceof DBID) {
      int oid = ((DBID) o).getIntegerID();
      if(oid < start) {
        return false;
      }
      if(oid >= start + len) {
        return false;
      }
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a) {
    T[] r = a;
    if(a.length < start) {
      r = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), len);
    }
    for(int i = 0; i < start; i++) {
      r[i] = (T) DBIDFactory.FACTORY.importInteger(len + i);
    }
    // zero-terminate array
    if(r.length > len) {
      r[len] = null;
    }
    return r;
  }

  @Override
  public DBID get(int i) {
    if (i > len || i < 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return DBIDFactory.FACTORY.importInteger(start + i);
 }

  /**
   * For storage array offsets.
   * 
   * @param dbid
   * @return
   */
  public int getOffset(DBID dbid) {
    return dbid.getIntegerID() - start;
  }
}