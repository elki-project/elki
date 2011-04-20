package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * A distance query serves as adapter layer for database and primitive distances.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has Distance
 * 
 * @param O Input object type
 * @param D Distance result type
 */
public interface DistanceQuery<O, D extends Distance<?>> extends DatabaseQuery {
  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  public abstract D distance(DBID id1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  public abstract D distance(O o1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  public abstract D distance(DBID id1, O o2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  public abstract D distance(O o1, O o2);

  /**
   * Method to get the distance functions factory.
   * 
   * @return Factory for distance objects
   */
  public abstract D getDistanceFactory();

  /**
   * Get the inner distance function.
   * 
   * @return Distance function
   */
  public abstract DistanceFunction<? super O, D> getDistanceFunction();

  /**
   * Provides an infinite distance.
   * 
   * @return an infinite distance
   */
  public abstract D infiniteDistance();

  /**
   * Provides a null distance.
   * 
   * @return a null distance
   */
  public abstract D nullDistance();

  /**
   * Provides an undefined distance.
   * 
   * @return an undefined distance
   */
  public abstract D undefinedDistance();
  
  /**
   * Access the underlying data query.
   * 
   * @return data query in use
   */
  public abstract Relation<? extends O> getRelation();
}