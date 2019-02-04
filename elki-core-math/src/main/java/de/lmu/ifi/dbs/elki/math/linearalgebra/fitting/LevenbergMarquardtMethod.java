/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.math.linearalgebra.fitting;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;

/**
 * Function parameter fitting using Levenberg-Marquardt method.
 * <p>
 * The Levenberg-Marquardt Algorithm (LMA) is a combination of the Gauss-Newton
 * Algorithm (GNA) and the method of steepest descent. As such it usually gives
 * more stable results and better convergence.
 * <p>
 * Implemented loosely based on the book:<br>
 * Numerical Recipes In C: The Art Of Scientific Computing<br>
 * Press, W.H. and Teukolsky, S.A. and Vetterling, W.T. and Flannery, B.P.<br>
 * Cambridge University Press, Cambridge, Mass, 1992
 * <p>
 * Due to their license, we cannot use their code, but we have to implement the
 * mathematics ourselves. We hope the loss in precision isn't too big.
 * <p>
 * TODO: Replace implementation by one based on<br>
 * M.I.A. Lourakis levmar<br>
 * Levenberg-Marquardt nonlinear least squares algorithms in C/C++
 * <p>
 * Which supposedly offers increased robustness.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @has - - - FittingFunction
 * @navassoc - create - FittingFunctionResult
 */
public class LevenbergMarquardtMethod {
  /**
   * Function to fit to
   */
  public FittingFunction func;

  /**
   * Data to fit the function to
   */
  private double[] x;

  private double[] y;

  private double[] s;

  /**
   * Number of parameters
   */
  private int numparams;

  /**
   * Parameters to use in fitting
   */
  private double[] params;

  /**
   * Chi-Squared information for parameters
   */
  private double chisq;

  /**
   * Number of parameters to fit
   */
  private int numfit;

  /**
   * Which parameters to fit
   */
  private boolean[] dofit;

  /**
   * Working space for covariance matrix
   */
  private double[][] covmat;

  /**
   * Working space for alphas
   */
  private double[][] alpha;

  /**
   * Lambda (refinement step size)
   */
  private double lambda;

  /**
   * More working buffers
   */
  private double[] paramstry;

  private double[] beta;

  private double[] deltaparams;

  /**
   * Maximum number of iterations in run()
   */
  public int maxruns = 1000;

  /**
   * Maximum number of small improvements (stopping condition)
   */
  public int maxsmall = 3;

  /**
   * "Small value" condition for stopping
   */
  public double small = 0.01;

  /**
   * Function fitting using Levenberg-Marquardt Method.
   * 
   * @param func Function to fit to
   * @param x Measurement points
   * @param y Actual function values
   * @param s Confidence / Variance in measurement data
   * @param params Initial parameters
   * @param dofit Flags on which parameters to optimize
   */
  public LevenbergMarquardtMethod(FittingFunction func, double[] params, boolean[] dofit, double[] x, double[] y, double[] s) {
    assert x.length == y.length;
    assert x.length == s.length;
    assert params.length == dofit.length;

    // function to optimize for
    this.func = func;

    // Store parameters
    this.x = x;
    this.y = y;
    this.s = s;
    this.params = params;
    this.dofit = dofit;

    // keep number of parameters ready
    this.numparams = this.params.length;

    // count how many parameters to fit
    numfit = 0;
    for(int i = 0; i < numparams; i++) {
      if(dofit[i]) {
        numfit++;
      }
    }

    assert (numfit > 0);

    // initialize working spaces
    covmat = new double[this.numfit][this.numfit];
    alpha = new double[this.numfit][this.numfit];

    // set lambda to initial value
    lambda = 0.001;

    // setup scratch spaces
    paramstry = params.clone();
    beta = new double[this.numfit];
    deltaparams = new double[numparams];

    chisq = simulateParameters(params);
  }

  /**
   * Compute new chisquared error
   * <p>
   * This function also modifies the alpha and beta matrixes!
   * 
   * @param curparams Parameters to use in computation.
   * @return new chi squared
   */
  private double simulateParameters(double[] curparams) {
    // Initialize alpha, beta
    for(int i = 0; i < numfit; i++) {
      Arrays.fill(alpha[i], 0.);
    }
    Arrays.fill(beta, 0.);

    double newchisq = 0.0;

    // Simulation loop over all data
    for(int di = 0; di < x.length; di++) {
      FittingFunctionResult res = func.eval(x[di], curparams);
      // compute inverse squared standard deviation of the point (confidence?)
      double sigma2inv = 1.0 / (s[di] * s[di]);
      double deltay = y[di] - res.y;
      // i2 and j2 are the indices that only count the params with dofit true!
      for(int i = 0, i2 = 0; i < numfit; i++) {
        if(dofit[i]) {
          double wt = res.gradients[i] * sigma2inv;
          // fill only half of the matrix, use symmetry below to complete the
          // remainder.
          for(int j = 0, j2 = 0; j <= i; j++) {
            if(dofit[j]) {
              alpha[i2][j2++] += wt * res.gradients[j];
            }
          }
          beta[i2++] += deltay * wt;
        }
      }
      newchisq += deltay * deltay * sigma2inv;
    }
    // fill symmetric side of matrix
    for(int i = 1; i < numfit; i++) {
      for(int j = i + 1; j < numfit; j++) {
        alpha[i][j] = alpha[j][i];
      }
    }

    return newchisq;
  }

  /**
   * Perform an iteration of the approximation loop.
   */
  public void iterate() {
    // build covmat out of fitting matrix by multiplying diagonal elements with
    // 1+lambda
    for(int i = 0; i < numfit; i++) {
      System.arraycopy(alpha[i], 0, covmat[i], 0, numfit);
      covmat[i][i] *= (1.0 + lambda);
    }
    // Solve the equation system (Gauss-Jordan)
    LinearEquationSystem ls = new LinearEquationSystem(covmat, beta);
    ls.solveByTotalPivotSearch();
    // update covmat with the inverse
    covmat = ls.getCoefficents();
    // and deltaparams with the solution vector
    deltaparams = ls.getRHS();
    // deltaparams = beta;
    for(int i = 0, i2 = 0; i < numparams; i++) {
      if(dofit[i]) {
        paramstry[i] = params[i] + deltaparams[i2++];
      }
    }
    double newchisq = simulateParameters(paramstry);
    // have the results improved?
    if(newchisq < chisq) {
      // TODO: Do we need a larger limit than MIN_NORMAL?
      if(lambda * 0.1 > Double.MIN_NORMAL) {
        lambda *= 0.1;
      }
      chisq = newchisq;
      // keep modified covmat as new alpha matrix
      // and da as new beta
      for(int i = 0; i < numfit; i++) {
        System.arraycopy(covmat[i], 0, alpha[i], 0, numfit);
        beta[i] = deltaparams[i];
      }
      System.arraycopy(paramstry, 0, params, 0, numparams);
    }
    else {
      // TODO: Do we need a larger limit than MAX_VALUE?
      // Does it ever make sense to go as far up?
      // Anyway, this should prevent overflows.
      if(lambda * 10 < Double.MAX_VALUE) {
        lambda *= 10;
      }
    }
  }

  /**
   * Get the final covariance matrix.
   * <p>
   * Parameters that were not to be optimized are filled with zeros.
   * 
   * @return covariance matrix for all parameters
   */
  public double[][] getCovmat() {
    // Since we worked only on params with dofit=true, we need to expand the
    // matrix to cover all parameters.
    double[][] fullcov = new double[numparams][numparams];
    for(int i = 0, i2 = 0; i < numparams; i++) {
      for(int j = 0, j2 = 0; j < numparams; j++) {
        fullcov[i][j] = (dofit[i] && dofit[j]) ? covmat[i2][j2] : 0;
        if(dofit[j]) {
          j2++;
        }
      }
      if(dofit[i]) {
        i2++;
      }
    }
    return fullcov;
  }

  /**
   * Get current parameters.
   * 
   * @return parameters
   */
  public double[] getParams() {
    return params;
  }

  /**
   * Get current ChiSquared (squared error sum)
   * 
   * @return error measure
   */
  public double getChiSq() {
    return chisq;
  }

  /**
   * Iterate until convergence, at most 100 times.
   */
  public void run() {
    int maxruns = this.maxruns, maxsmall = this.maxsmall;
    double oldchi = getChiSq();
    while(maxruns-- > 0) {
      iterate();
      double newchi = getChiSq(), deltachi = newchi - oldchi;
      oldchi = newchi;
      // stop condition: only a small improvement in Chi.
      if(deltachi < 0 && deltachi > -small && --maxsmall < 0) {
        break;
      }
    }
  }
}
