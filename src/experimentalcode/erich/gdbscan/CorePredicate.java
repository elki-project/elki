package experimentalcode.erich.gdbscan;

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

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Predicate for GeneralizedDBSCAN to evaluate whether a point is a core point
 * or not.
 * 
 * Note the Factory/Instance split of this interface.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Instance
 */
public interface CorePredicate {
  /**
   * Instantiate for a database.
   * 
   * @param database Database to instantiate for
   * @return Instance
   */
  public Instance instantiate(Database database);
  
  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static interface Instance {
    /**
     * Decide whether the point is a core point, based on its neighborhood.
     * 
     * @param point Query point
     * @param neighbors Neighbors
     * @return core point property
     */
    public boolean isCorePoint(DBID point, DBIDs neighbors);
  }
}