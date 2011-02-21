package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * This is a pseudo outlier scoring obtained by only considering the ranks of
 * the objects. However, the ranks are not mapped linarly to scores, but using
 * a normal distribution.
 * 
 * @author Erich Schubert
 */
public class RankingPseudoOutlierScaling implements OutlierScalingFunction {
  /**
   * The actual scores
   */
  private double[] scores;

  private boolean inverted = false;

  @Override
  public void prepare(DBIDs ids, OutlierResult or) {
    // collect all outlier scores
    scores = new double[ids.size()];
    int pos = 0;
    if (or.getOutlierMeta() instanceof InvertedOutlierScoreMeta) {
      inverted = true;
    }
    for(DBID id : ids) {
      scores[pos] = or.getScores().getValueFor(id);
      pos++;
    }
    if(pos != ids.size()) {
      throw new AbortException("Database size is incorrect!");
    }
    // sort them
    // TODO: Inverted scores!
    Arrays.sort(scores);
  }

  @Override
  public double getMax() {
    return 1.0;
  }

  @Override
  public double getMin() {
    return 0.0;
  }

  @Override
  public double getScaled(double value) {
    int pos = Arrays.binarySearch(scores, value);
    if(inverted) {
      return 1.0 - ((double) pos) / scores.length;
    }
    else {
      return ((double) pos) / scores.length;
    }
  }
}