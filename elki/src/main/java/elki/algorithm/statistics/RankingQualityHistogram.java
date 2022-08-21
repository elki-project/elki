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
package elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;

import elki.Algorithm;
import elki.clustering.trivial.ByLabelOrAllInOneClustering;
import elki.data.Cluster;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.KNNList;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.clustering.EvaluateClustering;
import elki.evaluation.scores.ROCEvaluation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MeanVariance;
import elki.result.HistogramResult;
import elki.result.Metadata;
import elki.utilities.datastructures.histogram.DoubleHistogram;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Evaluate a distance function with respect to kNN queries. For each point, the
 * neighbors are sorted by distance, then the AUROC is computed. A score of 1
 * means that the distance function provides a perfect ordering of relevant
 * neighbors first, then irrelevant neighbors. A value of 0.5 can be obtained by
 * random sorting. A value of 0 means the distance function is inverted, i.e. a
 * similarity.
 * <p>
 * TODO: Add sampling
 *
 * @author Erich Schubert
 * @since 0.2
 * @param <O> Object type
 */
@Title("Ranking Quality Histogram")
@Description("Evaluates the effectiveness of a distance function via the obtained rankings.")
public class RankingQualityHistogram<O> implements Algorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RankingQualityHistogram.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Number of bins to use.
   */
  protected int numbins = 100;

  /**
   * Constructor.
   *
   * @param distance Distance function to evaluate
   * @param numbins Number of bins
   */
  public RankingQualityHistogram(Distance<? super O> distance, int numbins) {
    super();
    this.distance = distance;
    this.numbins = numbins;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Process a relation
   *
   * @param database Database to process
   * @param relation Relation to process
   * @return Histogram of ranking qualities
   */
  public HistogramResult run(Database database, Relation<O> relation) {
    KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, distance).kNNByDBID(relation.size());

    if(LOG.isVerbose()) {
      LOG.verbose("Preprocessing clusters...");
    }
    // Cluster by labels
    Collection<Cluster<Model>> split = (new ByLabelOrAllInOneClustering()).autorun(database).getAllClusters();

    DoubleHistogram hist = new DoubleHistogram(numbins, 0.0, 1.0);

    if(LOG.isVerbose()) {
      LOG.verbose("Processing points...");
    }
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Computing AUROC values", relation.size(), LOG) : null;

    ROCEvaluation roc = new ROCEvaluation();
    MeanVariance mv = new MeanVariance();
    // sort neighbors
    for(Cluster<?> clus : split) {
      for(DBIDIter iter = clus.getIDs().iter(); iter.valid(); iter.advance()) {
        KNNList knn = knnQuery.getKNN(iter, relation.size());
        double result = EvaluateClustering.evaluateRanking(roc, clus, knn);

        mv.put(result);
        hist.increment(result, 1. / relation.size());

        LOG.incrementProcessed(progress);
      }
    }
    LOG.ensureCompleted(progress);

    // Transform Histogram into a Double Vector array.
    Collection<double[]> res = new ArrayList<>(relation.size());
    for(DoubleHistogram.Iter iter = hist.iter(); iter.valid(); iter.advance()) {
      res.add(new double[] { iter.getCenter(), iter.getValue() });
    }
    HistogramResult result = new HistogramResult(res);
    Metadata.of(result).setLongName("Ranking Quality Histogram");
    result.addHeader("Mean: " + mv.getMean() + " Variance: " + mv.getSampleVariance());
    return result;
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
  public static class Par<O> implements Parameterizer {
    /**
     * Option to configure the number of bins to use.
     */
    public static final OptionID HISTOGRAM_BINS_ID = new OptionID("rankqual.bins", "Number of bins to use in the histogram");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Number of bins.
     */
    protected int numbins = 20;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(HISTOGRAM_BINS_ID, 100) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> numbins = x);
    }

    @Override
    public RankingQualityHistogram<O> make() {
      return new RankingQualityHistogram<>(distance, numbins);
    }
  }
}
