package de.lmu.ifi.dbs.elki.database.query.similarity;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * A similarity query serves as adapter layer for database and primitive
 * similarity functions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has Distance
 * 
 * @param O Input object type
 * @param D Distance result type
 */
public interface SimilarityQuery<O extends DatabaseObject, D extends Distance<?>> extends DatabaseQuery {
  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  public abstract D similarity(DBID id1, DBID id2);

  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param o1 first object
   * @param id2 second object id
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  public abstract D similarity(O o1, DBID id2);

  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param id1 first object id
   * @param o2 second object
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  public abstract D similarity(DBID id1, O o2);

  /**
   * Returns the similarity between the two objects specified by their object
   * ids.
   * 
   * @param o1 first object
   * @param o2 second object
   * @return the similarity between the two objects specified by their object
   *         ids
   */
  public abstract D similarity(O o1, O o2);

  /**
   * Method to get the distance functions factory.
   * 
   * @return Factory for distance objects
   */
  public abstract D getDistanceFactory();
}