package de.lmu.ifi.dbs.elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
 * @apiviz.landmark
 * @apiviz.has ScoreResult oneway - - «create»
 */
public class JudgeOutlierScores implements Evaluator {
  /**
   * Logger for debug output.
   */
  protected static final Logging logger = Logging.getLogger(JudgeOutlierScores.class);

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
  public static final OptionID POSITIVE_CLASS_NAME_ID = OptionID.getOrCreateOptionID("comphist.positive", "Class label for the 'positive' class.");

  /**
   * Parameter to specify a scaling function to use.
   * <p>
   * Key: {@code -comphist.scaling}
   * </p>
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("comphist.scaling", "Class to use as scaling function.");

  /**
   * Stores the "positive" class.
   */
  private Pattern positiveClassName;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Constructor.
   * 
   * @param positive_class_name Positive class name
   * @param scaling Scaling function
   */
  public JudgeOutlierScores(Pattern positive_class_name, ScalingFunction scaling) {
    super();
    this.positiveClassName = positive_class_name;
    this.scaling = scaling;
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
  protected ScoreResult computeScore(DBIDs ids, DBIDs outlierIds, Database<?> database, OutlierResult or) throws IllegalStateException {
    if(scaling instanceof OutlierScalingFunction) {
      OutlierScalingFunction oscaling = (OutlierScalingFunction) scaling;
      oscaling.prepare(database.getIDs(), or);
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
    for(DBID id : ids) {
      double result = or.getScores().getValueFor(id);
      result = innerScaling.getScaled(scaling.getScaled(result));
      posscore += (1.0 - result);
    }
    for(DBID id : outlierIds) {
      double result = or.getScores().getValueFor(id);
      result = innerScaling.getScaled(scaling.getScaled(result));
      negscore += result;
    }
    posscore /= ids.size();
    negscore /= outlierIds.size();

    logger.verbose("Scores: " + posscore + " " + negscore);

    ArrayList<Vector> s = new ArrayList<Vector>(1);
    s.add(new Vector(new double[] { (posscore + negscore) / 2, posscore, negscore }));
    return new ScoreResult(s);
  }

  @Override
  public void processResult(Database<?> db, Result result, ResultHierarchy hierarchy) {
    List<OutlierResult> ors = ResultUtil.filterResults(result, OutlierResult.class);
    if(ors == null || ors.size() <= 0) {
      // logger.warning("No results found for "+JudgeOutlierScores.class.getSimpleName());
      return;
    }

    ModifiableDBIDs ids = DBIDUtil.newHashSet(db.getIDs());
    DBIDs outlierIds = DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName);
    ids.removeDBIDs(outlierIds);

    for(OutlierResult or : ors) {
      hierarchy.add(or, computeScore(ids, outlierIds, db, or));
    }
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<?> normalization) {
    // Normalizations are ignored.
  }

  /**
   * Result object for outlier score judgements.
   * 
   * @author Erich Schubert
   */
  public class ScoreResult extends CollectionResult<Vector> {
    /**
     * Constructor.
     * 
     * @param col score result
     */
    public ScoreResult(Collection<Vector> col) {
      super("Outlier Score", "outlier-score", col);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Stores the "positive" class.
     */
    private Pattern positiveClassName;

    /**
     * Scaling function to use
     */
    private ScalingFunction scaling;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      PatternParameter positiveClassNameP = new PatternParameter(POSITIVE_CLASS_NAME_ID);
      if(config.grab(positiveClassNameP)) {
        positiveClassName = positiveClassNameP.getValue();
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }
    }

    @Override
    protected JudgeOutlierScores makeInstance() {
      return new JudgeOutlierScores(positiveClassName, scaling);
    }
  }
}