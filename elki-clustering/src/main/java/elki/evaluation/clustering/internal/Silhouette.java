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
package elki.evaluation.clustering.internal;

import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.evaluation.Evaluator;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.math.MeanVariance;
import elki.result.EvaluationResult;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the silhouette of a data set.
 * <p>
 * Reference:
 * <p>
 * P. J. Rousseeuw<br>
 * Silhouettes: A graphical aid to the interpretation and validation of cluster
 * analysis<br>
 * In: Journal of Computational and Applied Mathematics Volume 20, November 1987
 * <p>
 * TODO: keep all silhouette values, and allow visualization!
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 *
 * @param <O> Object type
 */
@Reference(authors = "P. J. Rousseeuw", //
    title = "Silhouettes: A graphical aid to the interpretation and validation of cluster analysis", //
    booktitle = "Journal of Computational and Applied Mathematics, Volume 20", //
    url = "https://doi.org/10.1016/0377-0427(87)90125-7", //
    bibkey = "doi:10.1016/0377-04278790125-7")
public class Silhouette<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(Silhouette.class);

  /**
   * Name of the silhouette result.
   */
  public static final String SILHOUETTE_NAME = "Silhouette scores";

  /**
   * Distance function to use.
   */
  private Distance<? super O> distance;

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  private boolean penalize;

  /**
   * Key for logging statistics.
   */
  private static final String key = Silhouette.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOption Handling of "noise" clusters.
   * @param penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
   */
  public Silhouette(Distance<? super O> distance, NoiseHandling noiseOption, boolean penalize) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOption;
    this.penalize = penalize;
  }

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, instead of breaking them
   *        into singletons.
   */
  public Silhouette(Distance<? super O> distance, boolean mergenoise) {
    this(distance, mergenoise ? NoiseHandling.MERGE_NOISE : NoiseHandling.TREAT_NOISE_AS_SINGLETONS, true);
  }

  /**
   * Evaluate a single clustering.
   *
   * @param rel Data relation
   * @param dq Distance query
   * @param c Clustering
   * @return Average silhouette
   */
  public double evaluateClustering(Relation<O> rel, DistanceQuery<O> dq, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    MeanVariance msil = new MeanVariance();
    int ignorednoise = 0;
    // Store values for the Silhouette plot
    WritableDoubleDataStore silhouettes = DataStoreFactory.FACTORY.makeDoubleStorage(rel.getDBIDs(), DataStoreFactory.HINT_DB, 0.);
    if(clusters.size() <= 1) {
      msil.put(0, rel.size()); // no other cluster exists, use silhouette 0
    }
    else {
      for(Cluster<?> cluster : clusters) {
        // Note: we treat 1-element clusters the same as noise.
        if(cluster.size() <= 1 || cluster.isNoise()) {
          switch(noiseOption){
          case IGNORE_NOISE:
            ignorednoise += cluster.size();
            continue; // Ignore noise elements
          case TREAT_NOISE_AS_SINGLETONS:
            // As suggested in Rousseeuw, we use 0 for singletons.
            msil.put(0., cluster.size());
            for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
              silhouettes.putDouble(it, 0);
            }
            continue;
          case MERGE_NOISE:
            break; // Treat as cluster below
          }
        }
        ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
        double[] as = new double[ids.size()]; // temporary storage.
        DBIDArrayIter it1 = ids.iter(), it2 = ids.iter();
        for(it1.seek(0); it1.valid(); it1.advance()) {
          // a: In-cluster distances
          double a = as[it1.getOffset()]; // Already computed distances
          for(it2.seek(it1.getOffset() + 1); it2.valid(); it2.advance()) {
            final double dist = dq.distance(it1, it2);
            a += dist;
            as[it2.getOffset()] += dist;
          }
          a /= ids.size() - 1;
          // b: minimum average distance to other clusters:
          double b = Double.POSITIVE_INFINITY;
          for(Cluster<?> ocluster : clusters) {
            if(ocluster == /* yes, reference identity */cluster) {
              continue; // Same cluster
            }
            if(ocluster.size() <= 1 || ocluster.isNoise()) {
              switch(noiseOption){
              case IGNORE_NOISE:
                continue; // Ignore noise elements
              case TREAT_NOISE_AS_SINGLETONS:
                // Treat noise cluster as singletons:
                for(DBIDIter it3 = ocluster.getIDs().iter(); it3.valid(); it3.advance()) {
                  final double dist = dq.distance(it1, it3);
                  b = dist < b ? dist : b; // Minimum average
                }
                continue;
              case MERGE_NOISE:
                break; // Treat as cluster below
              }
            }
            final DBIDs oids = ocluster.getIDs();
            double btmp = 0.;
            for(DBIDIter it3 = oids.iter(); it3.valid(); it3.advance()) {
              btmp += dq.distance(it1, it3);
            }
            btmp /= oids.size(); // Average
            b = btmp < b ? btmp : b; // Minimum average
          }
          // One cluster only? Then use 0.
          final double s = b < Double.POSITIVE_INFINITY ? (b - a) / (b > a ? b : a) : 0;
          msil.put(s);
          silhouettes.putDouble(it1, s);
        }
      }
    }
    double penalty = 1.;
    // Only if {@link NoiseHandling#IGNORE_NOISE}:
    if(penalize && ignorednoise > 0) {
      penalty = (rel.size() - ignorednoise) / (double) rel.size();
    }
    final double meansil = penalty * msil.getMean();
    final double stdsil = penalty * msil.getSampleStddev();
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".silhouette.noise-handling", noiseOption.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".silhouette.noise", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".silhouette.mean", meansil));
      LOG.statistics(new DoubleStatistic(key + ".silhouette.stddev", stdsil));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("Silhouette +-" + FormatUtil.NF2.format(stdsil), meansil, -1., 1., 0., false);
    if(!Metadata.hierarchyOf(c).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
    Metadata.hierarchyOf(c).addChild(new MaterializedDoubleRelation(SILHOUETTE_NAME, rel.getDBIDs(), silhouettes));
    return meansil;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<O> relation = db.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> dq = new QueryBuilder<>(relation, distance).distanceQuery();
    for(Clustering<?> c : crs) {
      evaluateClustering(relation, dq, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("silhouette.distance", "Distance function to use for computing the silhouette.");

    /**
     * Parameter to treat noise as a single cluster.
     */
    public static final OptionID NOISE_ID = new OptionID("silhouette.noisehandling", "Control how noise should be treated.");

    /**
     * Do not penalize ignored noise.
     */
    public static final OptionID NO_PENALIZE_ID = new OptionID("silhouette.no-penalize-noise", "Do not penalize ignored noise.");

    /**
     * Distance function to use.
     */
    private Distance<? super O> distance;

    /**
     * Noise handling
     */
    private NoiseHandling noiseOption;

    /**
     * Penalize noise, if {@link NoiseHandling#IGNORE_NOISE} is set.
     */
    private boolean penalize = true;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS) //
          .grab(config, x -> noiseOption = x);
      if(noiseOption == NoiseHandling.IGNORE_NOISE) {
        new Flag(NO_PENALIZE_ID).grab(config, x -> penalize = !x);
      }
    }

    @Override
    public Silhouette<O> make() {
      return new Silhouette<>(distance, noiseOption, penalize);
    }
  }
}
