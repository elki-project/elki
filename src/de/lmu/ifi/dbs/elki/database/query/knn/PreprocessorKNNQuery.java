package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
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
public class PreprocessorKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractKNNQuery<O, D> {
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
  public PreprocessorKNNQuery(Parameterization config) {
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
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    return new Instance<T, D>(database, preprocessor);
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

  /**
   * Instance for a particular database, invoking the preprocessor.
   * 
   * @author Erich Schubert
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractKNNQuery.Instance<O, D> {
    /**
     * The last preprocessor result
     */
    final private MaterializeKNNPreprocessor.Instance<O, D> preprocessor;
    
    /**
     * Warn only once.
     */
    private boolean warned = false;

    /**
     * Constructor.
     * 
     * @param database Database to query
     */
    public Instance(Database<O> database, MaterializeKNNPreprocessor<? super O, D> preprocessor) {
      super(database);
      this.preprocessor = preprocessor.instantiate(database);
    }

    @Override
    public List<DistanceResultPair<D>> getForDBID(DBID id, int k) {
      if (!warned && k > preprocessor.getK()) {
        LoggingUtil.warning("Requested more neighbors than preprocessed!");
      }
      return preprocessor.get(id);
    }

    /**
     * Get the preprocessor instance.
     * 
     * @return preprocessor instance
     */
    public MaterializeKNNPreprocessor.Instance<O, D> getPreprocessor() {
      return preprocessor;
    }

    @Override
    public D getDistanceFactory() {
      return preprocessor.getDistanceFactory();
    }

    @Override
    public List<DistanceResultPair<D>> getForObject(O obj, int k) {
      DBID id = obj.getID();
      if(id != null) {
        return getForDBID(id, k);
      }
      throw new AbortException("Preprocessor KNN query used with previously unseen objects.");
    }

    @Override
    public DistanceQuery<O, D> getDistanceQuery() {
      // TODO: remove? throw an exception?
      return preprocessor.getDistanceQuery();
    }
  }
}