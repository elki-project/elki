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
 * Support Vector Data Description for outlier detection.
 * <p>
 * Reference:
 * <p>
 * D. M. J. Tax and R. P. W. Duin<br>
 * Support Vector Data Description<br>
 * Mach. Learn. 54(1): 45-66
 * 
 * @author Erich Schubert
 * @since 0.8.0
 * 
 * @param <V> Object type
 */
@Reference(authors = "D. M. J. Tax and R. P. W. Duin", //
    title = "Support Vector Data Description", //
    booktitle = "Mach. Learn. 54(1): 45-66", //
    url = "https://doi.org/10.1023/B:MACH.0000008084.60811.49", //
    bibkey = "DBLP:journals/ml/TaxD04")
public class SVDD<V> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SVDD.class);

  /**
   * Kernel function.
   */
  PrimitiveSimilarity<? super V> kernel;

  /**
   * C parameter.
   */
  double C;

  /**
   * Constructor.
   * 
   * @param kernel Kernel to use with SVM.
   * @param C C parameter
   */
  public SVDD(PrimitiveSimilarity<? super V> kernel, double C) {
    super();
    this.kernel = kernel;
    this.C = C;
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
    elki.svm.SVDD svm = new elki.svm.SVDD(1e-4, true, 1000 /* MB */, C > 0 ? C : 20. / ids.size());
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
    DoubleRelation scoreResult = new MaterializedDoubleRelation("SVDD Score", ids, scores);
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
     * SVM C parameter
     */
    public static final OptionID C_ID = new OptionID("svm.C", "SVM C parameter.");

    /**
     * Kernel in use.
     */
    protected PrimitiveSimilarity<? super V> kernel;

    /**
     * C parameter.
     */
    protected double C;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<PrimitiveSimilarity<? super V>>(KERNEL_ID, PrimitiveSimilarity.class, RadialBasisFunctionKernel.class) //
          .grab(config, x -> kernel = x);
      new DoubleParameter(C_ID, 0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> C = x);
    }

    @Override
    public SVDD<V> make() {
      return new SVDD<>(kernel, C);
    }
  }
}
