/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * This code is directly derived from libSVM, and hence the libSVM copyright
 * apply:
 * 
 * Copyright (c) 2000-2019 Chih-Chung Chang and Chih-Jen Lin
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither name of copyright holders nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package elki.svm.solver;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.qmatrix.QMatrix;
import elki.utilities.datastructures.arrays.ArrayUtil;

/**
 * SMO solver for support vector machines, derived from libSVM.
 * <p>
 * An SMO algorithm in Fan et al., JMLR 6(2005), p. 1889--1918
 * <p>
 * Solves:
 * <br>
 * \[\min 0.5(\alpha^T Q \alpha) + p^T \alpha\]
 * <br>
 * \[y^T \alpha = \delta\]
 * \[y_i = \pm 1\]
 * \[0 &leq; alpha_i &leq; Cp \text{for} y_i = 1\]
 * \[0 &leq; alpha_i &leq; Cn \text{for} y_i = -1\]
 * <br>
 * Given:
 * <br>
 * Q, p, y, Cp, Cn, and an initial feasible point \(\alpha\)<br>
 * l is the size of vectors and matrices<br>
 * eps is the stopping tolerance<br>
 *
 * solution will be put in \(\alpha\), objective value will be put in obj
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class Solver {
  private static final Logging LOG = Logging.getLogger(Solver.class);

  int active_size;

  byte[] y;

  double[] G; // gradient of objective function

  static final byte LOWER_BOUND = 0;

  static final byte UPPER_BOUND = 1;

  static final byte FREE = 2;

  byte[] alpha_status; // LOWER_BOUND, UPPER_BOUND, FREE

  double[] alpha;

  QMatrix Q;

  double eps;

  double Cp, Cn;

  double[] p;

  int[] active_set;

  double[] G_bar; // gradient, if we treat free variables as 0

  float[] Q_i, Q_j;

  int l;

  boolean unshrink; // XXX

  double get_C(int i) {
    return (y[i] > 0) ? Cp : Cn;
  }

  void update_alpha_status(int i) {
    final double alpha_i = alpha[i];
    alpha_status[i] = (alpha_i >= get_C(i)) ? UPPER_BOUND //
        : (alpha_i <= 0) ? LOWER_BOUND : FREE;
  }

  boolean is_upper_bound(int i) {
    return alpha_status[i] == UPPER_BOUND;
  }

  boolean is_lower_bound(int i) {
    return alpha_status[i] == LOWER_BOUND;
  }

  boolean is_free(int i) {
    return alpha_status[i] == FREE;
  }

  // All the information returned
  public static class SolutionInfo {
    public double obj, rho;

    public double upper_bound_p, upper_bound_n;

    public double[] alpha;
    
    public double r_square; // Only SVDD
  }

  void swap_index(int i, int j) {
    // This will also swap QD:
    Q.swap_index(i, j);
    ArrayUtil.swap(y, i, j);
    ArrayUtil.swap(G, i, j);
    ArrayUtil.swap(alpha_status, i, j);
    ArrayUtil.swap(alpha, i, j);
    ArrayUtil.swap(p, i, j);
    ArrayUtil.swap(active_set, i, j);
    ArrayUtil.swap(G_bar, i, j);
  }

  void reconstruct_gradient() {
    // reconstruct inactive elements of G from G_bar and free variables
    if(active_size == l) {
      return;
    }

    for(int j = active_size; j < l; j++) {
      G[j] = G_bar[j] + p[j];
    }

    int nr_free = 0;
    for(int j = 0; j < active_size; j++) {
      if(is_free(j)) {
        nr_free++;
      }
    }

    if(nr_free << 1 < active_size) {
      LOG.info("The optimization may be faster with shrinknig disabled.");
    }

    if(nr_free * l > (active_size << 1) * (l - active_size)) {
      for(int i = active_size; i < l; i++) {
        Q.get_Q(i, active_size, Q_i);
        for(int j = 0; j < active_size; j++) {
          if(is_free(j)) {
            G[i] += alpha[j] * Q_i[j];
          }
        }
      }
    }
    else {
      for(int i = 0; i < active_size; i++) {
        if(is_free(i)) {
          Q.get_Q(i, l, Q_i);
          final double alpha_i = alpha[i];
          for(int j = active_size; j < l; j++) {
            G[j] += alpha_i * Q_i[j];
          }
        }
      }
    }
  }

  public SolutionInfo solve(int l, QMatrix Q, double[] p_, byte[] y_, double[] alpha_, double Cp, double Cn, double eps, boolean shrinking) {
    this.l = l;
    this.Q = Q;
    // this.QD = Q.get_QD();
    this.p = p_.clone();
    this.y = y_.clone();
    this.alpha = alpha_.clone();
    this.Cp = Cp;
    this.Cn = Cn;
    this.eps = eps;
    this.unshrink = false;
    this.Q_i = new float[l];
    this.Q_j = new float[l];

    initializeAlpha();
    initializeActiveSet();
    initializeGradient();

    // optimization step

    int max_iter = Math.max(10000000, l > Integer.MAX_VALUE / 100 ? Integer.MAX_VALUE : 100 * l);
    int counter = Math.min(l, 1000) + 1;
    int[] working_set = new int[2];

    int iter = 0;
    while(++iter <= max_iter) {
      // show progress and do shrinking
      if(--counter == 0) {
        counter = Math.min(l, 1000);
        if(shrinking) {
          do_shrinking();
        }
      }

      if(select_working_set(working_set)) {
        // reconstruct the whole gradient
        reconstruct_gradient();
        // reset active set size and check
        active_size = l;
        if(select_working_set(working_set)) {
          break;
        }
        counter = 1; // do shrinking next iteration
      }

      final int i = working_set[0], j = working_set[1];

      // update alpha[i] and alpha[j], handle bounds carefully

      double[] QD = Q.get_QD();
      Q.get_Q(i, active_size, Q_i);
      Q.get_Q(j, active_size, Q_j);

      final double C_i = get_C(i), C_j = get_C(j);
      final double old_alpha_i = alpha[i], old_alpha_j = alpha[j];

      if(y[i] != y[j]) {
        double quad_coef = QD[i] + QD[j] + 2 * Q_i[j];
        double delta = (-G[i] - G[j]) / nonzero(quad_coef);
        double diff = alpha[i] - alpha[j];
        alpha[i] += delta;
        alpha[j] += delta;

        if(diff > 0) {
          if(alpha[j] < 0) {
            alpha[j] = 0;
            alpha[i] = diff;
          }
        }
        else {
          if(alpha[i] < 0) {
            alpha[i] = 0;
            alpha[j] = -diff;
          }
        }
        if(diff > C_i - C_j) {
          if(alpha[i] > C_i) {
            alpha[i] = C_i;
            alpha[j] = C_i - diff;
          }
        }
        else {
          if(alpha[j] > C_j) {
            alpha[j] = C_j;
            alpha[i] = C_j + diff;
          }
        }
      }
      else {
        double quad_coef = QD[i] + QD[j] - 2 * Q_i[j];
        double delta = (G[i] - G[j]) / nonzero(quad_coef);
        double sum = alpha[i] + alpha[j];
        alpha[i] -= delta;
        alpha[j] += delta;

        if(sum > C_i) {
          if(alpha[i] > C_i) {
            alpha[i] = C_i;
            alpha[j] = sum - C_i;
          }
        }
        else {
          if(alpha[j] < 0) {
            alpha[j] = 0;
            alpha[i] = sum;
          }
        }
        if(sum > C_j) {
          if(alpha[j] > C_j) {
            alpha[j] = C_j;
            alpha[i] = sum - C_j;
          }
        }
        else {
          if(alpha[i] < 0) {
            alpha[i] = 0;
            alpha[j] = sum;
          }
        }
      }

      // update G
      update_G(i, j, old_alpha_i, old_alpha_j);

      // update alpha_status and G_bar
      final boolean ui = is_upper_bound(i), uj = is_upper_bound(j);
      update_alpha_status(i);
      update_alpha_status(j);
      if(ui != is_upper_bound(i)) { // changed
        Q.get_Q(i, l, Q_i);
        update_G_bar(ui ? -C_i : C_i, Q_i);
      }
      if(uj != is_upper_bound(j)) { // changed
        Q.get_Q(j, l, Q_j);
        update_G_bar(uj ? -C_j : C_j, Q_j);
      }

      if(iter >= max_iter) {
        if(active_size < l) {
          // reconstruct the whole gradient to calculate objective value
          reconstruct_gradient();
          active_size = l;
        }
        LOG.warning("WARNING: reaching max number of iterations");
      }
    }
    if(LOG.isVerbose()) {
      LOG.verbose("optimization finished, #iter = " + iter);
    }
    return buildSolutionInfo(l, Cp, Cn);
  }

  SolutionInfo buildSolutionInfo(int l, double Cp, double Cn) {
    SolutionInfo si = new SolutionInfo();
    // calculate rho
    si.rho = calculate_rho();

    // calculate objective value
    si.obj = calculate_obj();

    // put back the solution, in original order
    si.alpha = new double[l];
    for(int i = 0; i < l; i++) {
      si.alpha[active_set[i]] = alpha[i];
    }

    si.upper_bound_p = Cp;
    si.upper_bound_n = Cn;
    return si;
  }

  /**
   * Initialize the alpha values.
   */
  protected void initializeAlpha() {
    alpha_status = new byte[l];
    for(int i = 0; i < l; i++) {
      update_alpha_status(i);
    }
  }

  /**
   * Initialize the active set.
   */
  protected void initializeActiveSet() {
    active_set = new int[l];
    for(int i = 0; i < l; i++) {
      active_set[i] = i;
    }
    active_size = l;
  }

  private void update_G(int i, int j, double old_alpha_i, double old_alpha_j) {
    final double delta_alpha_i = alpha[i] - old_alpha_i;
    final double delta_alpha_j = alpha[j] - old_alpha_j;
    for(int k = 0; k < active_size; k++) {
      G[k] += Q_i[k] * delta_alpha_i + Q_j[k] * delta_alpha_j;
    }
  }

  private void update_G_bar(double C_i, float[] Q_i) {
    for(int k = 0; k < l; k++) {
      G_bar[k] += C_i * Q_i[k];
    }
  }

  public void initializeGradient() {
    G = Arrays.copyOf(p, l);
    G_bar = new double[l];
    for(int i = 0; i < l; i++) {
      if(!is_lower_bound(i)) {
        Q.get_Q(i, l, Q_i);
        final double alpha_i = alpha[i];
        for(int j = 0; j < l; j++) {
          G[j] += alpha_i * Q_i[j];
        }
        if(is_upper_bound(i)) {
          for(int j = 0; j < l; j++) {
            G_bar[j] += get_C(i) * Q_i[j];
          }
        }
      }
    }
  }

  protected double calculate_obj() {
    double v = 0.;
    for(int i = 0; i < l; i++) {
      v += alpha[i] * (G[i] + p[i]);
    }
    return v * .5;
  }

  protected static double nonzero(double d) {
    return d > 0 ? d : 1e-12;
  }

  // @return true if already optimal
  boolean select_working_set(int[] working_set) {
    final double Gmax = maxViolating(working_set);
    final double Gmax2 = minViolating(working_set, Gmax);
    return (Gmax + Gmax2 < eps) || Gmax2 != Gmax2;
  }

  // Classic SMO
  // i: maximizes -y_i * grad(f)_i, i in I_up(\alpha)
  protected double maxViolating(int[] working_set) {
    double Gmax = Double.NEGATIVE_INFINITY;
    int Gmax_idx = -1;

    for(int t = 0; t < active_size; t++) {
      if(y[t] == +1) {
        if(!is_upper_bound(t)) {
          if(-G[t] >= Gmax) {
            Gmax = -G[t];
            Gmax_idx = t;
          }
        }
      }
      else {
        if(!is_lower_bound(t)) {
          if(G[t] >= Gmax) {
            Gmax = G[t];
            Gmax_idx = t;
          }
        }
      }
    }
    working_set[0] = Gmax_idx;
    return Gmax;
  }

  // LibSVM enhancement to SMO.
  //
  // Exploits that we need the kernel values with respect to the
  // previously chosen i (working_set[0]) anyway. So we can compute
  // them now, and use them to choose a better candidate.
  //
  // j: minimizes the decrease of obj value
  // (if quadratic coefficient <= 0, replace it with tau)
  // -y_j*grad(f)_j < -y_i*grad(f)_i, j in I_low(\alpha)
  protected double minViolating(int[] working_set, double Gi) {
    final int i = working_set[0];
    double[] QD = Q.get_QD();
    if(i != -1) { // null Q_i not accessed: Gmax=-INF if i=-1
      // Prepare cache.
      Q.get_Q(i, active_size, Q_i);
    }

    double Gjmax = Double.NEGATIVE_INFINITY;
    int Gmin_idx = -1;
    double obj_diff_min = Double.POSITIVE_INFINITY;
    for(int j = 0; j < active_size; j++) {
      if(y[j] == +1) {
        if(!is_lower_bound(j)) {
          double grad_diff = Gi + G[j];
          Gjmax = (G[j] >= Gjmax) ? G[j] : Gjmax;
          if(grad_diff > 0) { // Constraint check
            double quad_coef = QD[i] + QD[j] - 2 * y[i] * Q_i[j];
            double obj_diff = -(grad_diff * grad_diff) / nonzero(quad_coef);

            if(obj_diff <= obj_diff_min) {
              Gmin_idx = j;
              obj_diff_min = obj_diff;
            }
          }
        }
      }
      else {
        if(!is_upper_bound(j)) {
          double grad_diff = Gi - G[j];
          Gjmax = (-G[j] >= Gjmax) ? -G[j] : Gjmax;
          if(grad_diff > 0) { // Constraint check
            double quad_coef = QD[i] + QD[j] + 2.0 * y[i] * Q_i[j];
            double obj_diff = -(grad_diff * grad_diff) / nonzero(quad_coef);

            if(obj_diff <= obj_diff_min) {
              Gmin_idx = j;
              obj_diff_min = obj_diff;
            }
          }
        }
      }
    }
    if(Gmin_idx < 0) {
      return Double.NaN; // Already optimal.
    }
    working_set[1] = Gmin_idx;
    return Gjmax;
  }

  void do_shrinking() {
    double Gmax1 = Double.NEGATIVE_INFINITY; // max { -y_i * grad(f)_i | i
    // in I_up(\alpha) }
    double Gmax2 = Double.NEGATIVE_INFINITY; // max { y_i * grad(f)_i | i in
    // I_low(\alpha) }

    // find maximal violating pair first
    for(int i = 0; i < active_size; i++) {
      final double Gi = G[i];
      if(y[i] == +1) {
        if(!is_upper_bound(i)) {
          Gmax1 = -Gi > Gmax1 ? -Gi : Gmax1;
        }
        if(!is_lower_bound(i)) {
          Gmax2 = Gi > Gmax2 ? Gi : Gmax2;
        }
      }
      else {
        if(!is_upper_bound(i)) {
          Gmax2 = -Gi >= Gmax2 ? -Gi : Gmax2;
        }
        if(!is_lower_bound(i)) {
          Gmax1 = Gi >= Gmax1 ? Gi : Gmax1;
        }
      }
    }

    if(unshrink == false && Gmax1 + Gmax2 <= eps * 10) {
      unshrink = true;
      reconstruct_gradient();
      active_size = l;
    }

    for(int i = 0; i < active_size; i++) {
      if(be_shrunk(i, Gmax1, Gmax2)) {
        for(active_size--; active_size > i; active_size--) {
          if(!be_shrunk(active_size, Gmax1, Gmax2)) {
            swap_index(i, active_size);
            break;
          }
        }
      }
    }
  }

  private boolean be_shrunk(int i, double Gmax1, double Gmax2) {
    return is_upper_bound(i) ? (y[i] == +1 ? -G[i] > Gmax1 : -G[i] > Gmax2) : //
        is_lower_bound(i) ? (y[i] == +1 ? G[i] > Gmax2 : G[i] > Gmax1) : false;
  }

  double calculate_rho() {
    int nr_free = 0;
    double ub = Double.POSITIVE_INFINITY, lb = Double.NEGATIVE_INFINITY,
        sum_free = 0;
    for(int i = 0; i < active_size; i++) {
      final double yG = y[i] * G[i];
      if(is_lower_bound(i)) {
        if(y[i] > 0) {
          ub = ub < yG ? ub : yG;
        }
        else {
          lb = lb > yG ? lb : yG;
        }
      }
      else if(is_upper_bound(i)) {
        if(y[i] < 0) {
          ub = ub < yG ? ub : yG;
        }
        else {
          lb = lb > yG ? lb : yG;
        }
      }
      else {
        ++nr_free;
        sum_free += yG;
      }
    }
    return (nr_free > 0) ? sum_free / nr_free : (ub + lb) * .5;
  }
}
