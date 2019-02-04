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
package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.weighted;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Adapter to use unweighted neighborhoods in an algorithm that requires
 * weighted neighborhoods.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class UnweightedNeighborhoodAdapter implements WeightedNeighborSetPredicate {
  /**
   * Actual predicate
   */
  NeighborSetPredicate inner;

  /**
   * Constructor.
   * 
   * @param inner Actual neighborhood
   */
  public UnweightedNeighborhoodAdapter(NeighborSetPredicate inner) {
    super();
    this.inner = inner;
  }

  @Override
  public Collection<DoubleDBIDPair> getWeightedNeighbors(DBIDRef reference) {
    DBIDs neighbors = inner.getNeighborDBIDs(reference);
    ArrayList<DoubleDBIDPair> adapted = new ArrayList<>(neighbors.size());
    for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
      adapted.add(DBIDUtil.newPair(1.0, iter));
    }
    return adapted;
  }

  /**
   * Factory class
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @has - - - UnweightedNeighborhoodAdapter
   * 
   * @param <O> Input object type
   */
  public static class Factory<O> implements WeightedNeighborSetPredicate.Factory<O> {
    /**
     * The inner predicate factory
     */
    NeighborSetPredicate.Factory<O> inner;

    /**
     * Constructor.
     * 
     * @param inner Actual (unweighted) predicate
     */
    public Factory(NeighborSetPredicate.Factory<O> inner) {
      super();
      this.inner = inner;
    }

    @Override
    public UnweightedNeighborhoodAdapter instantiate(Database database, Relation<? extends O> relation) {
      return new UnweightedNeighborhoodAdapter(inner.instantiate(database, relation));
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return inner.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @hidden
     * 
     * @param <O> Input object type
     */
    public static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * The parameter to give the non-weighted neighborhood to use.
       */
      public static final OptionID INNER_ID = new OptionID("neighborhood.inner", "Parameter for the non-weighted neighborhood to use.");

      /**
       * The actual predicate.
       */
      NeighborSetPredicate.Factory<O> inner;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<NeighborSetPredicate.Factory<O>> innerP = new ObjectParameter<>(INNER_ID, NeighborSetPredicate.Factory.class);
        if(config.grab(innerP)) {
          inner = innerP.instantiateClass(config);
        }
      }

      @Override
      protected UnweightedNeighborhoodAdapter.Factory<O> makeInstance() {
        return new UnweightedNeighborhoodAdapter.Factory<>(inner);
      }
    }
  }
}