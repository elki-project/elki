package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses PrimitiveDistanceFunction
 * 
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public class PrimitiveDistanceQuery<O, D extends Distance<D>> extends AbstractDistanceQuery<O, D> {
  /**
   * The distance function we use.
   */
  final protected PrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param rep Representation to use.
   * @param distanceFunction Our distance function
   */
  public PrimitiveDistanceQuery(Relation<? extends O> rep, PrimitiveDistanceFunction<? super O, D> distanceFunction) {
    super(rep);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public D distance(DBID id1, DBID id2) {
    O o1 = rep.get(id1);
    O o2 = rep.get(id2);
    return distance(o1, o2);
  }

  @Override
  public D distance(O o1, DBID id2) {
    O o2 = rep.get(id2);
    return distance(o1, o2);
  }

  @Override
  public D distance(DBID id1, O o2) {
    O o1 = rep.get(id1);
    return distance(o1, o2);
  }

  @Override
  public D distance(O o1, O o2) {
    if(o1 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    if(o2 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    return distanceFunction.distance(o1, o2);
  }

  @Override
  public PrimitiveDistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }
}