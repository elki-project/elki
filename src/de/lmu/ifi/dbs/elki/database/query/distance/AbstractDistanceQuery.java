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
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * A distance query serves as adapter layer for database and primitive
 * distances.
 * 
 * @author Erich Schubert
 * 
 * @param O Input object type
 * @param D Distance result type
 */
public abstract class AbstractDistanceQuery<O, D extends Distance<D>> extends AbstractDataBasedQuery<O> implements DistanceQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param relation Relation to use.
   */
  public AbstractDistanceQuery(Relation<? extends O> relation) {
    super(relation);
  }

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(DBID id1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param id2 second object id
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(O o1, DBID id2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param id1 first object id
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(DBID id1, O o2);

  /**
   * Returns the distance between the two objects specified by their object ids.
   * 
   * @param o1 first object
   * @param o2 second object
   * @return the distance between the two objects specified by their object ids
   */
  @Override
  public abstract D distance(O o1, O o2);

  @Override
  public D getDistanceFactory() {
    return getDistanceFunction().getDistanceFactory();
  }

  /**
   * Provides an infinite distance.
   * 
   * @return an infinite distance
   */
  @Override
  public D infiniteDistance() {
    return getDistanceFunction().getDistanceFactory().infiniteDistance();
  }

  /**
   * Provides a null distance.
   * 
   * @return a null distance
   */
  @Override
  public D nullDistance() {
    return getDistanceFunction().getDistanceFactory().nullDistance();
  }

  /**
   * Provides an undefined distance.
   * 
   * @return an undefined distance
   */
  @Override
  public D undefinedDistance() {
    return getDistanceFunction().getDistanceFactory().undefinedDistance();
  }
}