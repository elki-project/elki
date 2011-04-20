package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public abstract class AbstractDatabaseDistanceQuery<O, D extends Distance<D>> extends AbstractDistanceQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param relation Relation to use.
   */
  public AbstractDatabaseDistanceQuery(Relation<? extends O> relation) {
    super(relation);
  }

  @Override
  public D distance(O o1, DBID id2) {
    if(o1 instanceof DBID) {
      return distance((DBID) o1, id2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

  @Override
  public D distance(DBID id1, O o2) {
    if(o2 instanceof DBID) {
      return distance(id1, (DBID) o2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

  @Override
  public D distance(O o1, O o2) {
    if(o1 instanceof DBID && o2 instanceof DBID) {
      return distance((DBID) o1, (DBID) o2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

}