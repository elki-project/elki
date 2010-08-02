package experimentalcode.erich.outlierensemble;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Combination rule based on Bayes theorems.
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingRestrictedBayes implements EnsembleVoting {
  /**
   * Option ID for the minimum and maximum vote
   */
  public final static OptionID MIN_ID = OptionID.getOrCreateOptionID("ensemble.bayes.min", "Minimum vote share.");

  /**
   * Minimum vote to cast.
   */
  private final DoubleParameter MIN_PARAM = new DoubleParameter(MIN_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.CLOSE, 1.0, IntervalConstraint.IntervalBoundary.OPEN), 0.05);

  /**
   * Option ID for the minimum and maximum vote
   */
  public final static OptionID MAX_ID = OptionID.getOrCreateOptionID("ensemble.bayes.max", "Maximum vote share.");

  /**
   * Minimum vote to cast.
   */
  private final DoubleParameter MAX_PARAM = new DoubleParameter(MAX_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.CLOSE), 0.95);

  /**
   * Minimum vote to cast
   */
  private double minvote = 0.05;

  /**
   * Maximum vote to cast
   */
  private double maxvote = 1.0;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EnsembleVotingRestrictedBayes(Parameterization config) {
    config = config.descend(this);
    if(config.grab(MIN_PARAM)) {
      minvote = MIN_PARAM.getValue();
    }
    if(config.grab(MAX_PARAM)) {
      maxvote = MAX_PARAM.getValue();
    }
  }

  @Override
  public double combine(List<Double> scores) {
    double pos = 1.0;
    double neg = 1.0;
    for(Double score : scores) {
      final double cscore = minvote + score * (maxvote - minvote);
      pos *= cscore;
      neg *= (1.0 - cscore);
    }
    return pos / (pos + neg);
  }
}