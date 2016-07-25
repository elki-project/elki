package de.lmu.ifi.dbs.elki.database.query.range;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Abstract base class for range queries that use a distance query in their
 * instance
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Database object type
 */
public abstract class AbstractDistanceRangeQuery<O> implements RangeQuery<O> {
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
   * @param distanceQuery Distance query
   */
  public AbstractDistanceRangeQuery(DistanceQuery<O> distanceQuery) {
    super();
    this.relation = distanceQuery.getRelation();
    this.distanceQuery = distanceQuery;
  }

  @Override
  public DoubleDBIDList getRangeForDBID(DBIDRef id, double range) {
    ModifiableDoubleDBIDList ret = DBIDUtil.newDistanceDBIDList();
    getRangeForObject(relation.get(id), range, ret);
    ret.sort();
    return ret;
  }

  @Override
  public DoubleDBIDList getRangeForObject(O obj, double range) {
    ModifiableDoubleDBIDList ret = DBIDUtil.newDistanceDBIDList();
    getRangeForObject(obj, range, ret);
    ret.sort();
    return ret;
  }

  @Override
  public void getRangeForDBID(DBIDRef id, double range, ModifiableDoubleDBIDList neighbors) {
    getRangeForObject(relation.get(id), range, neighbors);
  }
}
