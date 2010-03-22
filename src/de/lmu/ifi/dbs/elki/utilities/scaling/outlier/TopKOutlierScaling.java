package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Outlier scaling function that only keeps the top k outliers.
 * 
 * @author Erich Schubert
 */
public class TopKOutlierScaling implements OutlierScalingFunction {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("topk.k", "Number of outliers to keep.");

  /**
   * Parameter to specify the number of outliers to keep
   * <p>
   * Key: {@code -topk.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));
  
  /**
   * Number of outliers to keep.
   */
  private int k = -1;
  
  /**
   * OptionID for {@link #BINARY_FLAG}
   */
  public static final OptionID BINARY_ID = OptionID.getOrCreateOptionID("topk.binary", "Make the top k a binary scaling.");

  /**
   * Parameter to specify the lambda value
   * <p>
   * Key: {@code -topk.binary}
   * </p>
   */
  private final Flag BINARY_FLAG = new Flag(BINARY_ID);
  
  /**
   * Do a binary decision
   */
  private boolean binary = false;
  
  /**
   * The value we cut off at.
   */
  private double cutoff;
  
  /**
   * The "ground" value
   */
  private double ground;
  
  /**
   * The maximum value
   */
  private double max;
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public TopKOutlierScaling(Parameterization config) {
    super();
    if (config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    if (config.grab(BINARY_FLAG)) {
      binary = BINARY_FLAG.getValue();
    }
  }

  @Override
  public void prepare(Database<?> db, @SuppressWarnings("unused") Result result, OutlierResult or) {
    if (k <= 0) {
      LoggingUtil.warning("No k configured for Top-k outlier scaling!");
    }
    IterableIterator<Integer> order = or.getOrdering().iter(db.getIDs());
    for (int i = 0; i < k; i++) {
      // stop if no more results.
      if (!order.hasNext()) {
        return;
      }
      Integer cur = order.next();
      cutoff = or.getScores().getValueFor(cur);
    }
    max = or.getOutlierMeta().getActualMaximum();
    ground = or.getOutlierMeta().getTheoreticalBaseline();
    if (Double.isInfinite(ground) || Double.isNaN(ground)) {
      ground = or.getOutlierMeta().getTheoreticalMinimum();
    }
    if (Double.isInfinite(ground) || Double.isNaN(ground)) {
      ground = or.getOutlierMeta().getActualMinimum();
    }
    if (Double.isInfinite(ground) || Double.isNaN(ground)) {
      ground = Math.min(0.0, cutoff);
    }
  }

  @Override
  public double getMax() {
    if (binary) {
      return 1.0;
    }
    return max;
  }

  @Override
  public double getMin() {
    if (binary) {
      return 0.0;
    }
    return ground;
  }

  @Override
  public double getScaled(double value) {
    if (binary) {
      if (value >= cutoff) {
        return 1;
      } else {
        return 0;
      }
    } else {
      if (value >= cutoff) {
        return (value - ground) / (max - ground);
      } else {
        return 0.0;
      }
    }
  }
}