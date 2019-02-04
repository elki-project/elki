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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DBIDRangeDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for distance functions that rely on integer offsets
 * within a consecutive range. This is beneficial for external distances.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractDBIDRangeDistanceFunction extends AbstractDatabaseDistanceFunction<DBID> implements DBIDRangeDistanceFunction {
  @Override
  public double distance(DBIDRef o1, DBIDRef o2) {
    throw new AbortException("This must be called via a distance query to determine the DBID offset, not directly.");
  }

  @Override
  public SimpleTypeInformation<DBID> getInputTypeRestriction() {
    return TypeUtil.DBID;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <O extends DBID> DistanceQuery<O> instantiate(Relation<O> database) {
    return (DistanceQuery<O>) new DBIDRangeDistanceQuery((Relation<DBID>) database, this);
  }
}
