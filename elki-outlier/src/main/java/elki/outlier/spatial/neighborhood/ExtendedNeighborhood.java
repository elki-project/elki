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
package elki.outlier.spatial.neighborhood;

import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.datastore.DataStore;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Neighborhood obtained by computing the k-fold closure of an existing
 * neighborhood.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class ExtendedNeighborhood extends AbstractPrecomputedNeighborhood {
  /**
   * The logger to use.
   */
  private static final Logging LOG = Logging.getLogger(ExtendedNeighborhood.class);

  /**
   * Constructor.
   * 
   * @param store The materialized data.
   */
  public ExtendedNeighborhood(DataStore<DBIDs> store) {
    super(store);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @stereotype factory
   * @navhas - produces - ExtendedNeighborhood
   */
  public static class Factory<O> extends AbstractPrecomputedNeighborhood.Factory<O> {
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
    public NeighborSetPredicate instantiate(Database database, Relation<? extends O> relation) {
      DataStore<DBIDs> store = extendNeighborhood(database, relation);
      ExtendedNeighborhood neighborhood = new ExtendedNeighborhood(store);
      return neighborhood;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return inner.getInputTypeRestriction();
    }

    /**
     * Method to load the external neighbors.
     */
    private DataStore<DBIDs> extendNeighborhood(Database database, Relation<? extends O> relation) {
      NeighborSetPredicate innerinst = inner.instantiate(database, relation);

      final WritableDataStore<DBIDs> store = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);

      // Expand multiple steps
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Expanding neighborhoods", relation.size(), LOG) : null;
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        HashSetModifiableDBIDs res = DBIDUtil.newHashSet();
        res.add(iter);
        DBIDs todo = DBIDUtil.deref(iter);
        for(int i = 0; i < steps; i++) {
          ModifiableDBIDs ntodo = DBIDUtil.newHashSet();
          for(DBIDIter iter2 = todo.iter(); iter2.valid(); iter2.advance()) {
            DBIDs add = innerinst.getNeighborDBIDs(iter2);
            if(add != null) {
              for(DBIDIter iter3 = add.iter(); iter3.valid(); iter3.advance()) {
                if(res.contains(iter3)) {
                  continue;
                }
                ntodo.add(iter3);
                res.add(iter3);
              }
            }
          }
          if(ntodo.size() == 0) {
            continue;
          }
          todo = ntodo;
        }
        store.put(iter, res);
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);

      return store;
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
      public ExtendedNeighborhood.Factory<O> make() {
        return new ExtendedNeighborhood.Factory<>(inner, steps);
      }
    }
  }
}
