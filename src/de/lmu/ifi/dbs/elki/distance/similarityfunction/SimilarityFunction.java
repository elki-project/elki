package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface SimilarityFunction describes the requirements of any similarity
 * function.
 *
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.has Distance
 * 
 * @param <O> object type
 * @param <D> distance type
 */
public interface SimilarityFunction<O extends DatabaseObject, D extends Distance<?>> extends Parameterizable {
  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  boolean isSymmetric();  

  /**
   * Get the input data type of the function.
   */
  Class<? super O> getInputDatatype();

  /**
   * Get a distance factory.
   * 
   * @return distance factory
   */
  D getDistanceFactory();

  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param database
   * @return Actual distance query.
   */
  public <T extends O> SimilarityQuery<T, D> instantiate(Database<T> database);
}