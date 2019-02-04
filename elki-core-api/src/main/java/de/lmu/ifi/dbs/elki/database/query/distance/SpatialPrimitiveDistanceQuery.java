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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;

/**
 * Distance query for spatial distance functions
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - SpatialPrimitiveDistanceFunction
 * 
 * @param <V> Vector type to use
 */
public class SpatialPrimitiveDistanceQuery<V extends SpatialComparable> extends PrimitiveDistanceQuery<V> implements SpatialDistanceQuery<V> {
  /**
   * The distance function we use.
   */
  final protected SpatialPrimitiveDistanceFunction<? super V> distanceFunction;
  
  /**
   * @param relation Representation to use
   * @param distanceFunction Distance function to use
   */
  public SpatialPrimitiveDistanceQuery(Relation<? extends V> relation, SpatialPrimitiveDistanceFunction<? super V> distanceFunction) {
    super(relation, distanceFunction);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public double minDist(SpatialComparable mbr, V v) {
    return distanceFunction.minDist(mbr, v);
  }

  @Override
  public double minDist(SpatialComparable mbr, DBIDRef id) {
    return distanceFunction.minDist(mbr, relation.get(id));
  }

  @Override
  public SpatialPrimitiveDistanceFunction<? super V> getDistanceFunction() {
    return distanceFunction;
  }
}