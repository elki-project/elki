package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * A distance query serves as adapter layer for database and primitive
 * distances.
 * 
 * @author Erich Schubert
 * 
 * @param O Input object type
 * @param D Distance result type
 */
public abstract class AbstractDistanceQuery<O, D extends Distance<D>> extends AbstractDataBasedQuery<O> implements DistanceQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param rep Representation to use.
   */
  public AbstractDistanceQuery(Relation<? extends O> rep) {
    super(rep);
  }

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(DBID id1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(O o1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(DBID id1, O o2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(O o1, O o2);

  @Override
  public D getDistanceFactory() {
    return getDistanceFunction().getDistanceFactory();
  }

  /**
   * Provides an infinite distance.
   * 
   * @return an infinite distance
   */
  @Override
  public D infiniteDistance() {
    return getDistanceFunction().getDistanceFactory().infiniteDistance();
  }

  /**
   * Provides a null distance.
   * 
   * @return a null distance
   */
  @Override
  public D nullDistance() {
    return getDistanceFunction().getDistanceFactory().nullDistance();
  }

  /**
   * Provides an undefined distance.
   * 
   * @return an undefined distance
   */
  @Override
  public D undefinedDistance() {
    return getDistanceFunction().getDistanceFactory().undefinedDistance();
  }
}