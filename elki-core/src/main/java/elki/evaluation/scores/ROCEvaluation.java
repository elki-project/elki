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

import elki.math.geometry.XYCurve;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Compute ROC (Receiver Operating Characteristics) curves.
 * <p>
 * A ROC curve compares the true positive rate (y-axis) and false positive rate
 * (x-axis).
 * <p>
 * It was first used in radio signal detection, but has since found widespread
 * use in information retrieval, in particular for evaluating binary
 * classification problems.
 * <p>
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
  public double evaluate(Adapter adapter) {
    return computeAUROC(adapter);
  }

  /**
   * Compute a ROC curve given a set of positive IDs and a sorted list of
   * (comparable, ID)s, where the comparable object is used to decided when two
   * objects are interchangeable.
   * 
   * @param adapter Adapter for different input data types
   * @return area under curve
   */
  public static ROCurve materializeROC(Adapter adapter) {
    ROCurve curve = new ROCurve();
    double acc = 0.;
    int poscnt = 0, negcnt = 0;
    curve.add(0.0, 0.0);
    while(adapter.valid()) {
      final int pospre = poscnt, negpre = negcnt;
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
      // Add a new point, update AUC.
      curve.addAndSimplify(negcnt, poscnt);
      acc += negcnt > negpre ? (poscnt + pospre) * .5 * (negcnt - negpre) : 0;
    }
    // Ensure we end up in the top right corner.
    // Simplification will skip this if we already were.
    curve.addAndSimplify(negcnt, poscnt);
    curve.rescale(1. / negcnt, 1. / adapter.numPositive());
    curve.setAxes(0, 0, 1, 1);
    acc /= negcnt * (long) poscnt;
    curve.auc = Double.isNaN(acc) ? 0.5 : acc;
    return curve;
  }

  /**
   * Compute the area under the ROC curve given a set of positive IDs and a
   * sorted list of (comparable, ID)s, where the comparable object is used to
   * decided when two objects are interchangeable.
   * 
   * @param adapter Adapter for different input data types
   * @return area under curve
   */
  public static double computeAUROC(Adapter adapter) {
    int poscnt = 0, negcnt = 0;
    double acc = 0.;
    while(adapter.valid()) {
      final int pospre = poscnt, negpre = negcnt;
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
      if(negcnt > negpre) {
        acc += (poscnt + pospre) * .5 * (negcnt - negpre);
      }
    }
    acc /= negcnt * (long) poscnt;
    return Double.isNaN(acc) ? 0.5 : acc; /* Detect NaN */
  }

  @Override
  public double expected(int pos, int all) {
    return .5;
  }

  /**
   * ROC Curve
   *
   * @author Erich Schubert
   */
  public static class ROCurve extends XYCurve {
    /**
     * Area under the curve cache.
     */
    private double auc = Double.NaN;

    /**
     * Constructor.
     */
    public ROCurve() {
      super("False Positive Rate", "True Positive Rate");
    }

    /**
     * @return area under the curve.
     */
    public double getAUC() {
      return auc;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public ROCEvaluation make() {
      return STATIC;
    }
  }
}
