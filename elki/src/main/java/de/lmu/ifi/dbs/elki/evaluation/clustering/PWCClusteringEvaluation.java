package de.lmu.ifi.dbs.elki.evaluation.clustering;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.cluster.ClusteringDistanceSimilarityFunction;
import de.lmu.ifi.dbs.elki.evaluation.AutomaticEvaluation;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 *
 * Class to wrap evaluation for a clustering of clusterings.
 *
 * @author Alexander Koos
 *
 */
public class PWCClusteringEvaluation<F extends ClusteringDistanceSimilarityFunction> extends AutomaticEvaluation {
  // Table values from literature.
  public final static double[] alphaMap = new double[] { //
      0.05, 1.65, //
      0.01, 2.33, //
      0.005, 2.58, //
  };

  private final F simFunc;

  private final double z;

  public PWCClusteringEvaluation(F simFunc, double alpha) {
    this.simFunc = simFunc;
    this.z = getAlphaQuantile(alpha);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    Database db = ResultUtil.findDatabase(hier);
    // FIXME: only process *new* results.
    for(Relation<?> r : db.getRelations()) {
      if(r.getDataTypeInformation().isAssignableFromType(new SimpleTypeInformation<>(Clustering.class))) {
        Relation<Clustering<Model>> rel = (Relation<Clustering<Model>>) r;
        List<Clustering<Model>> clusterings = new ArrayList<Clustering<Model>>();
        for(DBIDIter iter = rel.iterDBIDs(); iter.valid(); iter.advance()) {
          clusterings.add(rel.get(iter));
        }
        for(Clustering<Model> c : clusterings) {
          autoEvaluateClusterings(hier, c);
        }

        evaluateMetaClustering(hier, newResult);
      }
    }
  }

  private static double getAlphaQuantile(final double alpha) {
    for(int i = 0; i < alphaMap.length; i += 2) {
      if(alphaMap[i] == alpha) {
        return alphaMap[i + 1];
      }
    }
    throw new AbortException("Currently, only alpha 0.05, 0.01, 0.005 are available.");
  }

  @SuppressWarnings("unchecked")
  private void evaluateMetaClustering(final Result newResult) {
    final Database db = ResultUtil.findDatabase(newResult);
    final List<Clustering<Model>> clusterings0 = ResultUtil.filterResults(newResult, Clustering.class);
    final List<Clustering<Model>> clusterings1 = new ArrayList<Clustering<Model>>();
    final List<Clustering<Model>> refClusterings = new ArrayList<Clustering<Model>>();
    final List<Pair<Clustering<Model>, Pair<Double, Double>>> bestClusterings = new ArrayList<Pair<Clustering<Model>, Pair<Double, Double>>>();
    final List<Clustering<Model>> clusteringsAll = new ArrayList<Clustering<Model>>();
    clusterings: for(final Clustering<Model> c : clusterings0) {
      Iterator<Cluster<Model>> clusters = c.getAllClusters().iterator();
      Cluster<Model> cluster = null;
      while(cluster == null || cluster.size() == 0) {
        if(!clusters.hasNext()) {
          continue clusterings;
        }
        cluster = clusters.next();
      }
      final SingleObjectBundle bundle = db.getBundle(cluster.getIDs().iter());
      for(int i = 0; i < bundle.metaLength(); i++) {
        if(bundle.data(i) != null && bundle.data(i).getClass().equals(Clustering.class)) {
          clusterings1.add(c);
        }
      }
    }
    for(Clustering<Model> cs : clusterings1) {
      for(Cluster<Model> cs2 : cs.getAllClusters()) {
        for(DBIDIter iter = cs2.getIDs().iter(); iter.valid(); iter.advance()) {
          for(int i = 0; i < db.getBundle(iter).metaLength(); i++) {
            Object b = db.getBundle(iter).data(i);
            if(b != null && b.getClass().equals(Clustering.class)) {
              clusteringsAll.add((Clustering<Model>) b);
            }
          }
        }
      }
    }
    for(Clustering<Model> cs : clusterings1) {
      List<Cluster<Model>> clusterings2 = new ArrayList<Cluster<Model>>();
      clusterings2.addAll(cs.getAllClusters());

      for(Cluster<Model> cs2 : clusterings2) {
        List<Clustering<Model>> clusterings = new ArrayList<Clustering<Model>>();
        for(DBIDIter iter = cs2.getIDs().iter(); iter.valid(); iter.advance()) {
          for(int i = 0; i < db.getBundle(iter).metaLength(); i++) {
            final Object b = db.getBundle(iter).data(i);
            if(b != null && b.getClass().equals(Clustering.class)) {
              clusterings.add((Clustering<Model>) b);
            }
          }
        }
        if(clusterings.size() == 0) {
          continue;
        }
        refClusterings.addAll(clusterings);
        double[] similaritySum = new double[clusterings.size()];
        double[] tau = new double[clusterings.size()];
        {
          int i = 0;
          for(final Clustering<Model> c : clusterings) {
            if(i == 0) {
              i++;
              continue;
            }
            // TODO: this should be optimized in some way
            {

            }
            double currentSumOfDists = 0;
            for(int j = 0; j < i; j++) {
              double res = simFunc.similarity(c, clusterings.get(j));
              similaritySum[j] += res;
              currentSumOfDists += res;
              res = 1 - res;
              if(res >= tau[i]) {
                tau[i] = res;
              }
              if(res >= tau[j]) {
                tau[j] = res;
              }
            }
            similaritySum[i++] = currentSumOfDists;
          }
        }
        int bestIndex = -1;
        double maxValue = 0;
        // final Clustering<Model> bestCombinationClustering = new
        // Clustering<Model>("","");
        // final HashSetModifiableDBIDs noiseIDs = DBIDUtil.newHashSet();
        for(int i = 0; i < similaritySum.length; i++) {
          // In special cases similaritySum can become 0 and there
          // is only one Clustering, therefore I want to
          // avoid an ArrayOutOfBoundException.
          if(similaritySum[i] >= maxValue) {
            // similaritySum is a sum of ranks for the distances
            // between clusters.
            //
            // The higher the similaritySum, the more representative
            // the clustering is amongst all given clusterings.
            bestIndex = i;
            maxValue = similaritySum[i];
          }
        }
        double tauAll = -1;
        for(final Clustering<Model> c : clusteringsAll) {
          final double res = 1 - simFunc.similarity(c, clusterings.get(bestIndex));
          if(tauAll < res) {
            tauAll = res;
          }
        }
        bestClusterings.add(new Pair<Clustering<Model>, Pair<Double, Double>>(clusterings.get(bestIndex), new Pair<Double, Double>(tauAll, tau[bestIndex])));
      }
    }
    for(Pair<Clustering<Model>, Pair<Double, Double>> c : bestClusterings) {
      PWCScoreResult psr = new PWCScoreResult(z, refClusterings, c.getFirst(), c.getSecond().getFirst(), c.getSecond().getSecond(), simFunc);
      db.getHierarchy().add(c.getFirst(), psr);
    }
  }

  public static class PWCScoreResult extends EvaluationResult {

    public PWCScoreResult(double z, List<Clustering<Model>> crs, Clustering<Model> c, double tauAll, double tau, ClusteringDistanceSimilarityFunction f) {
      super("Possible-Worlds-Clustering Score", "PWC Score");

      final double eprob = probabilityEstimator(c, tau, crs, f);
      final double cprob = Math.abs(eprob - z * Math.sqrt((eprob * (1 - eprob)) / crs.size()));

      final MeasurementGroup g = newGroup("PWC Measures");
      g.addMeasure("Tau-All-Reference-Measure", tauAll, 0, 1, true);
      g.addMeasure("Tau-Measure", tau, 0, 1, true);
      g.addMeasure("Confidence-Probability", cprob, 0, 1, false);
    }

    private double probabilityEstimator(Clustering<Model> c, double tau, List<Clustering<Model>> crs, ClusteringDistanceSimilarityFunction f) {
      final double itau = 1 - tau;
      double x = 0;
      for(Clustering<Model> c1 : crs) {
        x += (f.similarity(c, c1) >= itau) ? 1 : 0;
      }
      return x / crs.size();
    }
  }

  public static class Parameterizer extends AbstractParameterizer {

    private ClusteringDistanceSimilarityFunction simFunc;

    private double alpha;

    public static final OptionID SIMILARITY_FUNCTION_ID = new OptionID("mce.similarityFunction", "Used for detection of the representatives. Recommended to use the same as for metaclustering.");

    public static final OptionID ALPHA_ID = new OptionID("mce.alpha", "Used to compute the confidence probability.");

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      ClassParameter<ClusteringDistanceSimilarityFunction> pSimFunc = new ClassParameter<>(SIMILARITY_FUNCTION_ID, ClusteringDistanceSimilarityFunction.class);
      if(config.grab(pSimFunc)) {
        simFunc = pSimFunc.instantiateClass(config);
      }
      DoubleParameter palpha = new DoubleParameter(ALPHA_ID, 0.05);
      if(config.grab(palpha)) {
        alpha = palpha.doubleValue();
      }
    }

    @Override
    protected PWCClusteringEvaluation<ClusteringDistanceSimilarityFunction> makeInstance() {
      return new PWCClusteringEvaluation<>(this.simFunc, this.alpha);
    }
  }
}
