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
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.datastore.*;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Influence Outliers using Symmetric Relationship (INFLO) using two-way search,
 * is an outlier detection method based on LOF; but also using the reverse kNN.
 * <p>
 * Reference:
 * <p>
 * W. Jin, A. Tung, J. Han, W. Wang<br>
 * Ranking outliers using symmetric neighborhood relationship<br>
 * Proc. 10th Pacific-Asia conference on Advances in Knowledge Discovery and
 * Data Mining, 2006.
 * <p>
 * There is an error in the two-way search algorithm proposed in the article
 * above. It does not correctly compute the RNN, but it will find only those RNN
 * that are in the kNN, making the entire RNN computation redundant, as it uses
 * the kNN + RNN later anyway.
 * <p>
 * Given the errors in the INFLO paper, as of ELKI 0.8, we do:
 * <ul>
 * <li>Assume \( IS_k(p) := kNN(p) \cup RkNN(p) \)</li>
 * <li>Implement the pruning of two-way search, i.e., use INFLO=1 if
 * \( |kNN(p) \cap RkNN(p)|\geq m\cdot |kNN(p)| \)</li>
 * </ul>
 *
 * @author Ahmed Hettab
 * @author Erich Schubert
 * @since 0.3
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of DatabaseObject the algorithm is applied on
 */
@Title("INFLO: Influenced Outlierness Factor")
@Description("Ranking Outliers Using Symmetric Neigborhood Relationship")
@Reference(authors = "W. Jin, A. Tung, J. Han, W. Wang", //
    title = "Ranking outliers using symmetric neighborhood relationship", //
    booktitle = "Proc. 10th Pacific-Asia conference on Advances in Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1007/11731139_68", //
    bibkey = "DBLP:conf/pakdd/JinTHW06")
@Alias("de.lmu.ifi.dbs.elki.algorithm.outlier.INFLO")
public class INFLO<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(INFLO.class);

  /**
   * Pruning threshold m.
   */
  private double m;

  /**
   * Number of neighbors to use.
   */
  private int kplus1;

  /**
   * Constructor with parameters.
   *
   * @param distanceFunction Distance function in use
   * @param m m Parameter
   * @param k k Parameter
   */
  public INFLO(DistanceFunction<? super O> distanceFunction, double m, int k) {
    super(distanceFunction);
    this.m = m;
    this.kplus1 = k + 1;
  }

  /**
   * Run the algorithm
   *
   * @param database Database to process
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("INFLO", 3) : null;

    // Step one: find the kNN
    LOG.beginStep(stepprog, 1, "Materializing nearest-neighbor sets.");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(database, relation, getDistanceFunction(), kplus1);

    // Step two: find the RkNN, minus kNN.
    LOG.beginStep(stepprog, 2, "Materialize reverse NN.");
    ModifiableDBIDs pruned = DBIDUtil.newHashSet();
    // kNNS
    WritableDataStore<SetDBIDs> knns = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, SetDBIDs.class);
    // We first need to convert the kNN into sets, because of performance below.
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      knns.put(iditer, DBIDUtil.ensureSet(knnq.getKNNForDBID(iditer, kplus1)));
    }
    // RNNS
    WritableDataStore<ModifiableDBIDs> rnnMinusKNNs = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, ModifiableDBIDs.class);
    // init the rNN
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      rnnMinusKNNs.put(iditer, DBIDUtil.newArray());
    }
    computeNeighborhoods(relation, knns, pruned, rnnMinusKNNs);
    // We do not need the set-copy of the knns anymore
    knns.clear();

    // Step three: compute INFLO scores
    LOG.beginStep(stepprog, 3, "Compute INFLO scores.");
    // Calculate INFLO for any Object
    DoubleMinMax inflominmax = new DoubleMinMax();
    WritableDoubleDataStore inflos = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    computeINFLO(relation, pruned, knnq, rnnMinusKNNs, inflos, inflominmax);
    LOG.setCompleted(stepprog);
    LOG.statistics(new LongStatistic(INFLO.class.getName() + ".pruned", pruned.size()));

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Influence Outlier Score", "inflo-outlier", inflos, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(inflominmax.getMin(), inflominmax.getMax(), 0., Double.POSITIVE_INFINITY, 1.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute the reverse kNN minus the kNN.
   * <p>
   * This is based on algorithm 2 (two-way search) from the INFLO paper, but
   * unfortunately this algorithm does not compute the RkNN correctly, but
   * rather \( RkNN \cap kNN \), which is quite useless given that we will use
   * the union of that with kNN later on. Therefore, we decided to rather follow
   * what appears to be the idea of the method, not the literal pseudocode
   * included.
   *
   * @param relation Data relation
   * @param knns Stored nearest neighbors
   * @param pruned Pruned objects: with too many neighbors
   * @param rNNminuskNNs reverse kNN storage
   */
  private void computeNeighborhoods(Relation<O> relation, DataStore<SetDBIDs> knns, ModifiableDBIDs pruned, WritableDataStore<ModifiableDBIDs> rNNminuskNNs) {
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Finding RkNN", relation.size(), LOG) : null;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBIDs knn = knns.get(iter);
      int count = 1; // The point itself.
      for(DBIDIter niter = knn.iter(); niter.valid(); niter.advance()) {
        // Ignore the query point itself.
        if(DBIDUtil.equal(iter, niter)) {
          continue;
        }
        // As we did not initialize count with the rNN size, we check all
        // neighbors here.
        if(knns.get(niter).contains(iter)) {
          count++;
        }
        else {
          // In contrast to INFLO pseudocode, we only update if it is not found,
          // i.e., if it is in RkNN \setminus kNN, to save memory.
          rNNminuskNNs.get(niter).add(iter);
        }
      }
      // INFLO pruning rule
      if(count >= knn.size() * m) {
        pruned.add(iter);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Compute the final INFLO scores.
   *
   * @param relation Data relation
   * @param pruned Pruned objects
   * @param knnq kNN query
   * @param rNNminuskNNs reverse kNN storage
   * @param inflos INFLO score storage
   * @param inflominmax Output of minimum and maximum
   */
  protected void computeINFLO(Relation<O> relation, ModifiableDBIDs pruned, KNNQuery<O> knnq, WritableDataStore<ModifiableDBIDs> rNNminuskNNs, WritableDoubleDataStore inflos, DoubleMinMax inflominmax) {
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing INFLOs", relation.size(), LOG) : null;
    HashSetModifiableDBIDs set = DBIDUtil.newHashSet();
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if(pruned.contains(iter)) {
        inflos.putDouble(iter, 1.);
        inflominmax.put(1.);
        LOG.incrementProcessed(prog);
        continue;
      }
      final KNNList knn = knnq.getKNNForDBID(iter, kplus1);
      if(knn.getKNNDistance() == 0.) {
        inflos.putDouble(iter, 1.);
        inflominmax.put(1.);
        LOG.incrementProcessed(prog);
        continue;
      }
      set.clear();
      set.addDBIDs(knn);
      set.addDBIDs(rNNminuskNNs.get(iter));
      // Compute mean density of NN \cup RNN
      double sum = 0.;
      int c = 0;
      for(DBIDIter niter = set.iter(); niter.valid(); niter.advance()) {
        if(DBIDUtil.equal(iter, niter)) {
          continue;
        }
        final double kdist = knnq.getKNNForDBID(niter, kplus1).getKNNDistance();
        if(kdist <= 0) {
          sum = Double.POSITIVE_INFINITY;
          c++;
          break;
        }
        sum += 1. / kdist;
        c++;
      }
      sum *= knn.getKNNDistance();
      final double inflo = sum == 0 ? 1. : sum / c;
      inflos.putDouble(iter, inflo);
      inflominmax.put(inflo);
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify if any object is a Core Object must be a double
     * greater than 0.0
     * <p>
     * see paper "Two-way search method" 3.2
     */
    public static final OptionID M_ID = new OptionID("inflo.m", "The pruning threshold");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its INFLO score.
     */
    public static final OptionID K_ID = new OptionID("inflo.k", "The number of nearest neighbors of an object to be considered for computing its INFLO score.");

    /**
     * M parameter
     */
    protected double m = 1.0;

    /**
     * Number of neighbors to use.
     */
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter mP = new DoubleParameter(M_ID, 1.0)//
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(mP)) {
        m = mP.doubleValue();
      }

      final IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected INFLO<O> makeInstance() {
      return new INFLO<>(distanceFunction, m, k);
    }
  }
}
