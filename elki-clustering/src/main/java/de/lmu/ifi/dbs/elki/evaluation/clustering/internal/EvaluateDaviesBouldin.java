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
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.Mean;
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

/**
 * Compute the Davies-Bouldin index of a data set.
 * <p>
 * Reference:
 * <p>
 * D. L. Davies, D. W. Bouldin<br>
 * A Cluster Separation Measure<br>
 * IEEE Transactions Pattern Analysis and Machine Intelligence 1(2)
 *
 * @author Stephan Baier
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
@Reference(authors = "D. L. Davies, D. W. Bouldin", //
    title = "A Cluster Separation Measure", //
    booktitle = "IEEE Transactions Pattern Analysis and Machine Intelligence 1(2)", //
    url = "https://doi.org/10.1109/TPAMI.1979.4766909", //
    bibkey = "DBLP:journals/pami/DaviesB79")
public class EvaluateDaviesBouldin implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateDaviesBouldin.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption;

  /**
   * Distance function to use.
   */
  private NumberVectorDistanceFunction<?> distanceFunction;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateDaviesBouldin.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseOpt Flag to control noise handling
   */
  public EvaluateDaviesBouldin(NumberVectorDistanceFunction<?> distance, NoiseHandling noiseOpt) {
    super();
    this.distanceFunction = distance;
    this.noiseOption = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return DB-index
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();
    NumberVector[] centroids = new NumberVector[clusters.size()];
    int noisecount = EvaluateSimplifiedSilhouette.centroids(rel, clusters, centroids, noiseOption);
    double[] withinGroupDistance = withinGroupDistances(rel, clusters, centroids);

    Mean daviesBouldin = new Mean();
    for(int i = 0; i < clusters.size(); i++) {
      final NumberVector centroid = centroids[i];
      final double withinGroupDistancei = withinGroupDistance[i];
      // maximum within-to-between cluster spread
      double max = 0;
      for(int j = 0; j < clusters.size(); j++) {
        NumberVector ocentroid = centroids[j];
        if(ocentroid == centroid) {
          continue;
        }
        // Both are real clusters:
        if(centroid != null && ocentroid != null) {
          // bD = between group distance
          double bD = distanceFunction.distance(centroid, ocentroid);
          // d = within-to-between cluster spread
          double d = (withinGroupDistancei + withinGroupDistance[j]) / bD;
          max = d > max ? d : max;
        }
        else if(noiseOption != NoiseHandling.IGNORE_NOISE) {
          if(centroid != null) {
            double d = Double.POSITIVE_INFINITY;
            // Find the closest element
            for(DBIDIter it = clusters.get(j).getIDs().iter(); it.valid(); it.advance()) {
              double d2 = distanceFunction.distance(centroid, rel.get(it));
              d = d2 < d ? d2 : d;
            }
            d = withinGroupDistancei / d;
            max = d > max ? d : max;
          }
          else if(ocentroid != null) {
            double d = Double.POSITIVE_INFINITY;
            // Find the closest element
            for(DBIDIter it = clusters.get(i).getIDs().iter(); it.valid(); it.advance()) {
              double d2 = distanceFunction.distance(rel.get(it), ocentroid);
              d = d2 < d ? d2 : d;
            }
            d = withinGroupDistance[j] / d;
            max = d > max ? d : max;
          } // else: (0+0) / d = 0.
        }
      }
      daviesBouldin.put(max);
    }

    // For a single cluster, we return 2 (result for equidistant points)
    final double daviesBouldinMean = daviesBouldin.getCount() > 1 ? daviesBouldin.getMean() : 2.;

    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".db-index.noise-handling", noiseOption.toString()));
      if(noisecount > 0) {
        LOG.statistics(new LongStatistic(key + ".db-index.ignored", noisecount));
      }
      LOG.statistics(new DoubleStatistic(key + ".db-index", daviesBouldinMean));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Davies Bouldin Index", daviesBouldinMean, 0., Double.POSITIVE_INFINITY, 0., true);
    db.getHierarchy().resultChanged(ev);
    return daviesBouldinMean;
  }

  public double[] withinGroupDistances(Relation<? extends NumberVector> rel, List<? extends Cluster<?>> clusters, NumberVector[] centroids) {
    double[] withinGroupDists = new double[clusters.size()];
    Iterator<? extends Cluster<?>> ci = clusters.iterator();
    for(int i = 0; ci.hasNext(); i++) {
      Cluster<?> cluster = ci.next();
      NumberVector centroid = centroids[i];
      if(centroid == null) { // Empty, noise or singleton cluster:
        withinGroupDists[i] = 0.;
        continue;
      }
      double wD = 0.;
      for(DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
        wD += distanceFunction.distance(centroid, rel.get(it));
      }
      withinGroupDists[i] = wD / cluster.size();
    }
    return withinGroupDists;
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
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("davies-bouldin.distance", "Distance function to use for computing the davies-bouldin index.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_ID = new OptionID("davies-bouldin.noisehandling", "Control how noise should be treated.");

    /**
     * Distance function to use.
     */
    private NumberVectorDistanceFunction<?> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<NumberVectorDistanceFunction<?>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, NumberVectorDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }
    }

    @Override
    protected EvaluateDaviesBouldin makeInstance() {
      return new EvaluateDaviesBouldin(distance, noiseOption);
    }
  }
}
