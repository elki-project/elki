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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborJaccardDistanceFunction computes the Jaccard
 * coefficient, which is a proper distance metric.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @assoc - - - SharedNearestNeighborIndex.Factory
 * @navhas - create - SharedNearestNeighborJaccardDistanceFunction.Instance
 *
 * @param <O> object type
 */
public class SharedNearestNeighborJaccardDistanceFunction<O> extends AbstractIndexBasedDistanceFunction<O, SharedNearestNeighborIndex.Factory<O>> {
  /**
   * Constructor.
   *
   * @param indexFactory Index factory.
   */
  public SharedNearestNeighborJaccardDistanceFunction(SharedNearestNeighborIndex.Factory<O> indexFactory) {
    super(indexFactory);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T> instantiate(Relation<T> database) {
    SharedNearestNeighborIndex<O> indexi = indexFactory.instantiate((Relation<O>) database);
    return (Instance<T>) new Instance<>((Relation<O>) database, indexi, this);
  }

  /**
   * Actual instance for a dataset.
   *
   * @author Erich Schubert
   *
   * @assoc - - - SharedNearestNeighborIndex
   *
   * @param <T> Object type
   */
  public static class Instance<T> extends AbstractIndexBasedDistanceFunction.Instance<T, SharedNearestNeighborIndex<T>, SharedNearestNeighborJaccardDistanceFunction<T>> {
    /**
     * Constructor.
     *
     * @param database Database
     * @param preprocessor Preprocessor
     * @param parent Parent distance
     */
    public Instance(Relation<T> database, SharedNearestNeighborIndex<T> preprocessor, SharedNearestNeighborJaccardDistanceFunction<T> parent) {
      super(database, preprocessor, parent);
    }

    /**
     * Compute the Jaccard coefficient
     *
     * @param neighbors1 SORTED neighbor ids of first
     * @param neighbors2 SORTED neighbor ids of second
     * @return Jaccard coefficient
     */
    static protected double jaccardCoefficient(DBIDs neighbors1, DBIDs neighbors2) {
      int intersection = 0;
      int union = 0;
      DBIDIter iter1 = neighbors1.iter();
      DBIDIter iter2 = neighbors2.iter();
      while(iter1.valid() && iter2.valid()) {
        final int comp = DBIDUtil.compare(iter1, iter2);
        union++;
        if(comp == 0) {
          intersection++;
          iter1.advance();
          iter2.advance();
        }
        else if(comp < 0) {
          iter1.advance();
        }
        else // iter2 < iter1
        {
          iter2.advance();
        }
      }
      // Count remaining objects
      for(; iter1.valid(); iter1.advance()) {
        union++;
      }
      for(; iter2.valid(); iter2.advance()) {
        union++;
      }
      return ((double) intersection) / union;
    }

    @Override
    public double distance(DBIDRef id1, DBIDRef id2) {
      DBIDs neighbors1 = index.getNearestNeighborSet(id1);
      DBIDs neighbors2 = index.getNearestNeighborSet(id2);
      return 1.0 - jaccardCoefficient(neighbors1, neighbors2);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractIndexBasedDistanceFunction.Parameterizer<SharedNearestNeighborIndex.Factory<O>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, SharedNearestNeighborIndex.Factory.class, SharedNearestNeighborPreprocessor.Factory.class);
    }

    @Override
    protected SharedNearestNeighborJaccardDistanceFunction<O> makeInstance() {
      return new SharedNearestNeighborJaccardDistanceFunction<>(factory);
    }
  }
}
