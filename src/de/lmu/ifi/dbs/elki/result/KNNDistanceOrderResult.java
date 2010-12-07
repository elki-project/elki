package de.lmu.ifi.dbs.elki.result;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Wraps a list containing the knn distances.
 * 
 * @author Arthur Zimek
 * @param <D> the type of Distance used by this Result
 * 
 */
public class KNNDistanceOrderResult<D extends Distance<D>> extends BasicResult implements IterableResult<D> {
  /**
   * Store the kNN Distances
   */
  private final List<D> knnDistances;

  /**
   * Construct result
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param knnDistances distance list to wrap.
   */
  public KNNDistanceOrderResult(String name, String shortname, final List<D> knnDistances) {
    super(name, shortname);
    this.knnDistances = knnDistances;
  }

  /**
   * Return an iterator.
   */
  @Override
  public Iterator<D> iterator() {
    return knnDistances.iterator();
  }
}
