package de.lmu.ifi.dbs.elki.database.query.range;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;

/**
 * Abstract base class for range queries that use a distance query in their
 * instance
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */
public abstract class AbstractDistanceRangeQuery<O> extends AbstractDataBasedQuery<O> implements RangeQuery<O> {
  /**
   * Hold the distance function to be used.
   */
  protected DistanceQuery<O> distanceQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance query
   */
  public AbstractDistanceRangeQuery(DistanceQuery<O> distanceQuery) {
    super(distanceQuery.getRelation());
    this.distanceQuery = distanceQuery;
  }

  @Override
  public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
    return getRangeForObject(relation.get(id), range);
  }

  @Override
  abstract public DoubleDBIDList getRangeForObject(O obj, double range);
}
