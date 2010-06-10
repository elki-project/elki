package experimentalcode.erich.outlierensemble;

import java.util.Arrays;
import java.util.List;

/**
 * Simple combination rule, by taking the median.
 * Note: median is similar to a majority voting!
 * 
 * @author Erich Schubert
 */
public class EnsembleVotingMean implements EnsembleVoting {
  /**
   * Quantile to use
   */
  private double quantile = 0.5;
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public EnsembleVotingMean() {
    // TODO make quantile configurable
  }

  @Override
  public double combine(List<Double> scores) {
    Double[] s = scores.toArray(new Double[]{});
    Arrays.sort(s);
    double pos = quantile * s.length;
    int u = (int) Math.ceil(pos);
    int l = (int) Math.floor(pos);
    if (u == l) {
      return s[u];
    } else {
      // weighted, for quantiles != 0.5 this can be other than 0.5
      double wu = u - pos;
      double wl = pos - l;
      return s[u] * wu + s[l] * wl;
    }
  }
}