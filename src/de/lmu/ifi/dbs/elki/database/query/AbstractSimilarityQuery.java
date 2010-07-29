package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * A distance query serves as adapter layer for database and primitive
 * similarity functions.
 * 
 * @author Erich Schubert
 * 
 * @param O Input object type
 * @param D Distance result type
 */
public abstract class AbstractSimilarityQuery<O extends DatabaseObject, D extends Distance<D>> implements SimilarityQuery<O, D> {
  /**
   * The database to use.
   */
  final protected Database<? extends O> database;

  /**
   * Constructor.
   * 
   * @param database Database to use.
   */
  public AbstractSimilarityQuery(Database<? extends O> database) {
    super();
    this.database = database;
  }

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D similarity(DBID id1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D similarity(O o1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D similarity(DBID id1, O o2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D similarity(O o1, O o2);
}