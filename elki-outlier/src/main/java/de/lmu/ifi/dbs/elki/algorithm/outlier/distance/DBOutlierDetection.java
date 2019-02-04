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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Simple distanced based outlier detection algorithm. User has to specify two
 * parameters An object is flagged as an outlier if at least a fraction p of all
 * data objects has a distance above d from c.
 * <p>
 * Reference:
 * <p>
 * E.M. Knorr, R. T. Ng:<br>
 * Algorithms for Mining Distance-Based Outliers in Large Datasets,<br>
 * In: Proc. Int. Conf. on Very Large Databases (VLDB'98)
 * <p>
 * This paper presents several Distance Based Outlier Detection algorithms.
 * Implemented here is a simple index based algorithm as presented in section
 * 3.1.
 *
 * @author Lisa Reichert
 * @since 0.3
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("DBOD: Distance Based Outlier Detection")
@Description("If the D-neighborhood of an object contains only very few objects (less than (1-p) percent of the data) this object is flagged as an outlier")
@Reference(authors = "E. M. Knorr, R. T. Ng", //
    title = "Algorithms for Mining Distance-Based Outliers in Large Datasets", //
    booktitle = "Proc. Int. Conf. on Very Large Databases (VLDB'98)", //
    url = "http://www.vldb.org/conf/1998/p392.pdf", //
    bibkey = "DBLP:conf/vldb/KnorrN98")
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
  protected DoubleDataStore computeOutlierScores(Database database, Relation<O> relation, double d) {
    DistanceQuery<O> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    // Prefer kNN query if available, as this will usually stop earlier.
    KNNQuery<O> knnQuery = database.getKNNQuery(distFunc, DatabaseQuery.HINT_OPTIMIZED_ONLY);
    RangeQuery<O> rangeQuery = knnQuery == null ? database.getRangeQuery(distFunc, DatabaseQuery.HINT_OPTIMIZED_ONLY, d) : null;

    // maximum number of objects in the D-neighborhood of an outlier
    int m = (int) Math.floor((distFunc.getRelation().size()) * (1 - p));

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(distFunc.getRelation().getDBIDs(), DataStoreFactory.HINT_STATIC);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("DBOutlier detection", distFunc.getRelation().size(), LOG) : null;
    // if index exists, kNN query. if the distance to the mth nearest neighbor
    // is more than d -> object is outlier
    if(knnQuery != null) {
      if(LOG.isVeryVerbose()) {
        LOG.veryverbose("Using kNN query: " + knnQuery.toString());
      }
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        KNNList knns = knnQuery.getKNNForDBID(iditer, m);
        scores.putDouble(iditer, (knns.getKNNDistance() > d) ? 1. : 0.);
        LOG.incrementProcessed(prog);
      }
    }
    else if(rangeQuery != null) {
      if(LOG.isVeryVerbose()) {
        LOG.veryverbose("Using range query: " + rangeQuery.toString());
      }
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        DoubleDBIDList neighbors = rangeQuery.getRangeForDBID(iditer, d);
        scores.putDouble(iditer, (neighbors.size() < m) ? 1. : 0.);
        LOG.incrementProcessed(prog);
      }
    }
    else {
      // Linear scan neighbors for each object, but stop early.
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        int count = 0;
        for(DBIDIter iterator = relation.iterDBIDs(); iterator.valid(); iterator.advance()) {
          double currentDistance = distFunc.distance(iditer, iterator);
          if(currentDistance <= d) {
            if(++count >= m) {
              break;
            }
          }
        }
        scores.putDouble(iditer, (count < m) ? 1.0 : 0);
        LOG.incrementProcessed(prog);
      }
    }
    LOG.ensureCompleted(prog);
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
      final DoubleParameter pP = new DoubleParameter(P_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
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
