package experimentalcode.shared.outlier.ensemble.voting;

import java.util.List;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;

/**
 * Simple combination rule, by taking the median.
 * Note: median is similar to a majority voting!
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingMin implements EnsembleVoting {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public EnsembleVotingMin() {
    // empty
  }

  @Override
  public double combine(List<Double> scores) {
    DoubleMinMax mm = new DoubleMinMax();
    mm.put(scores);
    return mm.getMin();
  }
}