package de.lmu.ifi.dbs.elki.distance;

public final class DistanceUtil {
  /**
   * Returns the maximum of the given Distances or the first, if none is greater
   * than the other one.
   * 
   * @param d1 first Distance
   * @param d2 second Distance
   * @return Distance the maximum of the given Distances or the first, if
   *         neither is greater than the other one
   */
  public static <D extends Distance<D>> D max(D d1, D d2) {
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
   * @param d1 first Distance
   * @param d2 second Distance
   * @return Distance the minimum of the given Distances or the first, if
   *         neither is less than the other one
   */
  public static <D extends Distance<D>> D min(D d1, D d2) {
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
}
