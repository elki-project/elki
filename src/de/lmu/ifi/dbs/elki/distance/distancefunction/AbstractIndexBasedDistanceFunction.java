package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a database index.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <I> the type of Index used
 * @param <D> the type of Distance used
 */
public abstract class AbstractIndexBasedDistanceFunction<O extends DatabaseObject, I extends Index<O>, D extends Distance<D>> extends AbstractDatabaseDistanceFunction<O, D> implements IndexBasedDistanceFunction<O, D> {
  /**
   * Parameter to specify the preprocessor to be used.
   * <p>
   * Key: {@code -distancefunction.preprocessor}
   * </p>
   */
  protected IndexFactory<O, I> index;

  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style
   * classes.
   * 
   * @param config Parameterization
   */
  public AbstractIndexBasedDistanceFunction(Parameterization config) {
    super();
    config = config.descend(this);
    ObjectParameter<IndexFactory<O, I>> param = new ObjectParameter<IndexFactory<O, I>>(INDEX_ID, getIndexFactoryRestriction(), getIndexFactoryDefaultClass());
    if(config.grab(param)) {
      index = param.instantiateClass(config);
    }
  }

  /**
   * Get the index factory restriction
   * 
   * @return Factory class restriction
   */
  abstract protected Class<?> getIndexFactoryRestriction();

  /**
   * Get the default index factory class.
   * 
   * @return Index factory
   */
  abstract protected Class<?> getIndexFactoryDefaultClass();
  
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
  abstract public static class Instance<O extends DatabaseObject, I extends Index<O>, D extends Distance<D>, F extends DistanceFunction<? super O, D>> extends AbstractDBIDDistanceQuery<O, D> implements IndexBasedDistanceFunction.Instance<O, I, D> {
    /**
     * Index we use
     */
    protected final I index;

    /**
     * Our parent distance function
     */
    protected F parent;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Index to use
     * @param parent Parent distance function
     */
    public Instance(Database<O> database, I index, F parent) {
      super(database);
      this.index = index;
      this.parent = parent;
    }

    @Override
    public I getIndex() {
      return index;
    }

    @Override
    public F getDistanceFunction() {
      return parent;
    }
  }
}