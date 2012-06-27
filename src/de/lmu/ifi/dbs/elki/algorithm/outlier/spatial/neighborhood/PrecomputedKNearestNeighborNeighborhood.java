package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood;
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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
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
 * 
 * @param <D> Distance to use
 */
public class PrecomputedKNearestNeighborNeighborhood<D extends Distance<D>> extends AbstractPrecomputedNeighborhood {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(PrecomputedKNearestNeighborNeighborhood.class);

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
    return logger;
  }

  /**
   * Factory class to instantiate for a particular relation.
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.stereotype factory
   * @apiviz.has PrecomputedKNearestNeighborNeighborhood
   *
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static class Factory<O, D extends Distance<D>> implements NeighborSetPredicate.Factory<O> {
    /**
     * parameter k
     */
    private int k;

    /**
     * distance function to use
     */
    private DistanceFunction<? super O, D> distFunc;

    /**
     * Factory Constructor
     */
    public Factory(int k, DistanceFunction<? super O, D> distFunc) {
      super();
      this.k = k;
      this.distFunc = distFunc;
    }

    @Override
    public NeighborSetPredicate instantiate(Relation<? extends O> relation) {
      KNNQuery<?, D> knnQuery = QueryUtil.getKNNQuery(relation, distFunc);

      // TODO: use bulk?
      WritableDataStore<DBIDs> s = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, DBIDs.class);
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        DBID id  = iditer.getDBID();
        KNNResult<D> neighbors = knnQuery.getKNNForDBID(id, k);
        ArrayModifiableDBIDs neighbours = DBIDUtil.newArray(neighbors.size());
        for(DistanceResultPair<D> dpair : neighbors) {
          neighbours.add(dpair.getDBID());
        }
        s.put(id, neighbours);
      }
      return new PrecomputedKNearestNeighborNeighborhood<D>(s);
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
     * @apiviz.exclude
     * 
     * @param <O> Object type
     * @param <D> Distance type
     */
    public static class Parameterizer<O, D extends Distance<D>> extends AbstractParameterizer {
      /**
       * Parameter k
       */
      public static final OptionID K_ID = OptionID.getOrCreateOptionID("neighborhood.k", "the number of neighbors");

      /**
       * Parameter to specify the distance function to use
       */
      public static final OptionID DISTANCEFUNCTION_ID = OptionID.getOrCreateOptionID("neighborhood.distancefunction", "the distance function to use");

      /**
       * Parameter k
       */
      int k;

      /**
       * Distance function
       */
      DistanceFunction<? super O, D> distFunc;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        final IntParameter kP = new IntParameter(K_ID);
        if(config.grab(kP)) {
          k = kP.getValue();
        }
        final ObjectParameter<DistanceFunction<? super O, D>> distP = new ObjectParameter<DistanceFunction<? super O, D>>(DISTANCEFUNCTION_ID, DistanceFunction.class);
        if(config.grab(distP)) {
          distFunc = distP.instantiateClass(config);
        }
      }

      @Override
      protected PrecomputedKNearestNeighborNeighborhood.Factory<O, D> makeInstance() {
        return new PrecomputedKNearestNeighborNeighborhood.Factory<O, D>(k, distFunc);
      }
    }
  }
}