package de.lmu.ifi.dbs.elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction;

/**
 * Compute a Histogram to evaluate a ranking algorithm.
 * 
 * The parameter {@code -hist.positive} specifies the class label of "positive"
 * hits.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */
public class JudgeOutlierScores<O extends DatabaseObject> implements Evaluator<O> {
  /**
   * Logger for debug output.
   */
  protected static final Logging logger = Logging.getLogger(JudgeOutlierScores.class);
  
  /**
   * OptionID for {@link #POSITIVE_CLASS_NAME_PARAM}
   */
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("comphist.positive", "Class label for the 'positive' class.");

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -comphist.positive}
   * </p>
   */
  private final PatternParameter POSITIVE_CLASS_NAME_PARAM = new PatternParameter(POSITIVE_CLASS_NAME_ID);

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  private final ObjectParameter<ScalingFunction> SCALING_PARAM = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);

  /**
   * Stores the "positive" class.
   */
  private Pattern positive_class_name;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Constructor.
   * 
   * @param config Parameters
   */
  public JudgeOutlierScores(Parameterization config) {
    super();
    if(config.grab(POSITIVE_CLASS_NAME_PARAM)) {
      positive_class_name = POSITIVE_CLASS_NAME_PARAM.getValue();
    }
    if(config.grab(SCALING_PARAM)) {
      scaling = SCALING_PARAM.instantiateClass(config);
    }
  }

  /**
   * Evaluate a single outlier score result.
   * 
   * @param ids Inlier IDs
   * @param outlierIds Outlier IDs
   * @param database Database
   * @param or Outlier Result to evaluate
   * @return Outlier score result
   * @throws IllegalStateException
   */
  protected CollectionResult<DoubleVector> computeScore(Collection<Integer> ids, Collection<Integer> outlierIds, Database<O> database, OutlierResult or) throws IllegalStateException {
    if(scaling instanceof OutlierScalingFunction) {
      OutlierScalingFunction oscaling = (OutlierScalingFunction) scaling;
      oscaling.prepare(database, or);
    }

    final ScalingFunction innerScaling;
    // If we have useful (finite) min/max, use these for binning.
    double min = scaling.getMin();
    double max = scaling.getMax();
    if(Double.isInfinite(min) || Double.isNaN(min) || Double.isInfinite(max) || Double.isNaN(max)) {
      innerScaling = new IdentityScaling();
      // TODO: does the outlier score give us this guarantee?
      logger.warning("JudgeOutlierScores expects values between 0.0 and 1.0, but we don't have such a guarantee by the scaling function: min:" + min + " max:" + max);
    }
    else {
      if(min == 0.0 && max == 1.0) {
        innerScaling = new IdentityScaling();
      }
      else {
        innerScaling = new LinearScaling(1.0 / (max - min), -min);
      }
    }

    double posscore = 0.0;
    double negscore = 0.0;
    // fill histogram with values of each object
    for(Integer id : ids) {
      double result = or.getScores().getValueFor(id);
      result = innerScaling.getScaled(scaling.getScaled(result));
      posscore += (1.0 - result);
    }
    for(Integer id : outlierIds) {
      double result = or.getScores().getValueFor(id);
      result = innerScaling.getScaled(scaling.getScaled(result));
      negscore += result;
    }
    posscore /= ids.size();
    negscore /= outlierIds.size();

    logger.verbose("Scores: " + posscore + " " + negscore);

    ArrayList<DoubleVector> s = new ArrayList<DoubleVector>(1);
    s.add(new DoubleVector(new double[] { (posscore + negscore) / 2, posscore, negscore }));
    return new CollectionResult<DoubleVector>(s);
  }

  @Override
  public MultiResult processResult(Database<O> db, MultiResult result) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors.size() <= 0) {
      logger.warning("No results found for "+JudgeOutlierScores.class.getSimpleName());
      return result;
    }
    
    Collection<Integer> ids = db.getIDs();
    Collection<Integer> outlierIds = DatabaseUtil.getObjectsByLabelMatch(db, positive_class_name);
    ids.removeAll(outlierIds);

    for (OutlierResult or : ors) {
      result.addResult(computeScore(ids, outlierIds, db, or));
    }
    
    return result;
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<O> normalization) {
    // Normalizations are ignored.
  }
}