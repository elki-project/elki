package experimentalcode.erich.histogram;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

public class NormalizeOutlierScoreMetaAlgorithm<O extends DatabaseObject> extends AbstractAlgorithm<O, OutlierResult> {
  /**
   * Association ID for scaled values
   */
  public static final AssociationID<Double> SCALED_SCORE = AssociationID.getOrCreateAssociationID("SCALED_SCORE", Double.class);

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * Parameter to specify the algorithm to be applied, must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}.
   * <p>
   * Key: {@code -algorithm}
   * </p>
   */
  private final ObjectParameter<Algorithm<O, Result>> ALGORITHM_PARAM = new ObjectParameter<Algorithm<O, Result>>(OptionID.ALGORITHM, Algorithm.class);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  private final ObjectParameter<ScalingFunction> SCALING_PARAM = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);

  /**
   * Holds the algorithm to run.
   */
  private Algorithm<O, Result> algorithm;

  /**
   * Stores the result object.
   */
  private OutlierResult result;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  public NormalizeOutlierScoreMetaAlgorithm(Parameterization config) {
    super(config);
    if(config.grab(this, ALGORITHM_PARAM)) {
      algorithm = ALGORITHM_PARAM.instantiateClass(config);
    }
    if(config.grab(this, SCALING_PARAM)) {
      scaling = SCALING_PARAM.instantiateClass(config);
    }
  }

  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    Result innerresult = algorithm.run(database);

    OutlierResult or = getOutlierResult(database, innerresult);
    if(scaling instanceof OutlierScalingFunction) {
      ((OutlierScalingFunction) scaling).prepare(database, innerresult, or);
    }

    HashMap<Integer, Double> scaledscores = new HashMap<Integer, Double>(database.size());

    for(Integer id : database) {
      double val = or.getScores().getValueFor(id);
      val = scaling.getScaled(val);
      scaledscores.put(id, val);
    }

    OutlierScoreMeta meta = new BasicOutlierScoreMeta(0.0, 1.0);
    AnnotationResult<Double> scoresult = new AnnotationFromHashMap<Double>(SCALED_SCORE, scaledscores);
    OrderingResult ordresult = new OrderingFromHashMap<Double>(scaledscores);
    result = new OutlierResult(meta, scoresult, ordresult);
    result.addResult(innerresult);

    return result;
  }

  /**
   * Find an OutlierResult to work with.
   * 
   * @param database Database context
   * @param result Result object
   * @return Iterator to work with
   */
  private OutlierResult getOutlierResult(Database<O> database, Result result) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors.size() > 0) {
      return ors.get(0);
    }
    throw new IllegalStateException("Comparison algorithm expected at least one outlier result.");
  }

  @Override
  public Description getDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OutlierResult getResult() {
    return result;
  }
}
