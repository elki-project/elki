package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Base implementation of a similarity function.
 * 
 * @author Arthur Zimek
 * @param <O> object type
 * @param <D> distance type
 */
public abstract class AbstractSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractMeasurementFunction<O, D> implements SimilarityFunction<O, D> {
  /**
   * Constructor.
   * 
   * @param distance Distance data type factory
   */
  protected AbstractSimilarityFunction(D distance) {
    super(distance);
  }

  @Override
  public D similarity(DBID id1, O o2) {
    return similarity(id1, o2.getID());
  }

  @Override
  public D similarity(O o1, DBID id2) {
    return similarity(o1.getID(), id2);
  }

  @Override
  public D similarity(O o1, O o2) {
    return similarity(o1.getID(), o2.getID());
  }

  @Override
  public boolean isSymmetric() {
    // Assume symmetric by default!
    return true;
  }
}