package de.lmu.ifi.dbs.elki.evaluation.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PWCPrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.evaluation.AutomaticEvaluation;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.APIViolationException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 *
 * Class to wrap evaluation for
 * a clustering of clusterings.
 *
 * @author Alexander Koos
 *
 */
@SuppressWarnings("serial")
public class PWCClusteringEvaluation<F extends PWCPrimitiveSimilarityFunction> extends AutomaticEvaluation {

  public static class Parameterizer extends AbstractParameterizer {

    private PWCPrimitiveSimilarityFunction simFunc;

    private double alpha;

    public static final OptionID SIMILARITY_FUNCTION_ID = new OptionID("mce.similarityFunction","Used for detection of the representatives. Recommended to use the same as for metaclustering.");

    public static final OptionID ALPHA_ID = new OptionID("mce.alpha","Used to compute the confidence probability.");

    @Override
    protected PWCClusteringEvaluation<PWCPrimitiveSimilarityFunction> makeInstance() {
      return new PWCClusteringEvaluation<>(this.simFunc, this.alpha);
    }

    @Override
    protected void makeOptions(final Parameterization config) {
      super.makeOptions(config);
      final ClassParameter<PWCPrimitiveSimilarityFunction> pSimFunc = new ClassParameter<>(Parameterizer.SIMILARITY_FUNCTION_ID, PWCPrimitiveSimilarityFunction.class);
      if(config.grab(pSimFunc)) {
        this.simFunc = pSimFunc.instantiateClass(config);
      }
      final DoubleParameter palpha = new DoubleParameter(Parameterizer.ALPHA_ID, 0.05);
      if(config.grab(palpha)) {
        this.alpha = palpha.doubleValue();
      }
    }

  }

  public static class PWCScoreResult extends EvaluationResult {

    private static final String DEFAULT_ALPHA = "0.05";

    public PWCScoreResult(final double alpha, final List<Clustering<Model>> crs, final Clustering<Model> c, final double tau, final PWCPrimitiveSimilarityFunction f) {
      super("Possible-Worlds-Clustering Score", "PWC Score");

      final double eprob = this.probabilityEstimator(c, tau, crs, f);
      final double z = this.getAlphaQuantil(alpha);
      final double cprob = eprob - z * Math.sqrt(( eprob * (1 - eprob) ) / crs.size());

      final MeasurementGroup g = this.newGroup("PWC Measures");
      g.addMeasure("Tau-Measure", tau, 0, 1, true);
      g.addMeasure("Confidence-Probability", cprob, 0, 1, false);
    }

    private double getAlphaQuantil(final double alpha) {
      return PWCClusteringEvaluation.alphaMap.get("" + alpha) != null ? PWCClusteringEvaluation.alphaMap.get("" + alpha) : PWCClusteringEvaluation.alphaMap.get(PWCScoreResult.DEFAULT_ALPHA);
    }

    private double probabilityEstimator(final Clustering<Model> c, final double tau, final List<Clustering<Model>> crs, final PWCPrimitiveSimilarityFunction f) {
      double x = 0;
      for(final Clustering<Model> c1 : crs) {
        x += ( 1 - f.getMetricScale( (new ClusterContingencyTable(true, true) {{ this.process(c1, c); };}).getPaircount() ) ) <= tau ? 1 : 0;
      }
      return x / crs.size();
    }

  }

  public final static HashMap<String, Double> alphaMap = new HashMap<String, Double>(){{
    this.put("0.05", 1.65);
    this.put("0.01", 2.33);
    this.put("0.005", 2.58);
  }};;

  private final F simFunc;

  private final double alpha;

  public PWCClusteringEvaluation(final F simFunc, final double alpha) {
    this.simFunc = simFunc;
    this.alpha = alpha;
    if(alpha < 0 || alpha > 1) {
      throw new APIViolationException("Confidence level alpha has to be in [0,1].");
    }
  }

  private double clusteringDistance(final Clustering<Model> c1, final Clustering<Model> c2) {
    return this.simFunc.similarity(c1, c2);
  }

  @SuppressWarnings("unchecked")
  private void evaluateMetaClustering(final Result newResult) {
    final Database db  = ResultUtil.findDatabase(newResult);
    final List<Clustering<Model>> clusterings0 = ResultUtil.filterResults(newResult, Clustering.class);
    final List<Clustering<Model>> clusterings1 = new ArrayList<Clustering<Model>>();
    final List<Clustering<Model>> refClusterings = new ArrayList<Clustering<Model>>();
    final List<Pair<Clustering<Model>,Double>> bestClusterings = new ArrayList<Pair<Clustering<Model>,Double>>();
    for(final Clustering<Model> c : clusterings0) {
      final SingleObjectBundle bundle = db.getBundle(c.getAllClusters().get(0).getIDs().iter());
      for(int i = 0; i < bundle.metaLength(); i++) {
        if(bundle.data(i) != null && bundle.data(i).getClass().equals(Clustering.class)) {
          clusterings1.add(c);
        }
      }
    }
    for(final Clustering<Model> cs : clusterings1) {
      final List<Cluster<Model>> clusterings2 = new ArrayList<Cluster<Model>>();
      clusterings2.addAll(cs.getAllClusters());

      for(final Cluster<Model> cs2 : clusterings2) {
        final List<Clustering<Model>> clusterings = new ArrayList<Clustering<Model>>();
        final DBIDs ids = cs2.getIDs();
        for(final DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
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
        final double[] similaritySum = new double[clusterings.size()];
        final double[] tau = new double[clusterings.size()];
        {
          int i = 0;
          for(final Clustering<Model> c : clusterings) {
            if(i == 0) {
              i++;
              continue;
            }

            double currentSumOfDists = 0;
            for(int j = 0; j < i; j++) {
              double res = this.clusteringDistance(c, clusterings.get(j));
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
        //        final Clustering<Model> bestCombinationClustering = new  Clustering<Model>("","");
        //        final HashSetModifiableDBIDs noiseIDs = DBIDUtil.newHashSet();
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
        bestClusterings.add(new Pair<Clustering<Model>, Double>(clusterings.get(bestIndex), tau[bestIndex]));
      }
    }
    for(final Pair<Clustering<Model>, Double> c : bestClusterings) {
      final PWCScoreResult psr = new PWCScoreResult(this.alpha, refClusterings, c.getFirst(), c.getSecond(), this.simFunc);
      db.getHierarchy().add(c.getFirst(), psr);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void processNewResult(final HierarchicalResult baseResult, final Result newResult) {
    if(newResult.getClass().equals(EvaluateClustering.ScoreResult.class) || ResultUtil.findDatabase(newResult) == null) {
      return;
    }
    final Database db = ResultUtil.findDatabase(baseResult);
    Relation<Clustering<Model>> rel = null;
    for(final Relation<?> r : db.getRelations()) {
      if(r.getDataTypeInformation().isAssignableFromType(new SimpleTypeInformation<>(Clustering.class))) {
        rel = (Relation<Clustering<Model>>) r;
        break;
      }
    }
    final List<Clustering<Model>> clusterings = new ArrayList<Clustering<Model>>();
    for(final DBIDIter iter = rel.getDBIDs().iter(); iter.valid(); iter.advance()) {
      clusterings.add(rel.get(iter));
    }
    for(final Clustering<Model> c : clusterings) {
      this.autoEvaluateClusterings(baseResult, c);
    }

    this.evaluateMetaClustering(newResult);
  }
}
