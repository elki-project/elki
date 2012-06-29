package de.lmu.ifi.dbs.elki.distance.similarityfunction;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
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

  /**
   * Compute the intersection size
   * 
   * @param neighbors1 SORTED neighbors of first
   * @param neighbors2 SORTED neighbors of second
   * @return Intersection size
   */
  static protected int countSharedNeighbors(DBIDs neighbors1, DBIDs neighbors2) {
    int intersection = 0;
    DBIDIter iter1 = neighbors1.iter();
    DBIDIter iter2 = neighbors2.iter();
    while(iter1.valid() && iter2.valid()) {
      final int comp = iter1.compareDBID(iter2);
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
    return intersection;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends O> Instance<T> instantiate(Relation<T> database) {
    SharedNearestNeighborIndex<O> indexi = indexFactory.instantiate((Relation<O>) database);
    return (Instance<T>) new Instance<O>((Relation<O>) database, indexi);
  }

  /**
   * Instance for a particular database.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.uses SharedNearestNeighborIndex
   * 
   * @param <O> Object type
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
    public IntegerDistance similarity(DBIDRef id1, DBIDRef id2) {
      DBIDs neighbors1 = index.getNearestNeighborSet(id1);
      DBIDs neighbors2 = index.getNearestNeighborSet(id2);
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