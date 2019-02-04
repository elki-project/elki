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
package de.lmu.ifi.dbs.elki.evaluation.clustering;

import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelOrAllInOneClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.scores.ScoreEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DBIDsTest;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DistanceResultAdapter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Evaluate a clustering result by comparing it to an existing cluster label.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - evaluates - Clustering
 * @composed - - - ClusterContingencyTable
 * @navhas - create - EvaluateClustering.ScoreResult
 */
@Alias("de.lmu.ifi.dbs.elki.evaluation.paircounting.EvaluatePairCountingFMeasure")
public class EvaluateClustering implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateClustering.class);

  /**
   * Reference algorithm.
   */
  private ClusteringAlgorithm<?> referencealg;

  /**
   * Apply special handling to noise "clusters".
   */
  private boolean noiseSpecialHandling;

  /**
   * Use self-pairing in pair-counting measures
   */
  private boolean selfPairing;

  /**
   * Constructor.
   *
   * @param referencealg Reference clustering
   * @param noiseSpecialHandling Noise handling flag
   * @param selfPairing Self-pairing flag
   */
  public EvaluateClustering(ClusteringAlgorithm<?> referencealg, boolean noiseSpecialHandling, boolean selfPairing) {
    super();
    this.referencealg = referencealg;
    this.noiseSpecialHandling = noiseSpecialHandling;
    this.selfPairing = selfPairing;
  }

  /**
   * Evaluate given a cluster (of positive elements) and a scoring list.
   *
   * @param eval Evaluation method
   * @param clus Cluster object
   * @param ranking Object ranking
   * @return Score
   */
  public static double evaluateRanking(ScoreEvaluation eval, Cluster<?> clus, DoubleDBIDList ranking) {
    return eval.evaluate(new DBIDsTest(DBIDUtil.ensureSet(clus.getIDs())), new DistanceResultAdapter(ranking.iter()));
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    // We may just have added this result.
    if(newResult instanceof Clustering && isReferenceResult((Clustering<?>) newResult)) {
      return;
    }
    Database db = ResultUtil.findDatabase(hier);
    List<Clustering<?>> crs = Clustering.getClusteringResults(newResult);
    if(crs == null || crs.isEmpty()) {
      return;
    }
    // Compute the reference clustering
    Clustering<?> refc = null;
    // Try to find an existing reference clustering (globally)
    {
      Collection<Clustering<?>> cs = ResultUtil.filterResults(hier, db, Clustering.class);
      for(Clustering<?> test : cs) {
        if(isReferenceResult(test)) {
          refc = test;
          break;
        }
      }
    }
    // Try to find an existing reference clustering (locally)
    if(refc == null) {
      Collection<Clustering<?>> cs = ResultUtil.filterResults(hier, newResult, Clustering.class);
      for(Clustering<?> test : cs) {
        if(isReferenceResult(test)) {
          refc = test;
          break;
        }
      }
    }
    if(refc == null) {
      LOG.debug("Generating a new reference clustering.");
      Result refres = referencealg.run(db);
      List<Clustering<?>> refcrs = Clustering.getClusteringResults(refres);
      if(refcrs.isEmpty()) {
        LOG.warning("Reference algorithm did not return a clustering result!");
        return;
      }
      if(refcrs.size() > 1) {
        LOG.warning("Reference algorithm returned more than one result!");
      }
      refc = refcrs.get(0);
    }
    else {
      LOG.debug("Using existing clustering: " + refc.getLongName() + " " + refc.getShortName());
    }
    for(Clustering<?> c : crs) {
      if(c == refc) {
        continue;
      }
      evaluteResult(db, c, refc);
    }
  }

  /**
   * Evaluate a clustering result.
   *
   * @param db Database
   * @param c Clustering
   * @param refc Reference clustering
   */
  protected void evaluteResult(Database db, Clustering<?> c, Clustering<?> refc) {
    ClusterContingencyTable contmat = new ClusterContingencyTable(selfPairing, noiseSpecialHandling);
    contmat.process(refc, c);

    ScoreResult sr = new ScoreResult(contmat);
    sr.addHeader(c.getLongName());
    db.getHierarchy().add(c, sr);
  }

  /**
   * Test if a clustering result is a valid reference result.
   *
   * @param t Clustering to test.
   * @return {@code true} if it is considered to be a reference result.
   */
  private boolean isReferenceResult(Clustering<?> t) {
    // FIXME: don't hard-code strings
    return "bylabel-clustering".equals(t.getShortName()) //
        || "bymodel-clustering".equals(t.getShortName()) //
        || "allinone-clustering".equals(t.getShortName()) //
        || "allinnoise-clustering".equals(t.getShortName());
  }

  /**
   * Result object for outlier score judgements.
   *
   * @author Erich Schubert
   *
   * @composed - - - ClusterContingencyTable
   */
  public static class ScoreResult extends EvaluationResult {
    /**
     * Cluster contingency table
     */
    protected ClusterContingencyTable contmat;

    /**
     * Constructor.
     *
     * @param contmat score result
     */
    public ScoreResult(ClusterContingencyTable contmat) {
      super("Cluster-Evalation", "cluster-evaluation");
      this.contmat = contmat;

      PairCounting paircount = contmat.getPaircount();
      MeasurementGroup g = newGroup("Pair counting measures");
      g.addMeasure("Jaccard", paircount.jaccard(), 0, 1, false);
      g.addMeasure("F1-Measure", paircount.f1Measure(), 0, 1, false);
      g.addMeasure("Precision", paircount.precision(), 0, 1, false);
      g.addMeasure("Recall", paircount.recall(), 0, 1, false);
      g.addMeasure("Rand", paircount.randIndex(), 0, 1, false);
      g.addMeasure("ARI", paircount.adjustedRandIndex(), 0, 1, false);
      g.addMeasure("FowlkesMallows", paircount.fowlkesMallows(), 0, 1, false);

      Entropy entropy = contmat.getEntropy();
      g = newGroup("Entropy based measures");
      g.addMeasure("NMI Joint", entropy.entropyNMIJoint(), 0, 1, false);
      g.addMeasure("NMI Sqrt", entropy.entropyNMISqrt(), 0, 1, false);

      BCubed bcubed = contmat.getBCubed();
      g = newGroup("BCubed-based measures");
      g.addMeasure("F1-Measure", bcubed.f1Measure(), 0, 1, false);
      g.addMeasure("Recall", bcubed.recall(), 0, 1, false);
      g.addMeasure("Precision", bcubed.precision(), 0, 1, false);

      SetMatchingPurity setm = contmat.getSetMatching();
      g = newGroup("Set-Matching-based measures");
      g.addMeasure("F1-Measure", setm.f1Measure(), 0, 1, false);
      g.addMeasure("Purity", setm.purity(), 0, 1, false);
      g.addMeasure("Inverse Purity", setm.inversePurity(), 0, 1, false);

      EditDistance edit = contmat.getEdit();
      g = newGroup("Editing-distance measures");
      g.addMeasure("F1-Measure", edit.f1Measure(), 0, 1, false);
      g.addMeasure("Precision", edit.editDistanceFirst(), 0, 1, false);
      g.addMeasure("Recall", edit.editDistanceSecond(), 0, 1, false);

      MeanVariance gini = contmat.averageSymmetricGini();
      g = newGroup("Gini measures");
      g.addMeasure("Mean +-" + FormatUtil.NF4.format(gini.getCount() > 1. ? gini.getSampleStddev() : 0.), gini.getMean(), 0, 1, false);
    }

    /**
     * Get the contingency table
     *
     * @return the contingency table
     */
    public ClusterContingencyTable getContingencyTable() {
      return contmat;
    }

    @Override
    public boolean visualizeSingleton() {
      return true;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to obtain the reference clustering. Defaults to a flat label
     * clustering.
     */
    public static final OptionID REFERENCE_ID = new OptionID("paircounting.reference", "Reference clustering to compare with. Defaults to a by-label clustering.");

    /**
     * Parameter flag for special noise handling.
     */
    public static final OptionID NOISE_ID = new OptionID("paircounting.noisespecial", "Use special handling for noise clusters.");

    /**
     * Parameter flag to disable self-pairing
     */
    public static final OptionID SELFPAIR_ID = new OptionID("paircounting.selfpair", "Enable self-pairing for cluster comparison.");

    /**
     * Reference algorithm.
     */
    private ClusteringAlgorithm<?> referencealg;

    /**
     * Apply special handling to noise "clusters".
     */
    private boolean noiseSpecialHandling;

    /**
     * Use self-pairing in pair-counting measures
     */
    private boolean selfPairing;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<ClusteringAlgorithm<?>> referencealgP = new ObjectParameter<>(REFERENCE_ID, ClusteringAlgorithm.class, ByLabelOrAllInOneClustering.class);
      if(config.grab(referencealgP)) {
        referencealg = referencealgP.instantiateClass(config);
      }

      Flag noiseSpecialHandlingF = new Flag(NOISE_ID);
      if(config.grab(noiseSpecialHandlingF)) {
        noiseSpecialHandling = noiseSpecialHandlingF.getValue();
      }

      Flag selfPairingF = new Flag(SELFPAIR_ID);
      if(config.grab(selfPairingF)) {
        selfPairing = selfPairingF.getValue();
      }
    }

    @Override
    protected EvaluateClustering makeInstance() {
      return new EvaluateClustering(referencealg, noiseSpecialHandling, !selfPairing);
    }
  }
}
