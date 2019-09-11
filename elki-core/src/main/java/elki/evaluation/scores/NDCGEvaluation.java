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
package elki.evaluation.scores;

import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

import net.jafama.FastMath;

/**
 * Normalized Discounted Cumulative Gain.
 * <p>
 * This evaluation metric would be able to use relevance information, but the
 * current implementation is for binary labels only (it is easy to add, but
 * requires API additions or changes).
 * <p>
 * Reference:
 * <p>
 * K. Järvelin, J. Kekäläinen<br>
 * Cumulated gain-based evaluation of IR techniques<br>
 * ACM Transactions on Information Systems (TOIS)
 * <p>
 * TODO: support weighted ground truth.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "K. Järvelin, J. Kekäläinen", //
    title = "Cumulated gain-based evaluation of IR techniques", //
    booktitle = "ACM Transactions on Information Systems (TOIS)", //
    url = "https://doi.org/10.1145/582415.582418", //
    bibkey = "DBLP:journals/tois/JarvelinK02")
public class NDCGEvaluation implements ScoreEvaluation {
  /**
   * Static instance
   */
  public static final NDCGEvaluation STATIC = new NDCGEvaluation();

  @Override
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    return computeNDCG(predicate, iter);
  }

  @Override
  public double expected(int pos, int all) {
    // Expected value: every rank is positive with probability pos/all.
    final double rdcg = DCGEvaluation.sumInvLog1p(1, all) * pos / (double) all;
    // Optimum value:
    final double idcg = DCGEvaluation.sumInvLog1p(1, pos);
    return rdcg / idcg; // log(2) base would disappear
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
  public static <I extends ScoreIter> double computeNDCG(Predicate<? super I> predicate, I iter) {
    double sum = 0.;
    int i = 0, positive = 0, tied = 0, totalpos = 0;
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
        sum += tied == 1 ? 1. / FastMath.log(i + 1) : //
            DCGEvaluation.sumInvLog1p(i - tied + 1, i) * positive / (double) tied;
        totalpos += positive;
      }
      positive = 0;
      tied = 0;
    }
    // Optimum value:
    double idcg = DCGEvaluation.sumInvLog1p(1, totalpos);
    return sum / idcg; // log(2) base would disappear
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public NDCGEvaluation make() {
      return STATIC;
    }
  }
}
