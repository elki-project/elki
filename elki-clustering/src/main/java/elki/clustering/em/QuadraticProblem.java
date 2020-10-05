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

import static elki.math.linearalgebra.VMath.scalarProduct;
import static elki.math.linearalgebra.VMath.times;

import java.util.Arrays;

import elki.clustering.em.KDTree.Boundingbox;
import elki.math.linearalgebra.CholeskyDecomposition;
import elki.utilities.pairs.DoubleDoublePair;
import elki.utilities.pairs.IntIntPair;

import net.jafama.FastMath;

public class QuadraticProblem {

  //////////////////////////////////////////////////////////////////////// from
  //////////////////////////////////////////////////////////////////////// quopt
  // private final int ATT_LOLIM = 0, ATT_HILIM = 1, UNCONSTR = 2, CONSTR = 3;

  public double maximumvalue;

  public double[] maxpoint;

  private ProblemData[] cache;

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
    maxpoint = new double[b.length];
    maximumvalue = constrained_quadopt(a, b, c, box, attTypes, true, maxpoint, maximumvalue);
  }

  public void reinit() {
    maximumvalue = Double.NEGATIVE_INFINITY;
    maxpoint = null;
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
      // comp deriv limits
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
   * calculates the min and max value of a linear function in the
   * hyperboundingbox
   * 
   * @param ak gradient/slope
   * @param bk y axis crossing
   * @param hyperboundingbox limitation of min max search
   * @return
   */
  private DoubleDoublePair calculateLinearDerivativeLimits(double[][] a, double[] b, Boundingbox hyperboundingbox, int dim) {
    double[] ak = cache[b.length - 1].derA;// new double[b.length];
    double bk = createPartialDerivative(a, b, dim, ak);
    // initialize with value at 0^k
    double min = bk;
    double max = bk;
    // then for each attribute add the y- diference to the bounds to the
    // according limits

    for(int i = 0; i < ak.length; i++) {
      double slope = ak[i];
      double hi_bound = hyperboundingbox.getHi(i);
      double lo_bound = hyperboundingbox.getLo(i);
      if(slope < 0) {
        min += hi_bound * slope;
        max += lo_bound * slope;
      }
      else {
        max += hi_bound * slope;
        min += lo_bound * slope;
      }
    }
    return new DoubleDoublePair(min, max);
  }

  private DoubleDoublePair[] ComputeAllLinearDerivativeLimits(double[][] a, double[] b, Boundingbox hyperboundingbox) {
    DoubleDoublePair[] result = new DoubleDoublePair[b.length];
    for(int i = 0; i < result.length; i++) {
      result[i] = calculateLinearDerivativeLimits(a, b, hyperboundingbox, i);
    }
    return result;
  }

  private double computeMaximumPossibleFuncValue(double[][] a, double[] b, double c, Boundingbox hyperboundingbox, DoubleDoublePair[] limits) {
    double[] mid = hyperboundingbox.getMidPoint();
    double fm = evalueateQuadraticFormula(a, b, c, mid);

    for(int i = 0; i < mid.length; i++) {
      double halfdist = (hyperboundingbox.getHi(i) - hyperboundingbox.getLo(i)) / 2.0;
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
   * @param att_types
   * @param do_cutoff_check
   */
  private double constrained_quadopt(double[][] a, double[] b, double c, Boundingbox bounds, AttributeState[] att_types, boolean do_cutoff_check, double[] result, double resultValue)
  // dyv **r_xopt,double *r_opt_value return values
  {
    // case for one attribute
    if(att_types.length == 1) {
      double res1d = constrained_quadopt_1d(a[0][0], b[0], c, bounds.getLo(0), bounds.getHi(0), result, resultValue);
      assert res1d >= resultValue;
      return res1d; // 1 dimensional solution
    }

    // find first constrained attribute
    int constrAtt = -1;
    for(int i = 0; i < att_types.length; i++) {
      if(att_types[i] == AttributeState.CONSTR) {
        constrAtt = i;
        break;
      }
    }
    // check if we have no constrained attribute -> we worked all attributes
    if(constrAtt < 0) {
      double[] opt = null;
      // if(att_types.length == 0) {
      // // do i actually need to do anything then?
      // // it would assume all attributes were driven to a limit in this pass
      // // and the value is set
      // opt = new double[0];
      // }
      // else {
      opt = findMaximumWithCholesky(a, b);
      // }
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
      if(do_cutoff_check) {
        double[] opt = findMaximumWithCholesky(a, b);
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
          double redVal = startReducedProblem(a, b, c, bounds, att_types, drivenAttribute.first, (drivenAttribute.second == 1 ? AttributeState.ATT_HILIM : AttributeState.ATT_LOLIM), result, resultValue);
          assert redVal >= resultValue;
          return redVal;
        }
        else {
          // if we cannot find such a value, make sure we can still find a good
          // value in the given bounds
          // get derivative limits
          DoubleDoublePair[] limits = ComputeAllLinearDerivativeLimits(a, b, bounds);
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
      for(int i = 0; i < att_types.length; i++) {
        if(att_types[i] == AttributeState.CONSTR)
          acc += 1;
      }
      double lastResult = resultValue;
      if(acc > 1) {
        lastResult = startReducedProblem(a, b, c, bounds, att_types, constrAtt, AttributeState.UNCONSTR, result, resultValue);
        assert lastResult >= resultValue;
        resultValue = lastResult;
      }
      lastResult = startReducedProblem(a, b, c, bounds, att_types, constrAtt, AttributeState.ATT_LOLIM, result, resultValue);
      assert lastResult >= resultValue;
      resultValue = lastResult;
      lastResult = startReducedProblem(a, b, c, bounds, att_types, constrAtt, AttributeState.ATT_HILIM, result, resultValue);
      assert lastResult >= resultValue;
      return lastResult; // best of the children or old
    }
    return resultValue; // we havent found anything better so return last result
  }

  private double constrained_quadopt_1d(double a, double b, double c, double lo, double hi, double[] result, double resultValue) {
    // has max if a <0
    boolean hasMax = a < 0.0;
    double optimum = Double.NaN;
    boolean foundmax = false;
    double optvalue = Double.NEGATIVE_INFINITY;

    if(hasMax) {
      optimum = -b / a;
      foundmax = optimum >= lo && optimum <= hi;
      optvalue = 0.5 * a * optimum * optimum + b * optimum + c;
    }
    if(!foundmax) {
      double lovalue = 0.5 * a * lo * lo + b * lo + c;
      double hivalue = 0.5 * a * hi * hi + b * hi + c;
      optvalue = lovalue >= hivalue ? lovalue : hivalue;
      optimum = lovalue >= hivalue ? lo : hi;
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
    assert reducedTo == AttributeState.UNCONSTR || reducedTo == AttributeState.ATT_LOLIM || reducedTo == AttributeState.ATT_HILIM : "trying to reduce to constrained att type";

    if(reducedTo == AttributeState.UNCONSTR) {
      // AttributeState[] redAttTypes = new AttributeState[attTypes.length];
      // System.arraycopy(attTypes, 0, redAttTypes, 0, attTypes.length);
      AttributeState tas = attTypes[reducedAtt];
      attTypes[reducedAtt] = reducedTo;
      // contrained quadopt should only update result, if the new result is
      // better than the old
      double childResValue = constrained_quadopt(a, b, c, bounds, attTypes, false, result, resultValue);
      attTypes[reducedAtt] = tas;
      assert childResValue >= resultValue;
      return childResValue;
    }
    else {
      double reduceToValue = reducedTo == AttributeState.ATT_LOLIM ? bounds.getLo(reducedAtt) : bounds.getHi(reducedAtt);
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

      double redResValue = constrained_quadopt(redA, redB, redC, redBounds, redAttTypes, true, redRes, resultValue);

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
      for(int i = 0; i < redRes.length; i++) {
        int oRefi = i < reducedAtt ? i : i + 1;
        redRes[i] = result[oRefi];
      }
    }
    return redRes;
  }

  private void reduceConstraints(Boundingbox bounds, Boundingbox redBounds, AttributeState[] attTypes, AttributeState[] redAttTypes, int reducedAtt) {
    int redSize = attTypes.length - 1;
    // double[][] bs = new double[3][redSize];
    for(int i = 0; i < redSize; i++) {
      int oRefi = i < reducedAtt ? i : i + 1;
      // bs[0][i] = bounds.getLo(oRefi);
      // bs[1][i] = bounds.getHi(oRefi);
      // bs[2][i] = bounds.getDiff(oRefi);
      redBounds.setDim(i, bounds.getLo(oRefi), bounds.getHi(oRefi), bounds.getDiff(oRefi));
      redAttTypes[i] = attTypes[oRefi];
      // redBounds.setBB(bs);
    }
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
   * mk_zdpoint in original impl
   * 
   * @param a
   * @param b
   * @return
   */
  private double[] findMaximumWithCholesky(double[][] a, double[] b) {
    double[][] am = times(a, -1);
    CholeskyDecomposition chol = new CholeskyDecomposition(am);
    if(chol.isSPD()) {
      return chol.solve(b);
    }
    return null;
  }

  public double evalueateQuadraticFormula(double[][] a, double[] b, double c, double[] point) {
    return 0.5 * scalarProduct(point, times(a, point)) + scalarProduct(point, b) + c;
  }

  /**
   * y(x) = 0.5 * x^T a x + b^T x
   * 
   * y'(x) = result^T x + return
   * 
   * return = b[k]
   * result[i] = a[k,i]
   * 
   * @param a
   * @param b
   * @param result
   * @return
   */
  private double createPartialDerivative(double[][] a, double[] b, int k, double[] result) {
    for(int i = 0; i < result.length; i++) {
      result[i] = a[k][i];
    }
    return b[k];
  }

  //////////////////////////////////////////////////////////////////////// from
  //////////////////////////////////////////////////////////////////////// quopt

  private enum AttributeState {
    ATT_LOLIM, ATT_HILIM, UNCONSTR, CONSTR
  }

  public static class ProblemData {
    double[][] a;

    double[] b, derA, result;

    double piPow;

    Boundingbox box;

    AttributeState[] attTypes;

    public ProblemData(int size) {
      a = new double[size][size];
      b = new double[size];
      result = new double[size];
      derA = new double[size];
      piPow = FastMath.pow(FastMath.sqrt(FastMath.PI), size);
      attTypes = new AttributeState[size];
      box = new Boundingbox(new double[3][size]);
    }
  }
}
