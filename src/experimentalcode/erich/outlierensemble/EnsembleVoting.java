package experimentalcode.erich.outlierensemble;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for ensemble voting rules
 * 
 * @author Erich Schubert
 */
public interface EnsembleVoting extends Parameterizable {
  /**
   * Combine scores function.
   * Note: it is assumed that the scores are comparable.
   * 
   * @param scores Scores to combine
   * @return combined score.
   */
  public double combine(List<Double> scores);
}
