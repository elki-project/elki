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

/**
 * Compute the area under the precision-recall-gain curve
 * <p>
 * References:
 * <p>
 * P. Flach and M. Knull<br>
 * Precision-Recall-Gain Curves: PR Analysis Done Right<br>
 * Neural Information Processing Systems (NIPS 2015)
 *
 * @author Robert Gehde
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "P. Flach and M. Knull", //
    title = "Precision-Recall-Gain Curves: {PR} Analysis Done Right", //
    booktitle = "Neural Information Processing Systems (NIPS 2015)", //
    url = "http://papers.nips.cc/paper/5867-precision-recall-gain-curves-pr-analysis-done-right", //
    bibkey = "DBLP:conf/nips/FlachK15")
public class PRGCEvaluation implements ScoreEvaluation {
  /**
   * Static instance
   */
  public static final PRGCEvaluation STATIC = new PRGCEvaluation();

  @Override
  public double evaluate(Adapter adapter) {
    return computePRGAUC(adapter, null);
  }

  /**
   * Compute the PRG curve given a predicate function and an iterator.
   *
   * @param adapter Adapter for different input data types
   * @return PRG curve
   */
  public static PRGCurve materializePRGC(Adapter adapter) {
    PRGCurve curve = new PRGCurve();
    computePRGAUC(adapter, curve);
    return curve;
  }

  /**
   * Compute the precision-recall-gain-curve
   *
   * @param adapter Input adapter
   * @param curve Optional output curve (may be {@code null})
   * @return AUC value
   */
  private static double computePRGAUC(Adapter adapter, PRGCurve curve) {
    int pos = 0, rank = 0;
    double recG = Double.NEGATIVE_INFINITY, preG = Double.NEGATIVE_INFINITY;
    final int numpos = adapter.numPositive();
    final double pi = numpos / (double) adapter.numTotal();
    final double odds = pi / (1. - pi);
    double acc = 0.;

    while(adapter.valid()) {
      final int prevpos = pos, prevrank = rank;
      double prevpreG = preG, prevrecG = recG;
      // positive or negative match?
      do {
        if(adapter.test()) {
          ++pos;
        }
        ++rank;
        adapter.advance();
      } // Loop while tied:
      while(adapter.valid() && adapter.tiedToPrevious());
      // For pos == 0, the recall gain is minus infinity
      if(pos == 0) {
        continue;
      }
      recG = 1 - odds * (numpos - pos) / (double) pos;
      preG = 1 - odds * (rank - pos) / (double) pos;
      // we can ignore everything in the negative recall area
      if(recG < 0) {
        continue;
      }
      // last value, avoid slight numerical difference from 0:
      if(!adapter.valid()) {
        recG = 1;
        preG = 0;
      }

      final int newpos = pos - prevpos, newneg = (rank - prevrank) - newpos;
      if(prevrecG < 0 && recG > 0) {
        // interpolate the position for recG = 0
        // based on the original implementation
        double alpha = newpos > 0 ? (numpos * pi - prevpos) / newpos : .5;
        prevpreG = 1 - odds * ((prevrank - prevpos) + alpha * newneg) / (prevpos + alpha * newpos);
        prevrecG = 0;
        if(curve != null) {
          curve.addAndSimplify(prevrecG, prevpreG);
        }
      }
      if(curve != null) {
        curve.addAndSimplify(recG, preG);
      }
      if(recG > 0) {
        acc += (recG - prevrecG) * (prevpreG + preG) * 0.5;
      }
    }
    if(curve != null) {
      curve.setAxes(0, 0, 1, 1);
      curve.setDrawingBounds(0, 0, 1, 1);
      curve.auc = acc;
    }
    return acc;
  }

  @Override
  public double expected(int pos, int all) {
    return 0.;
  }

  /**
   * Precision-Recall-Gain curve.
   *
   * @author Robert Gehde
   */
  public static class PRGCurve extends XYCurve {
    /**
     * Area under the curve cache.
     */
    private double auc = Double.NaN;

    /**
     * Constructor.
     */
    public PRGCurve() {
      super("Recall-Gain", "Precision-Gain");
      Metadata.of(this).setLongName("Precision-Recall-Gain-Curve");
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
   * @author Robert Gehde
   */
  public static class Par implements Parameterizer {
    @Override
    public PRGCEvaluation make() {
      return STATIC;
    }
  }
}
