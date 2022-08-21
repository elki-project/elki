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
package elki.outlier.svm;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.EnumParameter;

import libsvm.*;

/**
 * Outlier-detection using one-class support vector machines.
 * <p>
 * Important note: from literature, the one-class SVM is trained as if 0 was the
 * only counterexample. Outliers will only be detected when they are close to
 * the origin in kernel space! In our experience, results from this method are
 * rather mixed, in particular as you would likely need to tune hyperparameters.
 * Results may be better if you have a training data set with positive examples
 * only, then apply it only to new data (which is currently not supported in
 * this implementation, it assumes a single-dataset scenario).
 * <p>
 * Reference:
 * <p>
 * B. Schölkopf, J. C. Platt, J. Shawe-Taylor, A. J. Smola, R. C.
 * Williamson<br>
 * Estimating the support of a high-dimensional distribution<br>
 * Neural computation 13.7
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> vector type
 */
@Reference(authors = "B. Schölkopf, J. C. Platt, J. Shawe-Taylor, A. J. Smola, R. C. Williamson", //
    title = "Estimating the support of a high-dimensional distribution", //
    booktitle = "Neural computation 13.7", //
    url = "https://doi.org/10.1162/089976601750264965", //
    bibkey = "DBLP:journals/neco/ScholkopfPSSW01")
public class LibSVMOneClassOutlierDetection<V extends NumberVector> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(LibSVMOneClassOutlierDetection.class);

  /**
   * Kernel functions. Expose as enum for convenience.
   */
  public enum SVMKernel { //
    LINEAR, // Linear
    QUADRATIC, // Quadratic
    CUBIC, // Cubic
    RBF, // Radial basis functions
    SIGMOID, // Sigmoid
  }

  /**
   * Kernel function in use.
   */
  protected SVMKernel kernel = SVMKernel.RBF;

  /**
   * Nu parameter.
   */
  double nu = 0.05;

  /**
   * Gamma parameter (not for linear kernel)
   */
  double gamma = 0;

  /**
   * Constructor.
   * 
   * @param kernel Kernel to use with SVM.
   * @param nu Nu parameter
   * @param gamma Gamma parameter
   */
  public LibSVMOneClassOutlierDetection(SVMKernel kernel, double nu, double gamma) {
    super();
    this.kernel = kernel;
    this.nu = nu;
    this.gamma = gamma;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Run one-class SVM.
   * 
   * @param relation Data relation
   * @return Outlier result.
   */
  public OutlierResult run(Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final DBIDArrayIter iter = ids.iter();

    svm.svm_set_print_string_function(LOG_HELPER);

    svm_parameter param = new svm_parameter();
    param.svm_type = svm_parameter.ONE_CLASS;
    param.kernel_type = svm_parameter.LINEAR;
    param.degree = 1;
    param.nu = nu;
    param.gamma = gamma > 0 ? gamma : 1. / dim;
    switch(kernel){
    case LINEAR:
      param.kernel_type = svm_parameter.LINEAR;
      break;
    case QUADRATIC:
      param.kernel_type = svm_parameter.POLY;
      param.degree = 2;
      break;
    case CUBIC:
      param.kernel_type = svm_parameter.POLY;
      param.degree = 3;
      break;
    case RBF:
      param.kernel_type = svm_parameter.RBF;
      break;
    case SIGMOID:
      param.kernel_type = svm_parameter.SIGMOID;
      break;
    default:
      throw new AbortException("Invalid kernel parameter: " + kernel);
    }
    // TODO: expose additional parameters to the end user!
    param.coef0 = 1.;
    param.cache_size = 1000;
    param.C = 1; // not used by one-class (nu svm)?
    param.eps = 1e-4; // not used by one-class?
    param.p = 0.1; // not used by one-class?
    param.shrinking = 1;
    param.probability = 0;
    param.nr_weight = 0;
    param.weight_label = new int[0];
    param.weight = new double[0];

    // Transform data:
    svm_problem prob = new svm_problem();
    prob.l = relation.size();
    prob.x = new svm_node[prob.l][];
    prob.y = new double[prob.l];
    for(iter.seek(0); iter.valid(); iter.advance()) {
      V vec = relation.get(iter);
      // TODO: support compact sparse vectors, too!
      svm_node[] x = new svm_node[dim];
      for(int d = 0; d < dim; d++) {
        x[d] = new svm_node();
        x[d].index = d + 1;
        x[d].value = vec.doubleValue(d);
      }
      prob.x[iter.getOffset()] = x;
      prob.y[iter.getOffset()] = +1;
    }

    if(LOG.isVerbose()) {
      LOG.verbose("Training one-class SVM...");
    }
    String err = svm.svm_check_parameter(prob, param);
    if(err != null) {
      LOG.error("svm_check_parameter: " + err);
    }
    svm_model model = svm.svm_train(prob, param);
    LOG.statistics(new LongStatistic(getClass().getCanonicalName() + ".numsv", model.l));

    if(LOG.isVerbose()) {
      LOG.verbose("Predicting...");
    }
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    DoubleMinMax mm = new DoubleMinMax();
    int nextidx = 0;
    for(iter.seek(0); iter.valid(); iter.advance()) {
      double score = 0;
      if(nextidx < model.l && iter.getOffset() == model.sv_indices[nextidx] - 1) {
        score = model.sv_coef[0][nextidx++];
      }
      scores.putDouble(iter, score);
      mm.put(score);
    }
    DoubleRelation scoreResult = new MaterializedDoubleRelation("One-Class SVM Decision", ids, scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Setup logging helper for SVM.
   */
  static final svm_print_interface LOG_HELPER = new svm_print_interface() {
    @Override
    public void print(String arg0) {
      if(LOG.isVerbose()) {
        LOG.verbose(arg0);
      }
    }
  };

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Parameter for kernel function.
     */
    public static final OptionID KERNEL_ID = new OptionID("svm.kernel", "Kernel to use with SVM.");

    /**
     * SVM nu parameter
     */
    public static final OptionID NU_ID = new OptionID("svm.nu", "SVM nu parameter.");

    /**
     * SVM gamma parameter
     */
    public static final OptionID GAMMA_ID = new OptionID("svm.gamma", "SVM gamma parameter (use 0 for 1/dim heuristic).");

    /**
     * Kernel in use.
     */
    protected SVMKernel kernel = SVMKernel.RBF;

    /**
     * Nu parameter.
     */
    protected double nu = 0.05;

    /**
     * Gamma parameter (not for linear kernel)
     */
    double gamma = 0;

    @Override
    public void configure(Parameterization config) {
      new EnumParameter<SVMKernel>(KERNEL_ID, SVMKernel.class, SVMKernel.RBF) //
          .grab(config, x -> kernel = x);
      new DoubleParameter(NU_ID, 0.05) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> nu = x);
      if(kernel != SVMKernel.LINEAR) {
        new DoubleParameter(GAMMA_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
            .setOptional(true) //
            .grab(config, x -> gamma = x);
      }
    }

    @Override
    public LibSVMOneClassOutlierDetection<V> make() {
      return new LibSVMOneClassOutlierDetection<>(kernel, nu, gamma);
    }
  }
}
