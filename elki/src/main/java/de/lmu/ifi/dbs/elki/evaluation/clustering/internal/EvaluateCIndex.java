package de.lmu.ifi.dbs.elki.evaluation.clustering.internal;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
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

/**
 * Compute the C-index of a data set.
 * 
 * Reference:
 * <p>
 * L. J. Hubert and J.R. Levin <br />
 * A general statistical framework for assessing categorical clustering in free
 * recall<br />
 * Psychological Bulletin, Vol. 83(6)
 * </p>
 * 
 * @author Stephan Baier
 * @author Erich Schubert
 */
@Reference(authors = "L. J. Hubert and J. R. Levin", //
title = "A general statistical framework for assessing categorical clustering in free recall.", //
booktitle = "Psychological Bulletin, Vol. 83(6)", //
url = "http://dx.doi.org/10.1037/0033-2909.83.6.1072")
public class EvaluateCIndex<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateCIndex.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseOption = NoiseHandling.TREAT_NOISE_AS_SINGLETONS;

  /**
   * Distance function to use.
   */
  private DistanceFunction<? super O> distance;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateCIndex.class.getName();

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateCIndex(DistanceFunction<? super O> distance, NoiseHandling noiseOpt) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   * 
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return C-Index
   */
  public double evaluateClustering(Database db, Relation<? extends O> rel, DistanceQuery<O> dq, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();

    int noisecount = 0;

    /* theta is the sum, w the number of within group distances */
    double theta = 0;
    int w = 0;
    TDoubleList pairDists = new TDoubleArrayList();

    for(int i = 0; i < clusters.size(); i++) {
      Cluster<?> cluster = clusters.get(i);
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseOption){
        case IGNORE_NOISE:
        case IGNORE_NOISE_WITH_PENALTY:
          noisecount += cluster.size();
          continue;
        case TREAT_NOISE_AS_SINGLETONS:
          continue;
        case MERGE_NOISE:
          break; // Treat like a cluster
        }
      }
      for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
        O obj = rel.get(it1);
        for(int j = i; j < clusters.size(); j++) {
          Cluster<?> ocluster = clusters.get(j);
          if(ocluster.size() <= 1 || ocluster.isNoise()) {
            switch(noiseOption){
            case IGNORE_NOISE:
            case IGNORE_NOISE_WITH_PENALTY:
              continue;
            case TREAT_NOISE_AS_SINGLETONS:
            case MERGE_NOISE:
              break; // Treat like a cluster
            }
          }
          for(DBIDIter it2 = ocluster.getIDs().iter(); it2.valid(); it2.advance()) {
            if(DBIDUtil.equal(it1, it2)) {
              continue;
            }
            double dist = dq.distance(obj, rel.get(it2));
            pairDists.add(dist);
            if(ocluster == cluster) {
              theta += dist;
              w++;
            }
          }
        }
      }
    }

    // Simulate best and worst cases:
    pairDists.sort();
    double min = 0, max = 0;
    for(int i = 0, j = pairDists.size() - 1; i < w; i++, j--) {
      min += pairDists.get(i);
      max += pairDists.get(j);
    }

    double cIndex = (max > min) ? (theta - min) / (max - min) : 0.;

    if(noiseOption == NoiseHandling.IGNORE_NOISE_WITH_PENALTY && noisecount > 0) {
      double penalty = (rel.size() - noisecount) / (double) rel.size();
      cIndex = penalty * cIndex;
    }

    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".c-index.noise-handling", noiseOption.toString()));
      LOG.statistics(new LongStatistic(key + ".c-index.noise", noisecount));
      LOG.statistics(new DoubleStatistic(key + ".c-index", cIndex));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based Evaluation");
    g.addMeasure("C-Index", cIndex, 0., 1., 0., true);
    db.getHierarchy().resultChanged(ev);
    return cIndex;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<O> rel = db.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> dq = db.getDistanceQuery(rel, distance);

    for(Clustering<?> c : crs) {
      evaluateClustering(db, rel, dq, c);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Stephan Baier
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("c-index.distance", "Distance function to use for computing the c-index.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_ID = new OptionID("c-index.noisehandling", "option, how noise should be treated.");

    /**
     * Distance function to use.
     */
    private DistanceFunction<? super O> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseOption = NoiseHandling.IGNORE_NOISE_WITH_PENALTY;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }
    }

    @Override
    protected EvaluateCIndex<O> makeInstance() {
      return new EvaluateCIndex<>(distance, noiseOption);
    }
  }

}
