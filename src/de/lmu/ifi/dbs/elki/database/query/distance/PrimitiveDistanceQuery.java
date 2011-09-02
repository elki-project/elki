package de.lmu.ifi.dbs.elki.database.query.distance;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses PrimitiveDistanceFunction
 * 
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public class PrimitiveDistanceQuery<O, D extends Distance<D>> extends AbstractDistanceQuery<O, D> {
  /**
   * The distance function we use.
   */
  final protected PrimitiveDistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param relation Representation to use.
   * @param distanceFunction Our distance function
   */
  public PrimitiveDistanceQuery(Relation<? extends O> relation, PrimitiveDistanceFunction<? super O, D> distanceFunction) {
    super(relation);
    this.distanceFunction = distanceFunction;
  }

  @Override
  public D distance(DBID id1, DBID id2) {
    O o1 = relation.get(id1);
    O o2 = relation.get(id2);
    return distance(o1, o2);
  }

  @Override
  public D distance(O o1, DBID id2) {
    O o2 = relation.get(id2);
    return distance(o1, o2);
  }

  @Override
  public D distance(DBID id1, O o2) {
    O o1 = relation.get(id1);
    return distance(o1, o2);
  }

  @Override
  public D distance(O o1, O o2) {
    if(o1 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    if(o2 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    return distanceFunction.distance(o1, o2);
  }

  @Override
  public PrimitiveDistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }
}