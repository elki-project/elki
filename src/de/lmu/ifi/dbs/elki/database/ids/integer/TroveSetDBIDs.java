package de.lmu.ifi.dbs.elki.database.ids.integer;

import gnu.trove.set.TIntSet;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;

/**
 * Abstract base class for GNU Trove array based lists.
 * 
 * @author Erich Schubert
 */
public abstract class TroveSetDBIDs extends AbstractSet<DBID> implements SetDBIDs {
  /**
   * Get the array store
   * 
   * @return the store
   */
  abstract protected TIntSet getStore();

  @Override
  public Iterator<DBID> iterator() {
    return new TroveIteratorAdapter(getStore().iterator());
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
  public boolean contains(Object o) {
    if(o instanceof DBID) {
      return getStore().contains(((DBID) o).getIntegerID());
    }
    return false;
  }
}