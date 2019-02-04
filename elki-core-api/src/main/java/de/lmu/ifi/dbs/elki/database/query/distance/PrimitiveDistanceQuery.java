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
package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @assoc - - - PrimitiveDistanceFunction
 * 
 * @param <O> Database object type.
 */
public class PrimitiveDistanceQuery<O> implements DistanceQuery<O> {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * The distance function we use.
   */
  final protected PrimitiveDistanceFunction<? super O> distanceFunction;

  /**
   * Constructor.
   * 
   * @param relation Representation to use.
   * @param distanceFunction Our distance function
   */
  public PrimitiveDistanceQuery(Relation<? extends O> relation, PrimitiveDistanceFunction<? super O> distanceFunction) {
    super();
    this.relation = relation;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public final double distance(DBIDRef id1, DBIDRef id2) {
    return distance(relation.get(id1), relation.get(id2));
  }

  @Override
  public final double distance(O o1, DBIDRef id2) {
    return distance(o1, relation.get(id2));
  }

  @Override
  public final double distance(DBIDRef id1, O o2) {
    return distance(relation.get(id1), o2);
  }

  @Override
  public double distance(O o1, O o2) {
    return distanceFunction.distance(o1, o2);
  }

  @Override
  public Relation<? extends O> getRelation() {
    return relation;
  }

  @Override
  public PrimitiveDistanceFunction<? super O> getDistanceFunction() {
    return distanceFunction;
  }
}
