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
package de.lmu.ifi.dbs.elki.evaluation.clustering.internal;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ModelUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import net.jafama.FastMath;

/**
 * Evaluate a clustering by reporting the squared errors (SSE, SSQ), as used by
 * k-means. This should be used with {@link SquaredEuclideanDistanceFunction}
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
public class EvaluateSquaredErrors implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSquaredErrors.class);

  /**
   * Handling of Noise clusters
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private NumberVectorDistanceFunction<?> distance;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateSquaredErrors.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function to use.
   * @param noiseOption Control noise handling.
   */
  public EvaluateSquaredErrors(NumberVectorDistanceFunction<?> distance, NoiseHandling noiseOption) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOption;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return ssq
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
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
      LOG.statistics(new DoubleStatistic(key + ".rmsd", FastMath.sqrt(ssq / div)));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Mean distance", sum / div, 0., Double.POSITIVE_INFINITY, true);
    g.addMeasure("Sum of Squares", ssq, 0., Double.POSITIVE_INFINITY, true);
    g.addMeasure("RMSD", FastMath.sqrt(ssq / div), 0., Double.POSITIVE_INFINITY, true);
    db.getHierarchy().add(c, ev);
    return ssq;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(hier);
    Relation<NumberVector> rel = db.getRelation(distance.getInputTypeRestriction());
    for(Clustering<?> c : crs) {
      evaluateClustering(db, rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
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
    private NumberVectorDistanceFunction<?> distance;

    /**
     * Handling of noise clusters.
     */
    private NoiseHandling noiseOption;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<NumberVectorDistanceFunction<?>> distP = new ObjectParameter<>(DISTANCE_ID, NumberVectorDistanceFunction.class, SquaredEuclideanDistanceFunction.class);
      if(config.grab(distP)) {
        distance = distP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }
    }

    @Override
    protected EvaluateSquaredErrors makeInstance() {
      return new EvaluateSquaredErrors(distance, noiseOption);
    }
  }
}
