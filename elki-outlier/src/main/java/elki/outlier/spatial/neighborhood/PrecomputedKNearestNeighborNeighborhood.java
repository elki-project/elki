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
package elki.outlier.spatial.neighborhood;

import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.datastore.DataStore;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.utilities.optionhandling.Parameterizer;
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
    private Distance<? super O> distance;

    /**
     * Factory Constructor
     */
    public Factory(int k, Distance<? super O> distance) {
      super();
      this.k = k;
      this.distance = distance;
    }

    @Override
    public NeighborSetPredicate instantiate(Database database, Relation<? extends O> relation) {
      KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, distance).kNNByDBID(k);
      WritableDataStore<DBIDs> s = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, DBIDs.class);
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        KNNList neighbors = knnQuery.getKNN(iditer, k);
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
      return distance.getInputTypeRestriction();
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
    public static class Par<O> implements Parameterizer {
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
      public void configure(Parameterization config) {
        new IntParameter(K_ID) //
            .grab(config, x -> k = x);
        new ObjectParameter<Distance<? super O>>(DISTANCEFUNCTION_ID, Distance.class) //
            .grab(config, x -> distFunc = x);
      }

      @Override
      public PrecomputedKNearestNeighborNeighborhood.Factory<O> make() {
        return new PrecomputedKNearestNeighborNeighborhood.Factory<>(k, distFunc);
      }
    }
  }
}
