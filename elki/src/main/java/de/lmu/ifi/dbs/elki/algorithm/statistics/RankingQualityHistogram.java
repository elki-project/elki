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
package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelOrAllInOneClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.clustering.EvaluateClustering;
import de.lmu.ifi.dbs.elki.evaluation.scores.ROCEvaluation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.histogram.DoubleHistogram;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Evaluate a distance function with respect to kNN queries. For each point, the
 * neighbors are sorted by distance, then the ROC AUC is computed. A score of 1
 * means that the distance function provides a perfect ordering of relevant
 * neighbors first, then irrelevant neighbors. A value of 0.5 can be obtained by
 * random sorting. A value of 0 means the distance function is inverted, i.e. a
 * similarity.
 *
 * TODO: Add sampling
 *
 * @author Erich Schubert
 * @since 0.2
 * @param <O> Object type
 */
@Title("Ranking Quality Histogram")
@Description("Evaluates the effectiveness of a distance function via the obtained rankings.")
public class RankingQualityHistogram<O> extends AbstractDistanceBasedAlgorithm<O, CollectionResult<double[]>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(RankingQualityHistogram.class);

  /**
   * Number of bins to use.
   */
  protected int numbins = 100;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to evaluate
   * @param numbins Number of bins
   */
  public RankingQualityHistogram(DistanceFunction<? super O> distanceFunction, int numbins) {
    super(distanceFunction);
    this.numbins = numbins;
  }

  /**
   * Process a database
   *
   * @param database Database to process
   * @param relation Relation to process
   * @return Histogram of ranking qualities
   */
  public HistogramResult run(Database database, Relation<O> relation) {
    final DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final KNNQuery<O> knnQuery = database.getKNNQuery(distanceQuery, relation.size());

    if(LOG.isVerbose()) {
      LOG.verbose("Preprocessing clusters...");
    }
    // Cluster by labels
    Collection<Cluster<Model>> split = (new ByLabelOrAllInOneClustering()).run(database).getAllClusters();

    DoubleHistogram hist = new DoubleHistogram(numbins, 0.0, 1.0);

    if(LOG.isVerbose()) {
      LOG.verbose("Processing points...");
    }
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Computing ROC AUC values", relation.size(), LOG) : null;

    ROCEvaluation roc = new ROCEvaluation();
    MeanVariance mv = new MeanVariance();
    // sort neighbors
    for(Cluster<?> clus : split) {
      for(DBIDIter iter = clus.getIDs().iter(); iter.valid(); iter.advance()) {
        KNNList knn = knnQuery.getKNNForDBID(iter, relation.size());
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
    HistogramResult result = new HistogramResult("Ranking Quality Histogram", "ranking-histogram", res);
    result.addHeader("Mean: " + mv.getMean() + " Variance: " + mv.getSampleVariance());
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Option to configure the number of bins to use.
     */
    public static final OptionID HISTOGRAM_BINS_ID = new OptionID("rankqual.bins", "Number of bins to use in the histogram");

    /**
     * Number of bins.
     */
    protected int numbins = 20;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter param = new IntParameter(HISTOGRAM_BINS_ID, 100) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(param)) {
        numbins = param.getValue();
      }
    }

    @Override
    protected RankingQualityHistogram<O> makeInstance() {
      return new RankingQualityHistogram<>(distanceFunction, numbins);
    }
  }
}
