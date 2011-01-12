package de.lmu.ifi.dbs.elki.distance;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ProxyDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Class with distance related utility functions.
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.distance.distancevalue.Distance oneway - -
 *              handles
 * 
 * @author Erich Schubert
 */
public final class DistanceUtil {
  /**
   * Returns the maximum of the given Distances or the first, if none is greater
   * than the other one.
   * 
   * @param <D> distance type
   * @param d1 first Distance
   * @param d2 second Distance
   * @return Distance the maximum of the given Distances or the first, if
   *         neither is greater than the other one
   */
  public static <D extends Distance<D>> D max(D d1, D d2) {
    if(d1 == null) {
      return d2;
    }
    if(d2 == null) {
      return d1;
    }
    if(d1.compareTo(d2) > 0) {
      return d1;
    }
    else if(d2.compareTo(d1) > 0) {
      return d2;
    }
    else {
      return d1;
    }
  }

  /**
   * Returns the minimum of the given Distances or the first, if none is less
   * than the other one.
   * 
   * @param <D> distance type
   * @param d1 first Distance
   * @param d2 second Distance
   * @return Distance the minimum of the given Distances or the first, if
   *         neither is less than the other one
   */
  public static <D extends Distance<D>> D min(D d1, D d2) {
    if(d1 == null) {
      return d2;
    }
    if(d2 == null) {
      return d1;
    }
    if(d1.compareTo(d2) < 0) {
      return d1;
    }
    else if(d2.compareTo(d1) < 0) {
      return d2;
    }
    else {
      return d1;
    }
  }

  /**
   * Helper function, to resolve any wrapped Proxy Distances
   * 
   * @param <V> Object type
   * @param <D> Distance type
   * @param dfun Distance function to unwrap.
   * @return unwrapped distance function
   */
  @SuppressWarnings("unchecked")
  public static <V extends DatabaseObject, T extends V, D extends Distance<D>> DistanceFunction<? super V, D> unwrapDistance(DistanceFunction<V, D> dfun) {
    if(ProxyDistanceFunction.class.isInstance(dfun)) {
      return unwrapDistance(((ProxyDistanceFunction<V, D>) dfun).getDistanceQuery().getDistanceFunction());
    }
    return dfun;
  }
}