package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Evaluate a distance function with respect to kNN queries. For each point, the
 * neighbors are sorted by distance, then the ROC AUC is computed. A score of 1
 * means that the distance function provides a perfect ordering of relevant
 * neighbors first, then irrelevant neighbors. A value of 0.5 can be obtained by
 * random sorting. A value of 0 means the distance function is inverted, i.e. a
 * similarity.
 * 
 * TODO: Allow fixed binning range, configurable
 * 
 * TODO: Add sampling
 * 
 * @author Erich Schubert
 * @param <V> Vector type
 */
@Title("Evaluate Ranking Quality")
@Description("Evaluates the effectiveness of a distance function via the obtained rankings.")
public class EvaluateRankingQuality<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends DistanceBasedAlgorithm<V, D, CollectionResult<DoubleVector>> {
  /**
   * OptionID for {@link #HISTOGRAM_BINS_OPTION}
   */
  public static final OptionID HISTOGRAM_BINS_ID = OptionID.getOrCreateOptionID("rankqual.bins", "Number of bins to use in the histogram");

  /**
   * Option to configure the number of bins to use.
   */
  private final IntParameter HISTOGRAM_BINS_OPTION = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), 20);

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public EvaluateRankingQuality(Parameterization config) {
    super(config);
    if(config.grab(HISTOGRAM_BINS_OPTION)) {
      numbins = HISTOGRAM_BINS_OPTION.getValue();
    }
  }

  /**
   * Number of bins to use.
   */
  int numbins = 20;

  /**
   * Run the algorithm.
   */
  @Override
  protected CollectionResult<DoubleVector> runInTime(Database<V> database) throws IllegalStateException {
    DistanceFunction<V, D> distFunc = getDistanceFunction();
    distFunc.setDatabase(database, isVerbose(), isTime());

    // local copy, not entirely necessary. I just like control, guaranteed
    // sequences
    // and stable+efficient array index -> id lookups.
    ArrayList<Integer> ids = new ArrayList<Integer>(database.getIDs());
    int size = ids.size();

    if(logger.isVerbose()) {
      logger.verbose("Preprocessing clusters...");
    }
    // Cluster by labels
    ByLabelClustering<V> splitter = new ByLabelClustering<V>();
    Collection<Cluster<Model>> split = splitter.run(database).getAllClusters();

    // Compute cluster averages and covariance matrix
    HashMap<Cluster<?>, V> averages = new HashMap<Cluster<?>, V>(split.size());
    HashMap<Cluster<?>, Matrix> covmats = new HashMap<Cluster<?>, Matrix>(split.size());
    for(Cluster<?> clus : split) {
      averages.put(clus, DatabaseUtil.centroid(database, clus.getIDs()));
      covmats.put(clus, DatabaseUtil.covarianceMatrix(database, clus.getIDs()));
    }

    AggregatingHistogram<MeanVariance, Double> hist = AggregatingHistogram.MeanVarianceHistogram(numbins, 0.0, 1.0);

    if(logger.isVerbose()) {
      logger.verbose("Processing points...");
    }
    FiniteProgress rocloop = new FiniteProgress("Computing ROC AUC values", size);
    int rocproc = 0;

    // sort neighbors
    for(Cluster<?> clus : split) {
      ArrayList<FCPair<Double, Integer>> cmem = new ArrayList<FCPair<Double, Integer>>(clus.size());
      Vector av = averages.get(clus).getColumnVector();
      Matrix covm = covmats.get(clus);

      for(Integer i1 : clus.getIDs()) {
        Double d = MathUtil.mahalanobisDistance(covm, av.minus(database.get(i1).getColumnVector()));
        cmem.add(new FCPair<Double, Integer>(d, i1));
      }
      Collections.sort(cmem);

      for(int ind = 0; ind < cmem.size(); ind++) {
        Integer i1 = cmem.get(ind).getSecond();
        List<DistanceResultPair<D>> knn = database.kNNQueryForID(i1, size, distFunc);
        double result = ROC.computeROCAUCDistanceResult(size, clus, knn);

        hist.aggregate(((double) ind) / clus.size(), result);

        if(logger.isVerbose()) {
          rocproc++;
          rocloop.setProcessed(rocproc);
          logger.progress(rocloop);
        }
      }
    }
    // Collections.sort(results);

    // Transform Histogram into a Double Vector array.
    Collection<DoubleVector> res = new ArrayList<DoubleVector>(size);
    for(Pair<Double, MeanVariance> pair : hist) {
      DoubleVector row = new DoubleVector(new double[] { pair.getFirst(), pair.getSecond().getCount(), pair.getSecond().getMean(), pair.getSecond().getVariance() });
      res.add(row);
    }
    return new CollectionResult<DoubleVector>(res);
  }
}
