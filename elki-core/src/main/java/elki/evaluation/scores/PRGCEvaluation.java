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

import java.util.LinkedList;

import elki.math.geometry.XYCurve;
import elki.result.Metadata;
import elki.utilities.optionhandling.Parameterizer;
/**
 * Compute the area under the precision-recall-gain curve
 * <p>
 * References:
 * <p>
 * P. Flach and M. Knull<br>
 * Precision-Recall-Gain Curves: PR Analysis Done Right<br>
 * https://papers.nips.cc/paper/5867-precision-recall-gain-curves-pr-analysis-done-right
 * 
 * @author Robert Gehde
 *
 */
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
    int amountPositiveIDs = adapter.numPositive();
    int posnotfound = amountPositiveIDs;
    double pi = amountPositiveIDs / (double) adapter.numTotal();
    LinkedList<Double> slices = new LinkedList<Double>();

    while(adapter.valid()) {
      final int prevpos = pos, prevrank = rank;
      final int prevposnotfound = posnotfound;
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
      
      final int newpos = pos - prevpos, ties = rank - prevrank;
      posnotfound -= newpos;
      // Interpolation based on Davis and Goadrich (class: AUPRCEval..)
      double f = newpos / (double) ties;
      // pos == 0 means that auc is infinite and not point is printed anyways
      if(pos == 0) {
        continue;
      }
      double recG = 1 - (pi / (1 - pi)) * (posnotfound / (double) pos);
      double preG = 1 - (pi / (1 - pi)) * ((rank - pos) / (double) pos);
      if(prevpos == 0) {
        if(recG >= 0) {
          slices.add(recG * preG);
        }
        if(/*preG >= 0 &&*/ recG >= 0) {
          curve.addAndSimplify(0, preG);
          curve.addAndSimplify(recG, preG);
        }
        continue;
      }
      
      assert (prevpos + prevposnotfound == amountPositiveIDs);
      
      double recGp = 1 - (pi / (1 - pi)) * (prevposnotfound / (double) prevpos);
      double preGp = 1 - (pi / (1 - pi)) * ((prevrank - prevpos) / (double) prevpos);

      for(int i = 1; i <= ties; i++) {
        double tpipol = prevpos + f * i;
        double fnipol = prevposnotfound - f * i;

        recG = 1 - (pi / (1 - pi)) * (fnipol / (double) tpipol);
        preG = 1 - (pi / (1 - pi)) * (((prevrank + i) - tpipol) / (double) tpipol);
        // the boundary for auc calculation was taken from the original
        // implementation
        if(recGp >= 0) { // this also assures recG > 0
          double width = recG - recGp;
          double height = (preG + preGp) / 2;
          slices.add(width * height);
          // if(preG >=0) {
          curve.addAndSimplify(recG, preG);
          // }
        }
        recGp = recG;
        preGp = preG;
      }
    }
    curve.setAxes(0, 0, 1, 1);
    double acc = .0;
    for(int i = 0; i < slices.size(); i++) {
      acc += slices.get(i);
    }
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

    PRGCurve curve = new PRGCurve();
    int pos = 0, rank = 0;
    int amountPositiveIDs = adapter.numPositive();
    int posnotfound = amountPositiveIDs;
    double pi = amountPositiveIDs / (double) adapter.numTotal();
    LinkedList<Double> slices = new LinkedList<Double>();

    while(adapter.valid()) {
      final int prevpos = pos, prevrank = rank;
      final int prevposnotfound = posnotfound;
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
      
      final int newpos = pos - prevpos, ties = rank - prevrank;
      posnotfound -= newpos;
      // Interpolation based on Davis and Goadrich (class: AUPRCEval..)
      double f = newpos / (double) ties;
      // pos == 0 means that auc is infinite and not point is printed anyways
      if(pos == 0) {
        continue;
      }
      double recG = 1 - (pi / (1 - pi)) * (posnotfound / (double) pos);
      double preG = 1 - (pi / (1 - pi)) * ((rank - pos) / (double) pos);
      if(prevpos == 0) {
        if(recG >= 0) {
          slices.add(recG * preG);
        }
        continue;
      }
      
      assert (prevpos + prevposnotfound == amountPositiveIDs);
      
      double recGp = 1 - (pi / (1 - pi)) * (prevposnotfound / (double) prevpos);
      double preGp = 1 - (pi / (1 - pi)) * ((prevrank - prevpos) / (double) prevpos);

      for(int i = 1; i <= ties; i++) {
        double tpipol = prevpos + f * i;
        double fnipol = prevposnotfound - f * i;

        recG = 1 - (pi / (1 - pi)) * (fnipol / (double) tpipol);
        preG = 1 - (pi / (1 - pi)) * (((prevrank + i) - tpipol) / (double) tpipol);
        // the boundary for auc calculation was taken from the original
        // implementation
        if(recGp >= 0) { // this also assures recG > 0
          double width = recG - recGp;
          double height = (preG + preGp) / 2;
          slices.add(width * height);
        }
        recGp = recG;
        preGp = preG;
      }
    }
    curve.setAxes(0, 0, 1, 1);
    double acc = .0;
    for(int i = 0; i < slices.size(); i++) {
      acc += slices.get(i);
    }
    return acc;
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
