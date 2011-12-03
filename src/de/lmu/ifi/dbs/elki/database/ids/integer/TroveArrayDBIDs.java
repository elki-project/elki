package de.lmu.ifi.dbs.elki.database.ids.integer;

import gnu.trove.list.TIntList;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;

/**
 * Abstract base class for GNU Trove array based lists.
 * 
 * @author Erich Schubert
 */
public abstract class TroveArrayDBIDs extends AbstractList<DBID> implements ArrayDBIDs {
  /**
   * Get the array store
   * 
   * @return the store
   */
  abstract protected TIntList getStore();

  @Override
  public Iterator<DBID> iterator() {
    return new TroveIteratorAdapter(getStore().iterator());
  }

  @Override
  public DBIDIter iter() {
    return new DBIDItr(getStore());
  }

  @Override
  public DBID get(int index) {
    return new IntegerDBID(getStore().get(index));
  }

  @Override
  public int size() {
    return getStore().size();
  }

  @Override
  public Collection<DBID> asCollection() {
    return this;
  }

  @Override
  public int indexOf(Object o) {
    return getStore().indexOf(((DBID) o).getIntegerID());
  }

  @Override
  public int lastIndexOf(Object o) {
    return getStore().lastIndexOf(((DBID) o).getIntegerID());
  }

  @Override
  public boolean contains(Object o) {
    if(o instanceof DBID) {
      return getStore().contains(((DBID) o).getIntegerID());
    }
    return false;
  }

  @Override
  public List<DBID> subList(int fromIndex, int toIndex) {
    return new TroveArrayStaticDBIDs(getStore().subList(fromIndex, toIndex));
  }

  /**
   * Iterate over a Trove IntList, ELKI/C-style
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class DBIDItr implements DBIDIter {
    /**
     * Current position
     */
    int pos = 0;

    /**
     * The actual store we use
     */
    TIntList store;
    
    /**
     * Constructor.
     *
     * @param store The actual trove store
     */
    public DBIDItr(TIntList store) {
      super();
      this.store = store;
    }

    @Override
    public boolean valid() {
      return pos < store.size();
    }

    @Override
    public void advance() {
      pos++;
    }

    @Override
    public int getIntegerID() {
      return store.get(pos);
    }

    @Override
    public DBID getDBID() {
      return new IntegerDBID(store.get(pos));
    }

  }
}