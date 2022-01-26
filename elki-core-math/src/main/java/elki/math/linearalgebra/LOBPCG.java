/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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

import elki.math.linearalgebra.pca.EigenPair;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.ParameterException;

/**
 * Calculate the first Eigenvalues of a symmetric matrix.
 * 
 * This is still WIP and does not yield the correct values right now.
 * 
 * 
 * <p>
 * Knyazev, A. V. and Argentati, M. E. and Lashuk, I. and Ovtchinnikov, E. E.
 * <br>
 * Block Locally Optimal Preconditioned Eigenvalue Xolvers (BLOPEX) in Hypre and
 * PETSc <br>
 * SIAM Journal on Scientific Computing, 2007
 * 
 * <p>
 * https://github.com/bytesnake/ndarray-linalg/blob/master/src/lobpcg/lobpcg.rs
 * 
 * @author Robert Gehde
 *
 */
@Reference(authors = "Knyazev, A. V. and Argentati, M. E. and Lashuk, I. and Ovtchinnikov, E. E.", //
    title = "Block Locally Optimal Preconditioned Eigenvalue Xolvers (BLOPEX) in Hypre and PETSc", //
    url = "https://doi.org/10.1137/060661624", //
    booktitle = "SIAM Journal on Scientific Computing 29, 2007")
public class LOBPCG {
  final int d;

  private MatrixAdapter adapter;

  private double threshold;

  /**
   * 
   * Constructor for the LOBPCG Method. Can be used multiple times.
   *
   * @param d dimensionality of the data
   * @param adapter adapter containing the data- and preconditioning matrices
   * @param threshold accuracy threshold
   * @throws ParameterException
   */
  public LOBPCG(int d, MatrixAdapter adapter, double threshold) throws ParameterException {
    this.d = d;
    this.threshold = threshold;
    if(d <= 0) {
      throw new ParameterException("musst contain data");
    }
    this.adapter = adapter;
  }

  /**
   * Calculate eigenpairs for this LOBPCG-Object. Perform at most maxiteration
   * iterations.
   * Calculates the next |x| eigenpairs which are orthogonal to y.
   * 
   * @param x matrix of initial vectors
   * @param y matrix of contraint vectors
   * @param maxiterations maximum number of iterations
   * @return
   */
  public EigenPair[] calculateEigenvalues(double[][] x, double[][] y, int maxiterations) {
    if(x == null || x.length != d) {
      return null;
    }
    // INITS
    final int m = x[0].length;
    // mats in R^nxm = R^dxm
    double[][] w = new double[d][m], p = new double[d][m], c = new double[d][m],
        ax = new double[d][m], aw = new double[d][m], ap = new double[d][m];

    // mats for partitioning
    double[][] cx = new double[m][m], cw = new double[m][m],
        cp = new double[m][m];

    // these mats need to be resized (up to m times).
    // gramA/B could be sparsely filled, but then we would need to allocate one
    // for gramB^-1 * gramA
    double[][] r = null, gramA = null, gramB = null;

    // check if conditions are given and create necessary mats (lxm)
    int l = (y != null && y.length > 0) ? y[0].length : 0;
    double[][] lm = l > 0 ? new double[l][m] : null;

    // CALCULATION
    // apply constraints to x
    double[][] yty = null;
    if(l != 0) {
      yty = VMath.transposeTimes(y, y);
      // other implementation uses cholesky decomp to calculate inverse (might
      // be faster?)
      yty = VMath.inverse(yty);
      lm = VMath.transposeTimes(y, x);
      lm = timesEqualsSecond(yty, lm);
      x = minusTimesEquals(x, y, lm);
    }

    // b orthonormalize x
    r = orthonormalize(x, r, null);
    // update ax
    adapter.ax(x, ax);

    // compute initial ritz vectors; solve eigenproblem
    EigenvalueDecomposition eigen = new EigenvalueDecomposition(VMath.transposeTimes(x, ax));
    double[][] eig = eigen.getV();
    double[] lambdadiag = eigen.getRealEigenvalues();

    // and compute x, ax
    x = timesEqualsFirst(x, eig);
    ax = timesEqualsFirst(ax, eig);

    // build index set
    long[] indices = BitsUtil.ones(m);
    int cursize = m;
    // flags for resizing r,gramA/B
    boolean reinitR = true, reinitGram = true;
    // iterate up to maxit times
    for(int i = 0; i < maxiterations; i++) {
      // compute the residuals
      w = reducedMinusReducedTimesDiagReduced(ax, x, lambdadiag, w, indices);
      // Exclude from the index set J the indices that correspond to residual
      // vectors for which the norm has become smaller than the tolerance.
      // If J, then becomes empty; exit loop.
      for(int j = BitsUtil.nextSetBit(indices, 0); j != -1; j = BitsUtil.nextSetBit(indices, j + 1)) {
        if(BitsUtil.get(indices, j)) {
          // FIXME: unnötige Kopie, dot-produkt direkt berechnen:
          double[] wj = VMath.getCol(w, j);
          if(VMath.dot(wj, wj) < threshold) {
            BitsUtil.clearI(indices, j);
            cursize--;
            reinitR = reinitGram = true;
          }
        }
      }
      if(cursize == 0) {
        break;
      }
      if(reinitR) {
        r = new double[cursize][cursize];
        reinitR = false;
      }

      // apply preconditioner to residuals W
      w = adapter.txReduced(w, w, indices);

      // apply the constraints to the preconditioned residuals W
      if(l != 0) {
        // yty contains inv(t(y) * y)
        lm = transposeTimesReducedIntoReduced(y, w, lm, indices);
        lm = timesReducedEqualsSecondReduced(yty, lm, indices);
        w = reducedMinusTimesReducedEqualsReduced(w, y, lm, indices);
      }

      // compute BW_j and b orthonormalize w_j
      // VMath.normalizeColumns(w);
      r = orthonormalize(w, r, indices);

      // compute aw_j
      aw = adapter.axReduced(w, aw, indices);
      if(i > 0) {
        // b-orthonormalize p_j
        // VMath.normalizeColumns(p);
        r = orthonormalize(p, r, indices);
        // update ap
        reducedTimesCompactEqualsFirstReduced(ap, r, indices);
      }

      // PERFORM RAYLEIGH RITZ METHOD
      // specify the initial gram matrix and set reinit gram 2 for 2nd iteration
      if(i == 0) {
        final int gramsize = m + cursize;
        gramA = new double[gramsize][gramsize];
        gramB = new double[gramsize][gramsize];
        reinitGram = true;
      }
      else if(reinitGram) {
        final int gramsize = m + 2 * cursize;
        gramA = new double[gramsize][gramsize];
        gramB = new double[gramsize][gramsize];
        reinitGram = false;
      }

      // compute symmetric gram matrices
      buildGramMatrices(gramA, gramB, lambdadiag, x, w, aw, p, ap, i, cursize, indices);

      // solve generalized eigenvalue problem
      // TODO: this calculates 3m eigenpairs, but we only need m
      // It could be done with Lanczos, but when i tried it i got alot of random
      // values, probably because my implementation was faulty or unstable.
      // As the other implementation uses a complete decomposition as well, I
      // would keep this for now until all errors are resolved

      double[][] t = VMath.times(VMath.inverse(gramB), gramA);
      // LanczosEigenvalue laneig = new LanczosEigenvalue(t,m);
      // c = laneig.eigv;
      // System.arraycopy(laneig.lambda, 0, lambdadiag, 0, m);
      eigen = new EigenvalueDecomposition(t);
      c = eigen.getV();
      System.arraycopy(eigen.getRealEigenvalues(), 0, lambdadiag, 0, m);

      // compute Ritz vectors
      if(i > 0) {
        // partition c into ... according to number of columns in x,w_j and p_j
        // ....| cx | m x m
        // c = | cw | j x m
        // ....| cp | j x m
        partition(c, cx, cw, cp, indices, cursize, m);
        // update p, ap
        p = reducedTimesPlusReducedTimesEqualsSecond(w, cw, p, cp, indices);
        ap = reducedTimesPlusReducedTimesEqualsSecond(aw, cw, ap, cp, indices);
        // update x, ax
        timesPlusEqualsFirst(x, cx, p);
        timesPlusEqualsFirst(ax, cx, ap);
      }
      else {
        // partition c into ... according to number of columns in x and w_j
        // ....| cx |
        // c = | cw |
        partition(c, cx, cw, indices, cursize, m);
        // update p, ap
        reducedTimesRowReduced(w, cw, p, indices);
        reducedTimesRowReduced(aw, cw, ap, indices);
        // update x, ax
        timesPlusEqualsFirst(x, cx, p);
        timesPlusEqualsFirst(ax, cx, ap);
      }
    }
    // return the eigenvectors X and the eigenvalues lambda.
    EigenPair[] res = new EigenPair[m];
    for(int i = 0; i < m; i++) {
      res[i] = new EigenPair(VMath.getCol(x, i), lambdadiag[i]);
    }
    return res;
  }

  /**
   * builds the gram matrices from the given matrices
   * 
   * @param gramA target gram matrix
   * @param gramB target gram matrix
   * @param lambdadiag current eigenvalues
   * @param x current eigenvectors
   * @param w current residuals
   * @param aw a* current residuals
   * @param p current "LOBPCG-directions"
   * @param ap a*current "LOBPCG-directions"
   * @param cursize remaining indices
   * @param indices indicates the filled columns
   * @param it number of iteration
   */
  protected void buildGramMatrices(double[][] gramA, double[][] gramB, double[] lambdadiag, double[][] x, double[][] w, double[][] aw, double[][] p, double[][] ap, int it, int cursize, long[] indices) {
    final int m = lambdadiag.length;
    // GRAMA
    for(int i = 0; i < m; i++) {
      gramA[i][i] = lambdadiag[i];
    }
    transposeTimesReducedIntoCompactSymmetric(x, aw, gramA, indices, 0, m);
    if(it > 0) {
      transposeTimesReducedIntoCompactSymmetric(x, ap, gramA, indices, 0, m + cursize);
    }
    reducedTransposeTimesReducedIntoCompactSymmetric(w, aw, gramA, indices, m, m);
    if(it > 0) {
      reducedTransposeTimesReducedIntoCompactSymmetric(w, ap, gramA, indices, m, m + cursize);
      reducedTransposeTimesReducedIntoCompactSymmetric(p, ap, gramA, indices, m + cursize, m + cursize);
    }
    // GRAMB
    final int t = m + (it > 0 ? 2 : 1) * cursize;
    // TODO: this diagonal can be swapped by xtx, wtw, ptp,
    // as done in the online implementation. But it should work like this as
    // well according to the paper
    for(int i = 0; i < t; i++) {
      gramB[i][i] = 1;
    }
    transposeTimesReducedIntoCompactSymmetric(x, w, gramB, indices, 0, m);
    if(it > 0) {
      transposeTimesReducedIntoCompactSymmetric(x, p, gramB, indices, 0, m + cursize);
      reducedTransposeTimesReducedIntoCompactSymmetric(w, p, gramB, indices, m, m + cursize);
    }
  }

  /**
   * partition c into cx and cw.
   * 
   * cw rows are filled at indices
   * 
   * @param c eigenvector matrix
   * @param cx target for partition
   * @param cw target for partition
   * @param indices indicates the filled columns
   * @param cursize remaining indices
   */
  protected void partition(double[][] c, double[][] cx, double[][] cw, long[] indices, int cursize, int m) {
    for(int i = 0; i < cx.length; i++) {
      System.arraycopy(c[i], 0, cx[i], 0, m);
    }
    int j = BitsUtil.nextSetBit(indices, 0);
    for(int i = cx.length; i < cx.length + cursize; i++) {
      System.arraycopy(c[i], 0, cw[j], 0, m);
      j = BitsUtil.nextSetBit(indices, j + 1);
    }
  }

  /**
   * partition c into cx,cw,cp according to indices
   * 
   * cw,cp rows are filled at indices
   * 
   * @param c eigenvector matrix
   * @param cx target for partition
   * @param cw target for partition
   * @param cp target for partition
   * @param indices indicates the filled columns
   * @param cursize remaining indices (size for cw, cp)
   * @param m number of vectors to calculate (size for cx)
   */
  protected void partition(double[][] c, double[][] cx, double[][] cw, double[][] cp, long[] indices, int cursize, int m) {
    for(int i = 0; i < cx.length; i++) {
      System.arraycopy(c[i], 0, cx[i], 0, m);
    }
    int j = BitsUtil.nextSetBit(indices, 0);
    for(int i = cx.length; i < cx.length + cursize; i++) {
      System.arraycopy(c[i], 0, cw[j], 0, m);
      j = BitsUtil.nextSetBit(indices, j + 1);
    }
    j = BitsUtil.nextSetBit(indices, 0);
    for(int i = cx.length + cursize; i < cx.length + 2 * cursize; i++) {
      System.arraycopy(c[i], 0, cp[j], 0, m);
      j = BitsUtil.nextSetBit(indices, j + 1);
    }
  }

  /**
   * orthonormalizes matrix via cholesky-decomposition and inversion. Cache
   * can be used to reduce array creation.
   * overwrites matrix and cache, but not base and indices
   * 
   * @param matrix matrix to orthonormalize
   * @param cache
   * @param indices
   * @return
   */
  protected static double[][] orthonormalize(double[][] matrix, double[][] cache, long[] indices) {
    if(indices != null) {
      cache = reducedTransposeTimesReducedIntoCompact(matrix, cache, indices);
      cache = new CholeskyDecomposition(cache).getL();
      cache = VMath.inverse(VMath.transpose(cache));
      // cache = VMath.inverse(cache);
      matrix = reducedTimesCompactEqualsFirstReduced(matrix, cache, indices);
    }
    else {
      cache = VMath.transposeTimes(matrix, matrix);
      cache = new CholeskyDecomposition(cache).getL();
      cache = VMath.inverse(VMath.transpose(cache));
      // cache = VMath.inverse(cache);
      matrix = timesEqualsFirst(matrix, cache);
    }
    return cache;
  }

  /*
   * MATRIX OPERATIONS:
   * 
   */

  /**
   * calculates a_j^T * a_j and saves it into target
   * 
   * @param a reduced, sparsely filled
   * @param target reduced, densely filled
   * @param indices indicates the filled columns
   * @return target
   */
  protected static double[][] reducedTransposeTimesReducedIntoCompact(double[][] a, double[][] target, long[] indices) {
    final int rowdim = a.length;
    double[] acol = new double[rowdim];

    for(int i = BitsUtil.nextSetBit(indices, 0),
        i2 = 0; i != -1; i = BitsUtil.nextSetBit(indices, i + 1), i2++) {
      for(int j = 0; j < rowdim; j++) {
        acol[j] = a[j][i];
      }
      // diagonal entry
      double res = 0;
      for(int k = 0; k < rowdim; k++) {
        res += acol[k] * acol[k];
      }
      target[i2][i2] = res;
      // other entries
      for(int j = BitsUtil.nextSetBit(indices, i + 1), j2 = i2 + 1; j != -1; j = BitsUtil.nextSetBit(indices, j + 1), j2++) {
        res = 0;
        for(int k = 0; k < rowdim; k++) {
          res += acol[k] * a[k][j];
        }
        target[i2][j2] = res;
        target[j2][i2] = res;
      }
    }
    return target;
  }

  /**
   * calculates a1_j * a2 and saves it into a1
   * 
   * @param a1 reduced, sparsely filled
   * @param a2 reduced, densely filled
   * @param indices indicates the filled columns
   * @return a1
   */
  protected static double[][] reducedTimesCompactEqualsFirstReduced(double[][] a1, double[][] a2, long[] indices) {
    final int rowdim1 = a1.length;
    final int rowdim2 = a2.length;
    double[] aline = new double[rowdim2];
    for(int i = 0; i < rowdim1; i++) {
      // copy important part of first row
      for(int j = BitsUtil.nextSetBit(indices, 0),
          j2 = 0; j != -1; j = BitsUtil.nextSetBit(indices, j + 1), j2++) {
        aline[j2] = a1[i][j];
      }
      for(int j = BitsUtil.nextSetBit(indices, 0), j2 = 0; j != -1; j = BitsUtil.nextSetBit(indices, j + 1), j2++) {
        double res = 0;
        for(int k = 0; k < rowdim2; k++) {
          res += aline[k] * a2[k][j2];
        }
        a1[i][j] = res;
      }
    }
    return a1;
  }

  /**
   * 
   * calculates a1_j * a2 and saves it into target
   * 
   * @param a1 reduced, filled sparsely (j columns filled)
   * @param a2 reduced, filled sparsely (j rows filled)
   * @param target fully filled matrix
   * @param indices indicates the filled columns/rows
   * @return target
   */
  protected static double[][] reducedTimesRowReduced(double[][] a1, double[][] a2, double[][] target, long[] indices) {
    final int rowdim1 = a1.length, coldim1 = a1[0].length;
    final int coldim2 = a2[0].length;
    // Optimized ala Jama.
    final double[] Bcolj = new double[coldim1];
    for(int j = 0; j < coldim2; j++) {
      // Make a linear copy of column j from B
      for(int k = BitsUtil.nextSetBit(indices, 0); k != -1; k = BitsUtil.nextSetBit(indices, k + 1)) {
        Bcolj[k] = a2[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < rowdim1; i++) {
        final double[] Arowi = a1[i];
        double s = 0;
        for(int k = BitsUtil.nextSetBit(indices, 0); k != -1; k = BitsUtil.nextSetBit(indices, k + 1)) {
          s += Arowi[k] * Bcolj[k];
        }
        target[i][j] = s;
      }
    }
    return target;
  }

  /**
   * calculates a1 * a2 + a3 and saves it into x
   * 
   * @param a1 fully filled matrix
   * @param a2 fully filled matrix
   * @param a3 fully filled matrix
   * @return a1
   */
  protected static double[][] timesPlusEqualsFirst(double[][] a1, double[][] a2, double[][] a3) {
    final int rowdim1 = a1.length;
    final int coldim1 = a1[0].length;
    double[] aline = new double[coldim1];
    for(int i = 0; i < rowdim1; i++) {
      System.arraycopy(a1[i], 0, aline, 0, coldim1);
      for(int j = 0; j < coldim1; j++) {
        double res = 0;
        for(int k = 0; k < coldim1; k++) {
          res += aline[k] * a2[k][j];
        }
        a1[i][j] = res + a3[i][j];
      }
    }
    return a1;
  }

  /**
   * calculates a2 = a1_j * cw + a2_j *cp and saves it into a2.
   * Note that a2 is overwritten completely in this function
   * 
   * @param a1 reduced, filled sparsely (j columns filled)
   * @param cw reduced, filled sparsely (j rows filled)
   * @param a2 reduced, filled sparsely (j columns filled)
   * @param cp reduced, filled sparsely (j rows filled)
   * @param indices indicates the filled columns/rows
   * @return completely filled a2
   */
  protected static double[][] reducedTimesPlusReducedTimesEqualsSecond(double[][] a1, double[][] cw, double[][] a2, double[][] cp, long[] indices) {
    final int rowdim1 = a1.length;
    final int coldim1 = a1[0].length;
    double[] aline = new double[coldim1];
    for(int i = 0; i < rowdim1; i++) {
      System.arraycopy(a2[i], 0, aline, 0, coldim1);
      for(int j = 0; j < coldim1; j++) {
        double res = 0;
        for(int k = BitsUtil.nextSetBit(indices, 0); k != -1; k = BitsUtil.nextSetBit(indices, k + 1)) {
          res += a1[i][k] * cw[k][j] + aline[k] * cp[k][j];
        }
        a2[i][j] = res;
      }
    }
    return a2;
  }

  /**
   * calculates a1_j - a2 * a3_j and saves it into a1
   * 
   * 
   * @param a1 reduced, sparsely filled
   * @param a2 fully filled
   * @param a3 reduced, sparsely filled
   * @param indices indicates the filled columns
   */
  protected static double[][] reducedMinusTimesReducedEqualsReduced(double[][] a1, double[][] a2, double[][] a3, long[] indices) {
    final int rowdim1 = a1.length;
    final int rowdim3 = a3.length;
    double[] ccol = new double[rowdim3];
    for(int j = BitsUtil.nextSetBit(indices, 0); j != -1; j = BitsUtil.nextSetBit(indices, j + 1)) {
      for(int i = 0; i < rowdim3; i++) {
        ccol[i] = a3[i][j];
      }
      for(int i = 0; i < rowdim1; i++) {
        double res = 0;
        for(int k = 0; k < rowdim3; k++) {
          res += a2[i][k] * ccol[k];
        }
        a1[i][j] -= res;
      }
    }
    return a1;
  }

  /**
   * calculates a1 * a2_j and saves it into a2_j
   * 
   * @param a1 in this case squared, so rowdim1 = coldim1 = rowdim2
   * @param a2 reduced, sparsely filled
   * @param indices indicates the filled columns
   * @return a2
   */
  protected static double[][] timesReducedEqualsSecondReduced(double[][] a1, double[][] a2, long[] indices) {
    final int rowdim2 = a2.length;
    double[] bcol = new double[rowdim2];
    for(int j = BitsUtil.nextSetBit(indices, 0); j != -1; j = BitsUtil.nextSetBit(indices, j + 1)) {
      for(int i = 0; i < rowdim2; i++) {
        bcol[i] = a2[i][j];
      }
      for(int i = 0; i < rowdim2; i++) {
        double res = 0;
        for(int k = 0; k < rowdim2; k++) {
          res += a1[i][k] * bcol[k];
        }
        a2[i][j] = res;
      }
    }
    return a2;
  }

  /**
   * calculates a1^T * a2_j and saves it into target_j
   * 
   * 
   * @param a1 fully filled
   * @param a2 reduced, sparsely filled
   * @param target reduced, sparsely filled
   * @param indices indicates the filled columns
   * @return target
   */
  protected static double[][] transposeTimesReducedIntoReduced(double[][] a1, double[][] a2, double[][] target, long[] indices) {
    final int coldim1 = a1[0].length;
    final int rowdim1 = a1.length;
    double[] acol = new double[rowdim1];
    for(int i = 0; i < coldim1; i++) {
      for(int j = 0; j < rowdim1; j++) {
        acol[j] = a1[j][i];
      }
      for(int j = BitsUtil.nextSetBit(indices, 0); j != -1; j = BitsUtil.nextSetBit(indices, j + 1)) {
        double res = 0;
        for(int k = 0; k < rowdim1; k++) {
          res += acol[k] * a2[k][j];
        }
        target[i][j] = res;
      }
    }
    return target;
  }

  /**
   * calculates a1^T * a2_j and saves it into target starting at istart and
   * jstart and keeping the symmetric property at the target
   * 
   * 
   * @param a1 fully filled
   * @param a2 reduced, sparsely filled
   * @param target in this class gram matrix
   * @param istart starting row/col coordinate
   * @param jstart starting col/row coordinate
   * @param indices indicates the filled columns
   * @return target
   */
  protected static double[][] transposeTimesReducedIntoCompactSymmetric(double[][] a1, double[][] a2, double[][] target, long[] indices, int istart, int jstart) {
    final int coldim1 = a1[0].length;
    final int rowdim1 = a1.length; // is diff to rowdimT bc transposed
    double[] acol = new double[rowdim1];
    for(int i = 0, i2 = istart; i < coldim1; i++, i2++) {
      for(int j = 0; j < rowdim1; j++) {
        acol[j] = a1[j][i];
      }
      for(int j = BitsUtil.nextSetBit(indices, 0), j2 = jstart; j != -1; j = BitsUtil.nextSetBit(indices, j + 1), j2++) {
        double res = 0;
        for(int k = 0; k < rowdim1; k++) {
          res += acol[k] * a2[k][j];
        }
        target[i2][j2] = target[j2][i2] = res;
      }
    }
    return target;
  }

  /**
   * calculates a1_j^T * a2_j and saves it into target starting at istart and
   * jstart and keeping the symmetric property at the target
   * 
   * @param y reduced, sparsely filled
   * @param w reduced, sparsely filled
   * @param target in this class gram Matrix
   * @param indices indicates the filled columns
   * @return target
   */
  protected static double[][] reducedTransposeTimesReducedIntoCompactSymmetric(double[][] y, double[][] w, double[][] target, long[] indices, int istart, int jstart) {
    final int rowdim1 = y.length; // is diff to rowdimT bc transposed
    double[] acol = new double[rowdim1];
    for(int i = BitsUtil.nextSetBit(indices, 0), i2 = istart; i != -1; i = BitsUtil.nextSetBit(indices, i + 1), i2++) {
      for(int j = 0; j < rowdim1; j++) {
        acol[j] = y[j][i];
      }
      for(int j = BitsUtil.nextSetBit(indices, 0), j2 = jstart; j != -1; j = BitsUtil.nextSetBit(indices, j + 1), j2++) {
        double res = 0;
        for(int k = 0; k < rowdim1; k++) {
          res += acol[k] * w[k][j];
        }
        target[i2][j2] = target[j2][i2] = res;
      }
    }
    return target;
  }

  /**
   * calculates a1_j - a2_j * DIAG_j and saves it into target
   * 
   * 
   * @param a1 reduced, sparsely filled
   * @param a2 reduced, sparsely filled
   * @param diag sparsely filled 1d array representing diagonal matrix
   * @param target reduced, sparsely filled
   * @param indices indicates the filled columns
   * @return target
   */
  protected static double[][] reducedMinusReducedTimesDiagReduced(double[][] a1, double[][] a2, double[] diag, double[][] target, long[] indices) {
    int rowdim1 = a1.length;
    for(int i = 0; i < rowdim1; i++) {
      for(int j = BitsUtil.nextSetBit(indices, 0); j != -1; j = BitsUtil.nextSetBit(indices, j + 1)) {
        // multiply it with each row from A
        target[i][j] = a1[i][j] - a2[i][j] * diag[j];
      }
    }
    return target;
  }

  /**
   * calculates a1 - a2 * a3 and saves it into a1
   * 
   * @param a1 fully filled
   * @param a2 fully filled
   * @param a3 fully filled
   * @return a1
   */
  protected static double[][] minusTimesEquals(double[][] a1, double[][] a2, double[][] a3) {
    int coldim2 = a3[0].length;
    int coldim1 = a2[0].length;
    int rowdim1 = a2.length;
    final double[] Bcolj = new double[coldim1];
    for(int j = 0; j < coldim2; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < coldim1; k++) {
        Bcolj[k] = a3[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < rowdim1; i++) {
        final double[] Arowi = a2[i];
        double s = 0;
        for(int k = 0; k < coldim1; k++) {
          s += Arowi[k] * Bcolj[k];
        }
        a1[i][j] = a1[i][j] - s;
      }
    }
    return a1;
  }

  /**
   * calculates a1 * a2 and saves it into a1
   * 
   * @param a1 fully filled
   * @param a2 fully filled
   * @return a1
   */
  protected static double[][] timesEqualsFirst(double[][] a1, double[][] a2) {
    final int rowdim1 = a1.length;
    final int coldim1 = a1[0].length;
    double[] aline = new double[coldim1];
    for(int i = 0; i < rowdim1; i++) {
      System.arraycopy(a1[i], 0, aline, 0, coldim1);
      for(int j = 0; j < coldim1; j++) {
        double res = 0;
        for(int k = 0; k < coldim1; k++) {
          res += aline[k] * a2[k][j];
        }
        a1[i][j] = res;
      }
    }
    return a1;
  }

  /**
   * calculates a1*a2 and saves it into a2
   * 
   * @param a1 fully filled
   * @param a2 fully filled
   * @return a2
   */
  protected static double[][] timesEqualsSecond(double[][] a1, double[][] a2) {
    int coldim2 = a2[0].length;
    int coldim1 = a1[0].length;
    int rowdim1 = a1.length;
    final double[] Bcolj = new double[coldim1];

    for(int j = 0; j < coldim2; j++) {
      // Make a linear copy of column j from B
      for(int k = 0; k < coldim1; k++) {
        Bcolj[k] = a2[k][j];
      }
      // multiply it with each row from A
      for(int i = 0; i < rowdim1; i++) {
        final double[] Arowi = a1[i];
        double s = 0;
        for(int k = 0; k < coldim1; k++) {
          s += Arowi[k] * Bcolj[k];
        }
        a2[i][j] = s;
      }
    }
    return a2;
  }

  /**
   * Matrix adapter used for LOBPCG eigenvalue calculation
   * 
   * @author Robert Gehde
   *
   */
  public static interface MatrixAdapter {
    /**
     * calculate target = a * x
     * 
     * @param x matrix
     * @param target target matrix
     * @return target
     */
    public void ax(double[][] x, double[][] target);

    /**
     * calculate target = t * x
     * (actually not used)
     * 
     * @param x matrix
     * @param target target matrix
     * @return target
     */
    public void tx(double[][] x, double[][] target);

    /**
     * this method IS called with w=target (so ensure inplace rules)
     * If you cant get it inplace, make sure to ignore target and return a newly
     * created array!
     * 
     * @param x sparsely filled matrix (|indices| cols are filled)
     * @param target target matrix
     * @param indices
     * @return target
     */
    public double[][] txReduced(double[][] x, double[][] target, long[] indices);

    /**
     * 
     * @param x sparsely filled matrix (|indices| cols are filled)
     * @param target target matrix
     * @param indices
     * @return target
     */
    public double[][] axReduced(double[][] x, double[][] target, long[] indices);
  }

  public static class DistanceArrayMatrixAdaper implements MatrixAdapter {

    final double[][] distance, precond;

    public DistanceArrayMatrixAdaper(double[][] distance, double[][] precond) {
      this.distance = distance;
      this.precond = precond;
    }

    /*
     * target = a * x 
     */
    @Override
    public void ax(double[][] x, double[][] target) {
      final int rowdim1 = distance.length, coldim1 = distance[0].length;
      final int rowdim2 = x.length, coldim2 = x[0].length;
      assert coldim1 == rowdim2;
      // Optimized implementation, exploiting the storage layout
      // Optimized ala Jama. jik order.
      final double[] Bcolj = new double[rowdim2];
      for(int j = 0; j < coldim2; j++) {
        // Make a linear copy of column j from B
        for(int k = 0; k < rowdim2; k++) {
          Bcolj[k] = x[k][j];
        }
        // multiply it with each row from A
        for(int i = 0; i < rowdim1; i++) {
          final double[] Arowi = distance[i];
          double s = 0;
          for(int k = 0; k < coldim1; k++) {
            s += Arowi[k] * Bcolj[k];
          }
          target[i][j] = s;
        }
      }
    }

    @Override
    public void tx(double[][] x, double[][] target) {
      final int rowdim1 = precond.length, coldim1 = precond[0].length;
      final int coldim2 = x[0].length;
      // Optimized implementation, exploiting the storage layout
      // Optimized ala Jama. jik order.
      final double[] Bcolj = new double[coldim1];
      for(int j = 0; j < coldim2; j++) {
        // Make a linear copy of column j from B
        for(int k = 0; k < coldim1; k++) {
          Bcolj[k] = x[k][j];
        }
        // multiply it with each row from A
        for(int i = 0; i < rowdim1; i++) {
          final double[] Arowi = precond[i];
          double s = 0;
          for(int k = 0; k < coldim1; k++) {
            s += Arowi[k] * Bcolj[k];
          }
          target[i][j] = s;
        }
      }
    }

    @Override
    public double[][] txReduced(double[][] w, double[][] target, long[] indices) {
      final int rowdim2 = w.length;
      double[] bcol = new double[rowdim2];
      for(int j = BitsUtil.nextSetBit(indices, 0); j != -1; j = BitsUtil.nextSetBit(indices, j + 1)) {
        for(int i = 0; i < rowdim2; i++) {
          bcol[i] = w[i][j];
        }
        for(int i = 0; i < rowdim2; i++) {
          double res = 0;
          for(int k = 0; k < rowdim2; k++) {
            res += precond[i][k] * bcol[k];
          }
          target[i][j] = res;
        }
      }
      return target;
    }

    @Override
    public double[][] axReduced(double[][] w, double[][] target, long[] indices) {
      final int rowdim2 = w.length;
      double[] bcol = new double[rowdim2];
      for(int j = BitsUtil.nextSetBit(indices, 0); j != -1; j = BitsUtil.nextSetBit(indices, j + 1)) {
        for(int i = 0; i < rowdim2; i++) {
          bcol[i] = w[i][j];
        }
        for(int i = 0; i < rowdim2; i++) {
          double res = 0;
          for(int k = 0; k < rowdim2; k++) {
            res += distance[i][k] * bcol[k];
          }
          target[i][j] = res;
        }
      }
      return target;
    }
  }
}
