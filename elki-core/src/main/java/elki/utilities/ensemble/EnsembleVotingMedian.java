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
package elki.utilities.ensemble;

import elki.utilities.datastructures.QuickSelect;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Simple combination rule, by taking the median.
 * 
 * Note: median is very similar to a <em>majority voting</em>!
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public class EnsembleVotingMedian implements EnsembleVoting {
  /**
   * Quantile to use
   */
  private double quantile = 0.5;

  /**
   * Constructor.
   * 
   * @param quantile Quantile
   */
  public EnsembleVotingMedian(double quantile) {
    this.quantile = quantile;
  }

  @Override
  public double combine(double[] scores) {
    return combine(scores, scores.length);
  }

  @Override
  public double combine(double[] scores, int count) {
    return QuickSelect.quantile(scores, 0, count, quantile);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option ID for the quantile
     */
    public static final OptionID QUANTILE_ID = new OptionID("ensemble.median.quantile", "Quantile to use in median voting.");

    /**
     * Quantile to use
     */
    private double quantile = 0.5;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(QUANTILE_ID, .5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> quantile = x);
    }

    @Override
    public EnsembleVotingMedian make() {
      return new EnsembleVotingMedian(quantile);
    }
  }
}
