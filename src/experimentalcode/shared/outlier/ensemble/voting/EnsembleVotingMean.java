package experimentalcode.shared.outlier.ensemble.voting;

import java.util.List;

import de.lmu.ifi.dbs.elki.math.MeanVariance;

/**
 * Simple combination rule, by taking the mean
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingMean implements EnsembleVoting {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public EnsembleVotingMean() {
    // Empty.
  }

  @Override
  public double combine(List<Double> scores) {
    MeanVariance mv = new MeanVariance();
    for (double score : scores) {
      mv.put(score);
    }
    return mv.getMean();
  }
}
