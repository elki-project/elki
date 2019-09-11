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
package elki.utilities.scaling.outlier;

import elki.database.ids.DBIDs;
import elki.result.OrderingResult;
import elki.result.outlier.OutlierResult;
import elki.utilities.datastructures.QuickSelect;
import elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Outlier scaling function that only keeps the top k outliers.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class TopKOutlierScaling implements OutlierScaling {
  /**
   * Number of outliers to keep.
   */
  private int k = -1;

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
   * Constructor.
   * 
   * @param k
   * @param binary
   */
  public TopKOutlierScaling(int k, boolean binary) {
    super();
    this.k = k;
    this.binary = binary;
  }

  @Override
  public void prepare(OutlierResult or) {
    OrderingResult ordering = or.getOrdering();
    DBIDs ids = ordering.getDBIDs();
    cutoff = or.getScores().doubleValue(ordering.order(ids).iter().seek(Math.min(k, ids.size()) - 1));
    max = or.getOutlierMeta().getActualMaximum();
    ground = or.getOutlierMeta().getTheoreticalBaseline();
    // Fallback options:
    ground = Double.isInfinite(ground) || Double.isNaN(ground) ? or.getOutlierMeta().getTheoreticalMinimum() : ground;
    ground = Double.isInfinite(ground) || Double.isNaN(ground) ? or.getOutlierMeta().getActualMinimum() : ground;
  }

  @Override
  public <A> void prepare(A array, NumberArrayAdapter<?, A> adapter) {
    double[] scores = ArrayLikeUtil.toPrimitiveDoubleArray(array, adapter);
    cutoff = QuickSelect.quickSelect(scores, k - 1);
    max = Double.NEGATIVE_INFINITY;
    for(double v : scores) {
      max = Math.max(max, v);
    }
    ground = Math.min(0., cutoff);
  }

  @Override
  public double getMax() {
    return binary ? 1. : max;
  }

  @Override
  public double getMin() {
    return binary ? 0. : ground;
  }

  @Override
  public double getScaled(double value) {
    return binary ? value >= cutoff ? 1 : 0 : //
        value >= cutoff ? (value - ground) / (max - ground) : 0.;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the number of outliers to keep
     */
    public static final OptionID K_ID = new OptionID("topk.k", "Number of outliers to keep.");

    /**
     * Parameter to specify the lambda value
     */
    public static final OptionID BINARY_ID = new OptionID("topk.binary", "Make the top k a binary scaling.");

    /**
     * Number of outliers to keep.
     */
    private int k = -1;

    /**
     * Do a binary decision
     */
    private boolean binary = false;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> k = x);
      new Flag(BINARY_ID).grab(config, x -> binary = x);
    }

    @Override
    public TopKOutlierScaling make() {
      return new TopKOutlierScaling(k, binary);
    }
  }
}
