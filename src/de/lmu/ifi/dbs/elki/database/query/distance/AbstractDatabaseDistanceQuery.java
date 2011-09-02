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
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public abstract class AbstractDatabaseDistanceQuery<O, D extends Distance<D>> extends AbstractDistanceQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param relation Relation to use.
   */
  public AbstractDatabaseDistanceQuery(Relation<? extends O> relation) {
    super(relation);
  }

  @Override
  public D distance(O o1, DBID id2) {
    if(o1 instanceof DBID) {
      return distance((DBID) o1, id2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

  @Override
  public D distance(DBID id1, O o2) {
    if(o2 instanceof DBID) {
      return distance(id1, (DBID) o2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

  @Override
  public D distance(O o1, O o2) {
    if(o1 instanceof DBID && o2 instanceof DBID) {
      return distance((DBID) o1, (DBID) o2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

}