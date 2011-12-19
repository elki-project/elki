package de.lmu.ifi.dbs.elki.evaluation.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.BasicResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Evaluate a clustering result by comparing it to an existing cluster label.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses ClusterContingencyTable
 * @apiviz.has EvaluateClustering.ScoreResult oneway - - «create»
 */
public class EvaluateClustering implements Evaluator {
  /**
   * Logger for debug output.
   */
  protected static final Logging logger = Logging.getLogger(EvaluateClustering.class);

  /**
   * Parameter to obtain the reference clustering. Defaults to a flat label
   * clustering.
   */
  public static final OptionID REFERENCE_ID = OptionID.getOrCreateOptionID("paircounting.reference", "Reference clustering to compare with. Defaults to a by-label clustering.");

  /**
   * Parameter flag for special noise handling.
   */
  public static final OptionID NOISE_ID = OptionID.getOrCreateOptionID("paircounting.noisespecial", "Use special handling for noise clusters.");

  /**
   * Parameter flag to disable self-pairing
   */
  public static final OptionID NOSELFPAIR_ID = OptionID.getOrCreateOptionID("paircounting.noselfpair", "Disable self-pairing for cluster comparison.");

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

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Database db = ResultUtil.findDatabase(baseResult);
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs == null || crs.size() < 1) {
      return;
    }
    // Compute the reference clustering
    Result refres = referencealg.run(db);
    List<Clustering<?>> refcrs = ResultUtil.getClusteringResults(refres);
    if(refcrs.size() == 0) {
      logger.warning("Reference algorithm did not return a clustering result!");
      return;
    }
    if(refcrs.size() > 1) {
      logger.warning("Reference algorithm returned more than one result!");
    }
    Clustering<?> refc = refcrs.get(0);
    for(Clustering<?> c : crs) {
      ClusterContingencyTable contmat = new ClusterContingencyTable(selfPairing, noiseSpecialHandling);
      contmat.process(c, refc);

      db.getHierarchy().add(c, new ScoreResult(contmat));
    }
  }

  /**
   * Result object for outlier score judgements.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.composedOf ClusterContingencyTable
   */
  public static class ScoreResult extends BasicResult implements TextWriteable {
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
    public void writeToText(TextWriterStream out, String label) {
      out.commentPrint("Pair-F1, ");
      out.commentPrint("Pair-Precision, ");
      out.commentPrint("Pair-Recall, ");
      out.commentPrint("Pair-Rand, ");
      out.commentPrint("Pair-AdjustedRand, ");
      out.commentPrint("Pair-FowlkesMallows, ");
      out.commentPrint("Pair-Jaccard, ");
      out.commentPrint("Pair-Mirkin, ");
      out.commentPrint("Entropy-VI, ");
      out.commentPrint("Entropy-NormalizedVI, ");
      out.commentPrint("Entropy-F1, ");
      out.commentPrint("Edit-F1, ");
      out.commentPrint("SM-InvPurity, ");
      out.commentPrint("SM-Purity, ");
      out.commentPrint("SM-F1, ");
      out.commentPrint("BCubed-Precision, ");
      out.commentPrint("BCubed-Recall, ");
      out.commentPrint("BCubed-F1");
      out.flush();
      out.inlinePrint(contmat.getPaircount().f1Measure());
      out.inlinePrint(contmat.getPaircount().precision());
      out.inlinePrint(contmat.getPaircount().recall());
      out.inlinePrint(contmat.getPaircount().randIndex());
      out.inlinePrint(contmat.getPaircount().adjustedRandIndex());
      out.inlinePrint(contmat.getPaircount().fowlkesMallows());
      out.inlinePrint(contmat.getPaircount().jaccard());
      out.inlinePrint(contmat.getPaircount().mirkin());
      out.inlinePrint(contmat.getEntropy().variationOfInformation());
      out.inlinePrint(contmat.getEntropy().normalizedVariationOfInformation());
      out.inlinePrint(contmat.getEdit().f1Measure());
      out.inlinePrint(contmat.getSetMatching().inversePurity());
      out.inlinePrint(contmat.getSetMatching().purity());
      out.inlinePrint(contmat.getSetMatching().f1Measure());
      out.inlinePrint(contmat.getBCubed().precision());
      out.inlinePrint(contmat.getBCubed().recall());
      out.inlinePrint(contmat.getBCubed().f1Measure());
      out.flush();
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected ClusteringAlgorithm<?> referencealg = null;

    protected boolean noiseSpecialHandling = false;

    protected boolean noSelfPairing = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<ClusteringAlgorithm<?>> referencealgP = new ObjectParameter<ClusteringAlgorithm<?>>(REFERENCE_ID, ClusteringAlgorithm.class, ByLabelClustering.class);
      if(config.grab(referencealgP)) {
        referencealg = referencealgP.instantiateClass(config);
      }

      Flag noiseSpecialHandlingF = new Flag(NOISE_ID);
      if(config.grab(noiseSpecialHandlingF)) {
        noiseSpecialHandling = noiseSpecialHandlingF.getValue();
      }

      Flag noSelfPairingF = new Flag(NOSELFPAIR_ID);
      if(config.grab(noSelfPairingF)) {
        noSelfPairing = noSelfPairingF.getValue();
      }
    }

    @Override
    protected EvaluateClustering makeInstance() {
      return new EvaluateClustering(referencealg, noiseSpecialHandling, !noSelfPairing);
    }
  }
}