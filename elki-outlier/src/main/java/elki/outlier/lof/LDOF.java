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
package elki.outlier.lof;

import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.DatabaseQuery;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.math.Mean;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Computes the LDOF (Local Distance-Based Outlier Factor) for all objects of a
 * Database.
 * <p>
 * Reference:
 * <p>
 * K. Zhang, M. Hutter, H. Jin<br>
 * A New Local Distance-Based Outlier Detection Approach for Scattered
 * Real-World Data.<br>
 * Proc. 13th Pacific-Asia Conf. Adv. Knowledge Discovery and Data Mining
 * (PAKDD 2009)
 *
 * @author Arthur Zimek
 * @since 0.3
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("LDOF: Local Distance-Based Outlier Factor")
@Description("Local outlier detection appraoch suitable for scattered data by averaging the kNN distance over all k nearest neighbors")
@Reference(authors = "K. Zhang, M. Hutter, H. Jin", //
    title = "A New Local Distance-Based Outlier Detection Approach for Scattered Real-World Data", //
    booktitle = "Proc. 13th Pacific-Asia Conf. Adv. Knowledge Discovery and Data Mining (PAKDD 2009)", //
    url = "https://doi.org/10.1007/978-3-642-01307-2_84", //
    bibkey = "DBLP:conf/pakdd/ZhangHJ09")
public class LDOF<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LDOF.class);

  /**
   * The baseline for LDOF values. The paper gives 0.5 for uniform
   * distributions, although one might also discuss using 1.0 as baseline.
   */
  private static final double LDOF_BASELINE = 0.5;

  /**
   * Number of neighbors to query + query point itself.
   */
  protected int kplus;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k Parameter
   */
  public LDOF(Distance<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.kplus = k + 1; // + query point
  }

  /**
   * Run the algorithm
   *
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    DistanceQuery<O> distFunc = relation.getDistanceQuery(getDistance(), DatabaseQuery.HINT_HEAVY_USE);
    KNNQuery<O> knnQuery = relation.getKNNQuery(distFunc, kplus);

    // track the maximum value for normalization
    DoubleMinMax ldofminmax = new DoubleMinMax();
    // compute the ldof values
    WritableDoubleDataStore ldofs = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);

    // compute LOF_SCORE of each db object
    if(LOG.isVerbose()) {
      LOG.verbose("Computing LDOFs");
    }
    FiniteProgress progressLDOFs = LOG.isVerbose() ? new FiniteProgress("LDOF for objects", relation.size(), LOG) : null;

    Mean dxp = new Mean(), Dxp = new Mean();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      KNNList neighbors = knnQuery.getKNNForDBID(iditer, kplus);
      dxp.reset();
      Dxp.reset();
      DoubleDBIDListIter neighbor1 = neighbors.iter(),
          neighbor2 = neighbors.iter();
      for(; neighbor1.valid(); neighbor1.advance()) {
        // skip the point itself
        if(DBIDUtil.equal(neighbor1, iditer)) {
          continue;
        }
        dxp.put(neighbor1.doubleValue());
        for(neighbor2.seek(neighbor1.getOffset() + 1); neighbor2.valid(); neighbor2.advance()) {
          // skip the point itself
          if(DBIDUtil.equal(neighbor2, iditer)) {
            continue;
          }
          Dxp.put(distFunc.distance(neighbor1, neighbor2));
        }
      }
      double ldof = dxp.getMean() / Dxp.getMean();
      if(Double.isNaN(ldof) || Double.isInfinite(ldof)) {
        ldof = 1.0;
      }
      ldofs.putDouble(iditer, ldof);
      // update maximum
      ldofminmax.put(ldof);

      LOG.incrementProcessed(progressLDOFs);
    }
    LOG.ensureCompleted(progressLDOFs);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("LDOF Outlier Score", relation.getDBIDs(), ldofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ldofminmax.getMin(), ldofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, LDOF_BASELINE);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
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
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LDOF_SCORE, must be an integer greater than
     * 1.
     */
    public static final OptionID K_ID = new OptionID("ldof.k", "The number of nearest neighbors of an object to be considered for computing its LDOF_SCORE.");

    /**
     * Number of neighbors to use
     */
    protected int k;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public LDOF<O> make() {
      return new LDOF<>(distanceFunction, k);
    }
  }
}
