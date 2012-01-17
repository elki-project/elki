package de.lmu.ifi.dbs.elki.database.query.distance;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Distance query for spatial distance functions
 * @author Erich Schubert
 *
 * @apiviz.uses SpatialPrimitiveDistanceFunction
 * 
 * @param <V> Vector type to use
 * @param <D> Distance result type
 */
public class SpatialPrimitiveDistanceQuery<V extends SpatialComparable, D extends Distance<D>> extends PrimitiveDistanceQuery<V, D> implements SpatialDistanceQuery<V, D> {
  /**
   * The distance function we use.
   */
  final protected SpatialPrimitiveDistanceFunction<? super V, D> distanceFunction;
  
  /**
   * @param relation Representation to use
   * @param distanceFunction Distance function to use
   */
  public SpatialPrimitiveDistanceQuery(Relation<? extends V> relation, SpatialPrimitiveDistanceFunction<? super V, D> distanceFunction) {
    super(relation, distanceFunction);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public D minDist(SpatialComparable mbr, V v) {
    return distanceFunction.minDist(mbr, v);
  }

  @Override
  public D minDist(SpatialComparable mbr, DBID id) {
    return distanceFunction.minDist(mbr, relation.get(id));
  }

  @Override
  public SpatialPrimitiveDistanceFunction<? super V, D> getDistanceFunction() {
    return distanceFunction;
  }
}