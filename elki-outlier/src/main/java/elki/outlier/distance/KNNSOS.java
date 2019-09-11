/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package elki.outlier.distance;

import elki.outlier.OutlierAlgorithm;
import elki.outlier.intrinsic.ISOS;
import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.ProbabilisticOutlierScore;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

/**
 * kNN-based adaption of Stochastic Outlier Selection.
 * <p>
 * This is a trivial variation of Stochastic Outlier Selection to benefit from
 * KNN indexes, but not discussed in the original publication. Instead of
 * setting perplexity, we choose the number of neighbors k, and set perplexity
 * simply to k/3. Objects outside of the kNN are not considered anymore.
 * <p>
 * Reference of the kNN variant:
 * <p>
 * Erich Schubert, Michael Gertz<br>
 * Intrinsic t-Stochastic Neighbor Embedding for Visualization and Outlier
 * Detection: A Remedy Against the Curse of Dimensionality?<br>
 * Proc. Int. Conf. Similarity Search and Applications, SISAP'2017
 * <p>
 * Original reference:
 * <p>
 * J. Janssens, F. Huszár, E. Postma, J. van den Herik<br>
 * Stochastic Outlier Selection<br>
 * TiCC TR 2012–001
 * 
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object type.
 */
@Title("KNNSOS: k-Nearest-Neighbor Stochastic Outlier Selection")
@Reference(authors = "Erich Schubert, Michael Gertz", //
    title = "Intrinsic t-Stochastic Neighbor Embedding for Visualization and Outlier Detection: A Remedy Against the Curse of Dimensionality?", //
    booktitle = "Proc. Int. Conf. Similarity Search and Applications, SISAP'2017", //
    url = "https://doi.org/10.1007/978-3-319-68474-1_13", //
    bibkey = "DBLP:conf/sisap/SchubertG17")
@Reference(authors = "J. Janssens, F. Huszár, E. Postma, J. van den Herik", //
    title = "Stochastic Outlier Selection", //
    booktitle = "TiCC TR 2012–001", //
    url = "https://www.tilburguniversity.edu/upload/b7bac5b2-9b00-402a-9261-7849aa019fbb_sostr.pdf", //
    bibkey = "tr/tilburg/JanssensHPv12")
public class KNNSOS<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KNNSOS.class);

  /**
   * Number of neighbors (not including query point).
   */
  protected int k;

  /**
   * Expected outlier rate.
   */
  protected double phi = 0.01;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k Number of neighbors to consider
   */
  public KNNSOS(Distance<? super O> distance, int k) {
    super(distance);
    this.k = k;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  /**
   * Run the algorithm.
   * 
   * @param relation data relation
   * @return outlier detection result
   */
  public OutlierResult run(Relation<O> relation) {
    final int k1 = k + 1; // Query size
    final double perplexity = k / 3.;
    KNNQuery<O> knnq = relation.getKNNQuery(getDistance(), k1);
    final double logPerp = perplexity > 1. ? FastMath.log(perplexity) : .1;

    double[] p = new double[k + 10];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("KNNSOS scores", relation.size(), LOG) : null;
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB, 1.);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      KNNList knns = knnq.getKNNForDBID(it, k1);
      if(p.length < knns.size() + 1) {
        p = new double[knns.size() + 10];
      }
      final DoubleDBIDListIter ki = knns.iter();
      // Compute affinities
      SOS.computePi(it, ki, p, perplexity, logPerp);
      // Normalization factor:
      double s = SOS.sumOfProbabilities(it, ki, p);
      if(s > 0) {
        ISOS.nominateNeighbors(it, ki, p, 1. / s, scores);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    // Find minimum and maximum.
    DoubleMinMax minmax = ISOS.transformScores(scores, relation.getDBIDs(), logPerp, phi);
    DoubleRelation scoreres = new MaterializedDoubleRelation("kNN Stoachastic Outlier Selection", relation.getDBIDs(), scores);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(minmax.getMin(), minmax.getMax(), 0.);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * Parameter to specify the number of neighbors
     */
    public static final OptionID KNN_ID = new OptionID("sos.k", "Number of neighbors to use. Should be about 3x the desired perplexity.");

    /**
     * Number of neighbors
     */
    int k = 15;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(KNN_ID, 15) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public KNNSOS<O> make() {
      return new KNNSOS<O>(distanceFunction, k);
    }
  }
}
