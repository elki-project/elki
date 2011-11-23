package de.lmu.ifi.dbs.elki.database.ids.integer;

import gnu.trove.list.TIntList;
import de.lmu.ifi.dbs.elki.database.ids.ArrayStaticDBIDs;

/**
 * Class accessing a trove int array.
 * 
 * @author Erich Schubert
 */
class TroveArrayStaticDBIDs extends TroveArrayDBIDs implements ArrayStaticDBIDs {
  /**
   * Actual trove store
   */
  private final TIntList store;

  /**
   * Constructor.
   * 
   * @param store Actual trove store.
   */
  protected TroveArrayStaticDBIDs(TIntList store) {
    super();
    this.store = store;
  }

  @Override
  protected TIntList getStore() {
    return store;
  }
}
