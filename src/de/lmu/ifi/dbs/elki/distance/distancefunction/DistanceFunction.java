package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Base interface for any kind of distances.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance result type
 * 
 * @apiviz.landmark
 * @apiviz.has Distance
 */
public interface DistanceFunction<O, D extends Distance<?>> extends Parameterizable {
  /**
   * Method to get the distance functions factory.
   * 
   * @return Factory for distance objects
   */
  D getDistanceFactory();

  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  boolean isSymmetric();

  /**
   * Is this distance function metric (in particular, does it satisfy the
   * triangle equation?)
   * 
   * @return {@code true} when metric.
   */
  boolean isMetric();

  /**
   * Get the input data type of the function.
   * 
   * @return Type restriction
   */
  TypeInformation getInputTypeRestriction();

  /**
   * Instantiate with a database to get the actual distance query.
   * 
   * @param rep The representation to use
   * @return Actual distance query.
   */
  public <T extends O> DistanceQuery<T, D> instantiate(Relation<T> rep);
}