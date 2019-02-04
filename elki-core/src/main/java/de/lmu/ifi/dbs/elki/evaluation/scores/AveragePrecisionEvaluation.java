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
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    int poscnt = 0, negcnt = 0, pospre = 0;
    double acc = 0.;
    while(iter.valid()) {
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++poscnt;
        }
        else {
          ++negcnt;
        }
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      // Add a new point.
      if(poscnt > pospre) {
        acc += (poscnt / (double) (poscnt + negcnt)) * (poscnt - pospre);
        pospre = poscnt;
      }
    }
    return (poscnt > 0) ? acc / poscnt : 0.;
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
    @Override
    protected AveragePrecisionEvaluation makeInstance() {
      return STATIC;
    }
  }
}
