package de.lmu.ifi.dbs.elki.algorithm.outlier.meta;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.BitSet;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A simple ensemble method called "Feature bagging" for outlier detection.
 * 
 * <p>
 * Since the proposed method is only sensible to run on multiple instances of
 * the same algorithm (due to incompatible score ranges), we do not allow using
 * arbitrary algorithms.
 * </p>
 * 
 * <p>
 * Reference: <br>
 * A. Lazarevic, V. Kumar: Feature Bagging for Outlier Detection<br />
 * In: Proc. of the 11th ACM SIGKDD international conference on Knowledge
 * discovery in data mining
 * </p>
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
@Title("Feature Bagging for Outlier Detection")
@Reference(title = "Feature Bagging for Outlier Detection", authors = "A. Lazarevic, V. Kumar", booktitle = "Proc. of the 11th ACM SIGKDD international conference on Knowledge discovery in data mining", url = "http://dx.doi.org/10.1145/1081870.1081891")
public class FeatureBagging extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(FeatureBagging.class);

  /**
   * Number of instances to use
   */
  protected int num = 1;

  /**
   * Cumulative sum or breadth first combinations
   */
  protected boolean breadth = false;

  /**
   * Random number generator for subspace choice
   */
  private Random RANDOM;

  /**
   * The parameters k for LOF.
   */
  private int k;

  /**
   * Constructor.
   * 
   * @param k k Parameter for LOF
   * @param num Number of subspaces to use
   * @param breadth Flag for breadth-first merging
   */
  public FeatureBagging(int k, int num, boolean breadth, Long seed) {
    super();
    this.k = k;
    this.num = num;
    this.breadth = breadth;
    if(seed != null) {
      this.RANDOM = new Random(seed);
    }
    else {
      this.RANDOM = new Random();
    }
  }

  /**
   * Run the algorithm on a data set.
   * 
   * @param relation Relation to use
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<NumberVector<?, ?>> relation) {
    final int dbdim = DatabaseUtil.dimensionality(relation);
    final int mindim = dbdim / 2;
    final int maxdim = dbdim - 1;

    ArrayList<OutlierResult> results = new ArrayList<OutlierResult>(num);
    {
      FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("LOF iterations", num, logger) : null;
      for(int i = 0; i < num; i++) {
        BitSet dimset = randomSubspace(dbdim, mindim, maxdim);
        SubspaceEuclideanDistanceFunction df = new SubspaceEuclideanDistanceFunction(dimset);
        LOF<NumberVector<?, ?>, DoubleDistance> lof = new LOF<NumberVector<?, ?>, DoubleDistance>(k, df, df);

        // run LOF and collect the result
        OutlierResult result = lof.run(relation);
        results.add(result);
        if(prog != null) {
          prog.incrementProcessed(logger);
        }
      }
      if(prog != null) {
        prog.ensureCompleted(logger);
      }
    }

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();
    if(breadth) {
      FiniteProgress cprog = logger.isVerbose() ? new FiniteProgress("Combining results", relation.size(), logger) : null;
      Pair<IterableIterator<DBID>, Relation<Double>>[] IDVectorOntoScoreVector = Pair.newPairArray(results.size());

      // Mapping score-sorted DBID-Iterators onto their corresponding scores.
      // We need to initialize them now be able to iterate them "in parallel".
      {
        int i = 0;
        for(OutlierResult r : results) {
          IDVectorOntoScoreVector[i] = new Pair<IterableIterator<DBID>, Relation<Double>>(r.getOrdering().iter(relation.getDBIDs()), r.getScores());
          i++;
        }
      }

      // Iterating over the *lines* of the AS_t(i)-matrix.
      for(int i = 0; i < relation.size(); i++) {
        // Iterating over the elements of a line (breadth-first).
        for(Pair<IterableIterator<DBID>, Relation<Double>> pair : IDVectorOntoScoreVector) {
          IterableIterator<DBID> iter = pair.first;
          // Always true if every algorithm returns a complete result (one score
          // for every DBID).
          if(iter.hasNext()) {
            DBID tmpID = iter.next();
            double score = pair.second.get(tmpID);
            if(Double.isNaN(scores.doubleValue(tmpID))) {
              scores.putDouble(tmpID, score);
              minmax.put(score);
            }
          }
          else {
            logger.warning("Incomplete result: Iterator does not contain |DB| DBIDs");
          }
        }
        // Progress does not take the initial mapping into account.
        if(cprog != null) {
          cprog.incrementProcessed(logger);
        }
      }
      if(cprog != null) {
        cprog.ensureCompleted(logger);
      }
    }
    else {
      FiniteProgress cprog = logger.isVerbose() ? new FiniteProgress("Combining results", relation.size(), logger) : null;
      for(DBID id : relation.iterDBIDs()) {
        double sum = 0.0;
        for(OutlierResult r : results) {
          final Double s = r.getScores().get(id);
          if (s != null && !Double.isNaN(s)) {
            sum += s;
          }
        }
        scores.putDouble(id, sum);
        minmax.put(sum);
        if(cprog != null) {
          cprog.incrementProcessed(logger);
        }
      }
      if(cprog != null) {
        cprog.ensureCompleted(logger);
      }
    }
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    Relation<Double> scoreres = new MaterializedRelation<Double>("Feature bagging", "fb-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    return new OutlierResult(meta, scoreres);
  }

  /**
   * Choose a random subspace
   * 
   * @param alldim Number of total dimensions
   * @param mindim Minimum number to choose
   * @param maxdim Maximum number to choose
   * @return Subspace as bits.
   */
  private BitSet randomSubspace(final int alldim, final int mindim, final int maxdim) {
    BitSet dimset = new BitSet();
    {
      // Fill with all dimensions
      int[] dims = new int[alldim];
      for(int d = 0; d < alldim; d++) {
        dims[d] = d;
      }
      // Target dimensionality:
      int subdim = mindim + RANDOM.nextInt(maxdim - mindim);
      // Shrink the subspace to the destination size
      for(int d = 0; d < alldim - subdim; d++) {
        int s = RANDOM.nextInt(alldim - d);
        dimset.set(dims[s]);
        dims[s] = dims[alldim - d - 1];
      }
    }
    return dimset;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the number of instances to use in the ensemble.
     * <p>
     * Key: {@code -fbagging.num}
     * </p>
     */
    public static final OptionID NUM_ID = OptionID.getOrCreateOptionID("fbagging.num", "The number of instances to use in the ensemble.");

    /**
     * The flag for using the breadth first approach
     * <p>
     * Key: {@code -fbagging.breadth}
     * </p>
     */
    public static final OptionID BREADTH_ID = OptionID.getOrCreateOptionID("fbagging.breadth", "Use the breadth first combinations instead of the cumulative sum approach");

    /**
     * The parameter to specify the random seed
     * <p>
     * Key: {@code -fbagging.seed}
     * </p>
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("fbagging.seed", "Specify a particular random seed.");

    /**
     * The neighborhood size to use
     */
    protected int k = 2;

    /**
     * Number of instances to use
     */
    protected int num = 1;

    /**
     * Cumulative sum or breadth first combinations
     */
    protected boolean breadth = false;

    /**
     * Random generator seed
     */
    protected Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter pK = new IntParameter(LOF.K_ID, new GreaterConstraint(1));
      if(config.grab(pK)) {
        k = pK.getValue();
      }
      IntParameter NUM_PARAM = new IntParameter(NUM_ID, new GreaterEqualConstraint(1));
      if(config.grab(NUM_PARAM)) {
        num = NUM_PARAM.getValue();
      }
      Flag BREADTH_FLAG = new Flag(BREADTH_ID);
      if(config.grab(BREADTH_FLAG)) {
        breadth = BREADTH_FLAG.getValue();
      }
      LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected FeatureBagging makeInstance() {
      // Default is to re-use the same distance
      return new FeatureBagging(k, num, breadth, seed);
    }
  }
}