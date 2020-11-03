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
    maximumValue = Double.NEGATIVE_INFINITY;
    argmaxPoint = null;
    this.cache = arrayCache;
    DimensionState[] dimStates = cache[b.length - 1].dimStates;
    Arrays.fill(dimStates, DimensionState.CONSTR);
    argmaxPoint = new double[b.length];
    maximumValue = evaluateConstrainedQuadraticFunction(a, b, c, box, dimStates, true, argmaxPoint, maximumValue);
  }

  /**
   * find the first dimension where the partial derivative wrt that dimension is
   * nonnegative or nonpositive. If the derivative is nonnegative (lo >=0) it
   * will be driven to the upper limit and vice versa.
   * 
   * This does NOT mean that the non-found dimensions are not driven to a limit.
   * This is just an implication, not an equivalence
   * 
   * @param a
   * @param b
   * @param hyperboundingbox
   * @return
   */
  private IntIntPair findLimitedDimensionWithDerivative(double[][] a, double[] b, Boundingbox hyperboundingbox) {
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
    // then for each dimension add the y-difference to the bounds to the
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

  /**
   * Calculates the maximum possible function value for a constrained quadratic
   * function. This function value does not need to exist, it is just an upper
   * limit.
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param boundingbox The bounds in which the maximum is calculated
   * @return The maximum Function value
   */
  private double computeMaximumPossibleFuncValue(double[][] a, double[] b, double c, Boundingbox boundingbox) {
    double[] upper = boundingbox.getUpperBounds();
    double[] lower = boundingbox.getLowerBounds();
    double[] middle = boundingbox.getMidPoint();

    double fm = evalueateQuadraticFormula(a, b, c, middle);

    for(int i = 0; i < middle.length; i++) {
      DoubleDoublePair derlim = calculateLinearDerivativeLimits(a, b, boundingbox, i);
      double halfdist = (upper[i] - lower[i]) / 2.0;
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
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param bounds The bounds in which the maximum is calculated
   * @param dimensionStates Current calculation state of the dimensions
   * @param cutoffCheck Flag if a cutoff check should be performed
   */
  private double evaluateConstrainedQuadraticFunction(double[][] a, double[] b, double c, Boundingbox bounds, DimensionState[] dimensionStates, boolean cutoffCheck, double[] result, double resultValue) {
    // case for one dimension
    if(dimensionStates.length == 1) {
      double res1d = evaluateConstrainedQuadraticFunction1D(a[0][0], b[0], c, bounds.getLowerBound(0), bounds.getUpperBound(0), result, resultValue);
      assert res1d >= resultValue;
      return res1d; // 1 dimensional solution
    }

    // find first constrained dimension
    int constrDim = -1;
    for(int i = 0; i < dimensionStates.length; i++) {
      if(dimensionStates[i] == DimensionState.CONSTR) {
        constrDim = i;
        break;
      }
    }
    // check if we have no constrained dimension -> we worked all dimension
    // then we just check if the maximum is inside the given bounds
    if(constrDim < 0) {
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
    else { // we found a constrained dimension
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
        IntIntPair drivenDimension = findLimitedDimensionWithDerivative(a, b, bounds);
        if(drivenDimension.first >= 0) {
          // we found a value that can be driven to lo or hi
          double redVal = startReducedProblem(a, b, c, bounds, dimensionStates, drivenDimension.first, (drivenDimension.second == 1 ? DimensionState.UPLIM : DimensionState.LOLIM), result, resultValue);
          assert redVal >= resultValue;
          return redVal;
        }
        // if we cannot find such a value, make sure we can still find a good
        // value in the given bounds
        double maxPossValue = computeMaximumPossibleFuncValue(a, b, c, bounds);
        if(maxPossValue <= resultValue) {
          return resultValue; // we cannot get any better in this sub-call-tree
        }
      }
      // all cutoff checks done, if we reach this line, there is no cutoff and
      // we proceed with calling all children

      // only do unconstrained child if its not the last constrained
      // count values in array
      int acc = 0;
      for(int i = 0; i < dimensionStates.length; i++) {
        if(dimensionStates[i] == DimensionState.CONSTR)
          acc += 1;
      }
      double lastResult = resultValue;
      if(acc > 1) {
        lastResult = startReducedProblem(a, b, c, bounds, dimensionStates, constrDim, DimensionState.UNCONSTR, result, resultValue);
        assert lastResult >= resultValue;
        resultValue = lastResult;
      }
      lastResult = startReducedProblem(a, b, c, bounds, dimensionStates, constrDim, DimensionState.LOLIM, result, resultValue);
      assert lastResult >= resultValue;
      resultValue = lastResult;
      lastResult = startReducedProblem(a, b, c, bounds, dimensionStates, constrDim, DimensionState.UPLIM, result, resultValue);
      assert lastResult >= resultValue;
      return lastResult; // best of the children or old
    }
    return resultValue; // we havent found anything better so return last result
  }

  private double evaluateConstrainedQuadraticFunction1D(double a, double b, double c, double lowerBound, double upperBound, double[] result, double resultValue) {
    // has max if a <0
    boolean hasMax = a < 0.0;
    double argmax = Double.NaN;
    double max = Double.NEGATIVE_INFINITY;

    if(hasMax) {
      // because f' = ax + b -> 0 = ax + b -> -b/a = x
      argmax = -b / a;
      max = 0.5 * a * argmax * argmax + b * argmax + c;
    }
    // Because we have a quadratic formula with optimum outside of the bounds,
    // the function is monotonic inside the bounds.
    if(!(argmax >= lowerBound && argmax <= upperBound)) {
      double lowerBoundValue = 0.5 * a * lowerBound * lowerBound + b * lowerBound + c;
      double higherBoundValue = 0.5 * a * upperBound * upperBound + b * upperBound + c;
      max = lowerBoundValue >= higherBoundValue ? lowerBoundValue : higherBoundValue;
      argmax = lowerBoundValue >= higherBoundValue ? lowerBound : upperBound;
    }

    if(max > resultValue) {
      if(calcPoint)
        result[0] = argmax;
      return max;
    }

    return resultValue;
  }

  /**
   * This function reduces the quadratic problem (given with a,b,c, bounds and
   * dimState) with the information given in reducedDim and reducedTo. It will
   * then call {@link evaluateConstrainedQuadraticFunction} to continue
   * evaluation.
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param bounds The bounds in which the maximum is calculated
   * @param dimStates Current calculation state of the dimensions
   * @param reducedDim
   * @param reducedTo
   * @return
   */
  private double startReducedProblem(double[][] a, double[] b, double c, Boundingbox bounds, DimensionState[] dimStates, int reducedDim, DimensionState reducedTo, double[] result, double resultValue) {
    assert dimStates[reducedDim] == DimensionState.CONSTR : "trying to reduce on allready reduced dimension";
    assert reducedTo == DimensionState.UNCONSTR || reducedTo == DimensionState.LOLIM || reducedTo == DimensionState.UPLIM : "trying to reduce to constrained dim state";

    if(reducedTo == DimensionState.UNCONSTR) {
      DimensionState dimState = dimStates[reducedDim];
      dimStates[reducedDim] = reducedTo;
      // contrained quadopt should only update result, if the new result is
      // better than the old
      double childResValue = evaluateConstrainedQuadraticFunction(a, b, c, bounds, dimStates, false, result, resultValue);
      dimStates[reducedDim] = dimState;
      assert childResValue >= resultValue;
      return childResValue;
    }
    else {
      double reduceToValue = reducedTo == DimensionState.LOLIM ? bounds.getLowerBound(reducedDim) : bounds.getUpperBound(reducedDim);
      int redSize = b.length - 1;
      double[][] redA = cache[redSize - 1].a;
      double[] redB = cache[redSize - 1].b;
      DimensionState[] redDimStates = cache[redSize - 1].dimStates;
      Boundingbox redBounds = cache[redSize - 1].box;
      double redC = reduceEquation(a, b, c, redA, redB, reducedDim, reduceToValue);
      reduceConstraints(bounds, redBounds, dimStates, redDimStates, reducedDim);

      double[] redRes = null;
      if(calcPoint) {
        redRes = reduceSolution(reducedDim, result);
      }
      // reduce solution to make it temporarily usable

      double redResValue = evaluateConstrainedQuadraticFunction(redA, redB, redC, redBounds, redDimStates, true, redRes, resultValue);

      // if we found a better value than the one saved in result_value, update
      // result.
      if(redResValue > resultValue) {
        if(calcPoint) {
          expandNewSolution(result, redRes, reducedDim, reduceToValue);
        }
        resultValue = redResValue;
      }
      return resultValue;
    }
  }

  private void expandNewSolution(double[] result, double[] redRes, int reducedDim, double reduceToValue) {
    if(redRes != null) {

      int expSize = redRes.length + 1;

      for(int i = 0; i < reducedDim; i++) {
        result[i] = redRes[i];
      }
      result[reducedDim] = reduceToValue;
      for(int i = reducedDim + 1; i < expSize; i++) {
        result[i] = redRes[i - 1];
      }
    }
  }

  private double[] reduceSolution(int reducedDim, double[] result) {
    double[] redRes = null;
    if(result != null) {
      redRes = cache[result.length - 2].result;// new double[result.length - 1];
      if(reducedDim > 0)
        System.arraycopy(result, 0, redRes, 0, reducedDim);
      if(redRes.length - (reducedDim) > 0)
        System.arraycopy(result, reducedDim + 1, redRes, reducedDim, redRes.length - reducedDim);
    }
    return redRes;
  }

  private void reduceConstraints(Boundingbox bounds, Boundingbox redBounds, DimensionState[] dimStates, DimensionState[] redDimStates, int reducedDim) {
    if(reducedDim > 0) {
      System.arraycopy(dimStates, 0, redDimStates, 0, reducedDim);
    }

    if(redDimStates.length - (reducedDim) > 0) {
      System.arraycopy(dimStates, reducedDim + 1, redDimStates, reducedDim, redDimStates.length - reducedDim);
    }
    bounds.reduceBoundingboxTo(redBounds, reducedDim);
  }

  private double reduceEquation(double[][] a, double[] b, double c, double[][] redA, double[] redB, int reducedDim, double reduceToValue) {
    int redSize = b.length - 1;
    for(int i = 0; i < redSize; i++) {
      int oRefi = i < reducedDim ? i : i + 1;
      redB[i] = b[oRefi] + reduceToValue * a[oRefi][reducedDim];
      for(int j = 0; j < redSize; j++) {
        int oRefj = j < reducedDim ? j : j + 1;
        redA[i][j] = a[oRefi][oRefj];
      }
    }
    return 0.5 * reduceToValue * reduceToValue * a[reducedDim][reducedDim] + reduceToValue * b[reducedDim];
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

  private enum DimensionState {
    LOLIM, UPLIM, UNCONSTR, CONSTR
  }

  public static class ProblemData {
    double[][] a;

    double[] b, result;

    double piPow;

    Boundingbox box;

    DimensionState[] dimStates;

    public ProblemData(int size) {
      a = new double[size][size];
      b = new double[size];
      result = new double[size];
      piPow = FastMath.pow(MathUtil.SQRTPI, size);
      dimStates = new DimensionState[size];
      box = new Boundingbox(new double[size], new double[size]);
    }
  }
}
