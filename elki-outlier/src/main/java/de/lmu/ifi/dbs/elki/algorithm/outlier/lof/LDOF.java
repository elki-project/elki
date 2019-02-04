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
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

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
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.LDOF" })
public class LDOF<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
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
   * Number of neighbors to query.
   */
  protected int k;

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k Parameter
   */
  public LDOF(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k + 1; // + query point
  }

  /**
   * Run the algorithm
   *
   * @param database Database to process
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    DistanceQuery<O> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnQuery = database.getKNNQuery(distFunc, k);

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
      KNNList neighbors = knnQuery.getKNNForDBID(iditer, k);
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
    DoubleRelation scoreResult = new MaterializedDoubleRelation("LDOF Outlier Score", "ldof-outlier", ldofs, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ldofminmax.getMin(), ldofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, LDOF_BASELINE);
    return new OutlierResult(scoreMeta, scoreResult);
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected LDOF<O> makeInstance() {
      return new LDOF<>(distanceFunction, k);
    }
  }
}
