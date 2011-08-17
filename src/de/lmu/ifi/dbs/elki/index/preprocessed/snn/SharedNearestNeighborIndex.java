package de.lmu.ifi.dbs.elki.index.preprocessed.snn;
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
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;

/**
 * Interface for an index providing nearest neighbor sets.
 * 
 * @author Erich Schubert
 */
public interface SharedNearestNeighborIndex<O> extends Index {
  /**
   * Get the precomputed nearest neighbors
   * 
   * @param objid Object ID
   * @return Neighbor DBIDs
   */
  public TreeSetDBIDs getNearestNeighborSet(DBID objid);

  /**
   * Get the number of neighbors
   * 
   * @return NN size
   */
  public int getNumberOfNeighbors();

  /**
   * Factory interface
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses SharedNearestNeighborIndex oneway - - «create»
   * 
   * @param <O> The input object type
   * @param <I> Index type produced
   */
  public static interface Factory<O, I extends SharedNearestNeighborIndex<O>> extends IndexFactory<O, I> {
    /**
     * Instantiate the index for a given database.
     * 
     * @param database Database type
     * 
     * @return Index
     */
    @Override
    public I instantiate(Relation<O> database);

    /**
     * Get the number of neighbors
     * 
     * @return NN size
     */
    public int getNumberOfNeighbors();
  }
}