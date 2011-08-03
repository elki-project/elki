package de.lmu.ifi.dbs.elki.algorithm.outlier.trivial;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Scale another outlier score using the given scaling function.
 * 
 * @author Erich Schubert
 */
public class RescaleMetaOutlierAlgorithm extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RescaleMetaOutlierAlgorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("metaoutlier.scaling", "Class to use as scaling function.");

  /**
   * Holds the algorithm to run.
   */
  private Algorithm algorithm;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Constructor.
   * 
   * @param algorithm Inner algorithm
   * @param scaling Scaling to apply.
   */
  public RescaleMetaOutlierAlgorithm(Algorithm algorithm, ScalingFunction scaling) {
    super();
    this.algorithm = algorithm;
    this.scaling = scaling;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);

    OutlierResult or = getOutlierResult(innerresult);
    final Relation<Double> scores = or.getScores();
    if(scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction) scaling).prepare(scores.getDBIDs(), or);
    }

    WritableDataStore<Double> scaledscores = DataStoreUtil.makeStorage(scores.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);

    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : scores.iterDBIDs()) {
      double val = scores.get(id);
      val = scaling.getScaled(val);
      scaledscores.put(id, val);
      minmax.put(val);
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), scaling.getMin(), scaling.getMax());
    Relation<Double> scoresult = new MaterializedRelation<Double>("Scaled Outlier", "scaled-outlier", TypeUtil.DOUBLE, scaledscores, scores.getDBIDs());
    OutlierResult result = new OutlierResult(meta, scoresult);
    result.addChildResult(innerresult);

    return result;
  }

  /**
   * Find an OutlierResult to work with.
   * 
   * @param result Result object
   * @return Iterator to work with
   */
  private OutlierResult getOutlierResult(Result result) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors.size() > 0) {
      return ors.get(0);
    }
    throw new IllegalStateException("Comparison algorithm expected at least one outlier result.");
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return algorithm.getInputTypeRestriction();
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Holds the algorithm to run.
     */
    private Algorithm algorithm;

    /**
     * Scaling function to use
     */
    private ScalingFunction scaling;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<Algorithm> algP = new ObjectParameter<Algorithm>(OptionID.ALGORITHM, OutlierAlgorithm.class);
      if(config.grab(algP)) {
        algorithm = algP.instantiateClass(config);
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }
    }

    @Override
    protected RescaleMetaOutlierAlgorithm makeInstance() {
      return new RescaleMetaOutlierAlgorithm(algorithm, scaling);
    }
  }
}