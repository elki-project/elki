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
import elki.database.QueryUtil;
import elki.database.datastore.DataStore;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Neighborhoods based on k nearest neighbors.
 * 
 * @author Ahmed Hettab
 * @since 0.4.0
 */
public class PrecomputedKNearestNeighborNeighborhood extends AbstractPrecomputedNeighborhood {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(PrecomputedKNearestNeighborNeighborhood.class);

  /**
   * Constructor.
   * 
   * @param store Store to access
   */
  public PrecomputedKNearestNeighborNeighborhood(DataStore<DBIDs> store) {
    super(store);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Factory class to instantiate for a particular relation.
   * 
   * @author Ahmed Hettab
   * 
   * @stereotype factory
   * @has - - - PrecomputedKNearestNeighborNeighborhood
   * 
   * @param <O> Object type
   */
  public static class Factory<O> implements NeighborSetPredicate.Factory<O> {
    /**
     * parameter k
     */
    private int k;

    /**
     * distance function to use
     */
    private Distance<? super O> distFunc;

    /**
     * Factory Constructor
     */
    public Factory(int k, Distance<? super O> distFunc) {
      super();
      this.k = k;
      this.distFunc = distFunc;
    }

    @Override
    public NeighborSetPredicate instantiate(Database database, Relation<? extends O> relation) {
      KNNQuery<?> knnQuery = QueryUtil.getKNNQuery(relation, distFunc);

      // TODO: use bulk?
      WritableDataStore<DBIDs> s = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, DBIDs.class);
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        KNNList neighbors = knnQuery.getKNNForDBID(iditer, k);
        ArrayModifiableDBIDs neighbours = DBIDUtil.newArray(neighbors.size());
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          neighbours.add(neighbor);
        }
        s.put(iditer, neighbours);
      }
      return new PrecomputedKNearestNeighborNeighborhood(s);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distFunc.getInputTypeRestriction();
    }

    /**
     * Parameterization class
     * 
     * @author Erich Schubert
     * 
     * @hidden
     * 
     * @param <O> Object type
     */
    public static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * Parameter k
       */
      public static final OptionID K_ID = new OptionID("neighborhood.k", "the number of neighbors");

      /**
       * Parameter to specify the distance function to use
       */
      public static final OptionID DISTANCEFUNCTION_ID = new OptionID("neighborhood.distancefunction", "the distance function to use");

      /**
       * Parameter k
       */
      int k;

      /**
       * Distance function
       */
      Distance<? super O> distFunc;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter kP = new IntParameter(K_ID);
        if(config.grab(kP)) {
          k = kP.getValue();
        }
        final ObjectParameter<Distance<? super O>> distP = new ObjectParameter<>(DISTANCEFUNCTION_ID, Distance.class);
        if(config.grab(distP)) {
          distFunc = distP.instantiateClass(config);
        }
      }

      @Override
      protected PrecomputedKNearestNeighborNeighborhood.Factory<O> makeInstance() {
        return new PrecomputedKNearestNeighborNeighborhood.Factory<>(k, distFunc);
      }
    }
  }
}