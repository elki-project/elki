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
package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarityFunction with a pattern defined to accept
 * Strings that define a non-negative Integer.
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @composed - - - SharedNearestNeighborIndex.Factory
 * @navhas - create - Instance
 *
 * @param <O> object type
 */
public class SharedNearestNeighborSimilarityFunction<O> extends AbstractIndexBasedSimilarityFunction<O, SharedNearestNeighborIndex.Factory<O>> {
  /**
   * Constructor.
   *
   * @param indexFactory Index factory.
   */
  public SharedNearestNeighborSimilarityFunction(SharedNearestNeighborIndex.Factory<O> indexFactory) {
    super(indexFactory);
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
      final int comp = DBIDUtil.compare(iter1, iter2);
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
    return (Instance<T>) new Instance<>((Relation<O>) database, indexi, this);
  }

  /**
   * Instance for a particular database.
   *
   * @author Erich Schubert
   *
   * @assoc - - - SharedNearestNeighborIndex
   *
   * @param <O> Object type
   */
  public static class Instance<O> extends AbstractIndexBasedSimilarityFunction.Instance<O, SharedNearestNeighborIndex<O>> {
    /**
     * Similarity function.
     */
    private SharedNearestNeighborSimilarityFunction<? super O> similarityFunction;

    /**
     * Constructor.
     *
     * @param database Database
     * @param preprocessor Index
     */
    public Instance(Relation<O> database, SharedNearestNeighborIndex<O> preprocessor, SharedNearestNeighborSimilarityFunction<? super O> similarityFunction) {
      super(database, preprocessor);
      this.similarityFunction = similarityFunction;
    }

    @Override
    public double similarity(DBIDRef id1, DBIDRef id2) {
      DBIDs neighbors1 = index.getNearestNeighborSet(id1);
      DBIDs neighbors2 = index.getNearestNeighborSet(id2);
      return countSharedNeighbors(neighbors1, neighbors2);
    }

    @Override
    public SimilarityFunction<? super O> getSimilarityFunction() {
      return similarityFunction;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractIndexBasedSimilarityFunction.Parameterizer<SharedNearestNeighborIndex.Factory<O>> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, SharedNearestNeighborIndex.Factory.class, SharedNearestNeighborPreprocessor.Factory.class);
    }

    @Override
    protected SharedNearestNeighborSimilarityFunction<O> makeInstance() {
      return new SharedNearestNeighborSimilarityFunction<>(factory);
    }
  }
}
