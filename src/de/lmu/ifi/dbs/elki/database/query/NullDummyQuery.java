package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * A dummy query that always returns "null".
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public class NullDummyQuery<O> implements DataQuery<O> {
  /**
   * The object type we claim to have.
   */
  private Class<? super O> type;
  
  /**
   * Constructor.
   * 
   * @param type Object type to claim to contain.
   */
  public NullDummyQuery(Class<? super O> type) {
    super();
    this.type = type;
  }

  @SuppressWarnings("unused")
  @Override
  public O get(DBID id) {
    return null;
  }

  @SuppressWarnings("unused")
  @Override
  public void set(DBID id, O val) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<? super O> getDataClass() {
    return type;
  }
}
