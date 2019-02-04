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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.AbstractIndexStructureTest;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeQuery;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.*;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.ApproximativeLeastOverlapInsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.AngTanLinearSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.GreeneSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.RTreeLinearSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.RTreeQuadraticSplit;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.BinarySplitSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.HilbertSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.PeanoSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

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
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using a file based database connection. With "fast"
   * mode enabled on an extreme level (since this should only reduce
   * performance, not correctness!)
   */
  @Test
  public void testRStarTreeFast() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(AbstractRStarTreeFactory.Parameterizer.INSERTION_STRATEGY_ID, ApproximativeLeastOverlapInsertionStrategy.class) //
        .with(ApproximativeLeastOverlapInsertionStrategy.Parameterizer.INSERTION_CANDIDATES_ID, 1) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeLinearSplit}
   */
  @Test
  public void testRTreeLinearSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, RTreeLinearSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeQuadraticSplit}
   */
  @Test
  public void testRTreeQuadraticSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, RTreeQuadraticSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link GreeneSplit}
   */
  @Test
  public void testRTreeGreeneSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, GreeneSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeLinearSplit}
   */
  @Test
  public void testRTreeAngTanLinearSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, AngTanLinearSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link FileOrderBulkSplit}
   */
  @Test
  public void testFileOrderBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, FileOrderBulkSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link MaxExtensionBulkSplit}
   */
  @Test
  public void testMaxExtensionBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, MaxExtensionBulkSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link OneDimSortBulkSplit}
   */
  @Test
  public void testOneDimSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, OneDimSortBulkSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link ZCurveSpatialSorter}
   */
  @Test
  public void testZCurveSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Parameterizer.SORTER_ID, ZCurveSpatialSorter.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link HilbertSpatialSorter}
   */
  @Test
  public void testHilbertSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Parameterizer.SORTER_ID, HilbertSpatialSorter.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link PeanoSpatialSorter}
   */
  @Test
  public void testPeanoSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Parameterizer.SORTER_ID, PeanoSpatialSorter.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link BinarySplitSpatialSorter}
   */
  @Test
  public void testBinarySplitSpatialSortBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class) //
        .with(SpatialSortBulkSplit.Parameterizer.SORTER_ID, BinarySplitSpatialSorter.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SortTileRecursiveBulkSplit}
   */
  @Test
  public void testSortTileRecursiveBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using
   * {@link MaxExtensionSortTileRecursiveBulkSplit}
   */
  @Test
  public void testMaxExtensionSortTileRecursiveBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, MaxExtensionSortTileRecursiveBulkSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using
   * {@link AdaptiveSortTileRecursiveBulkSplit}
   */
  @Test
  public void testAdaptiveSortTileRecursiveBulkSplit() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300) //
        .with(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, AdaptiveSortTileRecursiveBulkSplit.class) //
        .build();
    testExactEuclidean(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testExactCosine(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    testSinglePoint(factory, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }
}
