package elki.svm;

import java.util.Arrays;

import elki.logging.Logging;
import elki.svm.data.ByteWeightedArrayDataSet;
import elki.svm.data.DataSet;
import elki.svm.model.ClassificationModel;
import elki.svm.model.ProbabilisticClassificationModel;
import elki.svm.solver.Solver;

import net.jafama.FastMath;

public abstract class AbstractSVC extends AbstractSingleSVM {
  public AbstractSVC(double eps, boolean shrinking, double cache_size) {
    super(eps, shrinking, cache_size);
  }

  boolean probability = false;

  public ClassificationModel train(DataSet x) {
    return train(x, null);
  }

  public ClassificationModel train(DataSet x, double[] weighted_C) {
    final int l = x.size();

    // group training data of the same class
    int[] perm = new int[l];
    int[][] group_ret = new int[3][];
    int nr_class = groupClasses(x, group_ret, perm);
    // Unpack multiple returns:
    int[] label = group_ret[0], start = group_ret[1], count = group_ret[2];

    Logging LOG = getLogger();
    if(nr_class == 1) {
      LOG.info("WARNING: training data in only one class. See README for details.");
    }

    // train k*(k-1)/2 binary models

    boolean[] nonzero = new boolean[l];
    Arrays.fill(nonzero, false);
    final int pairs = (nr_class * (nr_class - 1)) >>> 1;
    double[][] f_alpha = new double[pairs][];
    double[] f_rho = new double[pairs];

    double[] probA = probability ? new double[pairs] : null;
    double[] probB = probability ? new double[pairs] : null;

    ByteWeightedArrayDataSet newx = new ByteWeightedArrayDataSet(x, l);
    for(int i = 0, p = 0; i < nr_class; i++) {
      for(int j = i + 1; j < nr_class; j++, p++) {
        final int si = start[i], sj = start[j];
        final int ci = count[i], cj = count[j];
        newx.clear();
        for(int k = 0, m = si; k < ci; ++k, ++m) {
          newx.add(perm[m], ONE);
        }
        for(int k = 0, m = sj; k < cj; ++k, ++m) {
          newx.add(perm[m], MONE);
        }

        if(probability) {
          double Ci = 1, Cj = 1;
          if(weighted_C != null) {
            Ci = weighted_C[i];
            Cj = weighted_C[j];
          }
          double[] probAB = binary_svc_probability(newx, Ci, Cj);
          probA[p] = probAB[0];
          probB[p] = probAB[1];
        }
        if(weighted_C != null) {
          set_weights(weighted_C[i], weighted_C[j]);
        }
        Solver.SolutionInfo s = train_one(newx);
        f_alpha[p] = s.alpha;
        f_rho[p] = s.rho;
        for(int k = 0, m = si; k < ci; k++, m++) {
          if(!nonzero[m] && nonzero(f_alpha[p][k])) {
            nonzero[m] = true;
          }
        }
        for(int k = 0, m = sj, n = ci; k < cj; k++, m++, n++) {
          if(!nonzero[m] && nonzero(f_alpha[p][n])) {
            nonzero[m] = true;
          }
        }
        LOG.verbose("Trained " + (p + 1) + " of " + ((nr_class * (nr_class - 1)) >>> 1) + " 1vs1 SVMs");
      }
    }

    // build output

    ClassificationModel model;
    if(probability) {
      ProbabilisticClassificationModel pmodel = new ProbabilisticClassificationModel();
      for(int i = 0; i < pairs; i++) {
        pmodel.probA[i] = probA[i];
        pmodel.probB[i] = probB[i];
      }
      model = pmodel;
    }
    else {
      model = new ClassificationModel();
    }
    model.nr_class = nr_class;
    model.label = Arrays.copyOf(label, nr_class); // Shrink
    model.rho = Arrays.copyOf(f_rho, pairs);

    int nnz = 0;
    model.nSV = new int[nr_class];
    for(int i = 0; i < nr_class; i++) {
      int nSV = 0;
      for(int j = 0; j < count[i]; j++) {
        if(nonzero[start[i] + j]) {
          ++nSV;
          ++nnz;
        }
      }
      model.nSV[i] = nSV;
    }

    LOG.verbose("Total nSV = " + nnz);

    model.l = nnz;
    // model.SV = new ArrayList<Object>(nnz);
    model.sv_indices = new int[nnz];

    for(int i = 0, p = 0; i < l; i++) {
      if(nonzero[i]) {
        // model.SV.add(x.get(perm[i]));
        model.sv_indices[p++] = perm[i];
      }
    }

    int[] nz_start = new int[nr_class];
    nz_start[0] = 0;
    for(int i = 1; i < nr_class; i++) {
      nz_start[i] = nz_start[i - 1] + model.nSV[i - 1];
    }

    model.sv_coef = new double[nr_class - 1][nnz];

    for(int i = 0, p = 0; i < nr_class; i++) {
      for(int j = i + 1; j < nr_class; j++, p++) {
        // classifier (i,j): coefficients with
        // i are in sv_coef[j-1][nz_start[i]...],
        // j are in sv_coef[i][nz_start[j]...]

        int si = start[i], sj = start[j];
        int ci = count[i], cj = count[j];

        int q = nz_start[i];
        for(int k = 0; k < ci; k++) {
          if(nonzero[si + k]) {
            model.sv_coef[j - 1][q++] = f_alpha[p][k];
          }
        }
        q = nz_start[j];
        for(int k = 0; k < cj; k++) {
          if(nonzero[sj + k]) {
            model.sv_coef[i][q++] = f_alpha[p][ci + k];
          }
        }
      }
    }
    return model;
  }

  // Stratified cross validation
  public void cross_validation(DataSet x, double[] weighted_C, int nr_fold, double[] target) {
    final int l = x.size();

    int[] perm = new int[l], fold_start;
    // stratified cv may not give leave-one-out rate
    // Each class to l folds -> some folds may have zero elements
    if(nr_fold < l) {
      fold_start = stratifiedFolds(x, nr_fold, perm);
    }
    else {
      perm = shuffledIndex(perm, l);
      fold_start = makeFolds(l, nr_fold);
    }

    ByteWeightedArrayDataSet newx = new ByteWeightedArrayDataSet(x, l);
    for(int i = 0; i < nr_fold; i++) {
      final int begin = fold_start[i], end = fold_start[i + 1];

      newx.clear();
      for(int j = 0; j < begin; ++j) {
        newx.add(perm[j], (byte) x.classnum(perm[j]));
      }
      for(int j = end; j < l; ++j) {
        newx.add(perm[j], (byte) x.classnum(perm[j]));
      }
      ClassificationModel submodel = train(newx, weighted_C);
      if(submodel instanceof ProbabilisticClassificationModel) {
        ProbabilisticClassificationModel pm = (ProbabilisticClassificationModel) submodel;
        double[] prob_estimates = new double[submodel.nr_class];
        for(int j = begin; j < end; j++) {
          target[perm[j]] = pm.predict_prob(x, perm[j], prob_estimates);
        }
      }
      else {
        for(int j = begin; j < end; j++) {
          target[perm[j]] = submodel.predict(x, perm[j]);
        }
      }
    }
  }

  // Platt's binary SVM Probablistic Output: an improvement from Lin et al.
  private double[] sigmoid_train(double[] dec_values, DataSet x) {
    final int l = x.size();

    // Count prior probabilities:
    double prior1 = 0;
    for(int i = 0; i < l; i++) {
      if(x.value(i) > 0) {
        ++prior1;
      }
    }
    double prior0 = l - prior1;

    final int MAX_ITER = 100; // Maximal number of iterations
    final double MIN_STEP = 1e-10; // Minimal step taken in line search
    final double SIGMA = 1e-12; // For numerically strict PD of Hessian
    final double EPS = 1e-5;
    double hiTarget = (prior1 + 1.) / (prior1 + 2.);
    double loTarget = 1. / (prior0 + 2.);
    double[] t = new double[l];

    // Initial Point and Initial Fun Value
    double A = 0., B = FastMath.log((prior0 + 1.) / (prior1 + 1.));
    double fval = 0.;

    for(int i = 0; i < l; i++) {
      t[i] = (x.value(i) > 0) ? hiTarget : loTarget;
      double fApB = dec_values[i] * A + B;
      if(fApB >= 0) {
        fval += t[i] * fApB + FastMath.log1p(FastMath.exp(-fApB));
      }
      else {
        fval += (t[i] - 1) * fApB + FastMath.log1p(FastMath.exp(fApB));
      }
    }
    for(int iter = 0; /* below: iter < MAX_ITER */; iter++) {
      // Update Gradient and Hessian (use H' = H + sigma I)
      // numerically ensures strict PD
      double h11 = SIGMA, h22 = SIGMA, h21 = 0.;
      double g1 = 0., g2 = 0.;
      for(int i = 0; i < l; i++) {
        double fApB = dec_values[i] * A + B;
        final double p, q;
        if(fApB >= 0) {
          p = FastMath.exp(-fApB) / (1.0 + FastMath.exp(-fApB));
          q = 1.0 / (1.0 + FastMath.exp(-fApB));
        }
        else {
          p = 1.0 / (1.0 + FastMath.exp(fApB));
          q = FastMath.exp(fApB) / (1.0 + FastMath.exp(fApB));
        }
        double d2 = p * q;
        h11 += dec_values[i] * dec_values[i] * d2;
        h22 += d2;
        h21 += dec_values[i] * d2;
        double d1 = t[i] - p;
        g1 += dec_values[i] * d1;
        g2 += d1;
      }

      // Stopping Criteria
      if(Math.abs(g1) < EPS && Math.abs(g2) < EPS) {
        break;
      }

      // Finding Newton direction: -inv(H') * g
      double det = h11 * h22 - h21 * h21;
      double dA = -(h22 * g1 - h21 * g2) / det;
      double dB = -(-h21 * g1 + h11 * g2) / det;
      double gd = g1 * dA + g2 * dB;

      double stepsize = 1.; // Line Search
      while(stepsize >= MIN_STEP) {
        double newA = A + stepsize * dA;
        double newB = B + stepsize * dB;

        // New function value
        double newf = 0.;
        for(int i = 0; i < l; i++) {
          double fApB = dec_values[i] * newA + newB;
          if(fApB >= 0) {
            newf += t[i] * fApB + FastMath.log1p(FastMath.exp(-fApB));
          }
          else {
            newf += (t[i] - 1) * fApB + FastMath.log1p(FastMath.exp(fApB));
          }
        }
        // Check sufficient decrease
        if(newf < fval + 0.0001 * stepsize * gd) {
          A = newA;
          B = newB;
          fval = newf;
          break;
        }
        stepsize *= .5;
      }

      if(stepsize < MIN_STEP) {
        getLogger().info("Line search fails in two-class probability estimates");
        break;
      }
      if(iter == MAX_ITER - 1) {
        getLogger().info("Reaching maximal iterations in two-class probability estimates");
        break; // redundant
      }
    }

    return new double[] { A, B };
  }

  // Cross-validation decision values for probability estimates
  private double[] binary_svc_probability(DataSet x, double Cp, double Cn) {
    final int l = x.size();
    final int nr_fold = 5;
    int[] perm = shuffledIndex(new int[l], l);
    double[] dec_values = new double[l];
    ByteWeightedArrayDataSet newx = new ByteWeightedArrayDataSet(x, l);
    for(int i = 0; i < nr_fold; i++) {
      int begin = i * l / nr_fold;
      int end = (i + 1) * l / nr_fold;

      newx.clear();
      for(int j = 0; j < begin; j++) {
        newx.add(perm[j], (byte) x.value(perm[j]));
      }
      for(int j = end; j < l; j++) {
        newx.add(perm[j], (byte) x.value(perm[j]));
      }
      int p_count = 0, n_count = 0;
      for(int j = 0; j < newx.size(); j++) {
        if(newx.value(j) > 0) {
          p_count++;
        }
        else {
          n_count++;
        }
      }

      if(p_count == 0 && n_count == 0) {
        for(int j = begin; j < end; j++) {
          dec_values[perm[j]] = 0;
        }
      }
      else if(p_count > 0 && n_count == 0) {
        for(int j = begin; j < end; j++) {
          dec_values[perm[j]] = 1;
        }
      }
      else if(p_count == 0 && n_count > 0) {
        for(int j = begin; j < end; j++) {
          dec_values[perm[j]] = -1;
        }
      }
      else {
        set_weights(Cp, Cn);
        Solver.SolutionInfo si = solve(newx);
        ClassificationModel submodel = new ClassificationModel();
        submodel.nr_class = 2;
        submodel.label = new int[] { +1, -1 };
        submodel.rho = new double[] { si.rho, si.rho };
        double[] dec_value = new double[1];
        for(int j = begin; j < end; j++) {
          submodel.predict(x, perm[j], dec_value);
          dec_values[perm[j]] = dec_value[0];
          // ensure +1 -1 order; reason not using CV subroutine
          dec_values[perm[j]] *= submodel.label[0];
        }
      }
    }
    return sigmoid_train(dec_values, x);
  }
}
