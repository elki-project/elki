package de.lmu.ifi.dbs.elki.database.query.knn;


import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;

/**
 * Preprocessor-based kNN query implementation.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class PreprocessorKNNQueryFactory<O extends DatabaseObject, D extends Distance<D>> extends AbstractKNNQueryFactory<O, D> {
  /**
   * OptionID for {@link #PREPROCESSOR_PARAM}
   */
  public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID("knn.preprocessor", "Preprocessor used to materialize the kNN neighborhoods.");

  /**
   * The preprocessor used to materialize the kNN neighborhoods.
   * 
   * Default value: {@link MaterializeKNNPreprocessor} </p>
   * <p>
   * Key: {@code -knn.preprocessor}
   * </p>
   */
  private final ClassParameter<MaterializeKNNPreprocessor<O, D>> PREPROCESSOR_PARAM = new ClassParameter<MaterializeKNNPreprocessor<O, D>>(PREPROCESSOR_ID, MaterializeKNNPreprocessor.class, MaterializeKNNPreprocessor.class);

  /**
   * Preprocessor used
   */
  protected MaterializeKNNPreprocessor<O, D> preprocessor;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public PreprocessorKNNQueryFactory(Parameterization config) {
    super(config);
    config = config.descend(this);
    // configure the preprocessor
    if(config.grab(PREPROCESSOR_PARAM)) {
      ListParameterization preprocParams = new ListParameterization();
      preprocParams.addParameter(MaterializeKNNPreprocessor.K_ID, k);
      ChainedParameterization chain = new ChainedParameterization(preprocParams, config);
      // chain.errorsTo(config);
      preprocessor = PREPROCESSOR_PARAM.instantiateClass(chain);
      preprocParams.reportInternalParameterizationErrors(config);
    }
  }

  @Override
  public <T extends O> PreprocessorKNNQuery<T, D> instantiate(Database<T> database) {
    return new PreprocessorKNNQuery<T, D>(database, preprocessor);
  }

  @SuppressWarnings("deprecation")
  @Override
  public DistanceFunction<? super O, D> getDistanceFunction() {
    return preprocessor.getDistanceFunction();
  }

  @Override
  public D getDistanceFactory() {
    return preprocessor.getDistanceFactory();
  }
}