package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood;
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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Neighborhood obtained by computing the k-fold closure of an existing
 * neighborhood.
 * 
 * @author Erich Schubert
 */
public class ExtendedNeighborhood extends AbstractPrecomputedNeighborhood {
  /**
   * The logger to use.
   */
  private static final Logging logger = Logging.getLogger(ExtendedNeighborhood.class);

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
    return logger;
  }

  @Override
  public String getLongName() {
    return "Extended Neighborhood";
  }

  @Override
  public String getShortName() {
    return "extended-neighborhood";
  }

  /**
   * Factory class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has ExtendedNeighborhood oneway - - «produces»
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
    public NeighborSetPredicate instantiate(Relation<? extends O> database) {
      DataStore<DBIDs> store = extendNeighborhood(database);
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
    private DataStore<DBIDs> extendNeighborhood(Relation<? extends O> database) {
      NeighborSetPredicate innerinst = inner.instantiate(database);

      final WritableDataStore<DBIDs> store = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);

      // Expand multiple steps
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Expanding neighborhoods", database.size(), logger) : null;
      for(final DBID id : database.iterDBIDs()) {
        ModifiableDBIDs res = DBIDUtil.newHashSet(id);
        DBIDs todo = id;
        for(int i = 0; i < steps; i++) {
          ModifiableDBIDs ntodo = DBIDUtil.newHashSet();
          for(final DBID oid : todo) {
            DBIDs add = innerinst.getNeighborDBIDs(oid);
            if(add != null) {
              for (DBID nid: add) {
                if (res.contains(add)) {
                  continue;
                }
                ntodo.add(nid);
                res.add(nid);
              }
            }
          }
          if (ntodo.size() == 0) {
            continue;
          }
          todo = ntodo;
        }
        store.put(id, res);
        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
      if(progress != null) {
        progress.ensureCompleted(logger);
      }

      return store;
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
      protected ExtendedNeighborhood.Factory<O> makeInstance() {
        return new ExtendedNeighborhood.Factory<O>(inner, steps);
      }
    }
  }
}