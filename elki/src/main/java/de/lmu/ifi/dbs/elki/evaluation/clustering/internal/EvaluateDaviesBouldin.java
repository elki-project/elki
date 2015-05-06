package de.lmu.ifi.dbs.elki.evaluation.clustering.internal;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ModelUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Compute the Davies-Bouldin index of a data set.
 * 
 * Reference:
 * <p>
 * D. L. Davies and D. W. Bouldin<br />
 * A Cluster Separation Measure<br />
 * In: IEEE Transactions Pattern Analysis and Machine Intelligence PAMI-1(2)
 * </p>
 * 
 * @author Stephan Baier
 */
@Reference(authors = "D. L. Davies and D. W. Bouldin", //
title = "A Cluster Separation Measure",//
booktitle = "IEEE Transactions Pattern Analysis and Machine Intelligence PAMI-1(2)", //
url = "http://dx.doi.org/10.1109/TPAMI.1979.4766909")
public class EvaluateDaviesBouldin implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateDaviesBouldin.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

  /**
   * Distance function to use.
   */
  private PrimitiveDistanceFunction<? super NumberVector> distanceFunction;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateDaviesBouldin.class.getName();

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateDaviesBouldin(PrimitiveDistanceFunction<? super NumberVector> distance, NoiseHandling noiseOpt) {
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
    int noisecount = 0;

    // precompute all centroids and within-group distances
    ArrayList<NumberVector> centroids = new ArrayList<NumberVector>();
    TDoubleList withinGroupDists = new TDoubleArrayList();
    for(Cluster<?> cluster : clusters) {
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
        case IGNORE_NOISE_WITH_PENALTY:
          noisecount += cluster.size();
          centroids.add(null);
          break;
        case MERGE_NOISE:
        case TREAT_NOISE_AS_SINGLETONS:
          break;
        }
      }
      NumberVector p = ModelUtil.getPrototype(cluster.getModel(), rel);
      if(p == null) {
        p = Centroid.make(rel, cluster.getIDs());
      }
      centroids.add(p);
      double wD = 0.;
      for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
        wD += distanceFunction.distance(p, rel.get(it1));
      }
      withinGroupDists.add(wD / cluster.size());
    }
    assert (withinGroupDists.size() == clusters.size());
    assert (centroids.size() == clusters.size());

    Mean daviesBouldin = new Mean();

    for(int i = 0; i < clusters.size(); i++) {
      NumberVector centroid = centroids.get(i);
      if(centroid == null) {
        continue; // Singleton / Noise
      }
      /* maximum within-to-between cluster spread */
      double max = 0;
      for(int j = 0; j < clusters.size(); j++) {
        NumberVector ocentroid = centroids.get(j);
        if(ocentroid == null || ocentroid == centroid) {
          continue;
        }
        /* bD = between group distance */
        double bD = distanceFunction.distance(centroid, ocentroid);
        /* d = within-to-between cluster spread */
        double d = (withinGroupDists.get(i) + withinGroupDists.get(j)) / bD;
        max = d > max ? d : max;
      }
      daviesBouldin.put(max);
    }

    double penalty = 1.;
    if(noiseOption == NoiseHandling.IGNORE_NOISE_WITH_PENALTY && noisecount > 0) {
      penalty = (rel.size() - noisecount) / (double) rel.size();
    }
    else if(noisecount > 0) {
      LOG.warning("Ignoring " + noisecount + " noise objects for Davies Bouldin index. The result may be biased.");
    }
    double daviesBouldinMean = penalty * daviesBouldin.getMean();

    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".db-index.noise-handling", noiseOption.toString()));
      LOG.statistics(new LongStatistic(key + ".db-index.noise", noisecount));
      LOG.statistics(new DoubleStatistic(key + ".db-index", daviesBouldinMean));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("Davies Bouldin Index", daviesBouldinMean, 0., 1., 0., true);
    db.getHierarchy().resultChanged(ev);
    return daviesBouldinMean;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<? extends NumberVector> rel = db.getRelation(this.distanceFunction.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      evaluateClustering(db, (Relation<? extends NumberVector>) rel, c);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Stephan Baier
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("davies-bouldin.distance", "Distance function to use for computing the davies-bouldin index.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_ID = new OptionID("davies-bouldin.noisehandling", "Controls how noise should be treated.");

    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<NumberVector> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption = NoiseHandling.IGNORE_NOISE_WITH_PENALTY;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<PrimitiveDistanceFunction<NumberVector>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class, ManhattanDistanceFunction.class);
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
