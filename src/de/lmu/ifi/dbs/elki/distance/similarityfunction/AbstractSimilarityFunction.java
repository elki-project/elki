package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction;
import de.lmu.ifi.dbs.elki.distance.Distance;

/**
 * @author Arthur Zimek
 * @param <O> object type
 * @param <D> distance type
 */
// TODO: Arthur: Documentation.
public abstract class AbstractSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractMeasurementFunction<O, D> implements SimilarityFunction<O, D> {
  protected AbstractSimilarityFunction(D distance) {
    super(distance);
  }

  public D similarity(Integer id1, O o2) {
    return similarity(id1, o2.getID());
  }

  public D similarity(O o1, O o2) {
    return similarity(o1.getID(), o2.getID());
  }
}
