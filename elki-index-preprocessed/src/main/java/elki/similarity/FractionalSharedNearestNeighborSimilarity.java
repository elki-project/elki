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
package elki.similarity;

import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.index.preprocessed.snn.SharedNearestNeighborIndex;
import elki.index.preprocessed.snn.SharedNearestNeighborPreprocessor;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * SharedNearestNeighborSimilarity with a pattern defined to accept
 * Strings that define a non-negative Integer.
 *
 * @author Arthur Zimek
 * @since 0.2
 *
 * @navhas - - - SharedNearestNeighborIndex.Factory
 * @navhas - create - Instance
 *
 * @param <O> object type
 */
public class FractionalSharedNearestNeighborSimilarity<O> extends AbstractIndexBasedSimilarity<O, SharedNearestNeighborIndex.Factory<O>> implements NormalizedSimilarity<O> {
  /**
   * Constructor.
   *
   * @param indexFactory Index factory.
   */
  public FractionalSharedNearestNeighborSimilarity(SharedNearestNeighborIndex.Factory<O> indexFactory) {
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
  public static class Instance<T> extends AbstractIndexBasedSimilarity.Instance<T, SharedNearestNeighborIndex<T>> {
    /**
     * Similarity function.
     */
    private FractionalSharedNearestNeighborSimilarity<? super T> similarityFunction;

    /**
     * Constructor.
     *
     * @param database Database
     * @param preprocessor Preprocessor
     */
    public Instance(Relation<T> database, SharedNearestNeighborIndex<T> preprocessor, FractionalSharedNearestNeighborSimilarity<? super T> similarityFunction) {
      super(database, preprocessor);
      this.similarityFunction = similarityFunction;
    }

    /**
     * Compute the intersection size.
     *
     * @param neighbors1 SORTED neighbor ids of first
     * @param neighbors2 SORTED neighbor ids of second
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

    @Override
    public double similarity(DBIDRef id1, DBIDRef id2) {
      DBIDs neighbors1 = index.getNearestNeighborSet(id1);
      DBIDs neighbors2 = index.getNearestNeighborSet(id2);
      int intersection = countSharedNeighbors(neighbors1, neighbors2);
      return (double) intersection / index.getNumberOfNeighbors();
    }

    @Override
    public Similarity<? super T> getSimilarity() {
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
   * @param <O> object type
   */
  public static class Par<O> extends AbstractIndexBasedSimilarity.Par<SharedNearestNeighborIndex.Factory<O>> {
    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      configIndexFactory(config, SharedNearestNeighborIndex.Factory.class, SharedNearestNeighborPreprocessor.Factory.class);
    }

    @Override
    public FractionalSharedNearestNeighborSimilarity<O> make() {
      return new FractionalSharedNearestNeighborSimilarity<>(factory);
    }
  }
}
