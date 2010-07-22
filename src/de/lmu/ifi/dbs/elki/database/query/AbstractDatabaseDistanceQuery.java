package de.lmu.ifi.dbs.elki.database.query;

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
public abstract class AbstractDatabaseDistanceQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceQuery<O, D> implements Cloneable {
  /**
   * Constructor.
   * 
   * @param database Database to use.
   * @param distanceFunction Our distance function
   */
  public AbstractDatabaseDistanceQuery(Database<? extends O> database) {
    super(database);
  }

  @Override
  public D distance(O o1, DBID id2) {
    DBID id1 = o1.getID();
    return distance(id1, id2);
  }

  @Override
  public D distance(DBID id1, O o2) {
    DBID id2 = o2.getID();
    return distance(id1, id2);
  }

  @Override
  public D distance(O o1, O o2) {
    DBID id1 = o1.getID();
    DBID id2 = o2.getID();
    return distance(id1, id2);
  }
}