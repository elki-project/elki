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

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
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
public class EvaluatePBMIndex implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluatePBMIndex.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseHandling;

  /**
   * Distance function to use.
   */
  private NumberVectorDistanceFunction<?> distanceFunction;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluatePBMIndex.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOpt Flag to control noise handling
   */
  public EvaluatePBMIndex(NumberVectorDistanceFunction<?> distance, NoiseHandling noiseOpt) {
    super();
    this.distanceFunction = distance;
    this.noiseHandling = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return PBM
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    NumberVector[] centroids = new NumberVector[clusters.size()];
    int ignorednoise = EvaluateSimplifiedSilhouette.centroids(rel, clusters, centroids, noiseHandling);

    // Build global centroid and cluster count:
    final int dim = RelationUtil.dimensionality(rel);
    Centroid overallCentroid = new Centroid(dim);
    EvaluateVarianceRatioCriteria.globalCentroid(overallCentroid, rel, clusters, centroids, noiseHandling);

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
              double dist = distanceFunction.distance(rel.get(iti), rel.get(itj));
              max = dist > max ? dist : max;
            }
          }
        }
        else if(centroids[i] == null) {
          for(DBIDIter iti = clusters.get(i).getIDs().iter(); iti.valid(); iti.advance()) {
            double dist = distanceFunction.distance(rel.get(iti), centroids[j]);
            max = dist > max ? dist : max;
          }
        }
        else if(centroids[j] == null) {
          for(DBIDIter itj = clusters.get(j).getIDs().iter(); itj.valid(); itj.advance()) {
            double dist = distanceFunction.distance(centroids[i], rel.get(itj));
            max = dist > max ? dist : max;
          }
        }
        else {
          double dist = distanceFunction.distance(centroids[i], centroids[j]);
          max = dist > max ? dist : max;
        }
      }
    }

    // a: Distance to own centroid
    // b: Distance to overall centroid
    double a = 0, b = 0;
    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> cluster = ci.next();
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseHandling){
        case IGNORE_NOISE:
          continue; // Ignored
        case TREAT_NOISE_AS_SINGLETONS:
          // Singletons: a = 0 by definition.
          for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
            b += SquaredEuclideanDistanceFunction.STATIC.distance(overallCentroid, rel.get(it));
          }
          continue; // with NEXT cluster.
        case MERGE_NOISE:
          break; // Treat like a cluster below:
        }
      }

      for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
        NumberVector obj = rel.get(it);
        a += distanceFunction.distance(centroids[i], obj);
        b += distanceFunction.distance(overallCentroid, obj);
      }
    }

    final double pbm = FastMath.pow((1. / centroids.length) * (b / a) * max, 2.);

    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".pbm.noise-handling", noiseHandling.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".pbm.ignored", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".pbm", pbm));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("PBM-Index", pbm, 0., Double.POSITIVE_INFINITY, 0., false);
    db.getHierarchy().resultChanged(ev);
    return pbm;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    List<Clustering<?>> crs = Clustering.getClusteringResults(result);
    if(crs.isEmpty()) {
      return;
    }
    Database db = ResultUtil.findDatabase(hier);
    Relation<? extends NumberVector> rel = db.getRelation(this.distanceFunction.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      evaluateClustering(db, (Relation<? extends NumberVector>) rel, c);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Stephan Baier
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
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
    private NumberVectorDistanceFunction<?> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseHandling;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<NumberVectorDistanceFunction<?>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, NumberVectorDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseHandling = noiseP.getValue();
      }
    }

    @Override
    protected EvaluatePBMIndex makeInstance() {
      return new EvaluatePBMIndex(distance, noiseHandling);
    }
  }

}
