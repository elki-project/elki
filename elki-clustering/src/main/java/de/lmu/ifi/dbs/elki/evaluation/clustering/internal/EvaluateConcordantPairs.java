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

import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import net.jafama.FastMath;

/**
 * Compute the Gamma Criterion of a data set.
 * <p>
 * References:
 * <p>
 * F. B. Baker, L. J. Hubert<br>
 * Measuring the Power of Hierarchical Cluster Analysis<br>
 * Journal of the American Statistical Association, 70(349)
 * <p>
 * Tau measures:
 * <p>
 * F. J. Rohlf<br>
 * Methods of comparing classifications<br>
 * Annual Review of Ecology and Systematics
 * <p>
 * The runtime complexity of this measure is O(n*n*log(n)).
 *
 * @author Stephan Baier
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - analyzes - Clustering
 * @composed - - - NoiseHandling
 */
@Reference(authors = "F. B. Baker, L. J. Hubert", //
    title = "Measuring the Power of Hierarchical Cluster Analysis", //
    booktitle = "Journal of the American Statistical Association, 70(349)", //
    url = "https://doi.org/10.1080/01621459.1975.10480256", //
    bibkey = "doi:10.1080/01621459.1975.10480256")
public class EvaluateConcordantPairs<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateConcordantPairs.class);

  /**
   * Option for noise handling.
   */
  private NoiseHandling noiseHandling;

  /**
   * Distance function to use.
   */
  private PrimitiveDistanceFunction<? super NumberVector> distanceFunction;

  /**
   * Key for logging statistics.
   */
  private String key = EvaluateConcordantPairs.class.getName();

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param noiseHandling Control noise handling
   */
  public EvaluateConcordantPairs(PrimitiveDistanceFunction<? super NumberVector> distance, NoiseHandling noiseHandling) {
    super();
    this.distanceFunction = distance;
    this.noiseHandling = noiseHandling;
  }

  /**
   * Evaluate a single clustering.
   *
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   * @return Gamma index
   */
  public double evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();

    int ignorednoise = 0, withinPairs = 0;
    for(Cluster<?> cluster : clusters) {
      if((cluster.size() <= 1 || cluster.isNoise())) {
        switch(noiseHandling){
        case IGNORE_NOISE:
          ignorednoise += cluster.size();
          continue;
        case TREAT_NOISE_AS_SINGLETONS:
          continue; // No concordant distances.
        case MERGE_NOISE:
          break; // Treat like a cluster below.
        }
      }
      withinPairs += (cluster.size() * (cluster.size() - 1)) >>> 1;
      if(withinPairs < 0) {
        throw new AbortException("Integer overflow - clusters too large to compute pairwise distances.");
      }
    }
    // Materialize within-cluster distances (sorted):
    double[] withinDistances = computeWithinDistances(rel, clusters, withinPairs);
    int[] withinTies = new int[withinDistances.length];
    // Count ties within
    countTies(withinDistances, withinTies);

    long concordantPairs = 0, discordantPairs = 0, betweenPairs = 0;

    // Step two, compute discordant distances:
    for(int i = 0; i < clusters.size(); i++) {
      Cluster<?> ocluster1 = clusters.get(i);
      if((ocluster1.size() <= 1 || ocluster1.isNoise()) //
          && noiseHandling.equals(NoiseHandling.IGNORE_NOISE)) {
        continue;
      }
      for(int j = i + 1; j < clusters.size(); j++) {
        Cluster<?> ocluster2 = clusters.get(j);
        if((ocluster2.size() <= 1 || ocluster2.isNoise()) //
            && noiseHandling.equals(NoiseHandling.IGNORE_NOISE)) {
          continue;
        }
        betweenPairs += ocluster1.size() * (long) ocluster2.size();
        for(DBIDIter oit1 = ocluster1.getIDs().iter(); oit1.valid(); oit1.advance()) {
          NumberVector obj = rel.get(oit1);
          for(DBIDIter oit2 = ocluster2.getIDs().iter(); oit2.valid(); oit2.advance()) {
            double dist = distanceFunction.distance(obj, rel.get(oit2));
            int p = Arrays.binarySearch(withinDistances, dist);
            if(p >= 0) { // Tied distances:
              while(p > 0 && withinDistances[p - 1] >= dist) {
                --p;
              }
              concordantPairs += p;
              discordantPairs += withinDistances.length - p - withinTies[p];
              continue;
            }
            p = -p - 1;
            concordantPairs += p;
            discordantPairs += withinDistances.length - p;
          }
        }
      }
    }

    // Total number of pairs possible:
    final long t = ((rel.size() - ignorednoise) * (long) (rel.size() - ignorednoise - 1)) >>> 1;
    final long tt = (t * (t - 1)) >>> 1;

    double gamma = (concordantPairs - discordantPairs) / (double) (concordantPairs + discordantPairs);
    double tau = computeTau(concordantPairs, discordantPairs, tt, withinDistances.length, betweenPairs);

    // Avoid NaN when everything is in a single cluster:
    gamma = gamma > 0. ? gamma : 0.;
    tau = tau > 0. ? tau : 0.;

    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(key + ".noise-handling", noiseHandling.toString()));
      if(ignorednoise > 0) {
        LOG.statistics(new LongStatistic(key + ".ignored", ignorednoise));
      }
      LOG.statistics(new DoubleStatistic(key + ".gamma", gamma));
      LOG.statistics(new DoubleStatistic(key + ".tau", tau));
    }

    EvaluationResult ev = EvaluationResult.findOrCreate(db.getHierarchy(), c, "Internal Clustering Evaluation", "internal evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Concordance-based Evaluation");
    g.addMeasure("Gamma", gamma, -1., 1., 0., false);
    g.addMeasure("Tau", tau, -1., +1., 0., false);
    db.getHierarchy().resultChanged(ev);
    return gamma;
  }

  /**
   * Count (and annotate) the number of tied values.
   *
   * @param withinDistances Distances array
   * @param withinTies Output array of tie counts.
   * @return Number of tied values.
   */
  protected int countTies(double[] withinDistances, int[] withinTies) {
    int wties = 0, running = 1;
    for(int i = 1; i <= withinDistances.length; ++i) {
      if(i == withinDistances.length || withinDistances[i - 1] != withinDistances[i]) {
        for(int j = i - running; j < i; j++) {
          withinTies[j] = running;
        }
        wties += running - 1;
        running = 1;
      }
      else {
        running++;
      }
    }
    return wties;
  }

  protected double[] computeWithinDistances(Relation<? extends NumberVector> rel, List<? extends Cluster<?>> clusters, int withinPairs) {
    double[] concordant = new double[withinPairs];
    int i = 0;
    for(Cluster<?> cluster : clusters) {
      if(cluster.size() <= 1 || cluster.isNoise()) {
        switch(noiseHandling){
        case IGNORE_NOISE:
          continue;
        case TREAT_NOISE_AS_SINGLETONS:
          continue; // No concordant distances.
        case MERGE_NOISE:
          break; // Treat like a cluster below.
        }
      }

      for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
        NumberVector obj = rel.get(it1);
        for(DBIDIter it2 = cluster.getIDs().iter(); it2.valid(); it2.advance()) {
          if(DBIDUtil.compare(it1, it2) <= 0) {
            continue;
          }
          concordant[i++] = distanceFunction.distance(obj, rel.get(it2));
        }
      }
    }
    assert (concordant.length == i);
    Arrays.sort(concordant);
    return concordant;
  }

  /**
   * Compute the Tau correlation measure
   *
   * @param c Concordant pairs
   * @param d Discordant pairs
   * @param m Total number of pairs
   * @param wd Number of within distances
   * @param bd Number of between distances
   * @return Gamma plus statistic
   */
  @Reference(authors = "F. J. Rohlf", title = "Methods of comparing classifications", //
      booktitle = "Annual Review of Ecology and Systematics", //
      url = "https://doi.org/10.1146/annurev.es.05.110174.000533", //
      bibkey = "doi:10.1146/annurev.es.05.110174.000533")
  public double computeTau(long c, long d, double m, long wd, long bd) {
    double tie = (wd * (wd - 1) + bd * (bd - 1)) >>> 1;
    return (c - d) / FastMath.sqrt((m - tie) * m);
    // return (4. * c - m) / m;
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
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("concordant.distance", "Distance function to use for measuring concordant and discordant pairs.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_ID = new OptionID("concordant-pairs.noisehandling", "Control how noise should be treated.");

    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<NumberVector> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseHandling noiseHandling;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistanceFunction<NumberVector>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseHandling> noiseP = new EnumParameter<NoiseHandling>(NOISE_ID, NoiseHandling.class, NoiseHandling.TREAT_NOISE_AS_SINGLETONS);
      if(config.grab(noiseP)) {
        noiseHandling = noiseP.getValue();
      }
    }

    @Override
    protected EvaluateConcordantPairs<O> makeInstance() {
      return new EvaluateConcordantPairs<>(distance, noiseHandling);
    }
  }
}
