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

import static elki.math.linearalgebra.VMath.*;

import java.util.Arrays;

import elki.clustering.em.KDTree.Boundingbox;
import elki.math.linearalgebra.CholeskyDecomposition;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Solve a constrained quadratic equation in the form
 * \( 0.5 \cdot x^T A x + b^T x + c \)
 * constrained by the given bounding box.
 * <p>
 * Works by recursion over the dimensions, searches the different possible
 * outcomes until it finds the best solution.
 * <p>
 * Reference:
 * <p>
 * A. W. Moore<br>
 * Very Fast EM-based Mixture Model Clustering using Multiresolution
 * kd-trees<br>
 * Neural Information Processing Systems (NIPS 1998)
 * 
 * @author Robert Gehde
 */
@Reference(authors = "A. W. Moore", //
    title = "Very Fast EM-based Mixture Model Clustering using Multiresolution kd-Trees", //
    booktitle = "Advances in Neural Information Processing Systems 11 (NIPS 1998)", //
    bibkey = "DBLP:conf/nips/Moore98")
public class QuadraticProblem {
  /**
   * Cache object
   */
  private ProblemData[] cache;

  /**
   * Private constructor.
   */
  private QuadraticProblem() {
    super(); // Internal use only!
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
   * @param box The bounds in which the maximum is calculated
   * @param arrayCache ArrayCache object
   */
  public static double solve(double[][] a, double[] b, double c, Boundingbox box, ProblemData[] cache, double[] argmaxPoint) {
    QuadraticProblem p = new QuadraticProblem();
    p.cache = cache;
    DimensionState[] dimStates = cache[b.length - 1].dimStates;
    Arrays.fill(dimStates, DimensionState.CONSTR);
    return p.evaluateConstrainedQuadraticFunction(a, b, c, box, dimStates, true, argmaxPoint, Double.NEGATIVE_INFINITY);
  }

  /**
   * Find the first dimension where the partial derivative wrt. that dimension
   * is nonnegative or nonpositive. If the derivative is nonnegative (lo >=0) it
   * will be driven to the upper limit and vice versa.
   * <p>
   * This does NOT mean that the non-found dimensions are not driven to a limit.
   * This is just an implication, not an equivalence
   * 
   * @param a a coefficient matrix of the function
   * @param b b coefficient vector of the function
   * @param hyperboundingbox bounds in which to check derivative
   * @return 0 if none, dim + 1 if lo, -dim-1 if hi
   */
  private int findLimitedDimensionWithDerivative(double[][] a, double[] b, Boundingbox hyperboundingbox) {
    double[] buf = new double[2];
    for(int i = 0; i < b.length; i++) {
      calculateLinearDerivativeLimits(a, b, hyperboundingbox, i, buf);
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
   * @param ak gradient/slope
   * @param bk y axis crossing
   * @param hyperboundingbox limitation of min max search
   * @param buf Return buffer
   */
  private void calculateLinearDerivativeLimits(double[][] a, double[] b, Boundingbox hyperboundingbox, int dim, double[] buf) {
    // initialize with value at 0^k
    double min = b[dim], max = min;
    // then for each dimension add the y-difference to the bounds to the
    // according limits
    for(int i = 0; i < b.length; i++) {
      // get slope of the derivative, is a[dim][i]
      double slope = a[dim][i];
      if(slope < 0) {
        min += hyperboundingbox.getUpperBound(i) * slope;
        max += hyperboundingbox.getLowerBound(i) * slope;
      }
      else {
        max += hyperboundingbox.getUpperBound(i) * slope;
        min += hyperboundingbox.getLowerBound(i) * slope;
      }
    }
    buf[0] = min; // return values
    buf[1] = max;
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
    double fm = evaluateQuadraticFormula(a, b, c, boundingbox.getMidPoint());
    double[] buf = new double[2];
    for(int i = 0; i < b.length; i++) {
      calculateLinearDerivativeLimits(a, b, boundingbox, i, buf);
      fm += boundingbox.getHalfwidth(i) * FastMath.max(-buf[0], buf[1]);
    }
    return fm;
  }

  /**
   * Main recursive function. We calculate 0.5 x^tax + bx +c
   * and only update the result array if we have a better result
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
      double[] opt = findMaximumWithFunctionValue(a, b);
      if(opt == null) {
        return resultValue; // no optimum with constraints
      }
      if(bounds.weaklyInsideBounds(opt)) {
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
        if(bounds.weaklyInsideBounds(opt)) {
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
      int drivenDimension = findLimitedDimensionWithDerivative(a, b, bounds);
      if(drivenDimension != 0) {
        // we found a value that can be driven to lower or upper limit
        final int dim = Math.abs(drivenDimension) - 1;
        DimensionState state = drivenDimension > 0 ? DimensionState.UPLIM : DimensionState.LOLIM;
        double redVal = startReducedProblem(a, b, c, bounds, dimensionStates, dim, state, result, resultValue);
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
      if(dimensionStates[i] == DimensionState.CONSTR) {
        acc += 1;
      }
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
   * @param bounds The bounds in which the maximum is calculated
   * @param dimStates Current calculation state of the dimensions
   * @param reducedDim the dimension to reduce
   * @param reducedTo the state the dimension is reduced to
   * @param result the current argmax
   * @param resultValue the current max
   * @return the new max value
   */
  private double startReducedProblem(double[][] a, double[] b, double c, Boundingbox bounds, DimensionState[] dimStates, int reducedDim, DimensionState reducedTo, double[] result, double resultValue) {
    assert dimStates[reducedDim] == DimensionState.CONSTR : "trying to reduce an already reduced dimension";
    assert reducedTo == DimensionState.UNCONSTR || reducedTo == DimensionState.LOLIM || reducedTo == DimensionState.UPLIM : "trying to reduce to constrained dim state";

    if(reducedTo == DimensionState.UNCONSTR) {
      DimensionState dimState = dimStates[reducedDim];
      dimStates[reducedDim] = reducedTo;
      // child call should only update result, if the new result is
      // better than the old
      double childResValue = evaluateConstrainedQuadraticFunction(a, b, c, bounds, dimStates, false, result, resultValue);
      assert childResValue >= resultValue;
      dimStates[reducedDim] = dimState; // Restore
      return childResValue;
    }
    double reduceToValue = reducedTo == DimensionState.LOLIM ? bounds.getLowerBound(reducedDim) : bounds.getUpperBound(reducedDim);
    final int redSizem1 = b.length - 2;
    double[][] redA = cache[redSizem1].a;
    double[] redB = cache[redSizem1].b;
    DimensionState[] redDimStates = cache[redSizem1].dimStates;
    Boundingbox redBounds = cache[redSizem1].box;
    double redC = reduceEquation(a, b, c, redA, redB, reducedDim, reduceToValue);
    reduceConstraints(bounds, redBounds, dimStates, redDimStates, reducedDim);

    double[] redRes = reduceSolution(result, reducedDim);
    // reduce solution to make it temporarily usable
    double redResValue = evaluateConstrainedQuadraticFunction(redA, redB, redC, redBounds, redDimStates, true, redRes, resultValue);
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
   * @param result the result
   * @param redRes the the reduced result
   * @param reducedDim the dimension that was reduced to gain redRes
   * @param reduceToValue the value the dimension was reduced to to gain redRes
   */
  private void expandNewSolution(double[] result, double[] redRes, int reducedDim, double reduceToValue) {
    System.arraycopy(redRes, 0, result, 0, reducedDim);
    result[reducedDim] = reduceToValue;
    System.arraycopy(redRes, reducedDim, result, reducedDim + 1, redRes.length - reducedDim);
  }

  /**
   * Reduce the solution to a problem with dim-1
   * 
   * @param result result of the array
   * @param reducedDim dimension to reduce
   * @return reduced result
   */
  private double[] reduceSolution(double[] result, int reducedDim) {
    double[] redRes = cache[result.length - 2].result;
    System.arraycopy(result, 0, redRes, 0, reducedDim);
    System.arraycopy(result, reducedDim + 1, redRes, reducedDim, redRes.length - reducedDim);
    return redRes;
  }

  /**
   * Reduces the constrains to a problem with dim-1
   * 
   * @param bounds boundingbox of the problem
   * @param redBounds result boundingbox
   * @param dimStates dimension states of the problem
   * @param redDimStates result array for dimension states
   * @param reducedDim dimension to reduce
   */
  private void reduceConstraints(Boundingbox bounds, Boundingbox redBounds, DimensionState[] dimStates, DimensionState[] redDimStates, int reducedDim) {
    System.arraycopy(dimStates, 0, redDimStates, 0, reducedDim);
    System.arraycopy(dimStates, reducedDim + 1, redDimStates, reducedDim, redDimStates.length - reducedDim);
    bounds.reduceBoundingboxTo(redBounds, reducedDim);
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
  private double reduceEquation(double[][] a, double[] b, double c, double[][] redA, double[] redB, int reducedDim, double reduceToValue) {
    int redSize = b.length - 1;
    for(int i = 0; i < redSize; i++) {
      int oRefi = i < reducedDim ? i : i + 1;
      redB[i] = b[oRefi] + reduceToValue * a[oRefi][reducedDim];
      System.arraycopy(a[oRefi], 0, redA[i], 0, reducedDim);
      System.arraycopy(a[oRefi], reducedDim + 1, redA[i], reducedDim, redSize - reducedDim);
    }
    return 0.5 * reduceToValue * reduceToValue * a[reducedDim][reducedDim] + reduceToValue * b[reducedDim];
  }

  /**
   * Finds the argmax for \( \tfrac12 A x^2 + b x \)
   * 
   * @param a coefficient matrix A of the function
   * @param b coefficient vector b of the function
   * @return the argmax of the given function
   */
  private double[] findMaximumWithFunctionValue(double[][] a, double[] b) {
    CholeskyDecomposition chol = new CholeskyDecomposition(times(a, -1));
    return chol.isSPD() ? chol.solve(b) : null;
  }

  /**
   * calculate \( 0.5 x^T A x + x^t b + c \) for the given values
   * 
   * @param a coefficient matrix A of the function
   * @param b coefficient vector b of the function
   * @param c coefficient scalar c of the function
   * @param point x to calculate the function at
   * @return function value
   */
  public double evaluateQuadraticFormula(double[][] a, double[] b, double c, double[] point) {
    return 0.5 * transposeTimesTimes(point, a, point) + scalarProduct(point, b) + c;
  }

  /**
   * Describes the calculation state of a Dimension
   */
  private enum DimensionState {
    LOLIM, // driven to lower bounding box limit
    UPLIM, // driven to upper bounding box limit
    UNCONSTR, // constrains lifted (results are ignored if outside of bounds)
    CONSTR // constrained (not yet visited)
  }

  /**
   * contains arrays for a specific size needed for the problem calculation
   * using this object saves the creation of all those arrays, because we can
   * just reuse them
   */
  public static class ProblemData {
    double[][] a;

    double[] b, result;

    Boundingbox box;

    DimensionState[] dimStates;

    public ProblemData(int size) {
      a = new double[size][size];
      b = new double[size];
      result = new double[size];
      dimStates = new DimensionState[size];
      box = new Boundingbox(new double[size], new double[size]);
    }

    public static ProblemData[] newCache(int d) {
      ProblemData[] cache = new ProblemData[d];
      for(int i = 0; i < d; i++) {
        cache[i] = new ProblemData(i + 1);
      }
      return cache;
    }
  }
}
