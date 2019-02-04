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

import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Compute ROC (Receiver Operating Characteristics) curves.
 * 
 * A ROC curve compares the true positive rate (y-axis) and false positive rate
 * (x-axis).
 * 
 * It was first used in radio signal detection, but has since found widespread
 * use in information retrieval, in particular for evaluating binary
 * classification problems.
 * 
 * ROC curves are particularly useful to evaluate a ranking of objects with
 * respect to a binary classification problem: a random sampling will
 * approximately achieve a ROC value of 0.5, while a perfect separation will
 * achieve 1.0 (all positives first) or 0.0 (all negatives first). In most use
 * cases, a score significantly below 0.5 indicates that the algorithm result
 * has been used the wrong way, and should be used backwards.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - XYCurve
 */
public class ROCEvaluation implements ScoreEvaluation {
  /**
   * Static instance
   */
  public static final ROCEvaluation STATIC = new ROCEvaluation();

  @Override
  public <I extends ScoreIter> double evaluate(Predicate<? super I> predicate, I iter) {
    return computeROCAUC(predicate, iter);
  }

  /**
   * Compute a ROC curve given a set of positive IDs and a sorted list of
   * (comparable, ID)s, where the comparable object is used to decided when two
   * objects are interchangeable.
   * 
   * @param <I> Iterator type
   * @param predicate Predicate to test for positive objects
   * @param iter Iterator over results, with ties.
   * @return area under curve
   */
  public static <I extends ScoreIter> XYCurve materializeROC(Predicate<? super I> predicate, I iter) {
    int poscnt = 0, negcnt = 0;
    XYCurve curve = new XYCurve("False Positive Rate", "True Positive Rate");

    // start in bottom left
    curve.add(0.0, 0.0);

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
      curve.addAndSimplify(negcnt, poscnt);
    }
    // Ensure we end up in the top right corner.
    // Simplification will skip this if we already were.
    curve.addAndSimplify(negcnt, poscnt);
    curve.rescale(1. / negcnt, 1. / poscnt);
    return curve;
  }

  @Override
  public double expected(int pos, int all) {
    return .5;
  }

  /**
   * Compute the area under the ROC curve given a set of positive IDs and a
   * sorted list of (comparable, ID)s, where the comparable object is used to
   * decided when two objects are interchangeable.
   * 
   * @param <I> Iterator type
   * @param predicate Predicate to test for positive objects
   * @param iter Iterator over results, with ties.
   * @return area under curve
   */
  public static <I extends ScoreIter> double computeROCAUC(Predicate<? super I> predicate, I iter) {
    int poscnt = 0, negcnt = 0, pospre = 0, negpre = 0;
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
      if(negcnt > negpre) {
        acc += (poscnt + pospre) * .5 * (negcnt - negpre);
        negpre = negcnt;
      }
      pospre = poscnt;
    }
    acc /= negcnt * (long) poscnt;
    return acc == acc ? acc : 0.5; /* Detect NaN */
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ROCEvaluation makeInstance() {
      return STATIC;
    }
  }
}
