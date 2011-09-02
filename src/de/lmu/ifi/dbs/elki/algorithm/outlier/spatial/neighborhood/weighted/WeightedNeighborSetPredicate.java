package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.weighted;

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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

/**
 * Neighbor predicate with weight support.
 * 
 * @author Erich Schubert
 */
public interface WeightedNeighborSetPredicate {
  /**
   * Get the neighbors of a reference object for DBSCAN.
   * 
   * @param reference Reference object
   * @return Weighted Neighborhood
   */
  public Collection<DoubleObjPair<DBID>> getWeightedNeighbors(DBID reference);

  /**
   * Factory interface to produce instances.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has WeightedNeighborSetPredicate
   * 
   * @param <O> Input relation object type restriction
   */
  public static interface Factory<O> extends Parameterizable {
    /**
     * Instantiation method.
     * 
     * @param relation Relation to instantiate for.
     * 
     * @return instance
     */
    public WeightedNeighborSetPredicate instantiate(Relation<? extends O> relation);

    /**
     * Get the input type information
     * 
     * @return input type
     */
    public TypeInformation getInputTypeRestriction();
  }
}