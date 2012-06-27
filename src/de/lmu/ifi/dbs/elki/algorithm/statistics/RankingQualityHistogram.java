package de.lmu.ifi.dbs.elki.algorithm.statistics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.histograms.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

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
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("Ranking Quality Histogram")
@Description("Evaluates the effectiveness of a distance function via the obtained rankings.")
public class RankingQualityHistogram<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, CollectionResult<DoubleVector>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RankingQualityHistogram.class);

  /**
   * Option to configure the number of bins to use.
   */
  public static final OptionID HISTOGRAM_BINS_ID = OptionID.getOrCreateOptionID("rankqual.bins", "Number of bins to use in the histogram");

  /**
   * Number of bins to use.
   */
  int numbins = 100;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function to evaluate
   * @param numbins Number of bins
   */
  public RankingQualityHistogram(DistanceFunction<? super O, D> distanceFunction, int numbins) {
    super(distanceFunction);
    this.numbins = numbins;
  }

  public HistogramResult<DoubleVector> run(Database database, Relation<O> relation) {
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    final KNNQuery<O, D> knnQuery = database.getKNNQuery(distanceQuery, relation.size());

    if(logger.isVerbose()) {
      logger.verbose("Preprocessing clusters...");
    }
    // Cluster by labels
    Collection<Cluster<Model>> split = (new ByLabelClustering()).run(database).getAllClusters();

    AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(numbins, 0.0, 1.0);

    if(logger.isVerbose()) {
      logger.verbose("Processing points...");
    }
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Computing ROC AUC values", relation.size(), logger) : null;

    MeanVariance mv = new MeanVariance();
    // sort neighbors
    for(Cluster<?> clus : split) {
      for(DBIDIter iter = clus.getIDs().iter(); iter.valid(); iter.advance()) {
        DBID i1 = iter.getDBID();
        KNNResult<D> knn = knnQuery.getKNNForDBID(i1, relation.size());
        double result = ROC.computeROCAUCDistanceResult(relation.size(), clus, knn);

        mv.put(result);
        hist.aggregate(result, 1. / relation.size());

        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    // Transform Histogram into a Double Vector array.
    Collection<DoubleVector> res = new ArrayList<DoubleVector>(relation.size());
    for(DoubleObjPair<Double> pair : hist) {
      DoubleVector row = new DoubleVector(new double[] { pair.first, pair.getSecond() });
      res.add(row);
    }
    HistogramResult<DoubleVector> result = new HistogramResult<DoubleVector>("Ranking Quality Histogram", "ranking-histogram", res);
    result.addHeader("Mean: " + mv.getMean() + " Variance: " + mv.getSampleVariance());
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected int numbins = 20;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter param = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), 100);
      if(config.grab(param)) {
        numbins = param.getValue();
      }
    }

    @Override
    protected RankingQualityHistogram<O, D> makeInstance() {
      return new RankingQualityHistogram<O, D>(distanceFunction, numbins);
    }
  }
}