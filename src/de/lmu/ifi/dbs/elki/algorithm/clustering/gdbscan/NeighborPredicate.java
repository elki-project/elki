package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;

/**
 * Get the neighbors of an object
 * 
 * Note the Factory/Instance split of this interface.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has Instance
 */
public interface NeighborPredicate {
  /**
   * Instantiate for a database.
   * 
   * @param database Database to instantiate for
   * @return Instance
   */
  public <T> Instance<T> instantiate(Database database, SimpleTypeInformation<?> type);

  /**
   * Input data type restriction.
   * 
   * @return Type restriction
   */
  public TypeInformation getInputTypeRestriction();

  /**
   * Output data type information.
   * 
   * @return Type information
   */
  public SimpleTypeInformation<?>[] getOutputType();

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static interface Instance<T> {
    /**
     * Get the neighbors of a reference object for DBSCAN.
     * 
     * @param reference Reference object
     * @return Neighborhood
     */
    public T getNeighbors(DBIDRef reference);

    /**
     * Get the IDs the predicate is defined for.
     * 
     * @return Database ids
     */
    public DBIDs getIDs();
    
    /**
     * Add the neighbors to a DBID set
     * 
     * @param ids ID set
     * @param neighbors Neighbors to add
     */
    public void addDBIDs(ModifiableDBIDs ids, T neighbors);
  }
}