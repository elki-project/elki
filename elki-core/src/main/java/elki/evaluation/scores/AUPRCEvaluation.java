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
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

import net.jafama.FastMath;

/**
 * Compute the area under the precision-recall curve (AUPRC).
 * <p>
 * References:
 * <p>
 * J. Davis and M. Goadrich<br>
 * The relationship between Precision-Recall and ROC curves<br>
 * Proc. 23rd Int. Conf. Machine Learning (ICML)
 * <p>
 * T. Saito and M. Rehmsmeier<br>
 * The Precision-Recall Plot is More Informative than the ROC Plot When
 * Evaluating Binary Classifiers on Imbalanced Datasets<br>
 * PLoS ONE 10(3)
 * 
 * @author Erich Schubert
 * @since 0.8.0
 * 
 * @has - - - XYCurve
 */
@Reference(authors = "J. Davis and M. Goadrich", //
    title = "The relationship between Precision-Recall and ROC curves", //
    booktitle = "Proc. 23rd Int. Conf. Machine Learning (ICML)", //
    url = "https://doi.org/10.1145/1143844.1143874", //
    bibkey = "DBLP:conf/icml/DavisG06")
@Reference(authors = "T. Saito and M. Rehmsmeier", //
    title = "The Precision-Recall Plot is More Informative than the ROC Plot When Evaluating Binary Classifiers on Imbalanced Datasets", //
    booktitle = "PLoS ONE 10(3)", //
    url = "https://doi.org/10.1371/journal.pone.0118432", //
    bibkey = "doi:10.1371/journal.pone.0118432")
public class AUPRCEvaluation implements ScoreEvaluation {
  /**
   * Static instance
   */
  public static final AUPRCEvaluation STATIC = new AUPRCEvaluation();

  @Override
  public double evaluate(Adapter adapter) {
    return computeAUPRC(adapter);
  }

  /**
   * Compute the PR curve given a predicate function and an iterator.
   *
   * @param adapter Adapter for different input data types
   * @return area under curve
   */
  public static PRCurve materializePRC(Adapter adapter) {
    PRCurve curve = new PRCurve();
    int pos = 0, rank = 0;
    double acc = 0;
    while(adapter.valid()) {
      final int prevpos = pos, prevrank = rank;
      // positive or negative match?
      do {
        if(adapter.test()) {
          ++pos;
        }
        ++rank;
        adapter.advance();
      } // Loop while tied:
      while(adapter.valid() && adapter.tiedToPrevious());
      if(pos == prevpos) {
        curve.addAndSimplify(pos, pos / (double) rank);
        continue;
      }
      final int newpos = pos - prevpos, ties = rank - prevrank;
      // Interpolation based on Davis and Goadrich
      double f = newpos / (double) ties;
      // Starting point for curve that may otherwise be undefined:
      if(prevrank == 0) {
        curve.addAndSimplify(0, f);
        acc = newpos / (double) ties * newpos;
        continue;
      }
      for(int i = 1; i <= ties; i++) {
        double ipol = prevpos + f * i;
        curve.addAndSimplify(ipol, ipol / (double) (prevrank + i));
      }
      final double l = FastMath.log(rank) - FastMath.log(prevrank);
      double integral = l * prevpos / (double) newpos - (l * prevrank / (double) ties - 1);
      acc += integral / (double) ties * newpos;
    }
    curve.rescale(1 / (double) pos, 1);
    curve.setAxes(0, 0, 1, 1);
    curve.auc = acc / pos;
    return curve;
  }

  /**
   * Compute the area under the PR curve given a set of positive IDs and a
   * sorted list of (comparable, ID)s, where the comparable object is used to
   * decided when two objects are interchangeable.
   * 
   * @param adapter Adapter for different input data types
   * @return area under curve
   */
  public static double computeAUPRC(Adapter adapter) {
    double acc = 0;
    int pos = 0, rank = 0;
    while(adapter.valid()) {
      final int prevpos = pos, prevrank = rank;
      // positive or negative match?
      do {
        if(adapter.test()) {
          ++pos;
        }
        ++rank;
        adapter.advance();
      } // Loop while tied:
      while(adapter.valid() && adapter.tiedToPrevious());
      if (pos == prevpos) {
        continue;
      }
      final int newpos = pos - prevpos, ties = rank - prevrank;
      // Interpolation based on Davis and Goadrich
      // Starting point for curve that may otherwise be undefined:
      if(prevrank == 0) {
        acc = newpos / (double) ties * newpos;
        continue;
      }
      final double l = FastMath.log(rank) - FastMath.log(prevrank);
      double integral = l * prevpos / (double) newpos - (l * prevrank / (double) ties - 1);
      acc += integral / (double) ties * newpos;
    }
    return acc / pos;
  }

  @Override
  public double expected(int pos, int all) {
    return pos / (double) all;
  }

  /**
   * ROC Curve
   *
   * @author Erich Schubert
   */
  public static class PRCurve extends XYCurve {
    /**
     * Area under the curve cache.
     */
    private double auc = Double.NaN;

    /**
     * Constructor.
     */
    public PRCurve() {
      super("Recall", "Precision");
      Metadata.of(this).setLongName("Precision-Recall-Curve");
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
    public AUPRCEvaluation make() {
      return STATIC;
    }
  }
}
