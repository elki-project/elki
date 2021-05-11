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

//
// Solver for nu-svm classification and regression
//
// additional constraint: e^T \alpha = constant
//
public class NuSolver extends Solver {
  public double r;

  @Override
  // return 1 if already optimal, return 0 otherwise
  boolean select_working_set(int[] working_set) {
    // return i,j such that y_i = y_j and
    // i: maximizes -y_i * grad(f)_i, i in I_up(\alpha)
    // j: minimizes the decrease of obj value
    // (if quadratic coefficeint <= 0, replace it with tau)
    // -y_j*grad(f)_j < -y_i*grad(f)_i, j in I_low(\alpha)

    double Gmaxp = -Double.POSITIVE_INFINITY;
    double Gmaxp2 = -Double.POSITIVE_INFINITY;
    int Gmaxp_idx = -1;

    double Gmaxn = -Double.POSITIVE_INFINITY;
    double Gmaxn2 = -Double.POSITIVE_INFINITY;
    int Gmaxn_idx = -1;

    for(int t = 0; t < active_size; t++) {
      if(y[t] == +1) {
        if(!is_upper_bound(t) && -G[t] >= Gmaxp) {
          Gmaxp = -G[t];
          Gmaxp_idx = t;
        }
      }
      else {
        if(!is_lower_bound(t) && G[t] >= Gmaxn) {
          Gmaxn = G[t];
          Gmaxn_idx = t;
        }
      }
    }

    double[] QD = Q.get_QD();
    int ip = Gmaxp_idx, in = Gmaxn_idx;
    float[] Q_ip = Q_i, Q_in = Q_j; // Reuse existing memory.
    if(ip != -1) { // null Q_ip not accessed: Gmaxp=-INF if ip=-1
      Q.get_Q(ip, active_size, Q_ip);
    }
    if(in != -1) {
      Q.get_Q(in, active_size, Q_in);
    }

    int Gmin_idx = -1;
    double obj_diff_min = Double.POSITIVE_INFINITY;

    for(int j = 0; j < active_size; j++) {
      if(y[j] == +1) {
        if(!is_lower_bound(j)) {
          double grad_diff = Gmaxp + G[j];
          if(G[j] >= Gmaxp2) {
            Gmaxp2 = G[j];
          }
          if(grad_diff > 0) {
            double quad_coef = QD[ip] + QD[j] - 2 * Q_ip[j];
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
          double grad_diff = Gmaxn - G[j];
          if(-G[j] >= Gmaxn2) {
            Gmaxn2 = -G[j];
          }
          if(grad_diff > 0) {
            double quad_coef = QD[in] + QD[j] - 2 * Q_in[j];
            double obj_diff = -(grad_diff * grad_diff) / nonzero(quad_coef);

            if(obj_diff <= obj_diff_min) {
              Gmin_idx = j;
              obj_diff_min = obj_diff;
            }
          }
        }
      }
    }
    if(Math.max(Gmaxp + Gmaxp2, Gmaxn + Gmaxn2) < eps || Gmin_idx < 0) {
      return true;
    }

    working_set[0] = y[Gmin_idx] == +1 ? Gmaxp_idx : Gmaxn_idx;
    working_set[1] = Gmin_idx;
    return false;
  }

  private boolean be_shrunk(int i, double Gmax1, double Gmax2, double Gmax3, double Gmax4) {
    return is_upper_bound(i) ? (y[i] == +1 ? -G[i] > Gmax1 : -G[i] > Gmax4) //
        : is_lower_bound(i) ? (y[i] == +1 ? G[i] > Gmax2 : G[i] > Gmax3) : false;
  }

  @Override
  void do_shrinking() {
    double Gmax1 = -Double.POSITIVE_INFINITY;
    // max { -y_i * grad(f)_i | y_i = +1, i in I_up(\alpha) }
    double Gmax2 = -Double.POSITIVE_INFINITY;
    // max { y_i * grad(f)_i | y_i = +1, i in I_low(\alpha) }
    double Gmax3 = -Double.POSITIVE_INFINITY;
    // max { -y_i * grad(f)_i | y_i = -1, i in I_up(\alpha) }
    double Gmax4 = -Double.POSITIVE_INFINITY;
    // max { y_i * grad(f)_i | y_i = -1, i in I_low(\alpha) }

    // find maximal violating pair first
    for(int i = 0; i < active_size; i++) {
      final double Gi = G[i];
      if(!is_upper_bound(i)) {
        if(y[i] == +1) {
          Gmax1 = (-Gi > Gmax1) ? -Gi : Gmax1;
        }
        else {
          Gmax4 = (-Gi > Gmax4) ? -Gi : Gmax4;
        }
      }
      if(!is_lower_bound(i)) {
        if(y[i] == +1) {
          Gmax2 = (Gi > Gmax2) ? Gi : Gmax2;
        }
        else {
          Gmax3 = (Gi > Gmax3) ? Gi : Gmax3;
        }
      }
    }

    if(unshrink == false && Math.max(Gmax1 + Gmax2, Gmax3 + Gmax4) <= eps * 10) {
      unshrink = true;
      reconstruct_gradient();
      active_size = l;
    }

    for(int i = 0; i < active_size; i++) {
      if(be_shrunk(i, Gmax1, Gmax2, Gmax3, Gmax4)) {
        for(active_size--; active_size > i; active_size--) {
          if(!be_shrunk(active_size, Gmax1, Gmax2, Gmax3, Gmax4)) {
            swap_index(i, active_size);
            break;
          }
        }
      }
    }
  }

  @Override
  double calculate_rho() {
    double ub1 = Double.POSITIVE_INFINITY, ub2 = Double.POSITIVE_INFINITY;
    double lb1 = -Double.POSITIVE_INFINITY, lb2 = -Double.POSITIVE_INFINITY;
    int nr_free1 = 0, nr_free2 = 0;
    double sum_free1 = 0, sum_free2 = 0;

    for(int i = 0; i < active_size; i++) {
      final double Gi = G[i];
      if(y[i] == +1) {
        if(is_lower_bound(i)) {
          ub1 = (ub1 < Gi) ? ub1 : Gi;
        }
        else if(is_upper_bound(i)) {
          lb1 = (lb1 > Gi) ? lb1 : Gi;
        }
        else {
          ++nr_free1;
          sum_free1 += Gi;
        }
      }
      else {
        if(is_lower_bound(i)) {
          ub2 = (ub2 < Gi) ? ub2 : Gi;
        }
        else if(is_upper_bound(i)) {
          lb2 = (lb2 > Gi) ? lb2 : Gi;
        }
        else {
          ++nr_free2;
          sum_free2 += Gi;
        }
      }
    }

    double r1 = (nr_free1 > 0) ? sum_free1 / nr_free1 : (ub1 + lb1) * .5;
    double r2 = (nr_free2 > 0) ? sum_free2 / nr_free2 : (ub2 + lb2) * .5;

    r = (r1 + r2) * .5;
    return (r1 - r2) * .5;
  }
}
