package de.lmu.ifi.dbs.elki.algorithm.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.AggregatingHistogram;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HistogramResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Evaluate a distance function with respect to kNN queries. For each point, the
 * neighbors are sorted by distance, then the ROC AUC is computed. A score of 1
 * means that the distance function provides a perfect ordering of relevant
 * neighbors first, then irrelevant neighbors. A value of 0.5 can be obtained by
 * random sorting. A value of 0 means the distance function is inverted, i.e. a
 * similarity.
 * 
 * TODO: Make number of bins configurable
 * 
 * TODO: Add sampling
 * 
 * @author Erich Schubert
 * @param <O> Object type
 * @param <D> Distance type
 */
@Title("Ranking Quality Histogram")
@Description("Evaluates the effectiveness of a distance function via the obtained rankings.")
public class RankingQualityHistogram<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, CollectionResult<DoubleVector>> {
  /**
   * Constructor.
   * 
   * @param distanceFunction
   */
  public RankingQualityHistogram(DistanceFunction<? super O, D> distanceFunction) {
    super(distanceFunction);
  }

  /**
   * Run the algorithm.
   */
  @Override
  protected HistogramResult<DoubleVector> runInTime(Database<O> database) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = getDistanceFunction().instantiate(database);

    // local copy, not entirely necessary. I just like control, guaranteed
    // sequences and stable+efficient array index -> id lookups.
    ArrayModifiableDBIDs ids = DBIDUtil.newArray(database.getIDs());
    int size = ids.size();

    if(logger.isVerbose()) {
      logger.verbose("Preprocessing clusters...");
    }
    // Cluster by labels
    ByLabelClustering<O> splitter = new ByLabelClustering<O>();
    Collection<Cluster<Model>> split = splitter.run(database).getAllClusters();

    AggregatingHistogram<Double, Double> hist = AggregatingHistogram.DoubleSumHistogram(100, 0.0, 1.0);

    if(logger.isVerbose()) {
      logger.verbose("Processing points...");
    }
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Computing ROC AUC values", size, logger) : null;

    // sort neighbors
    for(Cluster<?> clus : split) {
      for(DBID i1 : clus.getIDs()) {
        List<DistanceResultPair<D>> knn = database.kNNQueryForID(i1, size, distFunc);
        double result = ROC.computeROCAUCDistanceResult(size, clus, knn);

        hist.aggregate(result, 1. / size);

        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }

    // Transform Histogram into a Double Vector array.
    Collection<DoubleVector> res = new ArrayList<DoubleVector>(size);
    for(Pair<Double, Double> pair : hist) {
      DoubleVector row = new DoubleVector(new double[] { pair.getFirst(), pair.getSecond() });
      res.add(row);
    }
    return new HistogramResult<DoubleVector>(res);
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> RankingQualityHistogram<O, D> parameterize(Parameterization config) {
    DistanceFunction<O, D> distanceFunction = getParameterDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new RankingQualityHistogram<O, D>(distanceFunction);
  }
}