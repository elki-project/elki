package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.PreprocessorBasedMeasurementFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @param <O> object type
 * @param <P> preprocessor type
 * @param <D> distance type
 */
public abstract class AbstractPreprocessorBasedSimilarityFunction<O extends DatabaseObject, P extends Preprocessor<O, ?>, D extends Distance<D>> extends AbstractSimilarityFunction<O, D> implements PreprocessorBasedMeasurementFunction<O, P, D> {
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
   * @param distance Distance factory
   */
  public AbstractPreprocessorBasedSimilarityFunction(Parameterization config, D distance) {
    super(distance);
    PREPROCESSOR_PARAM = new ObjectParameter<P>(PREPROCESSOR_ID, getPreprocessorSuperClass(), getDefaultPreprocessorClass());
    PREPROCESSOR_PARAM.setShortDescription(getPreprocessorDescription());
    if(config.grab(PREPROCESSOR_PARAM)) {
      preprocessor = PREPROCESSOR_PARAM.instantiateClass(config);
    }
  }

  /**
   * Calls
   * {@link de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction#setDatabase}
   * and runs the preprocessor on the database.
   * 
   * @param database the database to be set
   */
  @Override
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    preprocessor.run(database);
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
}