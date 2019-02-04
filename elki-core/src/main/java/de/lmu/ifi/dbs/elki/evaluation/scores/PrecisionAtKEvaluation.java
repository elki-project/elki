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
package de.lmu.ifi.dbs.elki.evaluation.scores;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Evaluate using Precision@k, or R-precision (when {@code k=0}).
 * 
 * When {@code k=0}, then it is set to the number of positive objects, and the
 * returned value is the R-precision, or the precision-recall break-even-point
 * (BEP).
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PrecisionAtKEvaluation implements ScoreEvaluation {
  /**
   * Static instance
   */
  public static final PrecisionAtKEvaluation RPRECISION = new PrecisionAtKEvaluation(0);

  /**
   * Parameter k.
   */
  int k;

  /**
   * Constructor.
   *
   * @param k k to evaluate at. May be 0.
   */
  public PrecisionAtKEvaluation(int k) {
    this.k = k;
  }

  @Override
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    final int k = (this.k > 0) ? this.k : predicate.numPositive();
    int total = 0;
    double score = 0.;
    while(iter.valid() && total < k) {
      int posthis = 0, cntthis = 0;
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++posthis;
        }
        ++cntthis;
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      // Special tie computations only when we reach k.
      if(total + cntthis > k) {
        // p = posthis / cntthis chance of being positive
        // n = (k-total) draws.
        // expected value = n * p
        score += posthis / (double) cntthis * (k - total);
        total = k;
        break;
      }
      score += posthis;
      total += cntthis;
    }
    return score / total;
  }

  @Override
  public double expected(int pos, int all) {
    return pos / (double) all;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option ID for the k parameter.
     */
    public static final OptionID K_ID = new OptionID("precision.k", //
    "k value for precision@k. Can be set to 0, to get R-precision, or the precision-recall-break-even-point.");

    /**
     * K parameter
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kP = new IntParameter(K_ID) //
      .setDefaultValue(0) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected PrecisionAtKEvaluation makeInstance() {
      return k > 0 ? new PrecisionAtKEvaluation(k) : RPRECISION;
    }
  }
}
