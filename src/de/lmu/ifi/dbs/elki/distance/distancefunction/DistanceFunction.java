package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.MeasurementFunction;

/**
 * Interface DistanceFunction describes the requirements of any distance
 * function.
 *
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used by this DistanceFunction
 * @author Arthur Zimek
 */
public interface DistanceFunction<O extends DatabaseObject, D extends Distance<D>> extends MeasurementFunction<O, D> {

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   *
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  D distance(O o1, O o2);

  /**
   * Returns the distance between the two objcts specified by their obejct ids.
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objcts specified by their obejct ids
   */
  D distance(Integer id1, Integer id2);

  /**
   * Returns the distance between the two specified objects.
   *
   * @param id1 first object id
   * @param o2  second DatabaseObject
   * @return the distance between the two objcts specified by their obejct ids
   */
  D distance(Integer id1, O o2);
}