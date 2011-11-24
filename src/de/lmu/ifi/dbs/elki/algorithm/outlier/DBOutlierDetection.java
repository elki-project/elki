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

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
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
 * <p>
 * Reference: E.M. Knorr, R. T. Ng: Algorithms for Mining Distance-Based
 * Outliers in Large Datasets, In: Procs Int. Conf. on Very Large Databases
 * (VLDB'98), New York, USA, 1998.
 * 
 * This paper presents several Distance Based Outlier Detection algorithms.
 * Implemented here is a simple index based algorithm as presented in section
 * 3.1.
 * 
 * @author Lisa Reichert
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("DBOD: Distance Based Outlier Detection")
@Description("If the D-neighborhood of an object contains only very few objects (less than (1-p) percent of the data) this object is flagged as an outlier")
@Reference(authors = "E.M. Knorr, R. T. Ng", title = "Algorithms for Mining Distance-Based Outliers in Large Datasets", booktitle = "Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998")
public class DBOutlierDetection<O, D extends Distance<D>> extends AbstractDBOutlier<O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DBOutlierDetection.class);

  /**
   * Parameter to specify the minimum fraction of objects that must be outside
   * the D- neighborhood of an outlier
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("dbod.p", "minimum fraction of objects that must be outside the D-neighborhood of an outlier");

  /**
   * Holds the value of {@link #P_ID}.
   */
  private double p;

  /**
   * Constructor with actual parameters.
   * 
   * @param distanceFunction distance function parameter
   * @param d distance query radius
   * @param p percentage parameter
   */
  public DBOutlierDetection(DistanceFunction<O, D> distanceFunction, D d, double p) {
    super(distanceFunction, d);
    this.p = p;
  }

  @Override
  protected DataStore<Double> computeOutlierScores(Database database, Relation<O> relation, D neighborhoodSize) {
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O, D> knnQuery = database.getKNNQuery(distFunc, DatabaseQuery.HINT_OPTIMIZED_ONLY);

    // maximum number of objects in the D-neighborhood of an outlier
    int m = (int) ((distFunc.getRelation().size()) * (1 - p));

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(distFunc.getRelation().getDBIDs(), DataStoreFactory.HINT_STATIC);
    if(logger.isVerbose()) {
      logger.verbose("computing outlier flag");
    }

    FiniteProgress progressOFlags = logger.isVerbose() ? new FiniteProgress("DBOutlier for objects", distFunc.getRelation().size(), logger) : null;
    int counter = 0;
    // if index exists, kNN query. if the distance to the mth nearest neighbor
    // is more than d -> object is outlier
    if(knnQuery != null) {
      for(DBID id : distFunc.getRelation().iterDBIDs()) {
        counter++;
        final KNNResult<D> knns = knnQuery.getKNNForDBID(id, m);
        if(logger.isDebugging()) {
          logger.debugFine("distance to mth nearest neighbour" + knns.toString());
        }
        if(knns.get(Math.min(m, knns.size()) - 1).getDistance().compareTo(neighborhoodSize) <= 0) {
          // flag as outlier
          scores.putDouble(id, 1.0);
        }
        else {
          // flag as no outlier
          scores.putDouble(id, 0.0);
        }
      }
      if(progressOFlags != null) {
        progressOFlags.setProcessed(counter, logger);
      }
    }
    else {
      // range query for each object. stop if m objects are found
      for(DBID id : distFunc.getRelation().iterDBIDs()) {
        counter++;
        Iterator<DBID> iterator = distFunc.getRelation().iterDBIDs();
        int count = 0;
        while(iterator.hasNext() && count < m) {
          DBID currentID = iterator.next();
          D currentDistance = distFunc.distance(id, currentID);

          if(currentDistance.compareTo(neighborhoodSize) <= 0) {
            count++;
          }
        }

        if(count < m) {
          // flag as outlier
          scores.putDouble(id, 1.0);
        }
        else {
          // flag as no outlier
          scores.putDouble(id, 0.0);
        }
      }

      if(progressOFlags != null) {
        progressOFlags.setProcessed(counter, logger);
      }
    }
    if(progressOFlags != null) {
      progressOFlags.ensureCompleted(logger);
    }
    return scores;
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
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDBOutlier.Parameterizer<O, D> {
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
    protected DBOutlierDetection<O, D> makeInstance() {
      return new DBOutlierDetection<O, D>(distanceFunction, d, p);
    }
  }
}