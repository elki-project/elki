package elki.svm.model;

import elki.logging.Logging;
import elki.svm.data.DataSet;

import net.jafama.FastMath;

/**
 * Probabilistic classification model
 */
public class ProbabilisticClassificationModel extends ClassificationModel {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ProbabilisticClassificationModel.class);

  /**
   * Pairwise probability information.
   */
  public double[] probA, probB;

  /**
   * Predict the class probabilities
   * 
   * @param x Data set
   * @param xi Object id
   * @param prob_estimates Probability estimates output
   * @return Classification label (argmax)
   */
  public int predict_prob(DataSet x, int xi, double[] prob_estimates) {
    double[] dec_values = new double[nr_class * (nr_class - 1) / 2];
    super.predict(x, xi, dec_values);

    double min_prob = 1e-7;
    double[][] pairwise_prob = new double[nr_class][nr_class];

    for(int i = 0, k = 0; i < nr_class; i++) {
      for(int j = i + 1; j < nr_class; j++, k++) {
        pairwise_prob[i][j] = Math.min(Math.max(sigmoid_predict(dec_values[k], probA[k], probB[k]), min_prob), 1 - min_prob);
        pairwise_prob[j][i] = 1 - pairwise_prob[i][j];
      }
    }
    if(nr_class == 2) {
      prob_estimates[0] = pairwise_prob[0][1];
      prob_estimates[1] = pairwise_prob[1][0];
    }
    else {
      multiclass_probability(nr_class, pairwise_prob, prob_estimates);
    }

    int prob_max_idx = 0;
    for(int i = 1; i < nr_class; i++) {
      if(prob_estimates[i] > prob_estimates[prob_max_idx]) {
        prob_max_idx = i;
      }
    }
    return label[prob_max_idx];
  }

  /**
   * Prediction helper
   * 
   * @param decision_value Decision value
   * @param A Prior A
   * @param B Prior B
   * @return Probability
   */
  private static double sigmoid_predict(double decision_value, double A, double B) {
    final double fApB = decision_value * A + B;
    if(fApB >= 0) {
      final double e_mfApB = FastMath.exp(-fApB);
      return e_mfApB / (1. + e_mfApB);
    }
    else {
      return 1. / (1. + FastMath.exp(fApB));
    }
  }

  /**
   * Method 2 from the multiclass_prob paper by Wu, Lin, and Weng
   * 
   * @param k Number of iterations
   * @param r Pairwise probabilities
   * @param p Probability estimate output
   */
  private static void multiclass_probability(int k, double[][] r, double[] p) {
    final int max_iter = Math.max(100, k);
    double[][] Q = new double[k][k];
    double[] Qp = new double[k];
    double pQp, eps = 0.005 / k;

    for(int t = 0; t < k; t++) {
      p[t] = 1. / k; // Valid if k >= 1
      double[] Q_t = Q[t], r_t = r[t];
      Q_t[t] = 0.;
      for(int j = 0; j < t; j++) {
        Q_t[t] += r[j][t] * r[j][t];
        Q_t[j] = Q[j][t];
      }
      for(int j = t + 1; j < k; j++) {
        Q_t[t] += r[j][t] * r[j][t];
        Q_t[j] = -r[j][t] * r_t[j];
      }
    }
    for(int iter = 0; iter < max_iter; iter++) {
      // stopping condition, recalculate QP,pQP for numerical accuracy
      pQp = 0.;
      for(int t = 0; t < k; t++) {
        double[] Q_t = Q[t];
        double Qp_t = 0.;
        for(int j = 0; j < k; j++) {
          Qp_t += Q_t[j] * p[j];
        }
        Qp[t] = Qp_t;
        pQp += p[t] * Qp_t;
      }
      double max_error = 0.;
      for(int t = 0; t < k; t++) {
        double error = Math.abs(Qp[t] - pQp);
        if(error > max_error) {
          max_error = error;
        }
      }
      if(max_error < eps) {
        break;
      }

      for(int t = 0; t < k; t++) {
        double[] Q_t = Q[t];
        double diff = (-Qp[t] + pQp) / Q_t[t];
        p[t] += diff;
        pQp = (pQp + diff * (diff * Q_t[t] + 2 * Qp[t])) / (1 + diff) / (1 + diff);
        for(int j = 0; j < k; j++) {
          Qp[j] = (Qp[j] + diff * Q_t[j]) / (1 + diff);
          p[j] /= (1 + diff);
        }
      }
      if(iter == max_iter - 1) {
        LOG.info("Exceeds max_iter in multiclass_prob");
      }
    }
  }
}
