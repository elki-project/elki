package elki.svm.model;

import elki.svm.data.DataSet;

public class ClassificationModel extends Model {
  public int[] label; // label of each class (label[k])

  public int[] nSV; // number of SVs for each class (nSV[k])

  // nSV[0] + nSV[1] + ... + nSV[k-1] = l

  public int predict(DataSet x, int xi, double[] dec_values) {
    double[] kvalue = new double[l];
    for(int i = 0; i < l; i++) {
      kvalue[i] = x.similarity(xi, sv_indices[i]);
    }

    int[] start = new int[nr_class];
    for(int i = 1; i < nr_class; i++) {
      start[i] = start[i - 1] + nSV[i - 1];
    }

    int[] vote = new int[nr_class];

    int p = 0;
    for(int i = 0; i < nr_class; i++) {
      for(int j = i + 1; j < nr_class; j++, p++) {
        double sum = 0;
        int si = start[i], sj = start[j];
        int ci = nSV[i], cj = nSV[j];

        double[] coef1 = sv_coef[j - 1], coef2 = sv_coef[i];
        for(int k = 0; k < ci; k++) {
          sum += coef1[si + k] * kvalue[si + k];
        }
        for(int k = 0; k < cj; k++) {
          sum += coef2[sj + k] * kvalue[sj + k];
        }
        sum -= rho[p];
        dec_values[p] = sum;

        vote[(sum > 0) ? i : j]++;
      }
    }

    int vote_max_idx = 0;
    for(int i = 1; i < nr_class; i++) {
      if(vote[i] > vote[vote_max_idx]) {
        vote_max_idx = i;
      }
    }
    return label[vote_max_idx];
  }

  public int predict(DataSet x, int xi) {
    double[] kvalue = new double[l];
    for(int i = 0; i < l; i++) {
      kvalue[i] = x.similarity(xi, sv_indices[i]);
    }

    int[] start = new int[nr_class];
    for(int i = 1; i < nr_class; i++) {
      start[i] = start[i - 1] + nSV[i - 1];
    }

    int[] vote = new int[nr_class];

    int p = 0;
    for(int i = 0; i < nr_class; i++) {
      for(int j = i + 1; j < nr_class; j++, p++) {
        double sum = 0;
        int si = start[i], sj = start[j];
        int ci = nSV[i], cj = nSV[j];

        double[] coef1 = sv_coef[j - 1], coef2 = sv_coef[i];
        for(int k = 0; k < ci; k++) {
          sum += coef1[si + k] * kvalue[si + k];
        }
        for(int k = 0; k < cj; k++) {
          sum += coef2[sj + k] * kvalue[sj + k];
        }
        sum -= rho[p];

        vote[(sum > 0) ? i : j]++;
      }
    }

    int vote_max_idx = 0;
    for(int i = 1; i < nr_class; i++) {
      if(vote[i] > vote[vote_max_idx]) {
        vote_max_idx = i;
      }
    }

    return label[vote_max_idx];
  }
}
