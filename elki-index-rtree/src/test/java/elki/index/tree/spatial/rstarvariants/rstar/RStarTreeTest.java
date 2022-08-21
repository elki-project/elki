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
package elki.index.tree.spatial.rstarvariants.rstar;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.NumberVector;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.index.AbstractIndexStructureTest;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import elki.index.tree.spatial.rstarvariants.query.EuclideanRStarTreeDistancePrioritySearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeKNNSearcher;
import elki.index.tree.spatial.rstarvariants.query.RStarTreeRangeSearcher;
import elki.index.tree.spatial.rstarvariants.strategies.insert.ApproximativeLeastOverlapInsertionStrategy;
import elki.persistent.AbstractPageFileFactory;
import elki.result.Metadata;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.parameterization.ListParameterization;

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
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<RStarTreeFactory<NumberVector>>(RStarTreeFactory.class) //
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
   * Trigger some additional integrity checks on the tree.
   */
  @Test
  public void runConsistencyChecks() {
    RStarTreeFactory<NumberVector> factory = new ELKIBuilder<>(RStarTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 200) //
        .build();
    ListParameterization inputparams = new ListParameterization() //
        .addParameter(StaticArrayDatabase.Par.INDEX_ID, factory);
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase("elki/testdata/unittests/hierarchical-3d2d1d.csv", 600, inputparams);
    It<RStarTreeIndex<?>> it = Metadata.hierarchyOf(db).iterDescendants().filter(RStarTreeIndex.class);
    assertTrue("No R*-tree found?", it.valid());
    it.get().getNode(it.get().getRootID()).integrityCheck(it.get());
  }
}
