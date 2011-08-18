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
import de.lmu.ifi.dbs.elki.database.ids.SetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex.Factory
 * @apiviz.uses Instance oneway - - «create»
 * 
 * @param <O> object type
 */
// todo arthur comment class
public class SharedNearestNeighborSimilarityFunction<O> extends AbstractIndexBasedSimilarityFunction<O, SharedNearestNeighborIndex<O>, SetDBIDs, IntegerDistance> {
  /**
   * Constructor.
   * 
   * @param indexFactory Index factory.
   */
  public SharedNearestNeighborSimilarityFunction(SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborIndex<O>> indexFactory) {
    super(indexFactory);
  }

  @Override
  public IntegerDistance getDistanceFactory() {
    return IntegerDistance.FACTORY;
  }

  static protected int countSharedNeighbors(SetDBIDs neighbors1, SetDBIDs neighbors2) {
    int intersection = 0;
    Iterator<DBID> iter1 = neighbors1.iterator();
    Iterator<DBID> iter2 = neighbors2.iterator();
    DBID neighbors1ID = iter1.hasNext() ? iter1.next() : null;
    DBID neighbors2ID = iter2.hasNext() ? iter2.next() : null;
    while((iter1.hasNext() || iter2.hasNext()) && neighbors1ID != null && neighbors2ID != null) {
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

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T> instantiate(Relation<T> database) {
    SharedNearestNeighborIndex<O> indexi = indexFactory.instantiate((Relation<O>) database);
    return (Instance<T>) new Instance<O>((Relation<O>) database, indexi);
  }

  /**
   * TODO: document
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses SharedNearestNeighborIndex
   * 
   * @param <D>
   */
  public static class Instance<O> extends AbstractIndexBasedSimilarityFunction.Instance<O, SharedNearestNeighborIndex<O>, SetDBIDs, IntegerDistance> {
    /**
     * Constructor.
     *
     * @param database Database
     * @param preprocessor Index
     */
    public Instance(Relation<O> database, SharedNearestNeighborIndex<O> preprocessor) {
      super(database, preprocessor);
    }

    @Override
    public IntegerDistance similarity(DBID id1, DBID id2) {
      SetDBIDs neighbors1 = index.getNearestNeighborSet(id1);
      SetDBIDs neighbors2 = index.getNearestNeighborSet(id2);
      return new IntegerDistance(countSharedNeighbors(neighbors1, neighbors2));
    }

    @Override
    public IntegerDistance getDistanceFactory() {
      return IntegerDistance.FACTORY;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractIndexBasedSimilarityFunction.Parameterizer<SharedNearestNeighborIndex.Factory<O, SharedNearestNeighborIndex<O>>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, SharedNearestNeighborIndex.Factory.class, SharedNearestNeighborPreprocessor.Factory.class);
    }

    @Override
    protected SharedNearestNeighborSimilarityFunction<O> makeInstance() {
      return new SharedNearestNeighborSimilarityFunction<O>(factory);
    }
  }
}