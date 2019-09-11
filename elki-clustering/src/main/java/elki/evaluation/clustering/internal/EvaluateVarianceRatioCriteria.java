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

import java.util.Iterator;
import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.math.linearalgebra.Centroid;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.Flag;

/**
 * Compute the Variance Ratio Criteria of a data set, also known as
 * Calinski-Harabasz index.
 * <p>
 * Reference:
 * <p>
 * R. B. Calinski, J. Harabasz<br>
 * A dendrite method for cluster analysis<br>
 * Communications in Statistics - Theory and Methods 3(1)
 *
 * @author Stephan Baier
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
@Reference(authors = "R. B. Calinski, J. Harabasz", //
    title = "A dendrite method for cluster analysis", //
    booktitle = "Communications in Statistics - Theory and Methods 3(1)", //
    url = "https://doi.org/10.1080/03610927408827101", //
    bibkey = "doi:10.1080/03610927408827101")
@Alias({ "calinski-harabasz" })
public class EvaluateVarianceRatioCriteria<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateVarianceRatioCriteria.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  private boolean penalize = true;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateVarianceRatioCriteria.class.getName();

  /**
   * Constructor.
   *
   * @param noiseOption Flag to control noise handling
   * @param penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  public EvaluateVarianceRatioCriteria(NoiseHandling noiseOption, boolean penalize) {
    super();
    this.noiseOption = noiseOption;
    this.penalize = penalize;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param rel Data relation
   * @param c Clustering
   * @return Variance Ratio Criteria
   */
  public double evaluateClustering(Relation<? extends NumberVector> rel, Clustering<?> c) {
    // FIXME: allow using a precomputed distance matrix!
    final SquaredEuclideanDistance df = SquaredEuclideanDistance.STATIC;
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    double vrc = 0.;
    int ignorednoise = 0;
    if(clusters.size() > 1) {
      NumberVector[] centroids = new NumberVector[clusters.size()];
      ignorednoise = EvaluateSimplifiedSilhouette.centroids(rel, clusters, centroids, noiseOption);

      // Build global centroid and cluster count:
      final int dim = RelationUtil.dimensionality(rel);
      Centroid overallCentroid = new Centroid(dim);
      int clustercount = globalCentroid(overallCentroid, rel, clusters, centroids, noiseOption);

      // a: Distance to own centroid
      // b: Distance to overall centroid
      double a = 0, b = 0;
      Iterator<? extends Cluster<?>> ci = clusters.iterator();
      for(int i = 0; ci.hasNext(); i++) {
        Cluster<?> cluster = ci.next();
        if(cluster.size() <= 1 || cluster.isNoise()) {
          switch(noiseOption){
          case IGNORE_NOISE:
            continue; // Ignored
          case TREAT_NOISE_AS_SINGLETONS:
            // Singletons: a = 0 by definition.
            for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
              b += df.distance(overallCentroid, rel.get(it));
            }
            continue; // with NEXT cluster.
          case MERGE_NOISE:
            break; // Treat like a cluster below:
          }
        }
        for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
          NumberVector vec = rel.get(it);
          a += df.distance(centroids[i], vec);
          b += df.distance(overallCentroid, vec);
        }
      }

      vrc = ((b - a) / a) * ((rel.size() - clustercount) / (clustercount - 1.));
      // Only if {@link NoiseHandling#IGNORE_NOISE}:
      if(penalize && ignorednoise > 0) {
        vrc *= (rel.size() - ignorednoise) / (double) rel.size();
      }
    }
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".vrc.noise-handling", noiseOption.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".vrc.ignored", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".vrc", vrc));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Variance Ratio Criteria", vrc, 0., 1., 0., false);
    Metadata.hierarchyOf(c).addChild(ev);
    // FIXME: notify of changes, if reused!
    return vrc;
  }

  /**
   * Update the global centroid.
   *
   * @param overallCentroid Centroid to udpate
   * @param rel Data relation
   * @param clusters Clusters
   * @param centroids Cluster centroids
   * @return Number of clusters
   */
  public static int globalCentroid(Centroid overallCentroid, Relation<? extends NumberVector> rel, List<? extends Cluster<?>> clusters, NumberVector[] centroids, NoiseHandling noiseOption) {
    int clustercount = 0;
    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> cluster = ci.next();
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
          continue; // Ignore completely
        case TREAT_NOISE_AS_SINGLETONS:
          clustercount += cluster.size();
          // Update global centroid:
          for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
            overallCentroid.put(rel.get(it));
          }
          continue; // With NEXT cluster.
        case MERGE_NOISE:
          break; // Treat as cluster below:
        }
      }
      // Update centroid:
      assert (centroids[i] != null);
      overallCentroid.put(centroids[i], cluster.size());
      ++clustercount;
    }
    return clustercount;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<? extends NumberVector> rel = db.getRelation(EuclideanDistance.STATIC.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      evaluateClustering(rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Stephan Baier
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_ID = new OptionID("vrc.noisehandling", "Control how noise should be treated.");

    /**
     * Do not penalize ignored noise.
     */
    public static final OptionID NO_PENALIZE_ID = new OptionID("silhouette.no-penalize-noise", "Do not penalize ignored noise.");

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption;

    /**
     * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
     */
    private boolean penalize = true;

    @Override
    public void configure(Parameterization config) {
      new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS) //
          .grab(config, x -> noiseOption = x);
      if(noiseOption == NoiseHandling.IGNORE_NOISE) {
        new Flag(NO_PENALIZE_ID).grab(config, x -> penalize = !x);
      }
    }

    @Override
    public EvaluateVarianceRatioCriteria<? extends NumberVector> make() {
      return new EvaluateVarianceRatioCriteria<>(noiseOption, penalize);
    }
  }
}
