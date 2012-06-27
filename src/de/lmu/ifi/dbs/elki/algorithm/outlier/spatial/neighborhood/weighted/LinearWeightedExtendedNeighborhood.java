package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.weighted;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;

/**
 * Neighborhood obtained by computing the k-fold closure of an existing
 * neighborhood. Objects are weighted linearly by their distance: the object
 * itself has a weight of 1 and this decreases linearly to 1/(n+1) for the
 * nth-step neighbors.
 * 
 * TODO: make actual weighting parameterizable?
 * 
 * @author Erich Schubert
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
  public Collection<DoubleObjPair<DBID>> getWeightedNeighbors(DBID reference) {
    ModifiableDBIDs seen = DBIDUtil.newHashSet();
    List<DoubleObjPair<DBID>> result = new ArrayList<DoubleObjPair<DBID>>();

    // Add starting object
    result.add(new DoubleObjPair<DBID>(computeWeight(0), reference));
    seen.add(reference);
    // Extend.
    DBIDs cur = reference;
    for(int i = 1; i <= steps; i++) {
      final double weight = computeWeight(i);
      // Collect newly discovered IDs
      ModifiableDBIDs add = DBIDUtil.newHashSet();
      for(DBIDIter iter = cur.iter(); iter.valid(); iter.advance()) {
        DBID id = iter.getDBID();
        for(DBIDIter iter2 = inner.getNeighborDBIDs(id).iter(); iter2.valid(); iter2.advance()) {
          DBID nid = iter2.getDBID();
          // Seen before?
          if(seen.contains(nid)) {
            continue;
          }
          add.add(nid);
          result.add(new DoubleObjPair<DBID>(weight, nid));
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
   * @apiviz.stereotype factory
   * @apiviz.has LinearWeightedExtendedNeighborhood oneway - - «produces»
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
    public LinearWeightedExtendedNeighborhood instantiate(Relation<? extends O> database) {
      return new LinearWeightedExtendedNeighborhood(inner.instantiate(database), steps);
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
     * @apiviz.exclude
     */
    public static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * Parameter to specify the neighborhood predicate to use.
       */
      public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("extendedneighbors.neighborhood", "The inner neighborhood predicate to use.");

      /**
       * Parameter to specify the number of steps allowed
       */
      public static final OptionID STEPS_ID = OptionID.getOrCreateOptionID("extendedneighbors.steps", "The number of steps allowed in the neighborhood graph.");

      /**
       * The number of steps to do.
       */
      private int steps;

      /**
       * Inner neighbor set predicate
       */
      private NeighborSetPredicate.Factory<O> inner;

      /**
       * Inner neighborhood parameter.
       * 
       * @param config Parameterization
       * @return Inner neighborhood.
       */
      protected static <O> NeighborSetPredicate.Factory<O> getParameterInnerNeighborhood(Parameterization config) {
        final ObjectParameter<NeighborSetPredicate.Factory<O>> param = new ObjectParameter<NeighborSetPredicate.Factory<O>>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class);
        if(config.grab(param)) {
          return param.instantiateClass(config);
        }
        return null;
      }

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        inner = getParameterInnerNeighborhood(config);
        steps = getParameterSteps(config);
      }

      /**
       * Get the number of steps to do in the neighborhood graph.
       * 
       * @param config Parameterization
       * @return number of steps, default 1
       */
      public static int getParameterSteps(Parameterization config) {
        final IntParameter param = new IntParameter(STEPS_ID, new GreaterEqualConstraint(1));
        if(config.grab(param)) {
          return param.getValue();
        }
        return 1;
      }

      @Override
      protected LinearWeightedExtendedNeighborhood.Factory<O> makeInstance() {
        return new LinearWeightedExtendedNeighborhood.Factory<O>(inner, steps);
      }
    }
  }
}