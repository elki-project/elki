/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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
    return computePRGAURC(adapter);
  }

  /**
   * Compute the PRG curve given a predicate function and an iterator.
   *
   * @param adapter Adapter for different input data types
   * @return PRG curve
   */
  public static PRGCurve materializePRGC(Adapter adapter) {
    PRGCurve curve = new PRGCurve();
    int pos = 0, rank = 0;
    double recG = .0, preG = .0;
    int amountPositiveIDs = adapter.numPositive();
    int posnotfound = amountPositiveIDs;
    boolean recallPositive = false;
    double pi = amountPositiveIDs / (double) adapter.numTotal();
    double acc = .0;

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
      if(rank == prevrank) {
        continue;
      }

      final int newpos = pos - prevpos;
      posnotfound -= newpos;
      // pos == 0 means that auc is infinite and not point is printed anyways
      if(pos == 0) {
        continue;
      }
      recG = 1 - (pi / (1 - pi)) * (posnotfound / (double) pos);
      preG = 1 - (pi / (1 - pi)) * ((rank - pos) / (double) pos);

      // detect first positive incident
      if(!recallPositive && recG >= 0) {
        // check if the new point is directly on 0
        if(recG == 0) {
          // this creates no slice
          curve.addAndSimplify(recG, preG);
          recallPositive = true;
          continue;
        }
        // calculate a position for recG = 0
        // this seems to not yield recG = 0 but something close.

        // taken from the original implementation (see citation)
        double alpha = .5;
        if(newpos > 0) {
          alpha = (amountPositiveIDs * pi - prevpos) / newpos;
        }
        // new = pre + delta * alpha
        double ttp = prevpos + alpha * newpos;
        double tfp = (prevrank - prevpos) + alpha * (prevrank - prevpos);
        prevpreG = 1. - (amountPositiveIDs / (double) (adapter.numTotal() - amountPositiveIDs)) * (tfp / ttp);
        prevrecG = 0.;

        curve.addAndSimplify(prevrecG, prevpreG);
        curve.addAndSimplify(recG, preG);
        acc += (calcSliceArea(prevrecG, recG, prevpreG, preG));

        recallPositive = true;
        continue;
      }
      // we can ignore everything in the negative recall area
      if(!recallPositive) {
        continue;
      }
      // check trailing point
      if(rank == adapter.numTotal()) {
        // fix possible floating point errors
        recG = 1.;
        preG = 0.;
      }

      // the original implementation adds y = 0 crosses as well
      // also taken from original impl.
      if(preG * prevpreG < 0) {
        double x = prevrecG + (-prevpreG) / (preG - prevpreG) * (recG - prevrecG);
        double alpha = .5;
        if(newpos > 0) {
          alpha = (amountPositiveIDs * amountPositiveIDs / (adapter.numTotal() - (adapter.numTotal() - amountPositiveIDs) * x) - prevpos) / newpos;
        }
        else {
          alpha = ((adapter.numTotal() - amountPositiveIDs) / (adapter.numTotal() - amountPositiveIDs) * prevpos - (prevrank - prevpos)) / ((rank - prevrank) - newpos);
        }
        // new = pre + delta*alhpa
        double ttp = prevpos + alpha * newpos;
        double tfn = amountPositiveIDs - ttp;
        double temprecG = 1. - (amountPositiveIDs / (double) (adapter.numTotal() - amountPositiveIDs)) * (tfn / ttp);

        curve.addAndSimplify(temprecG, 0);
        acc += calcSliceArea(prevrecG, temprecG, prevpreG, 0);

        prevpreG = 0;
        prevrecG = temprecG;
      }
      // add the new point
      curve.addAndSimplify(recG, preG);
      // add the slice
      acc += (calcSliceArea(prevrecG, recG, prevpreG, preG));
    }
    curve.setAxes(0, 0, 1, 1);
    curve.auc = acc;
    return curve;
  }

  /**
   * Compute the area under the PRG curve given a set of positive IDs and a
   * sorted list of (comparable, ID)s, where the comparable object is used to
   * decided when two objects are interchangeable.
   * 
   * @param adapter Adapter for different input data types
   * @return area under curve
   */
  private static double computePRGAURC(Adapter adapter) {
    int pos = 0, rank = 0;
    double recG = .0, preG = .0;
    int amountPositiveIDs = adapter.numPositive();
    int posnotfound = amountPositiveIDs;
    boolean recallPositive = false;
    double pi = amountPositiveIDs / (double) adapter.numTotal();
    double acc = .0;

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
      if(rank == prevrank) {
        continue;
      }

      final int newpos = pos - prevpos;
      posnotfound -= newpos;
      // pos == 0 means that auc is infinite and not point is printed anyways
      if(pos == 0) {
        continue;
      }
      recG = 1 - (pi / (1 - pi)) * (posnotfound / (double) pos);
      preG = 1 - (pi / (1 - pi)) * ((rank - pos) / (double) pos);

      // detect first positive incident
      if(!recallPositive && recG >= 0) {
        // check if the new point is directly on 0
        if(recG == 0) {
          // this creates no slice
          recallPositive = true;
          continue;
        }
        // calculate a position for recG = 0
        // this seems to not yield recG = 0 but something close.

        // taken from the original implementation (see citation)
        double alpha = .5;
        if(newpos > 0) {
          alpha = (amountPositiveIDs * pi - prevpos) / newpos;
        }
        // new = pre + delta*alhpa
        double ttp = prevpos + alpha * newpos;
        double tfp = (prevrank - prevpos) + alpha * (prevrank - prevpos);
        prevpreG = 1. - (amountPositiveIDs / (double) (adapter.numTotal() - amountPositiveIDs)) * (tfp / ttp);
        prevrecG = 0.;

        acc += (calcSliceArea(prevrecG, recG, prevpreG, preG));

        recallPositive = true;
        continue;
      }
      // we can ignore everything in the negative recall area
      if(!recallPositive) {
        continue;
      }
      // check trailing point
      if(rank == adapter.numTotal()) {
        // fix possible floating point errors
        recG = 1.;
        preG = 0.;
      }

      // the original implementation adds y = 0 crosses as well
      // add the slice
      acc += (calcSliceArea(prevrecG, recG, prevpreG, preG));
    }
    return acc;
  }

  private static double calcSliceArea(double prevRG, double rG, double prevPG, double pG) {
    return (rG - prevRG) * (prevPG + pG) * 0.5;
  }

  @Override
  public double expected(int pos, int all) {
    return .0;
  }

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
