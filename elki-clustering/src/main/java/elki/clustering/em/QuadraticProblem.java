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
package elki.clustering.em;

import static elki.math.linearalgebra.VMath.times;
import static elki.math.linearalgebra.VMath.scalarProduct;
import static elki.math.linearalgebra.VMath.transposeTimesTimes;

import java.util.Arrays;

import elki.clustering.em.KDTree.Boundingbox;
import elki.math.MathUtil;
import elki.math.linearalgebra.CholeskyDecomposition;
import elki.utilities.pairs.DoubleDoublePair;
import elki.utilities.pairs.IntIntPair;

import net.jafama.FastMath;

public class QuadraticProblem {

  /**
   * maximum value
   */
  public double maximumValue;

  /**
   * point at maximum value
   */
  public double[] argmaxPoint;

  private ProblemData[] cache;

  /**
   * possibility to ignore point calculation, currently not supported in KDTree
   */
  boolean calcPoint = true;

  public QuadraticProblem() {
    reinit();
  }

  /**
   * Calculate 0.5x^tax + b + c
   * we use it in KDTrees with b = (0)^d and c = 0. If you use it like this you
   * can multiply the result with 2 to get x^tax + b + c.
   * Constructor.
   *
   * @param a
   * @param b
   * @param c
   * @param box
   */
  public QuadraticProblem(double[][] a, double[] b, double c, Boundingbox box, ProblemData[] arrayCache) {
    reinit();
    this.cache = arrayCache;
    AttributeState[] attTypes = cache[b.length - 1].attTypes;
    Arrays.fill(attTypes, AttributeState.CONSTR);
    argmaxPoint = new double[b.length];
    maximumValue = evaluateConstrainedQuadraticFunction(a, b, c, box, attTypes, true, argmaxPoint, maximumValue);
  }

  public void reinit() {
    maximumValue = Double.NEGATIVE_INFINITY;
    argmaxPoint = null;
  }

  /**
   * find the first attribute where the partial derivative wrt that attribute is
   * nonnegative or nonpositive. If the derivative is nonnegative (lo >=0) it
   * will be driven to the upper limit and vice versa.
   * 
   * This does NOT mean that the non-found attributes are not driven to a limit.
   * This is just an implication, not an equivalence
   * 
   * @param a
   * @param b
   * @param hyperboundingbox
   * @return
   */
  private IntIntPair find_known_lo_or_hi_att_num(double[][] a, double[] b, Boundingbox hyperboundingbox) {
    for(int i = 0; i < b.length; i++) {
      DoubleDoublePair limits = calculateLinearDerivativeLimits(a, b, hyperboundingbox, i);

      // if hi < 0 or lo > 0 -> hit
      if(limits.first >= 0.0 && limits.second <= 0.0) {
        return new IntIntPair(i, limits.first >= 0.0 ? 1 : 0);
        // return pair(i, lo=0 || hi=1)
      }
    }
    return new IntIntPair(-1, -1);// none found
  }

  /**
   * calculates the min and max value of a linear derivative of a quadratic
   * function in the hyperboundingbox.
   * The function is in the form 0.5ax^2 + bx + c
   * 
   * @param ak gradient/slope
   * @param bk y axis crossing
   * @param hyperboundingbox limitation of min max search
   * @return
   */
  private DoubleDoublePair calculateLinearDerivativeLimits(double[][] a, double[] b, Boundingbox hyperboundingbox, int dim) {
    double bk = b[dim];
    // initialize with value at 0^k
    double min = bk;
    double max = bk;
    // then for each attribute add the y-difference to the bounds to the
    // according limits
    for(int i = 0; i < b.length; i++) {
      // get slope of the derivative, is a[dim][i]
      double slope = a[dim][i];
      double upperBound = hyperboundingbox.getUpperBound(i);
      double lowerBound = hyperboundingbox.getLowerBound(i);
      if(slope < 0) {
        min += upperBound * slope;
        max += lowerBound * slope;
      }
      else {
        max += upperBound * slope;
        min += lowerBound * slope;
      }
    }
    return new DoubleDoublePair(min, max);
  }

  private double computeMaximumPossibleFuncValue(double[][] a, double[] b, double c, Boundingbox boundingbox, DoubleDoublePair[] limits) {
    double[] upper = boundingbox.getUpperBounds();
    double[] lower = boundingbox.getLowerBounds();
    double[] middle = boundingbox.getMidPoint();

    double fm = evalueateQuadraticFormula(a, b, c, middle);

    for(int i = 0; i < middle.length; i++) {
      double halfdist = (upper[i] - lower[i]) / 2.0;
      DoubleDoublePair derlim = limits[i];
      fm += halfdist * FastMath.max(-derlim.first, derlim.second);
    }
    return fm;
  }

  /**
   * main recursive function
   * we calculate 0.5 x^tax + bx +c
   * it should only update the resultarray if we have a better result than
   * before
   * 
   * @param a
   * @param b
   * @param c
   * @param bounds
   * @param attributeTypes
   * @param cutoffCheck
   */
  private double evaluateConstrainedQuadraticFunction(double[][] a, double[] b, double c, Boundingbox bounds, AttributeState[] attributeTypes, boolean cutoffCheck, double[] result, double resultValue) {
    // case for one attribute
    if(attributeTypes.length == 1) {
      double res1d = evaluateConstrainedQuadraticFunction1D(a[0][0], b[0], c, bounds.getLowerBound(0), bounds.getUpperBound(0), result, resultValue);
      assert res1d >= resultValue;
      return res1d; // 1 dimensional solution
    }

    // find first constrained attribute
    int constrAtt = -1;
    for(int i = 0; i < attributeTypes.length; i++) {
      if(attributeTypes[i] == AttributeState.CONSTR) {
        constrAtt = i;
        break;
      }
    }
    // check if we have no constrained attribute -> we worked all attributes
    // then we just check if the maximum is inside the given bounds
    if(constrAtt < 0) {
      double[] opt = findMaximumWithfunctionValue(a, b);
      if(opt == null) {
        return resultValue; // no optimum with constraints
      }
      if(bounds.weaklyInsideBounds(opt)) {
        double optValue = evalueateQuadraticFormula(a, b, c, opt);
        if(optValue > resultValue) {
          if(calcPoint) {
            assert opt.length == result.length;
            System.arraycopy(opt, 0, result, 0, opt.length);
          }
          return optValue; // better optimum with constraints
        }
      }
    }
    else { // we found a constrained attribute
      if(cutoffCheck) {
        double[] opt = findMaximumWithfunctionValue(a, b);
        if(opt != null) {
          double optValue = evalueateQuadraticFormula(a, b, c, opt);
          if(bounds.weaklyInsideBounds(opt)) {
            if(optValue > resultValue) {
              // copy result and return
              if(calcPoint) {
                assert result.length == opt.length;
                System.arraycopy(opt, 0, result, 0, opt.length);
              }
              resultValue = optValue;
            }
            return resultValue; // found value in cutoffcheck
            // no point in checking children, because we found a value in bounds
            // and will not get anything better
          }
          else {
            if(resultValue > optValue) {
              return resultValue; // no better solution better in this
                                  // sub-call-tree
              // even though the value is outside of bounds, that value still
              // will only drop in children, so its not worth checking any
              // further
            }
          }
        }
        // At this point opt is either null or outside bounds and we can still
        // get a good value
        IntIntPair drivenAttribute = find_known_lo_or_hi_att_num(a, b, bounds);
        if(drivenAttribute.first >= 0) {
          // we found a value that can be driven to lo or hi
          double redVal = startReducedProblem(a, b, c, bounds, attributeTypes, drivenAttribute.first, (drivenAttribute.second == 1 ? AttributeState.UPLIM : AttributeState.LOLIM), result, resultValue);
          assert redVal >= resultValue;
          return redVal;
        }
        else {
          // if we cannot find such a value, make sure we can still find a good
          // value in the given bounds

          // get derivative limits
          DoubleDoublePair[] limits = new DoubleDoublePair[b.length];
          for(int i = 0; i < limits.length; i++) {
            limits[i] = calculateLinearDerivativeLimits(a, b, bounds, i);
          }
          double maxPossValue = computeMaximumPossibleFuncValue(a, b, c, bounds, limits);
          if(maxPossValue <= resultValue) {
            return resultValue; // we cannot get any better in this
                                // sub-call-tree
          }
        }

      }
      // all cutoff checks done, if we reach this line, there is no cutoff and
      // we proceed with calling all children

      // only do unconstrained child if its not the last constrained
      // count values in array
      int acc = 0;
      for(int i = 0; i < attributeTypes.length; i++) {
        if(attributeTypes[i] == AttributeState.CONSTR)
          acc += 1;
      }
      double lastResult = resultValue;
      if(acc > 1) {
        lastResult = startReducedProblem(a, b, c, bounds, attributeTypes, constrAtt, AttributeState.UNCONSTR, result, resultValue);
        assert lastResult >= resultValue;
        resultValue = lastResult;
      }
      lastResult = startReducedProblem(a, b, c, bounds, attributeTypes, constrAtt, AttributeState.LOLIM, result, resultValue);
      assert lastResult >= resultValue;
      resultValue = lastResult;
      lastResult = startReducedProblem(a, b, c, bounds, attributeTypes, constrAtt, AttributeState.UPLIM, result, resultValue);
      assert lastResult >= resultValue;
      return lastResult; // best of the children or old
    }
    return resultValue; // we havent found anything better so return last result
  }

  private double evaluateConstrainedQuadraticFunction1D(double a, double b, double c, double lower, double upper, double[] result, double resultValue) {
    // has max if a <0
    boolean hasMax = a < 0.0;
    double optimum = Double.NaN;
    boolean foundmax = false;
    double optvalue = Double.NEGATIVE_INFINITY;

    if(hasMax) {
      optimum = -b / a;
      foundmax = optimum >= lower && optimum <= upper;
      optvalue = 0.5 * a * optimum * optimum + b * optimum + c;
    }
    if(!foundmax) {
      double lovalue = 0.5 * a * lower * lower + b * lower + c;
      double hivalue = 0.5 * a * upper * upper + b * upper + c;
      optvalue = lovalue >= hivalue ? lovalue : hivalue;
      optimum = lovalue >= hivalue ? lower : upper;
    }

    if(optvalue > resultValue) {
      if(calcPoint)
        result[0] = optimum;
      return optvalue;
    }

    return resultValue;
  }

  /**
   * helper-function for recursion
   * 
   * @param a
   * @param b
   * @param c
   * @param bounds
   * @param attTypes
   * @param reducedAtt
   * @param reducedTo
   * @return
   */
  private double startReducedProblem(double[][] a, double[] b, double c, Boundingbox bounds, AttributeState[] attTypes, int reducedAtt, AttributeState reducedTo, double[] result, double resultValue) {
    assert attTypes[reducedAtt] == AttributeState.CONSTR : "trying to reduce on allready reduced attribute";
    assert reducedTo == AttributeState.UNCONSTR || reducedTo == AttributeState.LOLIM || reducedTo == AttributeState.UPLIM : "trying to reduce to constrained att type";

    if(reducedTo == AttributeState.UNCONSTR) {
      // AttributeState[] redAttTypes = new AttributeState[attTypes.length];
      // System.arraycopy(attTypes, 0, redAttTypes, 0, attTypes.length);
      AttributeState tas = attTypes[reducedAtt];
      attTypes[reducedAtt] = reducedTo;
      // contrained quadopt should only update result, if the new result is
      // better than the old
      double childResValue = evaluateConstrainedQuadraticFunction(a, b, c, bounds, attTypes, false, result, resultValue);
      attTypes[reducedAtt] = tas;
      assert childResValue >= resultValue;
      return childResValue;
    }
    else {
      double reduceToValue = reducedTo == AttributeState.LOLIM ? bounds.getLowerBound(reducedAtt) : bounds.getUpperBound(reducedAtt);
      int redSize = b.length - 1;
      double[][] redA = cache[redSize - 1].a;// new double[redSize][redSize];
      double[] redB = cache[redSize - 1].b;// new double[redSize];
      AttributeState[] redAttTypes = cache[redSize - 1].attTypes;// new
                                                                 // AttributeState[redSize];
      Boundingbox redBounds = cache[redSize - 1].box;// new Boundingbox(null);
      double redC = reduceEquation(a, b, c, redA, redB, reducedAtt, reduceToValue);
      reduceConstraints(bounds, redBounds, attTypes, redAttTypes, reducedAtt);

      double[] redRes = null;
      if(calcPoint) {
        redRes = reduceSolution(reducedAtt, result);
      }
      // reduce solution to make it temporarily usable

      double redResValue = evaluateConstrainedQuadraticFunction(redA, redB, redC, redBounds, redAttTypes, true, redRes, resultValue);

      // if we found a better value than the one saved in result_value, update
      // result.
      // this check seems to be important, according to original implementation
      // TODO: this might need nullchecks -> shouldnt, result init is neginf
      if(redResValue > resultValue) {
        if(calcPoint) {
          expandNewSolution(result, redRes, reducedAtt, reduceToValue);
        }
        resultValue = redResValue;
      }
      return resultValue;
    }
  }

  private void expandNewSolution(double[] result, double[] redRes, int reducedAtt, double reduceToValue) {
    if(redRes != null) {

      int expSize = redRes.length + 1;

      for(int i = 0; i < reducedAtt; i++) {
        result[i] = redRes[i];
      }
      result[reducedAtt] = reduceToValue;
      for(int i = reducedAtt + 1; i < expSize; i++) {
        result[i] = redRes[i - 1];
      }
    }
  }

  private double[] reduceSolution(int reducedAtt, double[] result) {
    double[] redRes = null;
    if(result != null) {
      redRes = cache[result.length - 2].result;// new double[result.length - 1];
      if(reducedAtt > 0)
        System.arraycopy(result, 0, redRes, 0, reducedAtt);
      if(redRes.length - (reducedAtt) > 0)
        System.arraycopy(result, reducedAtt + 1, redRes, reducedAtt, redRes.length - reducedAtt);
    }
    return redRes;
  }

  private void reduceConstraints(Boundingbox bounds, Boundingbox redBounds, AttributeState[] attTypes, AttributeState[] redAttTypes, int reducedAtt) {
    if(reducedAtt > 0) {
      System.arraycopy(attTypes, 0, redAttTypes, 0, reducedAtt);
    }

    if(redAttTypes.length - (reducedAtt) > 0) {
      System.arraycopy(attTypes, reducedAtt + 1, redAttTypes, reducedAtt, redAttTypes.length - reducedAtt);
    }
    // for(int i = 0; i < redSize; i++) {
    // int oRefi = i < reducedAtt ? i : i + 1;
    // redAttTypes[i] = attTypes[oRefi];
    // }
    bounds.reduceBoundingboxTo(redBounds, reducedAtt);
  }

  private double reduceEquation(double[][] a, double[] b, double c, double[][] redA, double[] redB, int reducedAtt, double reduceToValue) {
    int redSize = b.length - 1;
    for(int i = 0; i < redSize; i++) {
      int oRefi = i < reducedAtt ? i : i + 1;
      redB[i] = b[oRefi] + reduceToValue * a[oRefi][reducedAtt];
      for(int j = 0; j < redSize; j++) {
        int oRefj = j < reducedAtt ? j : j + 1;
        redA[i][j] = a[oRefi][oRefj];
      }
    }
    return 0.5 * reduceToValue * reduceToValue * a[reducedAtt][reducedAtt] + reduceToValue * b[reducedAtt];
  }

  /**
   * Finds the argmax for 1/2 * a * x^2 + b * x
   * 
   * @param a
   * @param b
   * @return
   */
  private double[] findMaximumWithfunctionValue(double[][] a, double[] b) {
    double[][] am = times(a, -1);
    CholeskyDecomposition chol = new CholeskyDecomposition(am);
    if(chol.isSPD()) {
      return chol.solve(b);
    }
    return null;
  }

  public double evalueateQuadraticFormula(double[][] a, double[] b, double c, double[] point) {
    return 0.5 * transposeTimesTimes(point, a, point) + scalarProduct(point, b) + c;
  }

  //////////////////////////////////////////////////////////////////////// from
  //////////////////////////////////////////////////////////////////////// quopt

  private enum AttributeState {
    LOLIM, UPLIM, UNCONSTR, CONSTR
  }

  public static class ProblemData {
    double[][] a;

    double[] b, result;

    double piPow;

    Boundingbox box;

    AttributeState[] attTypes;

    public ProblemData(int size) {
      a = new double[size][size];
      b = new double[size];
      result = new double[size];
      piPow = FastMath.pow(MathUtil.SQRTPI, size);
      attTypes = new AttributeState[size];
      box = new Boundingbox(new double[size], new double[size]);
    }
  }
}
