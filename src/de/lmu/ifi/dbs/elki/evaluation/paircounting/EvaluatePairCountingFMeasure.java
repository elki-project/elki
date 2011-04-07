package de.lmu.ifi.dbs.elki.evaluation.paircounting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.outlier.JudgeOutlierScores;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairSortedGeneratorInterface;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

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
  private Algorithm<?, ?> referencealg;

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
  public EvaluatePairCountingFMeasure(Algorithm<?, ?> referencealg, boolean noiseSpecialHandling) {
    super();
    this.referencealg = referencealg;
    this.noiseSpecialHandling = noiseSpecialHandling;
  }

  @Override
  public void processResult(Database<?> db, Result result, ResultHierarchy hierarchy) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs == null || crs.size() < 1) {
      // logger.warning("No clustering results found - nothing to evaluate!");
      return;
    }
    // Compute the reference clustering
    Result refres = tryReferenceClustering(db);
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
      PairSortedGeneratorInterface first = PairCountingFMeasure.getPairGenerator(c, noiseSpecialHandling, false);
      PairSortedGeneratorInterface second = PairCountingFMeasure.getPairGenerator(refc, noiseSpecialHandling, false);
      Triple<Integer, Integer, Integer> countedPairs = PairCountingFMeasure.countPairs(first, second);
      // Use double, since we want double results at the end!
      double sum = countedPairs.first + countedPairs.second + countedPairs.third;
      double inboth = countedPairs.first / sum;
      double infirst = countedPairs.second / sum;
      double insecond = countedPairs.third / sum;
      double fmeasure = PairCountingFMeasure.fMeasure(countedPairs.first, countedPairs.second, countedPairs.third, 1.0);
      ArrayList<Vector> s = new ArrayList<Vector>(4);
      s.add(new Vector(new double[] { fmeasure, inboth, infirst, insecond }));
      hierarchy.add(c, new ScoreResult(s));
    }
  }

  /**
   * Try to run the reference clustering algorithm.
   * 
   * @param db Database
   * @return clustering result
   */
  private Result tryReferenceClustering(Database<?> db) {
    @SuppressWarnings("unchecked")
    Result refres = ((ClusteringAlgorithm<?, DatabaseObject>) referencealg).run((Database<DatabaseObject>) db);
    return refres;
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<?> normalization) {
    // Nothing to do.
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
    protected Algorithm<?, ?> referencealg = null;

    protected boolean noiseSpecialHandling = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<ClusteringAlgorithm<?, ?>> referencealgP = new ObjectParameter<ClusteringAlgorithm<?, ?>>(REFERENCE_ID, ClusteringAlgorithm.class, ByLabelClustering.class);
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