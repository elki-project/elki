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
package elki.evaluation.clustering.internal;

import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ModelUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Evaluate a clustering by reporting the squared errors (SSE, SSQ), as used by
 * k-means. This should be used with {@link SquaredEuclideanDistance}
 * only (when used with other distances, it will manually square the values; but
 * beware that the result is less meaningful with other distance functions).
 * <p>
 * For clusterings that provide a cluster prototype object (e.g. k-means), the
 * prototype will be used. For other algorithms, the centroid will be
 * recomputed.
 * <p>
 * TODO: support non-vector based clusterings, too, if the algorithm provided a
 * prototype object (e.g. PAM).
 * <p>
 * TODO: when combined with k-means, detect if the distance functions agree
 * (both should be using squared Euclidean), and reuse the SSQ values provided
 * by k-means.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
public class SquaredErrors implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(SquaredErrors.class);

  /**
   * Handling of Noise clusters
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private NumberVectorDistance<?> distance;

  /**
   * Key for logging statistics.
   */
  private String key = SquaredErrors.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function to use.
   * @param noiseOption Control noise handling.
   */
  public SquaredErrors(NumberVectorDistance<?> distance, NoiseHandling noiseOption) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOption;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param rel Data relation
   * @param c Clustering
   * @return ssq
   */
  public double evaluateClustering(Relation<? extends NumberVector> rel, Clustering<?> c) {
    boolean square = !distance.isSquared();
    int ignorednoise = 0;

    List<? extends Cluster<?>> clusters = c.getAllClusters();
    double ssq = 0, sum = 0;
    for(Cluster<?> cluster : clusters) {
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          ignorednoise += cluster.size();
          continue;
        case TREAT_NOISE_AS_SINGLETONS:
          continue;
        case MERGE_NOISE:
          break; // Treat as cluster below:
        }
      }
      NumberVector center = ModelUtil.getPrototypeOrCentroid(cluster.getModel(), rel, cluster.getIDs());
      for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
        final double d = distance.distance(center, rel.get(it1));
        sum += d;
        ssq += square ? d * d : d;
      }
    }
    final int div = Math.max(1, rel.size() - ignorednoise);
    if(LOG.isStatistics()) {
      LOG.statistics(new DoubleStatistic(key + ".mean", sum / div));
      LOG.statistics(new DoubleStatistic(key + ".ssq", ssq));
      LOG.statistics(new DoubleStatistic(key + ".rmsd", Math.sqrt(ssq / div)));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("Mean distance", sum / div, 0., Double.POSITIVE_INFINITY, true);
    g.addMeasure("Sum of Squares", ssq, 0., Double.POSITIVE_INFINITY, true);
    g.addMeasure("RMSD", Math.sqrt(ssq / div), 0., Double.POSITIVE_INFINITY, true);
    if(!Metadata.hierarchyOf(c).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
    return ssq;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<NumberVector> rel = db.getRelation(distance.getInputTypeRestriction());
    for(Clustering<?> c : crs) {
      evaluateClustering(rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("ssq.distance", "Distance function to use for computing the SSQ.");

    /**
     * Parameter to treat noise as a single cluster.
     */
    public static final OptionID NOISE_ID = new OptionID("ssq.noisehandling", "Control how noise should be treated.");

    /**
     * Distance function to use.
     */
    private NumberVectorDistance<?> distance;

    /**
     * Handling of noise clusters.
     */
    private NoiseHandling noiseOption;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<NumberVectorDistance<?>>(DISTANCE_ID, NumberVectorDistance.class, SquaredEuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS) //
          .grab(config, x -> noiseOption = x);
    }

    @Override
    public SquaredErrors make() {
      return new SquaredErrors(distance, noiseOption);
    }
  }
}
