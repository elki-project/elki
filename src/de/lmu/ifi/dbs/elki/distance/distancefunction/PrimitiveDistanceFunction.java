package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Primitive distance function that is defined on some kind of object.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @param <O> input object type
 * @param <D> distance result type
 */
public interface PrimitiveDistanceFunction<O, D extends Distance<?>> extends DistanceFunction<O, D> {
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
  
  @Override
  SimpleTypeInformation<? super O> getInputTypeRestriction();  
}