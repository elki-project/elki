/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.evaluation.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.IdentityScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScaling;

/**
 * Compute a Histogram to evaluate a ranking algorithm.
 * <p>
 * The parameter {@code -hist.positive} specifies the class label of "positive"
 * hits.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @navhas - create - ScoreResult
 */
public class JudgeOutlierScores implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(JudgeOutlierScores.class);

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
   * @param or Outlier Result to evaluate
   * @return Outlier score result
   * @throws IllegalStateException
   */
  protected ScoreResult computeScore(DBIDs ids, DBIDs outlierIds, OutlierResult or) throws IllegalStateException {
    if(scaling instanceof OutlierScaling) {
      OutlierScaling oscaling = (OutlierScaling) scaling;
      oscaling.prepare(or);
    }

    final ScalingFunction innerScaling;
    // If we have useful (finite) min/max, use these for binning.
    double min = scaling.getMin();
    double max = scaling.getMax();
    if(Double.isInfinite(min) || Double.isNaN(min) || Double.isInfinite(max) || Double.isNaN(max)) {
      innerScaling = new IdentityScaling();
      // TODO: does the outlier score give us this guarantee?
      LOG.warning("JudgeOutlierScores expects values between 0.0 and 1.0, but we don't have such a guarantee by the scaling function: min:" + min + " max:" + max);
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
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double result = or.getScores().doubleValue(iter);
      result = innerScaling.getScaled(scaling.getScaled(result));
      posscore += (1.0 - result);
    }
    for(DBIDIter iter = outlierIds.iter(); iter.valid(); iter.advance()) {
      double result = or.getScores().doubleValue(iter);
      result = innerScaling.getScaled(scaling.getScaled(result));
      negscore += result;
    }
    posscore /= ids.size();
    negscore /= outlierIds.size();

    LOG.verbose("Scores: " + posscore + " " + negscore);

    ArrayList<double[]> s = new ArrayList<>(1);
    s.add(new double[] { (posscore + negscore) * .5, posscore, negscore });
    return new ScoreResult(s);
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    Database db = ResultUtil.findDatabase(hier);
    List<OutlierResult> ors = ResultUtil.filterResults(hier, OutlierResult.class);
    if(ors == null || ors.isEmpty()) {
      // logger.warning("No results found for
      // "+JudgeOutlierScores.class.getSimpleName());
      return;
    }

    ModifiableDBIDs ids = DBIDUtil.newHashSet(ors.iterator().next().getScores().getDBIDs());
    DBIDs outlierIds = DatabaseUtil.getObjectsByLabelMatch(db, positiveClassName);
    ids.removeDBIDs(outlierIds);

    for(OutlierResult or : ors) {
      db.getHierarchy().add(or, computeScore(ids, outlierIds, or));
    }
  }

  /**
   * Result object for outlier score judgements.
   * 
   * @author Erich Schubert
   */
  public class ScoreResult extends CollectionResult<double[]> {
    /**
     * Constructor.
     * 
     * @param col score result
     */
    public ScoreResult(Collection<double[]> col) {
      super("Outlier Score", "outlier-score", col);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The distance function to determine the reachability distance between
     * database objects.
     */
    public static final OptionID POSITIVE_CLASS_NAME_ID = new OptionID("comphist.positive", "Class label for the 'positive' class.");

    /**
     * Parameter to specify a scaling function to use.
     */
    public static final OptionID SCALING_ID = new OptionID("comphist.scaling", "Class to use as scaling function.");

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

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class, IdentityScaling.class);
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
