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
package elki.evaluation.clustering;

import java.util.Collection;
import java.util.List;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.trivial.ByLabelOrAllInOneClustering;
import elki.clustering.trivial.ReferenceClustering;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.database.Database;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDList;
import elki.evaluation.Evaluator;
import elki.evaluation.scores.ScoreEvaluation;
import elki.evaluation.scores.adapter.DistanceResultAdapter;
import elki.logging.Logging;
import elki.math.MeanVariance;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
    return eval.evaluate(new DistanceResultAdapter(DBIDUtil.ensureSet(clus.getIDs()), ranking.iter(), ranking.size()));
  }

  @Override
  public void processNewResult(Object newResult) {
    // We may just have added this result.
    if(newResult instanceof Clustering && isReferenceResult((Clustering<?>) newResult)) {
      return;
    }
    Database db = ResultUtil.findDatabase(newResult);
    List<Clustering<?>> crs = Clustering.getClusteringResults(newResult);
    if(crs == null || crs.isEmpty()) {
      return;
    }
    // Compute the reference clustering
    Clustering<?> refc = null;
    // Try to find an existing reference clustering (globally)
    {
      Collection<Clustering<?>> cs = ResultUtil.filterResults(db, Clustering.class);
      for(Clustering<?> test : cs) {
        if(isReferenceResult(test)) {
          refc = test;
          break;
        }
      }
    }
    // Try to find an existing reference clustering (locally)
    if(refc == null) {
      Collection<Clustering<?>> cs = ResultUtil.filterResults(newResult, Clustering.class);
      for(Clustering<?> test : cs) {
        if(isReferenceResult(test)) {
          refc = test;
          break;
        }
      }
    }
    if(refc == null) {
      LOG.debug("Generating a new reference clustering.");
      Object refres = referencealg.autorun(db);
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
      LOG.debug("Using existing clustering: " + Metadata.of(refc).getLongName());
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
    ClusterContingencyTable contmat = new ClusterContingencyTable(selfPairing, noiseSpecialHandling, refc, c);

    ScoreResult sr = new ScoreResult(contmat);
    sr.addHeader(Metadata.of(c).getLongName());
    Metadata.hierarchyOf(c).addChild(sr);
  }

  /**
   * Test if a clustering result is a valid reference result.
   *
   * @param t Clustering to test.
   * @return {@code true} if it is considered to be a reference result.
   */
  private boolean isReferenceResult(Clustering<?> t) {
    return t instanceof ReferenceClustering;
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
      super();
      Metadata.of(this).setLongName("Clustering Evaluation");
      this.contmat = contmat;

      PairCounting paircount = contmat.getPaircount();
      newGroup("Pair counting") //
          .addMeasure("Jaccard", paircount.jaccard(), 0, 1, false) //
          .addMeasure("F1-Measure", paircount.f1Measure(), 0, 1, false) //
          .addMeasure("Precision", paircount.precision(), 0, 1, false) //
          .addMeasure("Recall", paircount.recall(), 0, 1, false) //
          .addMeasure("Rand", paircount.randIndex(), 0, 1, false) //
          .addMeasure("ARI", paircount.adjustedRandIndex(), 0, 1, false) //
          .addMeasure("Fowlkes-Mallows", paircount.fowlkesMallows(), 0, 1, false);

      Entropy entropy = contmat.getEntropy();
      MeasurementGroup g = newGroup("Entropy based") //
          .addMeasure("MI", entropy.mutualInformation(), 0, entropy.upperBoundMI(), false) //
          .addMeasure("VI", entropy.variationOfInformation(), 0, entropy.upperBoundVI(), true) //
          .addMeasure("Homogeneity", entropy.mutualInformation() / entropy.entropyFirst(), 0, 1, false) //
          .addMeasure("Completeness", entropy.mutualInformation() / entropy.entropySecond(), 0, 1, false) //
          .addMeasure("Arithmetic NMI", entropy.arithmeticNMI(), 0, 1, false) //
          .addMeasure("Geometric NMI", entropy.geometricNMI(), 0, 1, false) //
          .addMeasure("Joint NMI", entropy.jointNMI(), 0, 1, false) //
          .addMeasure("NVI", entropy.normalizedVariationOfInformation(), 0, 1, true) //
          .addMeasure("NID", entropy.normalizedInformationDistance(), 0, 1, true);
      // For large data sets, we do not compute EMI/AMI values.
      if(entropy.expectedMutualInformation() > 0) {
        g.addMeasure("Arithmetic AMI", entropy.adjustedArithmeticMI(), 0, 1, false) //
            .addMeasure("Geometric AMI", entropy.adjustedGeometricMI(), 0, 1, false) //
            .addMeasure("Joint AMI", entropy.adjustedJointMI(), 0, 1, false); //
      }

      BCubed bcubed = contmat.getBCubed();
      newGroup("B3") //
          .addMeasure("F1-Measure", bcubed.f1Measure(), 0, 1, false) //
          .addMeasure("Precision", bcubed.precision(), 0, 1, false) //
          .addMeasure("Recall", bcubed.recall(), 0, 1, false);

      MaximumMatchingAccuracy kmwacc = contmat.getMaximumMatchingAccuracy();
      SetMatchingPurity setm = contmat.getSetMatchingPurity();
      PairSetsIndex psi = contmat.getPairSetsIndex();
      newGroup("Set Matching") //
          .addMeasure("Maximum Accuracy", kmwacc.getAccuracy(), 0, 1, false) //
          .addMeasure("Purity", setm.purity(), 0, 1, false) //
          .addMeasure("Inverse Purity", setm.inversePurity(), 0, 1, false) //
          .addMeasure("F1-Measure", setm.f1Measure(), 0, 1, false) //
          .addMeasure("Pair Sets Index", psi.psi, 0, 1, false);

      EditDistance edit = contmat.getEdit();
      newGroup("Edit Distance") //
          .addMeasure("F1-Measure", edit.f1Measure(), 0, 1, false) //
          .addMeasure("Precision", edit.editDistanceFirst(), 0, 1, false) //
          .addMeasure("Recall", edit.editDistanceSecond(), 0, 1, false);

      MeanVariance gini = contmat.averageSymmetricGini();
      MeanVariance agini = contmat.adjustedSymmetricGini();
      newGroup("Gini") //
          .addMeasure("Mean", gini.getMean(), 0, 1, false) //
          .addMeasure("Adjusted Mean", agini.getMean(), 0, 1, false);
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
  public static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new ObjectParameter<ClusteringAlgorithm<?>>(REFERENCE_ID, ClusteringAlgorithm.class, ByLabelOrAllInOneClustering.class) //
          .grab(config, x -> referencealg = x);
      new Flag(NOISE_ID).grab(config, x -> noiseSpecialHandling = x);
      new Flag(SELFPAIR_ID).grab(config, x -> selfPairing = x);
    }

    @Override
    public EvaluateClustering make() {
      return new EvaluateClustering(referencealg, noiseSpecialHandling, !selfPairing);
    }
  }
}
