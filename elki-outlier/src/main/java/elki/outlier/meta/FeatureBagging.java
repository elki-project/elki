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
package elki.outlier.meta;

import java.util.ArrayList;
import java.util.Random;

import elki.outlier.OutlierAlgorithm;
import elki.outlier.lof.LOF;
import elki.AbstractAlgorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.subspace.SubspaceEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.pairs.Pair;
import elki.utilities.random.RandomFactory;

/**
 * A simple ensemble method called "Feature bagging" for outlier detection.
 * <p>
 * Since the proposed method is only sensible to run on multiple instances of
 * the same algorithm (due to incompatible score ranges), we do not allow using
 * arbitrary algorithms.
 * <p>
 * Reference:<br>
 * A. Lazarevic, V. Kumar<br>
 * Feature Bagging for Outlier Detection<br>
 * Proc. 11th ACM SIGKDD Int. Conf. on Knowledge Discovery in Data Mining
 *
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * @since 0.4.0
 */
@Title("Feature Bagging for Outlier Detection")
@Reference(authors = "A. Lazarevic, V. Kumar", //
    title = "Feature Bagging for Outlier Detection", //
    booktitle = "Proc. 11th ACM SIGKDD Int. Conf. on Knowledge Discovery in Data Mining", //
    url = "https://doi.org/10.1145/1081870.1081891", //
    bibkey = "DBLP:conf/kdd/LazarevicK05")
public class FeatureBagging extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FeatureBagging.class);

  /**
   * Number of instances to use.
   */
  protected int num = 1;

  /**
   * Cumulative sum or breadth first combinations.
   */
  protected boolean breadth = false;

  /**
   * Random number generator for subspace choice.
   */
  private RandomFactory rnd;

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
   * @param rnd Random generator
   */
  public FeatureBagging(int k, int num, boolean breadth, RandomFactory rnd) {
    super();
    this.k = k;
    this.num = num;
    this.breadth = breadth;
    this.rnd = rnd;
  }

  /**
   * Run the algorithm on a data set.
   *
   * @param database Database context
   * @param relation Relation to use
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<NumberVector> relation) {
    final int dbdim = RelationUtil.dimensionality(relation);
    final int mindim = dbdim >> 1;
    final int maxdim = dbdim - 1;
    final Random rand = rnd.getSingleThreadedRandom();

    ArrayList<OutlierResult> results = new ArrayList<>(num);
    {
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("LOF iterations", num, LOG) : null;
      for(int i = 0; i < num; i++) {
        long[] dimset = randomSubspace(dbdim, mindim, maxdim, rand);
        SubspaceEuclideanDistance df = new SubspaceEuclideanDistance(dimset);
        // run LOF and collect the result
        results.add(new LOF<>(k, df).run(relation));
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
    }

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    DoubleMinMax minmax = new DoubleMinMax();
    if(breadth) {
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Combining results", relation.size(), LOG) : null;
      @SuppressWarnings("unchecked")
      Pair<DBIDIter, DoubleRelation>[] IDVectorOntoScoreVector = (Pair<DBIDIter, DoubleRelation>[]) new Pair[results.size()];

      // Mapping score-sorted DBID-Iterators onto their corresponding scores.
      // We need to initialize them now be able to iterate them "in parallel".
      {
        int i = 0;
        for(OutlierResult r : results) {
          IDVectorOntoScoreVector[i] = new Pair<DBIDIter, DoubleRelation>(r.getOrdering().order(relation.getDBIDs()).iter(), r.getScores());
          i++;
        }
      }

      // Iterating over the *lines* of the AS_t(i)-matrix.
      for(int i = 0; i < relation.size(); i++) {
        // Iterating over the elements of a line (breadth-first).
        for(Pair<DBIDIter, DoubleRelation> pair : IDVectorOntoScoreVector) {
          DBIDIter iter = pair.first;
          // Always true if every algorithm returns a complete result (one score
          // for every DBID).
          if(iter.valid()) {
            double score = pair.second.doubleValue(iter);
            if(Double.isNaN(scores.doubleValue(iter))) {
              scores.putDouble(iter, score);
              minmax.put(score);
            }
            iter.advance();
          }
          else {
            LOG.warning("Incomplete result: Iterator does not contain |DB| DBIDs");
          }
        }
        // Progress does not take the initial mapping into account.
        LOG.incrementProcessed(cprog);
      }
      LOG.ensureCompleted(cprog);
    }
    else {
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Combining results", relation.size(), LOG) : null;
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        double sum = 0.0;
        for(OutlierResult r : results) {
          final double s = r.getScores().doubleValue(iter);
          if(!Double.isNaN(s)) {
            sum += s;
          }
        }
        scores.putDouble(iter, sum);
        minmax.put(sum);
        LOG.incrementProcessed(cprog);
      }
      LOG.ensureCompleted(cprog);
    }
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    DoubleRelation scoreres = new MaterializedDoubleRelation("Feature bagging", relation.getDBIDs(), scores);
    return new OutlierResult(meta, scoreres);
  }

  /**
   * Choose a random subspace.
   *
   * @param alldim Number of total dimensions
   * @param mindim Minimum number to choose
   * @param maxdim Maximum number to choose
   * @return Subspace as bits.
   */
  private long[] randomSubspace(final int alldim, final int mindim, final int maxdim, final Random rand) {
    long[] dimset = BitsUtil.zero(alldim);
    // Fill with all dimensions
    int[] dims = new int[alldim];
    for(int d = 0; d < alldim; d++) {
      dims[d] = d;
    }
    // Target dimensionality:
    int subdim = mindim + rand.nextInt(maxdim - mindim);
    // Shrink the subspace to the destination size
    for(int d = 0; d < alldim - subdim; d++) {
      int s = rand.nextInt(alldim - d);
      BitsUtil.setI(dimset, dims[s]);
      dims[s] = dims[alldim - d - 1];
    }
    return dimset;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the number of instances to use in the ensemble.
     */
    public static final OptionID NUM_ID = new OptionID("fbagging.num", "The number of instances to use in the ensemble.");

    /**
     * The flag for using the breadth first approach.
     */
    public static final OptionID BREADTH_ID = new OptionID("fbagging.breadth", "Use the breadth first combinations instead of the cumulative sum approach");

    /**
     * The parameter to specify the random seed.
     */
    public static final OptionID SEED_ID = new OptionID("fbagging.seed", "Specify a particular random seed.");

    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    /**
     * Number of instances to use.
     */
    protected int num = 1;

    /**
     * Cumulative sum or breadth first combinations.
     */
    protected boolean breadth = false;

    /**
     * Random generator.
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(LOF.Par.K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> k = x);
      new IntParameter(NUM_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> num = x);
      new Flag(BREADTH_ID).grab(config, x -> breadth = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public FeatureBagging make() {
      // Default is to re-use the same distance
      return new FeatureBagging(k, num, breadth, rnd);
    }
  }
}
