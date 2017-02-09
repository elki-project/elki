/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

import net.jafama.FastMath;

/**
 * kNN-based adaption of Stochastic Outlier Selection.
 * 
 * This is a trivial variation of Stochastic Outlier Selection to benefit from
 * KNN indexes, but not discussed in the original publication. Instead of
 * setting perplexity, we choose the number of neighbors k, and set perplexity
 * simply to k/3. Objects outside of the kNN are not considered anymore.
 * 
 * If you use this variant, please credit the ELKI version you used, as this was
 * not discussed in the original paper.
 * 
 * Original reference:
 * <p>
 * J. Janssens, F. Huszár, E. Postma, J. van den Herik<br />
 * Stochastic Outlier Selection<br />
 * TiCC TR 2012–001
 * </p>
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type.
 */
@Reference(authors = "J. Janssens, F. Huszár, E. Postma, J. van den Herik", //
    title = "Stochastic Outlier Selection", //
    booktitle = "TiCC TR 2012–001", //
    url = "https://www.tilburguniversity.edu/upload/b7bac5b2-9b00-402a-9261-7849aa019fbb_sostr.pdf")
public class KNNSOS<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(KNNSOS.class);

  /**
   * Number of neighbors (not including query point).
   */
  protected int k;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k Number of neighbors to consider
   */
  public KNNSOS(DistanceFunction<? super O> distance, int k) {
    super(distance);
    this.k = k;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  /**
   * Run the algorithm.
   * 
   * @param relation data relation.
   * @return
   */
  public OutlierResult run(Relation<O> relation) {
    final int k1 = k + 1; // Query size
    final double perplexity = k / 3.;
    KNNQuery<O> knnq = relation.getKNNQuery(getDistanceFunction(), k1);
    final double logPerp = FastMath.log(perplexity);

    double[] p = new double[k + 10];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("SOS scores", relation.size(), LOG) : null;
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB, 1.);
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      KNNList knns = knnq.getKNNForDBID(it, k1);
      if(p.length < knns.size() + 1) {
        p = new double[knns.size() + 10];
      }
      final DoubleDBIDListIter ki = knns.iter();
      // Compute affinities
      SOS.computePi(it, ki, p, perplexity, logPerp);
      // Normalization factor:
      double s = SOS.sumOfProbabilities(it, ki, p);
      if(s > 0) {
        SOS.nominateNeighbors(it, ki, p, 1. / s, scores);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    // Find minimum and maximum.
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter it2 = relation.iterDBIDs(); it2.valid(); it2.advance()) {
      minmax.put(scores.doubleValue(it2));
    }
    DoubleRelation scoreres = new MaterializedDoubleRelation("Stoachastic Outlier Selection", "sos-outlier", scores, relation.getDBIDs());
    OutlierScoreMeta meta = new ProbabilisticOutlierScore(minmax.getMin(), minmax.getMax(), 0.);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify the number of neighbors
     */
    public static final OptionID KNN_ID = new OptionID("sos.k", "Number of neighbors to use. Should be about 3x the desired perplexity.");

    /**
     * Number of neighbors
     */
    int k = 15;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter kP = new IntParameter(KNN_ID, 15) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.intValue();
      }
    }

    @Override
    protected KNNSOS<O> makeInstance() {
      return new KNNSOS<O>(distanceFunction, k);
    }
  }
}
