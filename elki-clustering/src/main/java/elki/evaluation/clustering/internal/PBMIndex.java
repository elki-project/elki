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

import java.util.Iterator;
import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
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
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Compute the PBM index of a clustering
 * <p>
 * Reference:
 * <p>
 * M. K. Pakhira, S. Bandyopadhyay, U. Maulik<br>
 * Validity index for crisp and fuzzy clusters<br>
 * Pattern recognition, 37(3)
 *
 * @author Stephan Baier
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
@Reference(authors = "M. K. Pakhira, S. Bandyopadhyay, U. Maulik", //
    title = "Validity index for crisp and fuzzy clusters", //
    booktitle = "Pattern recognition, 37(3)", //
    url = "https://doi.org/10.1016/j.patcog.2003.06.005", //
    bibkey = "DBLP:journals/pr/PakhiraBM04")
public class PBMIndex implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(PBMIndex.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseHandling;

  /**
   * Distance function to use.
   */
  private NumberVectorDistance<?> distance;

  /**
   * Key for logging statistics.
   */
  private String key = PBMIndex.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOpt Flag to control noise handling
   */
  public PBMIndex(NumberVectorDistance<?> distance, NoiseHandling noiseOpt) {
    super();
    this.distance = distance;
    this.noiseHandling = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param rel Data relation
   * @param c Clustering
   * @return PBM
   */
  public double evaluateClustering(Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    NumberVector[] centroids = new NumberVector[clusters.size()];
    int ignorednoise = SimplifiedSilhouette.centroids(rel, clusters, centroids, noiseHandling);

    // Build global centroid and cluster count:
    final int dim = RelationUtil.dimensionality(rel);
    Centroid overallCentroid = new Centroid(dim);
    VarianceRatioCriterion.globalCentroid(overallCentroid, rel, clusters, centroids, noiseHandling);

    // Maximum distance between centroids:
    double max = 0;
    for(int i = 0; i < centroids.length; i++) {
      if(centroids[i] == null && noiseHandling != NoiseHandling.TREAT_NOISE_AS_SINGLETONS) {
        continue;
      }
      for(int j = i + 1; j < centroids.length; j++) {
        if(centroids[j] == null && noiseHandling != NoiseHandling.TREAT_NOISE_AS_SINGLETONS) {
          continue;
        }
        if(centroids[i] == null && centroids[j] == null) {
          // Need to compute pairwise distances of noise clusters.
          for(DBIDIter iti = clusters.get(i).getIDs().iter(); iti.valid(); iti.advance()) {
            for(DBIDIter itj = clusters.get(j).getIDs().iter(); itj.valid(); itj.advance()) {
              double dist = distance.distance(rel.get(iti), rel.get(itj));
              max = dist > max ? dist : max;
            }
          }
        }
        else if(centroids[i] == null) {
          for(DBIDIter iti = clusters.get(i).getIDs().iter(); iti.valid(); iti.advance()) {
            double dist = distance.distance(rel.get(iti), centroids[j]);
            max = dist > max ? dist : max;
          }
        }
        else if(centroids[j] == null) {
          for(DBIDIter itj = clusters.get(j).getIDs().iter(); itj.valid(); itj.advance()) {
            double dist = distance.distance(centroids[i], rel.get(itj));
            max = dist > max ? dist : max;
          }
        }
        else {
          double dist = distance.distance(centroids[i], centroids[j]);
          max = dist > max ? dist : max;
        }
      }
    }

    // a: Distance to own centroid
    // b: Distance to overall centroid
    // nCL: Number of actual Clusters (needed for Singleton option)
    double a = 0, b = 0;
    int nCl = clusters.size();
    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> cluster = ci.next();
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseHandling){
        case IGNORE_NOISE:
          nCl -= 1; // adjust for ignored cluster
          continue; // Ignored
        case TREAT_NOISE_AS_SINGLETONS:
          // Singletons: a = 0 by definition.
          for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
            b += distance.distance(overallCentroid, rel.get(it));
          }
          nCl += cluster.size() - 1; // expand number of clusters
          continue; // with NEXT cluster.
        case MERGE_NOISE:
          break; // Treat like a cluster below:
        }
      }

      for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
        NumberVector obj = rel.get(it);
        a += distance.distance(centroids[i], obj);
        b += distance.distance(overallCentroid, obj);
      }
    }

    final double pbm = FastMath.pow((1. / nCl) * (b / a) * max, 2.);

    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".pbm.noise-handling", noiseHandling.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".pbm.ignored", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".pbm", pbm));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(c, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("PBM-Index", pbm, 0., Double.POSITIVE_INFINITY, 0., false);
    if(!Metadata.hierarchyOf(c).addChild(ev)) {
      Metadata.of(ev).notifyChanged();
    }
    return pbm;
  }

  @Override
  public void processNewResult(Object result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(result);
    Relation<? extends NumberVector> rel = db.getRelation(this.distance.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      evaluateClustering(rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Stephan Baier
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("pbm.distance", "Distance function to use for computing PBM.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_ID = new OptionID("pbm.noisehandling", "Control how noise should be treated.");

    /**
     * Distance function to use.
     */
    private NumberVectorDistance<?> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseHandling;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<NumberVectorDistance<?>>(DISTANCE_ID, NumberVectorDistance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS) //
          .grab(config, x -> noiseHandling = x);
    }

    @Override
    public PBMIndex make() {
      return new PBMIndex(distance, noiseHandling);
    }
  }

}
