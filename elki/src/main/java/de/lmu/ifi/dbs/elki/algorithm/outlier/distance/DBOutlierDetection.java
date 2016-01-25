package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Simple distanced based outlier detection algorithm. User has to specify two
 * parameters An object is flagged as an outlier if at least a fraction p of all
 * data objects has a distance above d from c
 *
 * Reference:
 * <p>
 * E.M. Knorr, R. T. Ng:<br />
 * Algorithms for Mining Distance-Based Outliers in Large Datasets,<br />
 * In: Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998.
 * </p>
 *
 * This paper presents several Distance Based Outlier Detection algorithms.
 * Implemented here is a simple index based algorithm as presented in section
 * 3.1.
 *
 * @author Lisa Reichert
 * @since 0.3
 *
 * @apiviz.has KNNQuery
 *
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
@Title("DBOD: Distance Based Outlier Detection")
@Description("If the D-neighborhood of an object contains only very few objects (less than (1-p) percent of the data) this object is flagged as an outlier")
@Reference(authors = "E.M. Knorr, R. T. Ng", //
title = "Algorithms for Mining Distance-Based Outliers in Large Datasets", //
booktitle = "Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.DBOutlierDetection" })
public class DBOutlierDetection<O> extends AbstractDBOutlier<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DBOutlierDetection.class);

  /**
   * Density threshold percentage p.
   */
  private double p;

  /**
   * Constructor with actual parameters.
   *
   * @param distanceFunction distance function parameter
   * @param d distance query radius
   * @param p percentage parameter
   */
  public DBOutlierDetection(DistanceFunction<? super O> distanceFunction, double d, double p) {
    super(distanceFunction, d);
    this.p = p;
  }

  @Override
  protected DoubleDataStore computeOutlierScores(Database database, Relation<O> relation, double neighborhoodSize) {
    DistanceQuery<O> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnQuery = database.getKNNQuery(distFunc, DatabaseQuery.HINT_OPTIMIZED_ONLY);

    // maximum number of objects in the D-neighborhood of an outlier
    int m = (int) ((distFunc.getRelation().size()) * (1 - p));

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(distFunc.getRelation().getDBIDs(), DataStoreFactory.HINT_STATIC);
    if(LOG.isVerbose()) {
      LOG.verbose("computing outlier flag");
    }

    FiniteProgress progressOFlags = LOG.isVerbose() ? new FiniteProgress("DBOutlier for objects", distFunc.getRelation().size(), LOG) : null;
    int counter = 0;
    // if index exists, kNN query. if the distance to the mth nearest neighbor
    // is more than d -> object is outlier
    if(knnQuery != null) {
      for(DBIDIter iditer = distFunc.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
        counter++;
        final KNNList knns = knnQuery.getKNNForDBID(iditer, m);
        if(LOG.isDebugging()) {
          LOG.debugFine("distance to mth nearest neighbour" + knns.toString());
        }
        if(knns.get(Math.min(m, knns.size()) - 1).doubleValue() <= neighborhoodSize) {
          // flag as outlier
          scores.putDouble(iditer, 1.0);
        }
        else {
          // flag as no outlier
          scores.putDouble(iditer, 0.0);
        }
      }
      if(progressOFlags != null) {
        progressOFlags.setProcessed(counter, LOG);
      }
    }
    else {
      // range query for each object. stop if m objects are found
      for(DBIDIter iditer = distFunc.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
        counter++;
        int count = 0;
        for(DBIDIter iterator = distFunc.getRelation().iterDBIDs(); iterator.valid() && count < m; iterator.advance()) {
          double currentDistance = distFunc.distance(iditer, iterator);
          if(currentDistance <= neighborhoodSize) {
            count++;
          }
        }
        scores.putDouble(iditer, (count < m) ? 1.0 : 0);
      }

      if(progressOFlags != null) {
        progressOFlags.setProcessed(counter, LOG);
      }
    }
    LOG.ensureCompleted(progressOFlags);
    return scores;
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
   */
  public static class Parameterizer<O> extends AbstractDBOutlier.Parameterizer<O> {
    /**
     * Parameter to specify the minimum fraction of objects that must be outside
     * the D- neighborhood of an outlier
     */
    public static final OptionID P_ID = new OptionID("dbod.p", "minimum fraction of objects that must be outside the D-neighborhood of an outlier");

    /**
     * Density threshold p.
     */
    protected double p = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter pP = new DoubleParameter(P_ID);
      if(config.grab(pP)) {
        p = pP.getValue();
      }
    }

    @Override
    protected DBOutlierDetection<O> makeInstance() {
      return new DBOutlierDetection<>(distanceFunction, d, p);
    }
  }
}