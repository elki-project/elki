package de.lmu.ifi.dbs.elki.database.ids.integer;

import gnu.trove.list.array.TIntArrayList;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Class using a GNU Trove int array list as storage.
 * 
 * @author Erich Schubert
 */
class TroveArrayModifiableDBIDs extends TroveArrayDBIDs implements ArrayModifiableDBIDs {
  /**
   * The actual trove array list
   */
  private TIntArrayList store;

  /**
   * Constructor.
   * 
   * @param size Initial size
   */
  protected TroveArrayModifiableDBIDs(int size) {
    super();
    this.store = new TIntArrayList(size);
  }

  /**
   * Constructor.
   */
  protected TroveArrayModifiableDBIDs() {
    super();
    this.store = new TIntArrayList();
  }

  /**
   * Constructor.
   *
   * @param existing Existing ids
   */
  protected TroveArrayModifiableDBIDs(DBIDs existing) {
    this(existing.size());
    this.addDBIDs(existing);
  }

  @Override
  protected TIntArrayList getStore() {
    return store;
  }

  @Override
  public boolean addDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      success |= store.add(iter.getIntegerID());
    }
    return success;
  }

  @Override
  public boolean removeDBIDs(DBIDs ids) {
    boolean success = false;
    for(DBID id : ids) {
      success |= store.remove(id.getIntegerID());
    }
    return success;
  }

  @Override
  public boolean add(DBID e) {
    return store.add(e.getIntegerID());
  }

  @Override
  public boolean remove(Object o) {
    return store.remove(((DBID) o).getIntegerID());
  }

  @Override
  public DBID set(int index, DBID element) {
    int prev = store.set(index, element.getIntegerID());
    return new IntegerDBID(prev);
  }

  @Override
  public void add(int index, DBID element) {
    store.insert(index, element.getIntegerID());
  }

  @Override
  public DBID remove(int index) {
    return new IntegerDBID(store.removeAt(index));
  }

  @Override
  public void sort() {
    store.sort();
  }
}