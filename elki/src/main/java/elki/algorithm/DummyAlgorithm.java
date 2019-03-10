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
package elki.algorithm;

import elki.AbstractAlgorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;

/**
 * Dummy algorithm, which just iterates over all points once, doing a 10NN query
 * each. Useful in testing e.g. index structures and as template for custom
 * algorithms. While this algorithm doesn't produce a result, it
 * still performs rather expensive operations. If you are looking for an
 * algorithm that does <em>nothing</em>,
 * you must use {@link elki.algorithm.NullAlgorithm
 * NullAlgorithm} instead.
 * 
 * @author Erich Schubert
 * @since 0.2
 * @param <O> Vector type
 * 
 * @assoc - - - KNNQuery
 */
@Title("Dummy Algorithm")
@Description("The algorithm executes an Euclidean 10NN query on all data points, and can be used in unit testing")
@Priority(Priority.SUPPLEMENTARY)
public class DummyAlgorithm<O extends NumberVector> extends AbstractAlgorithm<Void> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(DummyAlgorithm.class);

  /**
   * Constructor.
   */
  public DummyAlgorithm() {
    super();
  }

  /**
   * Run the algorithm.
   * 
   * @param database Database
   * @param relation Relation
   * @return Null result
   */
  public Void run(Database database, Relation<O> relation) {
    // Get a distance and knn query for the Euclidean distance
    // Hardcoded, only use this if you only allow the eucliden distance
    DistanceQuery<O> distQuery = database.getDistanceQuery(relation, EuclideanDistance.STATIC);
    KNNQuery<O> knnQuery = database.getKNNQuery(distQuery, 10);

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // Get the actual object from the database (but discard the result)
      relation.get(iditer);
      // run a 10NN query for each point (but discard the result)
      knnQuery.getKNNForDBID(iditer, 10);
    }
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(EuclideanDistance.STATIC.getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
