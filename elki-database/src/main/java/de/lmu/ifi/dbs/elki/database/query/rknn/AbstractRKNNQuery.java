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
package de.lmu.ifi.dbs.elki.database.query.rknn;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Instance for the query on a particular database.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public abstract class AbstractRKNNQuery<O> implements RKNNQuery<O> {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * Hold the distance function to be used.
   */
  final protected DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery distance query
   */
  public AbstractRKNNQuery(DistanceQuery<O> distanceQuery) {
    super();
    this.relation = distanceQuery.getRelation();
    this.distanceQuery = distanceQuery;
  }

  @Override
  abstract public DoubleDBIDList getRKNNForDBID(DBIDRef id, int k);
}