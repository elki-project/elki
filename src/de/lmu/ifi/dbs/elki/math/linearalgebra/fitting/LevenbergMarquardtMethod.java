package de.lmu.ifi.dbs.elki.math.linearalgebra.fitting;

import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;

/**
 * Function parameter fitting using Levenberg-Marquardt method
 * 
 * Implemented mostly based on:
 *   Numerical Recipes In C: The Art Of Scientific Computing
 *   ISBN 0-521-43108-5
 *   Press, W.H. and Teukolsky, S.A. and Vetterling, W.T. and Flannery, B.P.
 *   Cambridge University Press, Cambridge, Mass, 1992
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
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
   * Function fitting using Levenberg-Marquardt Method.
   * 
   * @param func Function to fit to
   * @param x Measurement points
   * @param y Actual function values
   * @param s Confidence / Variance in measurement data
   * @param params Initial parameters
   * @param dofit Flags on which parameters to optimize
   */
  public LevenbergMarquardtMethod(FittingFunction func, double params[], boolean dofit[], double[] x, double[] y, double[] s) {
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
    for (int i=0; i<numparams; i++)
      if (dofit[i]) numfit++;
    
    assert(numfit > 0);

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
   * 
   * This function also modifies the alpha and beta matrixes!
   * 
   * @param curparams Parameters to use in computation.
   * @return new chi squared
   */
  private double simulateParameters(double[] curparams) {
    // Initialize alpha, beta
    for (int i=0; i < numfit; i++)
      for (int j=0; j < numfit; j++)
        alpha[i][j] = 0.0;
    for (int i=0; i < numfit; i++)
      beta[i] = 0.0;

    double newchisq = 0.0;
    
    // Simulation loop over all data
    for (int di = 0; di < x.length; di ++) {
      FittingFunctionResult res = func.eval(x[di], curparams);
      // compute inverse squared standard deviation of the point (confidence?)
      double sigma2inv = 1.0 / (s[di]*s[di]);
      double deltay = y[di] - res.y;
      // i2 and j2 are the indices that only count the params with dofit true!
      int i2 = 0;
      for (int i=0; i < numfit; i++)
        if (dofit[i]) {
          double wt = res.gradients[i] * sigma2inv;
          int j2 = 0;
          // fill only half of the matrix, use symmetry below to complete the remainder.
          for (int j = 0; j <= i; j++)
            if (dofit[j]) {
              alpha[i2][j2] += wt * res.gradients[j];
              j2++;
            }
          beta[i2] = beta[i2] + deltay * wt;
          i2++;
        }
      newchisq = newchisq + deltay * deltay * sigma2inv;
    }
    // fill symmetric side of matrix
    for (int i=1; i < numfit; i++)
      for (int j=i+1; j < numfit; j++)
        alpha[i][j] = alpha[j][i];
    
    return newchisq;
  }
  
  public void iterate() {
    // build covmat out of fitting matrix by multiplying diagonal elements with 1+lambda
    for (int i=0; i < numfit; i++) {
      for (int j=0; j < numfit; j++)
        covmat[i][j] = alpha[i][j];
      covmat[i][i] = alpha[i][i] * (1.0  + lambda);
    }
    //System.out.println("Chisq: " + chisq);
    //System.out.println("Lambda: " + lambda);
    //System.out.print("beta: ");
    //for (double d : beta)
    //  System.out.print(d + " ");
    //System.out.println();
    // Solve the equation system (Gauss-Jordan)
    LinearEquationSystem ls = new LinearEquationSystem(covmat, beta);
    ls.solveByTotalPivotSearch();
    // update covmat with the inverse
    covmat = ls.getCoefficents();
    // and deltaparams with the solution vector
    deltaparams = ls.getRHS();
    //deltaparams = beta;
    //System.out.print("deltaparams: ");
    //for (double d : deltaparams) 
    //  System.out.print(d + " ");
    //System.out.println();
    int i2 = 0;
    for (int i=0; i < numparams; i++)
      if (dofit[i]) {
        paramstry[i] = params[i] + deltaparams[i2];
        i2++;
      }
    double newchisq = simulateParameters(paramstry);
    // have the results improved?
    if (newchisq < chisq) {
      lambda = lambda * 0.1;
      chisq = newchisq;
      // keep modified covmat as new alpha matrix
      // and da as new beta
      for (int i=0; i<numfit; i++) {
        for (int j=0; j<numfit; j++) {
          alpha[i][j] = covmat[i][j];
        }
        beta[i] = deltaparams[i];
      }
      for (int i=0; i < numparams; i++)
        params[i] = paramstry[i];
    } else {
      lambda = lambda * 10;
    }
  }

  /**
   * Get the final covariance matrix.
   * 
   * Parameters that were not to be optimized are filled with zeros.
   * 
   * @return covariance matrix for all parameters
   */
  public double[][] getCovmat() {
    // Since we worked only on params with dofit=true, we need to expand the matrix to cover all
    // parameters.
    double[][] fullcov = new double[numparams][numparams];
    int i2 = 0;
    for (int i=0; i < numparams; i++) {
      int j2 = 0;
      for (int j=0; j < numparams; j++) {
        if (dofit[i] && dofit[j])
          fullcov[i][j] = covmat[i2][j2];
        else
          fullcov[i][j] = 0.0;
        if (dofit[j]) j2++;
      }
      if (dofit[i]) i2++;
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
}
