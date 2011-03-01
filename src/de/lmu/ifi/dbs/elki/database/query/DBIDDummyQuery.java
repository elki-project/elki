package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Pseudo data query that returns the DBID.
 * 
 * This is the identity map; however this can serve as a default class
 * when ID mappings are needed somewhere.
 * 
 * @author Erich Schubert
 */
public class DBIDDummyQuery implements DataQuery<DBID> {
  /**
   * Constructor.
   */
  public DBIDDummyQuery() {
    super();
  }

  @Override
  public DBID get(DBID id) {
    return id;
  }

  @SuppressWarnings("unused")
  @Override
  public void set(DBID id, DBID val) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<? super DBID> getDataClass() {
    return DBID.class;
  }
}
