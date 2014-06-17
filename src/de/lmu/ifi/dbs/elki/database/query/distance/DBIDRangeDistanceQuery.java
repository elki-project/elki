package de.lmu.ifi.dbs.elki.database.query.distance;

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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DBIDDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DBIDRangeDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Run a distance query based on DBIDRanges
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses DBIDRangeDistanceFunction
 */
public class DBIDRangeDistanceQuery extends DBIDDistanceQuery {
  /**
   * The distance function we use.
   */
  final protected DBIDRangeDistanceFunction distanceFunction;

  /**
   * The DBID range we are accessing.
   */
  final protected DBIDRange range;

  /**
   * Constructor.
   * 
   * @param relation Database to use.
   * @param distanceFunction Our distance function
   */
  public DBIDRangeDistanceQuery(Relation<DBID> relation, DBIDRangeDistanceFunction distanceFunction) {
    super(relation, distanceFunction);
    DBIDs ids = relation.getDBIDs();
    if(!(ids instanceof DBIDRange)) {
      throw new AbortException("This distance function can only be used with DBID ranges / static databases.");
    }
    this.range = (DBIDRange) ids;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public double distance(DBIDRef id1, DBIDRef id2) {
    return distanceFunction.distance(range.getOffset(id1), range.getOffset(id2));
  }

  @Override
  public DBIDDistanceFunction getDistanceFunction() {
    return distanceFunction;
  }
}