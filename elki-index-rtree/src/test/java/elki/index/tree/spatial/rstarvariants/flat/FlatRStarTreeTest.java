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
package elki.index.tree.spatial.rstarvariants.flat;

import org.junit.Test;

import elki.data.NumberVector;
import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.index.AbstractIndexStructureTest;
import elki.index.tree.spatial.rstarvariants.query.EuclideanRStarTreeDistancePrioritySearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNSearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeSearcher;
import elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import elki.index.tree.spatial.rstarvariants.strategies.bulk.SortTileRecursiveBulkSplit;
import elki.persistent.AbstractPageFileFactory;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the flat R*-tree index.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class FlatRStarTreeTest extends AbstractIndexStructureTest {
  @Test
  public void testFlatRStarTree() {
    FlatRStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(FlatRStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  @Test
  public void testBulkFlatRStarTree() {
    FlatRStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(FlatRStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
