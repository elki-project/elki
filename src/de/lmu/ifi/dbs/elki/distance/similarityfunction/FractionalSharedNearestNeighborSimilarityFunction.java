package de.lmu.ifi.dbs.elki.distance.similarityfunction;

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

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has 
 *             de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex
 *             .Factory
 * @apiviz.uses Instance oneway - - «create»
 * 
 * @param <O> object type
 */
// todo arthur comment class
public class FractionalSharedNearestNeighborSimilarityFunction<O> extends AbstractIndexBasedSimilarityFunction<O, SharedNearestNeighborIndex<O>, TreeSetDBIDs, DoubleDistance> implements NormalizedSimilarityFunction<O, DoubleDistance> {
  /**
   * Constructor.
   * 
   * @param indexFactory Index factory.
   */
  public FractionalSharedNearestNeighborSimilarityFunction(SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborIndex<O>> indexFactory) {
    super(indexFactory);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T> instantiate(Relation<T> database) {
    SharedNearestNeighborIndex<O> indexi = indexFactory.instantiate((Relation<O>) database);
    return (Instance<T>) new Instance<O>((Relation<O>) database, indexi);
  }

  /**
   * Actual instance for a dataset.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses SharedNearestNeighborIndex
   * 
   * @param <T> Object type
   */
  public static class Instance<T> extends AbstractIndexBasedSimilarityFunction.Instance<T, SharedNearestNeighborIndex<T>, TreeSetDBIDs, DoubleDistance> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     */
    public Instance(Relation<T> database, SharedNearestNeighborIndex<T> preprocessor) {
      super(database, preprocessor);
    }

    static protected int countSharedNeighbors(TreeSetDBIDs neighbors1, TreeSetDBIDs neighbors2) {
      int intersection = 0;
      Iterator<DBID> iter1 = neighbors1.iterator();
      Iterator<DBID> iter2 = neighbors2.iterator();
      DBID neighbors1ID = iter1.hasNext() ? iter1.next() : null;
      DBID neighbors2ID = iter2.hasNext() ? iter2.next() : null;
      while(neighbors1ID != null && neighbors2ID != null) {
        if(neighbors1ID.equals(neighbors2ID)) {
          intersection++;
          neighbors1ID = iter1.hasNext() ? iter1.next() : null;
          neighbors2ID = iter2.hasNext() ? iter2.next() : null;
        }
        else if(neighbors2ID.compareTo(neighbors1ID) > 0) {
          neighbors1ID = iter1.hasNext() ? iter1.next() : null;
        }
        else // neighbors1ID > neighbors2ID
        {
          neighbors2ID = iter2.hasNext() ? iter2.next() : null;
        }
      }
      return intersection;
    }

    @Override
    public DoubleDistance similarity(DBID id1, DBID id2) {
      TreeSetDBIDs neighbors1 = index.getNearestNeighborSet(id1);
      TreeSetDBIDs neighbors2 = index.getNearestNeighborSet(id2);
      int intersection = countSharedNeighbors(neighbors1, neighbors2);
      return new DoubleDistance((double) intersection / index.getNumberOfNeighbors());
    }

    @Override
    public DoubleDistance getDistanceFactory() {
      return DoubleDistance.FACTORY;
    }
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> object type
   */
  public static class Parameterizer<O> extends AbstractIndexBasedSimilarityFunction.Parameterizer<SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborIndex<O>>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, SharedNearestNeighborIndex.Factory.class, SharedNearestNeighborPreprocessor.Factory.class);
    }

    @Override
    protected FractionalSharedNearestNeighborSimilarityFunction<O> makeInstance() {
      return new FractionalSharedNearestNeighborSimilarityFunction<O>(factory);
    }
  }
}