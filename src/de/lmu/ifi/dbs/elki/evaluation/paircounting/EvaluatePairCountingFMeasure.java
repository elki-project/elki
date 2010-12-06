package de.lmu.ifi.dbs.elki.evaluation.paircounting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ByLabelClustering;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.evaluation.outlier.JudgeOutlierScores;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairSortedGeneratorInterface;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

/**
 * Evaluate a clustering result by comparing it to an existing cluster label.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has PairCountingFMeasure
 * @apiviz.uses EvaluatePairCountingFMeasure.ScoreResult oneway - - «create»
 * 
 * @param <O> Database
 */
public class EvaluatePairCountingFMeasure<O extends DatabaseObject> implements Evaluator<O> {
  /**
   * Logger for debug output.
   */
  protected static final Logging logger = Logging.getLogger(JudgeOutlierScores.class);  

  /**
   * OptionID for {@link #REFERENCE_PARAM}
   */
  public static final OptionID REFERENCE_ID = OptionID.getOrCreateOptionID("paircounting.reference", "Reference clustering to compare with. Defaults to a by-label clustering.");

  /**
   * Parameter to obtain the reference clustering. Defaults to a flat label clustering.
   */
  private final ObjectParameter<Algorithm<O, ?>> REFERENCE_PARAM = new ObjectParameter<Algorithm<O, ?>>(REFERENCE_ID, Algorithm.class, ByLabelClustering.class);
  
  /**
   * Reference algorithm.
   */
  private Algorithm<O, ?> referencealg;
  
  /**
   * Constructor.
   * 
   * @param config Parameters
   */
  public EvaluatePairCountingFMeasure(Parameterization config) {
    super();
    config = config.descend(this);
    if (config.grab(REFERENCE_PARAM)) {
      referencealg = REFERENCE_PARAM.instantiateClass(config);
    }
  }

  @Override
  public void processResult(Database<O> db, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if (crs.size() < 1) {
      logger.warning("No clustering results found - nothing to evaluate!");
      return;
    }
    // Compute the reference clustering
    AnyResult refres = referencealg.run(db);
    List<Clustering<?>> refcrs = ResultUtil.getClusteringResults(refres);
    if (refcrs.size() == 0) {
      logger.warning("Reference algorithm did not return a clustering result!");
      return;
    }
    if (refcrs.size() > 1) {
      logger.warning("Reference algorithm returned more than one result!");
    }
    Clustering<?> refc = refcrs.get(0);
    for (Clustering<?> c : crs) {
      PairSortedGeneratorInterface first = PairCountingFMeasure.getPairGenerator(c);
      PairSortedGeneratorInterface second = PairCountingFMeasure.getPairGenerator(refc);
      Triple<Integer, Integer, Integer> countedPairs = PairCountingFMeasure.countPairs(first, second);
      // Use double, since we want double results at the end!
      double sum = countedPairs.first + countedPairs.second + countedPairs.third;
      double inboth = countedPairs.first / sum;
      double infirst = countedPairs.second / sum;
      double insecond = countedPairs.third / sum;
      double fmeasure = PairCountingFMeasure.fMeasure(countedPairs.first, countedPairs.second, countedPairs.third, 1.0);
      ArrayList<Vector> s = new ArrayList<Vector>(4);
      s.add(new Vector(new double[] { fmeasure, inboth, infirst, insecond }));
      c.addDerivedResult(new ScoreResult(s));
    }
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<O> normalization) {
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
}