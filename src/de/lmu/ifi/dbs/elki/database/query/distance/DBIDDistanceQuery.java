package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DBIDDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a distance query based on DBIDs
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DBIDDistanceFunction
 * 
 * @param <D> Distance result type.
 */
public class DBIDDistanceQuery<D extends Distance<D>> extends AbstractDatabaseDistanceQuery<DBID, D> {
  /**
   * The distance function we use.
   */
  final protected DBIDDistanceFunction<D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param rep Database to use.
   * @param distanceFunction Our distance function
   */
  public DBIDDistanceQuery(Relation<DBID> rep, DBIDDistanceFunction<D> distanceFunction) {
    super(rep);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public D distance(DBID id1, DBID id2) {
    if(id1 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for objects stored in the database.");
    }
    if(id2 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for objects stored in the database.");
    }
    return distanceFunction.distance(id1, id2);
  }

  @Override
  public DBIDDistanceFunction<D> getDistanceFunction() {
    return distanceFunction;
  }
}