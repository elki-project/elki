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
package elki.algorithm.statistics;

import static elki.math.linearalgebra.VMath.minusEquals;
import static elki.math.linearalgebra.VMath.transposeTimesTimes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import elki.AbstractDistanceBasedAlgorithm;
import elki.clustering.trivial.ByLabelOrAllInOneClustering;
import elki.data.Cluster;
import elki.data.NumberVector;
import elki.data.model.Model;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.evaluation.clustering.EvaluateClustering;
import elki.evaluation.scores.ROCEvaluation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MeanVariance;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.result.CollectionResult;
import elki.result.HistogramResult;
import elki.result.Metadata;
import elki.utilities.datastructures.histogram.ObjHistogram;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Evaluate a distance function with respect to kNN queries. For each point, the
 * neighbors are sorted by distance, then the ROC AUC is computed. A score of 1
 * means that the distance function provides a perfect ordering of relevant
 * neighbors first, then irrelevant neighbors. A value of 0.5 can be obtained by
 * random sorting. A value of 0 means the distance function is inverted, i.e. a
 * similarity.
 * <p>
 * In contrast to {@link RankingQualityHistogram}, this method uses a binning
 * based on the centrality of objects. This allows analyzing whether or not a
 * particular distance degrades for the outer parts of a cluster.
 * <p>
 * TODO: Allow fixed binning range, configurable
 * <p>
 * TODO: Add sampling
 *
 * @author Erich Schubert
 * @since 0.2
 * @param <V> Vector type
 */
@Title("Evaluate Ranking Quality")
@Description("Evaluates the effectiveness of a distance function via the obtained rankings.")
public class EvaluateRankingQuality<V extends NumberVector> extends AbstractDistanceBasedAlgorithm<Distance<? super V>, CollectionResult<double[]>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateRankingQuality.class);

  /**
   * Number of bins to use.
   */
  int numbins = 20;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param numbins Number of bins
   */
  public EvaluateRankingQuality(Distance<? super V> distanceFunction, int numbins) {
    super(distanceFunction);
    this.numbins = numbins;
  }

  @Override
  public HistogramResult run(Database database) {
    final Relation<V> relation = database.getRelation(getInputTypeRestriction()[0]);
    final KNNQuery<V> knnQuery = relation.getKNNQuery(getDistance(), relation.size());

    if(LOG.isVerbose()) {
      LOG.verbose("Preprocessing clusters...");
    }
    // Cluster by labels
    Collection<Cluster<Model>> split = (new ByLabelOrAllInOneClustering()).run(database).getAllClusters();

    // Compute cluster averages and covariance matrix
    HashMap<Cluster<?>, double[]> averages = new HashMap<>(split.size());
    HashMap<Cluster<?>, double[][]> covmats = new HashMap<>(split.size());
    for(Cluster<?> clus : split) {
      CovarianceMatrix covmat = CovarianceMatrix.make(relation, clus.getIDs());
      averages.put(clus, covmat.getMeanVector());
      covmats.put(clus, covmat.destroyToPopulationMatrix());
    }

    ObjHistogram<MeanVariance> hist = new ObjHistogram<MeanVariance>(numbins, 0.0, 1.0, MeanVariance::new);

    if(LOG.isVerbose()) {
      LOG.verbose("Processing points...");
    }
    FiniteProgress rocloop = LOG.isVerbose() ? new FiniteProgress("Computing ROC AUC values", relation.size(), LOG) : null;

    ROCEvaluation roc = new ROCEvaluation();
    // sort neighbors
    for(Cluster<?> clus : split) {
      ModifiableDoubleDBIDList cmem = DBIDUtil.newDistanceDBIDList(clus.size());
      double[] av = averages.get(clus);
      double[][] covm = covmats.get(clus);

      for(DBIDIter iter = clus.getIDs().iter(); iter.valid(); iter.advance()) {
        double[] v = minusEquals(relation.get(iter).toArray(), av);
        cmem.add(transposeTimesTimes(v, covm, v), iter);
      }
      cmem.sort();

      for(DBIDArrayIter it = cmem.iter(); it.valid(); it.advance()) {
        KNNList knn = knnQuery.getKNNForDBID(it, relation.size());
        double result = EvaluateClustering.evaluateRanking(roc, clus, knn);
        hist.get(((double) it.getOffset()) / clus.size()).put(result);

        LOG.incrementProcessed(rocloop);
      }
    }
    LOG.ensureCompleted(rocloop);
    // Collections.sort(results);

    // Transform Histogram into a Double Vector array.
    Collection<double[]> res = new ArrayList<>(relation.size());
    for(ObjHistogram<MeanVariance>.Iter iter = hist.iter(); iter.valid(); iter.advance()) {
      res.add(new double[] { iter.getCenter(), iter.getValue().getCount(), iter.getValue().getMean(), iter.getValue().getSampleVariance() });
    }
    HistogramResult result = new HistogramResult(res);
    Metadata.of(result).setLongName("Ranking Quality Histogram");
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new CombinedTypeInformation(getDistance().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD));
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
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super V>> {
    /**
     * Option to configure the number of bins to use.
     */
    public static final OptionID HISTOGRAM_BINS_ID = new OptionID("rankqual.bins", "Number of bins to use in the histogram");

    /**
     * Number of bins to use.
     */
    protected int numbins = 20;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(HISTOGRAM_BINS_ID, 20) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> numbins = x);
    }

    @Override
    public EvaluateRankingQuality<V> make() {
      return new EvaluateRankingQuality<>(distanceFunction, numbins);
    }
  }
}
