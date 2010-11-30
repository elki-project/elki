package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.similarity.AbstractDBIDSimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses Preprocessor
 * 
 * @param <O> object type
 * @param <P> preprocessor type
 * @param <D> distance type
 */
public abstract class AbstractPreprocessorBasedSimilarityFunction<O extends DatabaseObject, P extends Preprocessor<O, R>, R, D extends Distance<D>> implements PreprocessorBasedSimilarityFunction<O, D> {
  /**
   * OptionID for {@link #PREPROCESSOR_PARAM}
   */
  public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID("similarityfunction.preprocessor", "Preprocessor to use.");

  /**
   * Parameter to specify the preprocessor to be used, must extend at least
   * {@link Preprocessor}.
   * <p>
   * Key: {@code -similarityfunction.preprocessor}
   * </p>
   */
  private final ObjectParameter<P> PREPROCESSOR_PARAM;

  /**
   * The preprocessor.
   */
  private P preprocessor;

  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractPreprocessorBasedSimilarityFunction(Parameterization config) {
    super();
    config = config.descend(this);
    PREPROCESSOR_PARAM = new ObjectParameter<P>(PREPROCESSOR_ID, getPreprocessorSuperClass(), getDefaultPreprocessorClass());
    PREPROCESSOR_PARAM.setShortDescription(getPreprocessorDescription());
    if(config.grab(PREPROCESSOR_PARAM)) {
      preprocessor = PREPROCESSOR_PARAM.instantiateClass(config);
    }
  }

  /**
   * Get the preprocessor managed by this handler
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
  abstract public <T extends O> Instance<T, ?, R, D> instantiate(Database<T> database);

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   * @param <P> Preprocessor type
   * @param <D> Distance result type
   */
  abstract public static class Instance<O extends DatabaseObject, P extends Preprocessor.Instance<R>, R, D extends Distance<D>> extends AbstractDBIDSimilarityQuery<O, D> implements PreprocessorBasedSimilarityFunction.Instance<O, P, D> {
    /**
     * Parent preprocessor
     */
    protected final P preprocessor;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     */
    public Instance(Database<O> database, P preprocessor) {
      super(database);
      this.preprocessor = preprocessor;
    }

    @Override
    public P getPreprocessorInstance() {
      return preprocessor;
    }
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }
}