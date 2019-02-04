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
package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
  public String getLongName() {
    return "K Nearest Neighbors Neighborhood";
  }

  @Override
  public String getShortName() {
    return "knn-neighborhood";
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
    private DistanceFunction<? super O> distFunc;

    /**
     * Factory Constructor
     */
    public Factory(int k, DistanceFunction<? super O> distFunc) {
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
      DistanceFunction<? super O> distFunc;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter kP = new IntParameter(K_ID);
        if(config.grab(kP)) {
          k = kP.getValue();
        }
        final ObjectParameter<DistanceFunction<? super O>> distP = new ObjectParameter<>(DISTANCEFUNCTION_ID, DistanceFunction.class);
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