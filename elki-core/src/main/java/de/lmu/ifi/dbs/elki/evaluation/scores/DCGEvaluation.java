/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

import net.jafama.FastMath;

/**
 * Discounted Cumulative Gain.
 *
 * This evaluation metric would be able to use relevance information, but the
 * current implementation is for binary labels only (it is easy to add, but
 * requires API additions or changes).
 * 
 * Reference:
 * <p>
 * K. Järvelin, J. Kekäläinen<br />
 * Cumulated gain-based evaluation of IR techniques<br />
 * ACM Transactions on Information Systems (TOIS)
 * </p>
 *
 * TODO: support weighted ground truth.
 * 
 * TODO: allow using other logarithms than 2?
 *
 * @author Erich Schubert
 */
@Reference(authors = "K. Järvelin, J. Kekäläinen", //
    title = "Cumulated gain-based evaluation of IR techniques", //
    booktitle = "ACM Transactions on Information Systems (TOIS)", //
    url = "http://dx.doi.org/10.1145/582415.582418")
public class DCGEvaluation implements ScoreEvaluation {
  /**
   * Static instance
   */
  public static final DCGEvaluation STATIC = new DCGEvaluation();

  @Override
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    return computeDCG(predicate, iter);
  }

  @Override
  public double expected(int pos, int all) {
    // Average rank (1..n) = .5 * (all + 1) = .5 * all + .5
    // 1 / log2(r+1) = log(2) / log(r+1)
    return pos * MathUtil.LOG2 / FastMath.log(.5 * all + 1.5);
  }

  /**
   * Compute the DCG given a set of positive IDs and a sorted list of entries,
   * which may include ties.
   * 
   * @param <I> Iterator type
   * @param predicate Predicate to test for positive objects
   * @param iter Iterator over results, with ties.
   * @return area under curve
   */
  public static <I extends ScoreIter> double computeDCG(Predicate<? super I> predicate, I iter) {
    double sum = 0.;
    int i = 0, positive = 0, tied = 0;
    while(iter.valid()) {
      // positive or negative match?
      do {
        if(predicate.test(iter)) {
          ++positive;
        }
        ++tied;
        ++i;
        iter.advance();
      } // Loop while tied:
      while(iter.valid() && iter.tiedToPrevious());
      // We only support binary labeling, and can ignore negative weight.
      if(positive > 0) {
        final double avgrank = i - .5 * (tied - 1);
        sum += positive / FastMath.log(avgrank + 1);
      }
      positive = 0;
      tied = 0;
    }
    return sum * MathUtil.LOG2; // Change base to log 2.
  }

  /**
   * Maximum DCG.
   *
   * @param pos Number of positive objects
   * @return Max
   */
  public static double maximum(int pos) {
    double sum = 0.;
    // TODO: closed form?
    for(int i = 0; i < pos; i++) {
      sum += 1 / FastMath.log(i + 2);
    }
    return sum * MathUtil.LOG2;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected DCGEvaluation makeInstance() {
      return STATIC;
    }
  }
}
