package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import org.junit.Test;

import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.index.AbstractTestIndexStructures;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeQuery;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.AdaptiveSortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.FileOrderBulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.MaxExtensionBulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.MaxExtensionSortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.OneDimSortBulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.SortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.SpatialSortBulkSplit;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the R*-tree index.
 *
 * @author Erich Schubert
 */
public class TestRStarTree extends AbstractTestIndexStructures {
  /**
   * Test {@link RStarTree} using a file based database connection.
   */
  @Test
  public void testRStarTree() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using a file based database connection. With "fast"
   * mode enabled on an extreme level (since this should only reduce
   * performance, not correctness!)
   */
  @Test
  public void testRStarTreeFast() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractRStarTreeFactory.Parameterizer.INSERTION_STRATEGY_ID, ApproximativeLeastOverlapInsertionStrategy.class);
    spatparams.addParameter(ApproximativeLeastOverlapInsertionStrategy.Parameterizer.INSERTION_CANDIDATES_ID, 1);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractRStarTreeFactory.Parameterizer.INSERTION_STRATEGY_ID, ApproximativeLeastOverlapInsertionStrategy.class);
    spatparams.addParameter(ApproximativeLeastOverlapInsertionStrategy.Parameterizer.INSERTION_CANDIDATES_ID, 1);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeLinearSplit}
   */
  @Test
  public void testRTreeLinearSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, RTreeLinearSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, RTreeLinearSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeQuadraticSplit}
   */
  @Test
  public void testRTreeQuadraticSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, RTreeQuadraticSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, RTreeQuadraticSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link GreeneSplit}
   */
  @Test
  public void testRTreeGreeneSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, GreeneSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, GreeneSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} using {@link RTreeLinearSplit}
   */
  @Test
  public void testRTreeAngTanLinearSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, AngTanLinearSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.SPLIT_STRATEGY_ID, AngTanLinearSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link FileOrderBulkSplit}
   */
  @Test
  public void testFileOrderBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, FileOrderBulkSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, FileOrderBulkSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link MaxExtensionBulkSplit}
   */
  @Test
  public void testMaxExtensionBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, MaxExtensionBulkSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, MaxExtensionBulkSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link OneDimSortBulkSplit}
   */
  @Test
  public void testOneDimSortBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, OneDimSortBulkSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, OneDimSortBulkSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link ZCurveSpatialSorter}
   */
  @Test
  public void testZCurveSpatialSortBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, ZCurveSpatialSorter.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, ZCurveSpatialSorter.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link HilbertSpatialSorter}
   */
  @Test
  public void testHilbertSpatialSortBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, HilbertSpatialSorter.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, HilbertSpatialSorter.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link PeanoSpatialSorter}
   */
  @Test
  public void testPeanoSpatialSortBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, PeanoSpatialSorter.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, PeanoSpatialSorter.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SpatialSortBulkSplit} with
   * {@link BinarySplitSpatialSorter}
   */
  @Test
  public void testBinarySplitSpatialSortBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, BinarySplitSpatialSorter.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SpatialSortBulkSplit.class);
    spatparams.addParameter(SpatialSortBulkSplit.Parameterizer.SORTER_ID, BinarySplitSpatialSorter.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using {@link SortTileRecursiveBulkSplit}
   */
  @Test
  public void testSortTileRecursiveBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using
   * {@link MaxExtensionSortTileRecursiveBulkSplit}
   */
  @Test
  public void testMaxExtensionSortTileRecursiveBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, MaxExtensionSortTileRecursiveBulkSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, MaxExtensionSortTileRecursiveBulkSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }

  /**
   * Test {@link RStarTree} bulk loaded using
   * {@link AdaptiveSortTileRecursiveBulkSplit}
   */
  @Test
  public void testAdaptiveSortTileRecursiveBulkSplit() {
    ListParameterization spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, AdaptiveSortTileRecursiveBulkSplit.class);
    testExactEuclidean(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
    //
    spatparams = new ListParameterization();
    spatparams.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
    spatparams.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, 300);
    spatparams.addParameter(RStarTreeFactory.Parameterizer.BULK_SPLIT_ID, AdaptiveSortTileRecursiveBulkSplit.class);
    testExactCosine(spatparams, RStarTreeKNNQuery.class, RStarTreeRangeQuery.class);
  }
}
