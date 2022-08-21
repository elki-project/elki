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

import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.database.query.QueryBuilder;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.similarity.PrimitiveSimilarity;
import elki.similarity.kernel.RadialBasisFunctionKernel;
import elki.svm.OneClassSVM;
import elki.svm.data.SimilarityQueryAdapter;
import elki.svm.model.RegressionModel;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @param <V> Object type
 */
@Reference(authors = "B. Schölkopf, J. C. Platt, J. Shawe-Taylor, A. J. Smola, R. C. Williamson", //
    title = "Estimating the support of a high-dimensional distribution", //
    booktitle = "Neural computation 13.7", //
    url = "https://doi.org/10.1162/089976601750264965", //
    bibkey = "DBLP:journals/neco/ScholkopfPSSW01")
public class OCSVM<V> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(OCSVM.class);

  /**
   * Kernel function.
   */
  PrimitiveSimilarity<? super V> kernel;

  /**
   * Nu parameter.
   */
  double nu = 0.05;

  /**
   * Constructor.
   * 
   * @param kernel Kernel to use with SVM.
   * @param nu Nu parameter
   */
  public OCSVM(PrimitiveSimilarity<? super V> kernel, double nu) {
    super();
    this.kernel = kernel;
    this.nu = nu;
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
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    SimilarityQuery<V> sim = new QueryBuilder<>(relation, kernel).similarityQuery();

    if(LOG.isVerbose()) {
      LOG.verbose("Training one-class SVM...");
    }
    SimilarityQueryAdapter adapter = new SimilarityQueryAdapter(sim, ids);
    OneClassSVM svm = new OneClassSVM(1e-4, true, 1000 /* MB */, nu);
    RegressionModel model = svm.train(adapter);
    LOG.statistics(new LongStatistic(getClass().getCanonicalName() + ".numsv", model.l));

    if(LOG.isVerbose()) {
      LOG.verbose("Predicting...");
    }
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB, 0);
    DoubleMinMax mm = new DoubleMinMax();
    mm.put(0);
    int nextidx = 0;
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double score = 0;
      if(nextidx < model.l && iter.getOffset() == model.sv_indices[nextidx]) {
        score = model.sv_coef[0][nextidx++];
      }
      scores.putDouble(iter, score);
      mm.put(score);
    }
    DoubleRelation scoreResult = new MaterializedDoubleRelation("One-Class SVM Score", ids, scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <V> Object type
   */
  public static class Par<V> implements Parameterizer {
    /**
     * Parameter for kernel function.
     */
    public static final OptionID KERNEL_ID = new OptionID("svm.kernel", "Kernel to use with SVM.");

    /**
     * SVM nu parameter
     */
    public static final OptionID NU_ID = new OptionID("svm.nu", "SVM nu parameter.");

    /**
     * Kernel in use.
     */
    protected PrimitiveSimilarity<? super V> kernel;

    /**
     * Nu parameter.
     */
    protected double nu = 0.05;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<PrimitiveSimilarity<? super V>>(KERNEL_ID, PrimitiveSimilarity.class, RadialBasisFunctionKernel.class) //
          .grab(config, x -> kernel = x);
      new DoubleParameter(NU_ID, 0.05) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> nu = x);
    }

    @Override
    public OCSVM<V> make() {
      return new OCSVM<>(kernel, nu);
    }
  }
}
