/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.svm;

import elki.math.MathUtil;
import elki.svm.data.DataSet;
import elki.svm.data.DoubleWeightedDataSet;
import elki.svm.model.ProbabilisticRegressionModel;
import elki.svm.model.RegressionModel;
import elki.svm.solver.Solver.SolutionInfo;

public abstract class AbstractSVR extends AbstractSingleSVM {
  public AbstractSVR(double eps, boolean shrinking, double cache_size) {
    super(eps, shrinking, cache_size);
  }

  // Perform cross-validation.
  public void cross_validation(DataSet x, int nr_fold, double[] target) {
    final int l = x.size();
    int[] perm = shuffledIndex(new int[l], l);
    // Split into folds
    int[] fold_start = makeFolds(l, nr_fold);

    DoubleWeightedDataSet newx = new DoubleWeightedDataSet(x, l);
    for(int i = 0; i < nr_fold; i++) {
      final int begin = fold_start[i], end = fold_start[i + 1];
      newx.clear();
      for(int j = 0; j < begin; ++j) {
        newx.add(perm[j], x.value(perm[j]));
      }
      for(int j = end; j < l; ++j) {
        newx.add(perm[j], x.value(perm[j]));
      }
      RegressionModel submodel = train(newx);
      for(int j = begin; j < end; j++) {
        target[perm[j]] = submodel.predict(x, perm[j]);
      }
    }
  }

  private double svr_probability(DataSet x, double[] probA) {
    final int l = x.size();
    int nr_fold = 5;

    double[] ymv = new double[l];
    double mae = 0;
    cross_validation(x, nr_fold, ymv);
    for(int i = 0; i < l; i++) {
      ymv[i] = x.value(i) - ymv[i];
      mae += Math.abs(ymv[i]);
    }
    double std = MathUtil.SQRT2 * mae / l;
    int count = 0;
    mae = 0;
    for(int i = 0; i < l; i++) {
      if(Math.abs(ymv[i]) > 5 * std) {
        ++count;
      }
      else {
        mae += Math.abs(ymv[i]);
      }
    }
    mae /= (l - count);
    getLogger().debug("Prob. model for test data: target value = predicted value + z,\n" + "z: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma=" + mae);
    return mae;
  }

  boolean probability = false;

  public RegressionModel train(DataSet x) {
    SolutionInfo si = train_one(x);
    RegressionModel model;
    if(!probability) {
      model = new RegressionModel();
    }
    else {
      double[] probA = new double[1];
      probA[0] = svr_probability(x, probA);
      model = new ProbabilisticRegressionModel(probA);
    }
    model.nr_class = 2;
    model.sv_coef = new double[1][];
    model.rho = new double[] { si.rho };
    model.r_square = si.r_square;

    int nSV = 0;
    for(int i = 0; i < x.size(); i++) {
      if(nonzero(si.alpha[i])) {
        ++nSV;
      }
    }
    model.l = nSV;
    // model.SV = new ArrayList<Object>(nSV);
    double[] coef = model.sv_coef[0] = new double[nSV];
    model.sv_indices = new int[nSV];
    for(int i = 0, j = 0; i < x.size(); i++) {
      if(nonzero(si.alpha[i])) {
        // model.SV.add(x.get(i));
        coef[j] = si.alpha[i];
        model.sv_indices[j] = i;
        ++j;
      }
    }
    return model;
  }
}
