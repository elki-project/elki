/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.outlier.spatial.neighborhood.weighted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.outlier.spatial.neighborhood.NeighborSetPredicate;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Neighborhood obtained by computing the k-fold closure of an existing
 * neighborhood. Objects are weighted linearly by their distance: the object
 * itself has a weight of 1 and this decreases linearly to 1/(n+1) for the
 * nth-step neighbors.
 * <p>
 * TODO: make actual weighting parameterizable?
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class LinearWeightedExtendedNeighborhood implements WeightedNeighborSetPredicate {
  /**
   * The data store to use
   */
  private NeighborSetPredicate inner;

  /**
   * The number of steps to extend to.
   */
  private int steps;

  /**
   * Constructor.
   * 
   * @param inner Inner neighborhood
   * @param steps Number of steps to expand
   */
  public LinearWeightedExtendedNeighborhood(NeighborSetPredicate inner, int steps) {
    super();
    this.inner = inner;
    this.steps = steps;
  }

  /**
   * Compute the weight from the number of steps needed.
   * 
   * @param tsteps steps to target
   * @return weight
   */
  private double computeWeight(int tsteps) {
    return 1.0 - (tsteps / (float) (steps + 1));
  }

  @Override
  public Collection<DoubleDBIDPair> getWeightedNeighbors(DBIDRef reference) {
    ModifiableDBIDs seen = DBIDUtil.newHashSet();
    List<DoubleDBIDPair> result = new ArrayList<>();

    // Add starting object
    result.add(DBIDUtil.newPair(computeWeight(0), reference));
    seen.add(reference);
    // Extend.
    DBIDs cur = DBIDUtil.deref(reference);
    for(int i = 1; i <= steps; i++) {
      final double weight = computeWeight(i);
      // Collect newly discovered IDs
      ModifiableDBIDs add = DBIDUtil.newHashSet();
      for(DBIDIter iter = cur.iter(); iter.valid(); iter.advance()) {
        for(DBIDIter iter2 = inner.getNeighborDBIDs(iter).iter(); iter2.valid(); iter2.advance()) {
          // Seen before?
          if(seen.contains(iter2)) {
            continue;
          }
          add.add(iter2);
          result.add(DBIDUtil.newPair(weight, iter2));
        }
      }
      if(add.size() == 0) {
        break;
      }
      cur = add;
    }
    return result;
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @navhas - produces - LinearWeightedExtendedNeighborhood
   */
  public static class Factory<O> implements WeightedNeighborSetPredicate.Factory<O> {
    /**
     * Inner neighbor set predicate
     */
    private NeighborSetPredicate.Factory<O> inner;

    /**
     * Number of steps to do
     */
    private int steps;

    /**
     * Constructor.
     * 
     * @param inner Inner neighbor set predicate
     * @param steps Number of steps to do
     */
    public Factory(NeighborSetPredicate.Factory<O> inner, int steps) {
      super();
      this.inner = inner;
      this.steps = steps;
    }

    @Override
    public LinearWeightedExtendedNeighborhood instantiate(Database database, Relation<? extends O> relation) {
      return new LinearWeightedExtendedNeighborhood(inner.instantiate(database, relation), steps);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return inner.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Par<O> implements Parameterizer {
      /**
       * Parameter to specify the neighborhood predicate to use.
       */
      public static final OptionID NEIGHBORHOOD_ID = new OptionID("extendedneighbors.neighborhood", "The inner neighborhood predicate to use.");

      /**
       * Parameter to specify the number of steps allowed
       */
      public static final OptionID STEPS_ID = new OptionID("extendedneighbors.steps", "The number of steps allowed in the neighborhood graph.");

      /**
       * The number of steps to do.
       */
      private int steps;

      /**
       * Inner neighbor set predicate
       */
      private NeighborSetPredicate.Factory<O> inner;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<NeighborSetPredicate.Factory<O>>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class) //
            .grab(config, x -> inner = x);
        new IntParameter(STEPS_ID, 1) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> steps = x);
      }

      @Override
      public LinearWeightedExtendedNeighborhood.Factory<O> make() {
        return new LinearWeightedExtendedNeighborhood.Factory<>(inner, steps);
      }
    }
  }
}
