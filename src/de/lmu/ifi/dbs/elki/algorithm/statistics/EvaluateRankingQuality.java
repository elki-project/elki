package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROCAUC;
import de.lmu.ifi.dbs.elki.math.Histogram;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

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
public class EvaluateRankingQuality<V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, DoubleDistance, CollectionResult<DoubleVector>> {
  private CollectionResult<DoubleVector> result;

  /**
   * OptionID for {@link #HISTOGRAM_BINS_OPTION}
   */
  public static final OptionID HISTOGRAM_BINS_ID = OptionID.getOrCreateOptionID("rankqual.bins", "Number of bins to use in the histogram");

  /**
   * Option to configure the number of bins to use.
   */
  private final IntParameter HISTOGRAM_BINS_OPTION = new IntParameter(HISTOGRAM_BINS_ID, new GreaterEqualConstraint(2), 20);
  
  /**
   * Empty constructor. Nothing to do.
   */
  public EvaluateRankingQuality() {
    super();
    addOption(HISTOGRAM_BINS_OPTION);
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
    DistanceFunction<V, DoubleDistance> distFunc = getDistanceFunction();
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
    ByLabelClustering<V> split = new ByLabelClustering<V>();
    Set<Cluster<Model>> splitted = split.run(database).getAllClusters();

    // Compute cluster averages
    HashMap<Cluster<?>, V> averages = new HashMap<Cluster<?>, V>(splitted.size());
    for(Cluster<?> clus : splitted) {
      V cent = DatabaseUtil.centroid(database, clus.getIDs());
      averages.put(clus, cent);
    }

    Histogram<MeanVariance> hist = Histogram.MeanVarianceHistogram(numbins, 0.0, 1.0);

    if(logger.isVerbose()) {
      logger.verbose("Processing points...");
    }
    FiniteProgress rocloop = new FiniteProgress("Computing ROC AUC values", size);
    int rocproc = 0;

    // sort neighbors
    for(Cluster<?> clus : splitted) {
      ArrayList<FCPair<Double, Integer>> cmem = new ArrayList<FCPair<Double, Integer>>(clus.size());
      V av = averages.get(clus);
      for(Integer i1 : clus.getIDs()) {
        Double d = distFunc.distance(database.get(i1), av).getValue();
        cmem.add(new FCPair<Double, Integer>(d, i1));
      }
      Collections.sort(cmem);

      for(int ind = 0; ind < cmem.size(); ind++) {
        Integer i1 = cmem.get(ind).getSecond();
        List<DistanceResultPair<DoubleDistance>> knn = database.kNNQueryForID(i1, size, distFunc);
        double result = ROCAUC.computeROCAUC(size, clus, knn);

        hist.get(((double)ind) / clus.size()).addData(result);

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
    for (Pair<Double, MeanVariance> pair : hist) {
      DoubleVector row = new DoubleVector(new double[] { pair.getFirst(), pair.getSecond().getCount(), pair.getSecond().getMean(), pair.getSecond().getVariance() });
      res.add(row);
    }
    result = new CollectionResult<DoubleVector>(res);
    return result;
  }

  /**
   * Describe the algorithm and it's use.
   */
  public Description getDescription() {
    return new Description("EvaluateRankingQuality", "EvaluateRankingQuality", "Evaluates the effectiveness of a distance function via the obtained rankings.", "");
  }

  /**
   * Return a result object
   */
  public CollectionResult<DoubleVector> getResult() {
    return result;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm#setParameters(java.lang.String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    
    numbins = HISTOGRAM_BINS_OPTION.getValue();

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }
}
