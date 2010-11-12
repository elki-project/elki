package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <P> the type of Preprocessor used
 * @param <D> the type of Distance used
 */
public abstract class AbstractPreprocessorBasedDistanceFunction<O extends DatabaseObject, P extends Preprocessor<?, ?>, D extends Distance<D>> extends AbstractDatabaseDistanceFunction<O, D> implements PreprocessorBasedDistanceFunction<O, D> {
  /**
   * Parameter to specify the preprocessor to be used, must extend at least
   * {@link Preprocessor}.
   * <p>
   * Key: {@code -distancefunction.preprocessor}
   * </p>
   */
  private final ObjectParameter<P> PREPROCESSOR_PARAM;

  /**
   * The preprocessor.
   */
  private P preprocessor;

  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style
   * classes.
   * 
   * @param config Parameterization
   */
  public AbstractPreprocessorBasedDistanceFunction(Parameterization config) {
    super();
    config = config.descend(this);
    PREPROCESSOR_PARAM = new ObjectParameter<P>(PREPROCESSOR_ID, getPreprocessorSuperClass(), getDefaultPreprocessorClass());
    PREPROCESSOR_PARAM.setShortDescription(getPreprocessorDescription());
    if(config.grab(PREPROCESSOR_PARAM)) {
      preprocessor = PREPROCESSOR_PARAM.instantiateClass(config);
    }
  }

  /**
   * Get the preprocessor of this handler
   * 
   * @return Preprocessor
   */
  // TODO: make protected?
  public final P getPreprocessor() {
    return preprocessor;
  }

  /**
   * @return the class of the preprocessor parameter default.
   */
  abstract public Class<?> getDefaultPreprocessorClass();

  /**
   * @return description for the preprocessor parameter
   */
  abstract public String getPreprocessorDescription();

  /**
   * @return the super class for the preprocessor parameter
   */
  abstract public Class<P> getPreprocessorSuperClass();

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
  abstract public static class Instance<O extends DatabaseObject, P extends Preprocessor.Instance<R>, R, D extends Distance<D>> extends AbstractDBIDDistanceQuery<O, D> implements PreprocessorBasedDistanceFunction.Instance<O, P, D> {
    /**
     * Distance function
     */
    protected final AbstractPreprocessorBasedDistanceFunction<? super O, ?, D> distanceFunction;

    /**
     * Parent preprocessor
     */
    protected final P preprocessor;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param distanceFunction parent distance function
     */
    public Instance(Database<O> database, P preprocessor, AbstractPreprocessorBasedDistanceFunction<? super O, ?, D> distanceFunction) {
      super(database);
      this.preprocessor = preprocessor;
      this.distanceFunction = distanceFunction;
    }

    @Override
    public DistanceFunction<? super O, D> getDistanceFunction() {
      return distanceFunction;
    }

    /**
     * Preprocessing result for a single ID
     * 
     * @param id Object id
     * @return preprocessor result
     */
    public R getPreprocessed(DBID id) {
      return preprocessor.get(id);
    }

    @Override
    public P getPreprocessorInstance() {
      return preprocessor;
    }
  }
}