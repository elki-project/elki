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

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Compute percentage of neighbors in the given neighborhood with size d.
 * 
 * Generalization of the DB Outlier Detection by using the fraction as outlier
 * score thus eliminating this parameter and turning the method into a ranking
 * method instead of a labelling one.
 * 
 * @author Lisa Reichert
 * 
 * @apiviz.has RangeQuery
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
@Title("Distance based outlier score")
@Description("Generalization of the original DB-Outlier approach to a ranking method, by turning the fraction parameter into the output value.")
@Reference(prefix = "Generalization of a method proposed in", authors = "E.M. Knorr, R. T. Ng", title = "Algorithms for Mining Distance-Based Outliers in Large Datasets", booktitle = "Procs Int. Conf. on Very Large Databases (VLDB'98), New York, USA, 1998")
public class DBOutlierScore<O, D extends Distance<D>> extends AbstractDBOutlier<O, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(DBOutlierScore.class);

  /**
   * Constructor with parameters.
   * 
   * @param distanceFunction Distance function
   * @param d distance radius parameter
   */
  public DBOutlierScore(DistanceFunction<O, D> distanceFunction, D d) {
    super(distanceFunction, d);
  }

  @Override
  protected DataStore<Double> computeOutlierScores(Database database, Relation<O> relation, D d) {
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, getDistanceFunction());
    RangeQuery<O, D> rangeQuery = database.getRangeQuery(distFunc);
    final double size = distFunc.getRelation().size();

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(distFunc.getRelation().getDBIDs(), DataStoreFactory.HINT_STATIC);
    // TODO: use bulk when implemented.
    for(DBIDIter iditer = distFunc.getRelation().iterDBIDs(); iditer.valid(); iditer.advance()) {
      // compute percentage of neighbors in the given neighborhood with size d
      double n = (rangeQuery.getRangeForDBID(iditer, d).size()) / size;
      scores.putDouble(iditer, 1.0 - n);
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
    @Override
    protected DBOutlierScore<O, D> makeInstance() {
      return new DBOutlierScore<O, D>(distanceFunction, d);
    }
  }
}