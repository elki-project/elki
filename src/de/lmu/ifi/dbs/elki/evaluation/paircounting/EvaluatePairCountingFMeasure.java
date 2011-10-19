package de.lmu.ifi.dbs.elki.evaluation.paircounting;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.outlier.JudgeOutlierScores;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
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
 * @apiviz.has PairCountingFMeasure
 * @apiviz.has EvaluatePairCountingFMeasure.ScoreResult oneway - - «create»
 */
public class EvaluatePairCountingFMeasure implements Evaluator {
  /**
   * Logger for debug output.
   */
  protected static final Logging logger = Logging.getLogger(JudgeOutlierScores.class);

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
   * Reference algorithm.
   */
  private ClusteringAlgorithm<?> referencealg;

  /**
   * Apply special handling to noise "clusters".
   */
  private boolean noiseSpecialHandling;

  /**
   * Constructor.
   * 
   * @param referencealg Reference clustering
   * @param noiseSpecialHandling Noise handling flag
   */
  public EvaluatePairCountingFMeasure(ClusteringAlgorithm<?> referencealg, boolean noiseSpecialHandling) {
    super();
    this.referencealg = referencealg;
    this.noiseSpecialHandling = noiseSpecialHandling;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Database db = ResultUtil.findDatabase(baseResult);
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs == null || crs.size() < 1) {
      // logger.warning("No clustering results found - nothing to evaluate!");
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
      int[] countedPairs = PairCountingFMeasure.countPairs(c, refc, noiseSpecialHandling);
      // Use double, since we want double results at the end!
      double sum = countedPairs[0] + countedPairs[1] + countedPairs[2];
      double inboth = countedPairs[0] / sum;
      double infirst = countedPairs[1] / sum;
      double insecond = countedPairs[2] / sum;
      double fmeasure = PairCountingFMeasure.fMeasure(countedPairs[0], countedPairs[1], countedPairs[2], 1.0);
      ArrayList<Vector> s = new ArrayList<Vector>(4);
      s.add(new Vector(new double[] { fmeasure, inboth, infirst, insecond }));
      db.getHierarchy().add(c, new ScoreResult(s));
    }
  }

  /**
   * Result object for outlier score judgements.
   * 
   * @author Erich Schubert
   */
  public static class ScoreResult extends CollectionResult<Vector> {
    /**
     * Constructor.
     * 
     * @param col score result
     */
    public ScoreResult(Collection<Vector> col) {
      super("Pair Counting F-Measure", "pair-fmeasure", col);
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
    protected ClusteringAlgorithm< ?> referencealg = null;

    protected boolean noiseSpecialHandling = false;

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
    }

    @Override
    protected EvaluatePairCountingFMeasure makeInstance() {
      return new EvaluatePairCountingFMeasure(referencealg, noiseSpecialHandling);
    }
  }
}