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
package elki.index.tree.spatial.rstarvariants.strategies.bulk;

import org.junit.Test;

import elki.data.NumberVector;
import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.index.AbstractIndexStructureTest;
import elki.index.tree.spatial.rstarvariants.query.EuclideanRStarTreeDistancePrioritySearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNSearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeSearcher;
import elki.index.tree.spatial.rstarvariants.rstar.RStarTree;
import elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import elki.math.spacefillingcurves.BinarySplitSpatialSorter;
import elki.math.spacefillingcurves.HilbertSpatialSorter;
import elki.math.spacefillingcurves.PeanoSpatialSorter;
import elki.math.spacefillingcurves.ZCurveSpatialSorter;
import elki.persistent.AbstractPageFileFactory;
import elki.utilities.ELKIBuilder;

/**
 * Test spatial sorting bulk splits.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public class SpatialSortBulkSplitTest extends AbstractIndexStructureTest {
  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link ZCurveSpatialSorter}
   */
  @Test
  public void testZCurveSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<RStarTreeFactory<NumberVector>>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Par.SORTER_ID, ZCurveSpatialSorter.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link HilbertSpatialSorter}
   */
  @Test
  public void testHilbertSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<RStarTreeFactory<NumberVector>>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Par.SORTER_ID, HilbertSpatialSorter.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link PeanoSpatialSorter}
   */
  @Test
  public void testPeanoSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<RStarTreeFactory<NumberVector>>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Par.SORTER_ID, PeanoSpatialSorter.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link BinarySplitSpatialSorter}
   */
  @Test
  public void testBinarySplitSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<RStarTreeFactory<NumberVector>>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Par.SORTER_ID, BinarySplitSpatialSorter.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
