package de.lmu.ifi.dbs.elki.result;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.distance.Distance;

/**
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Result
 * @param <D> the type of Distance used by this Result todo arthur comment
 */
public class KNNDistanceOrderResult<D extends Distance<D>> implements IterableResult<D> {
  /**
   * Store the kNN Distances
   */
  private final List<D> knnDistances;

  /**
   * @param db
   * @param knnDistances
   */
  public KNNDistanceOrderResult(final List<D> knnDistances) {
    this.knnDistances = knnDistances;
  }

  /**
   * Return an iterator.
   */
  @Override
  public Iterator<D> iter() {
    return knnDistances.iterator();
  }
}
