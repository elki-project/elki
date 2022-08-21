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
package elki.math.linearalgebra;

import static elki.math.linearalgebra.VMath.*;

import java.util.Arrays;

/**
 * Solve a constrained quadratic equation in the form
 * \( \tfrac12 x^T A x + b^T x + c \)
 * constrained by a bounding box.
 * <p>
 * Works by recursion over the dimensions, searches the different possible
 * outcomes until it finds the best solution.
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public class ConstrainedQuadraticProblemSolver {
  /**
   * Cache object
   */
  private ProblemData[] cache;

  /**
   * Constructor.
   * 
   * @param dim Maximum dimensionality (for the cache).
   */
  public ConstrainedQuadraticProblemSolver(int dim) {
    super(); // Internal use only!
    cache = new ProblemData[dim];
    for(int i = 0; i < dim; i++) {
      cache[i] = new ProblemData(i + 1);
    }
  }

  /**
   * Calculate \( 0.5 x^T A x + b + c \)
   * we use it in KDTrees with \( b = (0)^d \) and \( c = 0 \).
   * If you use it like this you can multiply the result with 2 to get
   * \( x^T A x + b + c \).
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param min Lower constraints
   * @param max Upper constraints
   * @param argmaxPoint point with the maximum
   */
  public double solve(double[][] a, double[] b, double c, double[] min, double[] max, double[] argmaxPoint) {
    DimensionState[] dimStates = cache[min.length - 1].dimStates;
    Arrays.fill(dimStates, DimensionState.CONSTR);
    return evaluateConstrainedQuadraticFunction(a, b, c, min, max, dimStates, true, argmaxPoint, Double.NEGATIVE_INFINITY);
  }

  /**
   * Find the first dimension where the partial derivative wrt. that dimension
   * is nonnegative or nonpositive. If the derivative is nonnegative (lo &geq;0) it
   * will be driven to the upper limit and vice versa.
   * <p>
   * This does NOT mean that the non-found dimensions are not driven to a limit.
   * This is just an implication, not an equivalence
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param min Lower constraints
   * @param max Upper constraints
   * @return 0 if none, dim + 1 if lo, -dim-1 if hi
   */
  private int findLimitedDimensionWithDerivative(double[][] a, double[] b, double[] min, double[] max) {
    double[] buf = new double[2];
    for(int i = 0; i < min.length; i++) {
      calculateLinearDerivativeLimits(a, b, min, max, i, buf);
      // if hi < 0 or lo > 0 -> hit
      if(buf[0] >= 0.0) {
        return i + 1;
      }
      else if(buf[1] <= 0.0) {
        return -(i + 1);
      }
    }
    return 0; // none found
  }

  /**
   * Calculates the min and max value of a linear derivative of a quadratic
   * function in the bounding box.
   * The function is of the form \( 0.5 A x^2 + b x + c \).
   * 
   * @param a gradient/slope
   * @param b y axis crossing
   * @param min Lower constraints
   * @param max Upper constraints
   * @param dim dimension of interest
   * @param buf Return buffer
   */
  private void calculateLinearDerivativeLimits(double[][] a, double[] b, double[] min, double[] max, int dim, double[] buf) {
    // initialize with value at 0^k
    double mi = b != null ? b[dim] : 0, ma = mi;
    // then for each dimension add the y-difference to the bounds to the
    // according limits
    for(int i = 0; i < min.length; i++) {
      // get slope of the derivative, is a[dim][i]
      double slope = a[dim][i];
      if(slope < 0) {
        mi += max[i] * slope;
        ma += min[i] * slope;
      }
      else {
        ma += max[i] * slope;
        mi += min[i] * slope;
      }
    }
    buf[0] = mi; // return values
    buf[1] = ma;
  }

  /**
   * Calculates the maximum possible function value for a constrained quadratic
   * function. This function value does not need to exist, it is just an upper
   * limit.
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param min Lower constraints
   * @param max Upper constraints
   * @return The maximum Function value
   */
  private double computeMaximumPossibleFuncValue(double[][] a, double[] b, double c, double[] min, double[] max) {
    double fm = evaluateQuadraticFormula(a, b, c, timesEquals(plus(min, max), 0.5));
    double[] buf = new double[2];
    for(int i = 0; i < min.length; i++) {
      calculateLinearDerivativeLimits(a, b, min, max, i, buf);
      fm += (max[i] - min[i]) * 0.5 * Math.max(-buf[0], buf[1]);
    }
    return fm;
  }

  /**
   * Main recursive function. We calculate \( \frac12 x^T A x + bx + c \)
   * and only update the result array if we have a better result
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param min Lower constraints
   * @param max Upper constraints
   * @param dimensionStates Current calculation state of the dimensions
   * @param cutoffCheck Flag if a cutoff check should be performed
   */
  private double evaluateConstrainedQuadraticFunction(double[][] a, double[] b, double c, double[] min, double[] max, DimensionState[] dimensionStates, boolean cutoffCheck, double[] result, double resultValue) {
    // case for one dimension
    if(dimensionStates.length == 1) {
      double res1d = evaluateConstrainedQuadraticFunction1D(a[0][0], b[0], c, min[0], max[0], result, resultValue);
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
      double[] opt = findMaximumWithFunctionValue(a, b);
      if(opt == null) {
        return resultValue; // no optimum with constraints
      }
      if(contains(min, max, opt)) {
        double optValue = evaluateQuadraticFormula(a, b, c, opt);
        if(optValue > resultValue) {
          System.arraycopy(opt, 0, result, 0, opt.length);
          return optValue; // better optimum with constraints
        }
      }
      return resultValue; // we haven't found anything better, keep previous
    }
    // we found a constrained dimension
    if(cutoffCheck) {
      double[] opt = findMaximumWithFunctionValue(a, b);
      if(opt != null) {
        double optValue = evaluateQuadraticFormula(a, b, c, opt);
        if(contains(min, max, opt)) {
          if(optValue > resultValue) {
            System.arraycopy(opt, 0, result, 0, opt.length);
            return optValue;
          }
          return resultValue; // found value in cutoff check
          // no point in checking children, because we found a value in bounds
          // and will not get anything better
        }
        else {
          if(resultValue > optValue) {
            return resultValue;
            // no better solution better in this sub-call-tree
            // even though the value is outside of bounds, that value still
            // will only drop in children, so its not worth checking any
            // further
          }
        }
      }
      // At this point opt is either null or outside bounds and we can still
      // get a good value
      int drivenDimension = findLimitedDimensionWithDerivative(a, b, min, max);
      if(drivenDimension != 0) {
        // we found a value that can be driven to lower or upper limit
        final int dim = Math.abs(drivenDimension) - 1;
        DimensionState state = drivenDimension > 0 ? DimensionState.UPLIM : DimensionState.LOLIM;
        double redVal = startReducedProblem(a, b, c, min, max, dimensionStates, dim, state, result, resultValue);
        assert redVal >= resultValue;
        return redVal;
      }
      // if we cannot find such a value, make sure we can still find a good
      // value in the given bounds
      double maxPossValue = computeMaximumPossibleFuncValue(a, b, c, min, max);
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
      if(dimensionStates[i] == DimensionState.CONSTR) {
        acc += 1;
      }
    }
    double lastResult = resultValue;
    if(acc > 1) {
      lastResult = startReducedProblem(a, b, c, min, max, dimensionStates, constrDim, DimensionState.UNCONSTR, result, resultValue);
      assert lastResult >= resultValue;
      resultValue = lastResult;
    }
    lastResult = startReducedProblem(a, b, c, min, max, dimensionStates, constrDim, DimensionState.LOLIM, result, resultValue);
    assert lastResult >= resultValue;
    resultValue = lastResult;
    lastResult = startReducedProblem(a, b, c, min, max, dimensionStates, constrDim, DimensionState.UPLIM, result, resultValue);
    assert lastResult >= resultValue;
    return lastResult; // best of the children or old
  }

  /**
   * Finds the maximum for a 1d constrained quadratic function. If a new maximum
   * is found, the argmax contained in result will be overwritten with the new
   * argmax.
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param lowerBound lower bound of valid input
   * @param upperBound upper bound of valid input
   * @param result the - so far - argmax
   * @param resultValue the - so far - max
   * @return the new max
   */
  private double evaluateConstrainedQuadraticFunction1D(double a, double b, double c, double lowerBound, double upperBound, double[] result, double resultValue) {
    // has max if a < 0
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
   * @param min Lower constraints
   * @param max Upper constraints
   * @param dimStates Current calculation state of the dimensions
   * @param reducedDim the dimension to reduce
   * @param reducedTo the state the dimension is reduced to
   * @param result the current argmax
   * @param resultValue the current max
   * @return the new max value
   */
  private double startReducedProblem(double[][] a, double[] b, double c, double[] min, double[] max, DimensionState[] dimStates, int reducedDim, DimensionState reducedTo, double[] result, double resultValue) {
    assert dimStates[reducedDim] == DimensionState.CONSTR : "trying to reduce an already reduced dimension";
    assert reducedTo == DimensionState.UNCONSTR || reducedTo == DimensionState.LOLIM || reducedTo == DimensionState.UPLIM : "trying to reduce to constrained dim state";

    if(reducedTo == DimensionState.UNCONSTR) {
      DimensionState dimState = dimStates[reducedDim];
      dimStates[reducedDim] = reducedTo;
      // child call should only update result, if the new result is
      // better than the old
      double childResValue = evaluateConstrainedQuadraticFunction(a, b, c, min, max, dimStates, false, result, resultValue);
      assert childResValue >= resultValue;
      dimStates[reducedDim] = dimState; // Restore
      return childResValue;
    }
    double reduceToValue = reducedTo == DimensionState.LOLIM ? min[reducedDim] : max[reducedDim];
    final int redSizem1 = min.length - 2;
    double[][] redA = cache[redSizem1].a;
    double[] redB = cache[redSizem1].b;
    DimensionState[] redDimStates = cache[redSizem1].dimStates;
    double[] redMin = cache[redSizem1].min, redMax = cache[redSizem1].max;
    double redC = reduceEquation(a, b, c, redA, redB, reducedDim, reduceToValue);
    reduceConstraints(min, max, redMin, redMax, dimStates, redDimStates, reducedDim);

    double[] redRes = reduceSolution(result, reducedDim);
    // reduce solution to make it temporarily usable
    double redResValue = evaluateConstrainedQuadraticFunction(redA, redB, redC, redMin, redMax, redDimStates, true, redRes, resultValue);
    // if we found a better value than the one in result_value, update
    if(redResValue <= resultValue) {
      return resultValue;
    }
    expandNewSolution(result, redRes, reducedDim, reduceToValue);
    return redResValue;
  }

  /**
   * Expands the redRes to a problem with dim+1 and saves it into result
   * 
   * @param outbox the result
   * @param inbox the the reduced result
   * @param insert the dimension that was reduced to gain redRes
   * @param insertValue the value the dimension was reduced to to gain redRes
   */
  private void expandNewSolution(double[] outbox, double[] inbox, int insert, double insertValue) {
    System.arraycopy(inbox, 0, outbox, 0, insert);
    outbox[insert] = insertValue;
    System.arraycopy(inbox, insert, outbox, insert + 1, inbox.length - insert);
  }

  /**
   * Reduce the solution to a problem with dim-1
   * 
   * @param result result of the array
   * @param omit dimension to omit
   * @return reduced result
   */
  private double[] reduceSolution(double[] result, int omit) {
    double[] redRes = cache[result.length - 2].result;
    System.arraycopy(result, 0, redRes, 0, omit);
    System.arraycopy(result, omit + 1, redRes, omit, redRes.length - omit);
    return redRes;
  }

  /**
   * Reduces the constrains to a problem with dim-1
   * 
   * @param inmin Input minima
   * @param inmax Input maxima
   * @param outmin Output minima
   * @param outmax Output maxima
   * @param instates dimension states of the problem
   * @param outstates result array for dimension states
   * @param omit dimension to omit
   */
  private static void reduceConstraints(double[] inmin, double[] inmax, double[] outmin, double[] outmax, DimensionState[] instates, DimensionState[] outstates, int omit) {
    System.arraycopy(instates, 0, outstates, 0, omit);
    System.arraycopy(instates, omit + 1, outstates, omit, outstates.length - omit);
    System.arraycopy(inmin, 0, outmin, 0, omit);
    System.arraycopy(inmin, omit + 1, outmin, omit, outmin.length - omit);
    System.arraycopy(inmax, 0, outmax, 0, omit);
    System.arraycopy(inmax, omit + 1, outmax, omit, outmax.length - omit);
  }

  /**
   * Reduces the equation/function the a problem with dim-1
   *
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param c c coefficient of the function
   * @param redA reduced a result array
   * @param redB reduced b result array
   * @param reducedDim dimension to reduce
   * @param reduceToValue state to reduce the dimension to
   * @return reduced c
   */
  private static double reduceEquation(double[][] a, double[] b, double c, double[][] redA, double[] redB, int reducedDim, double reduceToValue) {
    int redSize = a.length - 1;
    for(int i = 0; i < redSize; i++) {
      int oRefi = i < reducedDim ? i : i + 1;
      redB[i] = (b != null ? b[oRefi] : 0) + reduceToValue * a[oRefi][reducedDim];
      System.arraycopy(a[oRefi], 0, redA[i], 0, reducedDim);
      System.arraycopy(a[oRefi], reducedDim + 1, redA[i], reducedDim, redSize - reducedDim);
    }
    return 0.5 * reduceToValue * reduceToValue * a[reducedDim][reducedDim] + (b != null ? reduceToValue * b[reducedDim] : 0);
  }

  /**
   * Finds the argmax for \( \tfrac12 A x^2 + b x \).
   * 
   * @param a coefficient matrix A of the function
   * @param b coefficient vector b of the function
   * @return the argmax of the given function
   */
  private double[] findMaximumWithFunctionValue(double[][] a, double[] b) {
    CholeskyDecomposition chol = new CholeskyDecomposition(times(a, -1));
    return chol.isSPD() ? chol.solveLtransposed(chol.solveLInplace(b != null ? copy(b) : new double[a.length])) : null;
  }

  /**
   * calculate \( \tfrac12 x^T A x + x^t b + c \) for the given values.
   * 
   * @param a coefficient matrix A of the function
   * @param b coefficient vector b of the function
   * @param c coefficient scalar c of the function
   * @param point x to calculate the function at
   * @return function value
   */
  private static double evaluateQuadraticFormula(double[][] a, double[] b, double c, double[] point) {
    return 0.5 * transposeTimesTimes(point, a, point) + (b != null ? scalarProduct(point, b) + c : c);
  }

  /**
   * Checks if the point is inside or on the bounds of this bounding box
   * 
   * @param min Lower constraints
   * @param max Upper constraints
   * @param point point to check
   * @return true if point is weakly inside bounds
   */
  private static boolean contains(double[] min, double[] max, double[] point) {
    for(int i = 0; i < min.length; i++) {
      final double v = point[i];
      if(v < min[i] || v > max[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Describes the calculation state of a Dimension
   */
  private static enum DimensionState {
    LOLIM, // driven to lower bounding box limit
    UPLIM, // driven to upper bounding box limit
    UNCONSTR, // constrains lifted (results are ignored if outside of bounds)
    CONSTR // constrained (not yet visited)
  }

  /**
   * Contains arrays for a specific size needed for the problem calculation
   * using this object saves the creation of all those arrays, because we can
   * just reuse them.
   */
  private static class ProblemData {
    double[][] a;

    double[] b, result;

    double[] min, max;

    DimensionState[] dimStates;

    public ProblemData(int size) {
      a = new double[size][size];
      b = new double[size];
      result = new double[size];
      dimStates = new DimensionState[size];
      min = new double[size];
      max = new double[size];
    }
  }
}
