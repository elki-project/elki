package de.lmu.ifi.dbs.elki.algorithm.outlier;

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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Computes the LDOF (Local Distance-Based Outlier Factor) for all objects of a
 * Database.
 * </p>
 * 
 * <p>
 * Reference:<br>
 * K. Zhang, M. Hutter, H. Jin: A New Local Distance-Based Outlier Detection
 * Approach for Scattered Real-World Data.<br>
 * In: Proc. 13th Pacific-Asia Conference on Advances in Knowledge Discovery and
 * Data Mining (PAKDD 2009), Bangkok, Thailand, 2009.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
@Title("LDOF: Local Distance-Based Outlier Factor")
@Description("Local outlier detection appraoch suitable for scattered data by averaging the kNN distance over all k nearest neighbors")
@Reference(authors = "K. Zhang, M. Hutter, H. Jin", title = "A New Local Distance-Based Outlier Detection Approach for Scattered Real-World Data", booktitle = "Proc. 13th Pacific-Asia Conference on Advances in Knowledge Discovery and Data Mining (PAKDD 2009), Bangkok, Thailand, 2009", url = "http://dx.doi.org/10.1007/978-3-642-01307-2_84")
public class LDOF<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(LDOF.class);

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LDOF_SCORE, must be an integer greater than 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("ldof.k", "The number of nearest neighbors of an object to be considered for computing its LDOF_SCORE.");

  /**
   * The baseline for LDOF values. The paper gives 0.5 for uniform
   * distributions, although one might also discuss using 1.0 as baseline.
   */
  private static final double LDOF_BASELINE = 0.5;

  /**
   * Holds the value of {@link #K_ID}.
   */
  int k;

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k Parameter
   */
  public LDOF(DistanceFunction<? super O, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  public OutlierResult run(Database database, Relation<O> relation) throws IllegalStateException {
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O, D> knnQuery = database.getKNNQuery(distFunc, k);

    // track the maximum value for normalization
    DoubleMinMax ldofminmax = new DoubleMinMax();
    // compute the ldof values
    WritableDataStore<Double> ldofs = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);

    // compute LOF_SCORE of each db object
    if(logger.isVerbose()) {
      logger.verbose("Computing LDOFs");
    }
    FiniteProgress progressLDOFs = logger.isVerbose() ? new FiniteProgress("LDOF_SCORE for objects", relation.size(), logger) : null;

    for(DBID id : relation.iterDBIDs()) {
      KNNResult<D> neighbors = knnQuery.getKNNForDBID(id, k);
      int nsize = neighbors.size() - 1;
      // skip the point itself
      double dxp = 0;
      double Dxp = 0;
      for(DistanceResultPair<D> neighbor1 : neighbors) {
        if(!neighbor1.getDBID().equals(id)) {
          dxp += neighbor1.getDistance().doubleValue();
          for(DistanceResultPair<D> neighbor2 : neighbors) {
            if(!neighbor1.getDBID().equals(neighbor2.getDBID()) && !neighbor2.getDBID().equals(id)) {
              Dxp += distFunc.distance(neighbor1.getDBID(), neighbor2.getDBID()).doubleValue();
            }
          }
        }
      }
      dxp /= nsize;
      Dxp /= (nsize * (nsize - 1));
      Double ldof = dxp / Dxp;
      if(ldof.isNaN() || ldof.isInfinite()) {
        ldof = 1.0;
      }
      ldofs.put(id, ldof);
      // update maximum
      ldofminmax.put(ldof);

      if(progressLDOFs != null) {
        progressLDOFs.incrementProcessed(logger);
      }
    }
    if(progressLDOFs != null) {
      progressLDOFs.ensureCompleted(logger);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<Double>("LDOF Outlier Score", "ldof-outlier", TypeUtil.DOUBLE, ldofs, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(ldofminmax.getMin(), ldofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, LDOF_BASELINE);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(1));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected LDOF<O, D> makeInstance() {
      return new LDOF<O, D>(distanceFunction, k);
    }
  }
}