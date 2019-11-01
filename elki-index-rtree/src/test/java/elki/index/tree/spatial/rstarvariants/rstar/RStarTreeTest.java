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
package elki.index.tree.spatial.rstarvariants.rstar;

import org.junit.Test;

import elki.data.NumberVector;
import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.index.AbstractIndexStructureTest;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import elki.index.tree.spatial.rstarvariants.query.EuclideanRStarTreeDistancePrioritySearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNSearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeSearcher;
import elki.index.tree.spatial.rstarvariants.strategies.bulk.*;
import elki.index.tree.spatial.rstarvariants.strategies.insert.ApproximativeLeastOverlapInsertionStrategy;
import elki.index.tree.spatial.rstarvariants.strategies.split.AngTanLinearSplit;
import elki.index.tree.spatial.rstarvariants.strategies.split.GreeneSplit;
import elki.index.tree.spatial.rstarvariants.strategies.split.RTreeLinearSplit;
import elki.index.tree.spatial.rstarvariants.strategies.split.RTreeQuadraticSplit;
import elki.math.spacefillingcurves.BinarySplitSpatialSorter;
import elki.math.spacefillingcurves.HilbertSpatialSorter;
import elki.math.spacefillingcurves.PeanoSpatialSorter;
import elki.math.spacefillingcurves.ZCurveSpatialSorter;
import elki.persistent.AbstractPageFileFactory;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the R*-tree index.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class RStarTreeTest extends AbstractIndexStructureTest {
  /**
   * Test {@link RStarTree} using a file based database connection.
   */
  @Test
  public void testRStarTree() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} using a file based database connection. With "fast"
   * mode enabled on an extreme level (since this should only reduce
   * performance, not correctness!)
   */
  @Test
  public void testRStarTreeFast() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(AbstractRStarTreeFactory.Par.INSERTION_STRATEGY_ID, ApproximativeLeastOverlapInsertionStrategy.class) //
        .with(ApproximativeLeastOverlapInsertionStrategy.Par.INSERTION_CANDIDATES_ID, 1) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeLinearSplit}
   */
  @Test
  public void testRTreeLinearSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.SPLIT_STRATEGY_ID, RTreeLinearSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeQuadraticSplit}
   */
  @Test
  public void testRTreeQuadraticSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.SPLIT_STRATEGY_ID, RTreeQuadraticSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} using {@link GreeneSplit}
   */
  @Test
  public void testRTreeGreeneSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.SPLIT_STRATEGY_ID, GreeneSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeLinearSplit}
   */
  @Test
  public void testRTreeAngTanLinearSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.SPLIT_STRATEGY_ID, AngTanLinearSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link FileOrderBulkSplit}
   */
  @Test
  public void testFileOrderBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, FileOrderBulkSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link MaxExtensionBulkSplit}
   */
  @Test
  public void testMaxExtensionBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, MaxExtensionBulkSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link OneDimSortBulkSplit}
   */
  @Test
  public void testOneDimSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, OneDimSortBulkSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link ZCurveSpatialSorter}
   */
  @Test
  public void testZCurveSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
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
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
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
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
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
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Par.SORTER_ID, BinarySplitSpatialSorter.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SortTileRecursiveBulkSplit}
   */
  @Test
  public void testSortTileRecursiveBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using
   * {@link MaxExtensionSortTileRecursiveBulkSplit}
   */
  @Test
  public void testMaxExtensionSortTileRecursiveBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, MaxExtensionSortTileRecursiveBulkSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using
   * {@link AdaptiveSortTileRecursiveBulkSplit}
   */
  @Test
  public void testAdaptiveSortTileRecursiveBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Par.BULK_SPLIT_ID, AdaptiveSortTileRecursiveBulkSplit.class) //
        .build();
    assertExactEuclidean(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, EuclideanRStarTreeDistancePrioritySearcher.class);
    assertExactCosine(factory, RStarTreeKNNSearcher.class, RStarTreeRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
