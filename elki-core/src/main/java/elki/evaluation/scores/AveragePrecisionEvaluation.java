/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.evaluation.scores;

import elki.utilities.optionhandling.Parameterizer;

/**
 * Evaluate using average precision.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class AveragePrecisionEvaluation implements ScoreEvaluation {
  /**
   * Static instance
   */
  public static final AveragePrecisionEvaluation STATIC = new AveragePrecisionEvaluation();

  @Override
  public double evaluate(Adapter adapter) {
    int poscnt = 0, negcnt = 0, pospre = 0;
    double acc = 0.;
    while(adapter.valid()) {
      // positive or negative match?
      do {
        if(adapter.test()) {
          ++poscnt;
        }
        else {
          ++negcnt;
        }
        adapter.advance();
      } // Loop while tied:
      while(adapter.valid() && adapter.tiedToPrevious());
      // Add a new point.
      if(poscnt > pospre) {
        acc += (poscnt / (double) (poscnt + negcnt)) * (poscnt - pospre);
        pospre = poscnt;
      }
    }
    return adapter.numPositive() > 0 ? acc / adapter.numPositive() : 0.;
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
  public static class Par implements Parameterizer {
    @Override
    public AveragePrecisionEvaluation make() {
      return STATIC;
    }
  }
}
