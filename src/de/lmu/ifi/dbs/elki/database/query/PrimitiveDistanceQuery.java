package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public class PrimitiveDistanceQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceQuery<O, D> {
  /**
   * The distance function we use.
   */
  final protected PrimitiveDistanceFunction<? super O, D> distanceFunction;
  
  /**
   * Constructor.
   * 
   * @param database Database to use.
   * @param distanceFunction Our distance function
   */
  public PrimitiveDistanceQuery(Database<? extends O> database, PrimitiveDistanceFunction<? super O, D> distanceFunction) {
    super(database);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public D distance(DBID id1, DBID id2) {
    O o1 = database.get(id1);
    O o2 = database.get(id2);
    return distance(o1, o2);
  }

  @Override
  public D distance(O o1, DBID id2) {
    O o2 = database.get(id2);
    return distance(o1, o2);
  }

  @Override
  public D distance(DBID id1, O o2) {
    O o1 = database.get(id1);
    return distance(o1, o2);
  }

  @Override
  public D distance(O o1, O o2) {
    if (o1 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    if (o2 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    return distanceFunction.distance(o1, o2);
  }

  @Override
  public PrimitiveDistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }
}