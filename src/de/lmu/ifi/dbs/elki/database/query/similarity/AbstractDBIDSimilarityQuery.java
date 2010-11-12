package de.lmu.ifi.dbs.elki.database.query.similarity;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public abstract class AbstractDBIDSimilarityQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractSimilarityQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param database Database to use.
   */
  public AbstractDBIDSimilarityQuery(Database<? extends O> database) {
    super(database);
  }

  @Override
  public D similarity(O o1, DBID id2) {
    DBID id1 = o1.getID();
    return similarity(id1, id2);
  }

  @Override
  public D similarity(DBID id1, O o2) {
    DBID id2 = o2.getID();
    return similarity(id1, id2);
  }

  @Override
  public D similarity(O o1, O o2) {
    DBID id1 = o1.getID();
    DBID id2 = o2.getID();
    return similarity(id1, id2);
  }
}