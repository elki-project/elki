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

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Database object type.
 */
public interface DatabaseDistanceQuery<O> extends DistanceQuery<O> {
  @Override
  default double distance(O o1, DBIDRef id2) {
    if(o1 instanceof DBIDRef) {
      return distance((DBIDRef) o1, id2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

  @Override
  default double distance(DBIDRef id1, O o2) {
    if(o2 instanceof DBIDRef) {
      return distance(id1, (DBIDRef) o2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

  @Override
  default double distance(O o1, O o2) {
    if(o1 instanceof DBIDRef && o2 instanceof DBIDRef) {
      return distance((DBIDRef) o1, (DBIDRef) o2);
    }
    throw new UnsupportedOperationException("This distance function is only defined for known DBIDs.");
  }

}
