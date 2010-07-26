package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.AbstractDBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used
 */
public abstract class AbstractDatabaseDistanceFunction<O extends DatabaseObject, D extends Distance<D>> implements DistanceFunction<O, D> {
  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style
   * classes.
   */
  public AbstractDatabaseDistanceFunction() {
    super();
  }

  abstract public D getDistanceFactory();

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public abstract Class<? super O> getInputDatatype();

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  abstract public class Instance<T extends O> extends AbstractDBIDDistanceQuery<T, D> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     */
    public Instance(Database<T> database) {
      super(database);
    }

    @Override
    public DistanceFunction<O, D> getDistanceFunction() {
      return AbstractDatabaseDistanceFunction.this;
    }
  }
}