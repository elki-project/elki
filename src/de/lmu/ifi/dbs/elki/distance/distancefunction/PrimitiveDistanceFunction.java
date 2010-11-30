package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Primitive distance function that is defined on some kind of object.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DatabaseObject oneway - - defined on
 * 
 * @param <O> input object type
 * @param <D> distance result type
 */
public interface PrimitiveDistanceFunction<O extends DatabaseObject, D extends Distance<D>> extends DistanceFunction<O, D> {
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
}