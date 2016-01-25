package de.lmu.ifi.dbs.elki.utilities.datastructures.unionfind;
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;

/**
 * Union-find algorithm factory, to choose the best implementation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class UnionFindUtil {
  /**
   * Make a new instance (automatically choosing the best implementation).
   *
   * @param ids ID set
   * @return Union find algorithm
   */
  public static UnionFind make(StaticDBIDs ids) {
    if(ids instanceof DBIDRange) {
      return new WeightedQuickUnionRangeDBIDs((DBIDRange) ids);
    }
    return new WeightedQuickUnionStaticDBIDs(ids);
  }
}
