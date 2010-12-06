package de.lmu.ifi.dbs.elki.database.query.knn;


import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * A KNNQueryFactory essentially is an index for KNN queries.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses KNNQuery oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public interface KNNQueryFactory<O extends DatabaseObject, D extends Distance<D>> {
  /**
   * OptionID for the 'k' parameter
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

  /**
   * OptionID for the distance function
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

  /**
   * Get an instance for a particular database
   * 
   * @param database Database
   */
  public <T extends O> KNNQuery<T, D> instantiate(Database<T> database);

  /**
   * Get the underlying distance function
   * 
   * @return get the distance function used.
   */
  @Deprecated
  public DistanceFunction<? super O, D> getDistanceFunction();

  /**
   * Get the distance data type of the function.
   */
  public D getDistanceFactory();
}