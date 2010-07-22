package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.AbstractDatabaseDistanceQuery;
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
public abstract class AbstractPreprocessorBasedDistanceFunction<O extends DatabaseObject, P extends Preprocessor<O, ?>, D extends Distance<D>> implements PreprocessorBasedDistanceFunction<O, P, D> {
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
   * Distance factory to use.
   */
  private D distanceFactory;

  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style
   * classes.
   * 
   * @param config Parameterization
   * @param distanceFactory Distance Factory
   */
  public AbstractPreprocessorBasedDistanceFunction(Parameterization config, D distanceFactory) {
    super();
    this.distanceFactory = distanceFactory;
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

  /*
   * @Override public DistanceQuery<O, D> preprocess(Database<O> database) {
   * preprocessor.run(database); // FIXME: CONTINUE return null; }
   */

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
  public D getDistanceFactory() {
    return distanceFactory;
  }

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
   * 
   * @param <O> Object type
   * @param <P> Preprocessor type
   * @param <D> Distance result type
   */
  abstract public static class Instance<O extends DatabaseObject, P extends Preprocessor<O, ?>, D extends Distance<D>> extends AbstractDatabaseDistanceQuery<O, D> {
    /**
     * Parent preprocessor
     */
    protected final P preprocessor;

    /**
     * Parent distance
     */
    protected final DistanceFunction<O, D> parent;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param parent Parent distance
     */
    public Instance(Database<O> database, P preprocessor, DistanceFunction<O, D> parent) {
      super(database);
      this.preprocessor = preprocessor;
      this.parent = parent;
      this.preprocessor.run(database);
    }

    @Override
    public DistanceFunction<O, D> getDistanceFunction() {
      return parent;
    }
  }
}