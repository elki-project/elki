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
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.SortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.ApproximativeLeastOverlapInsertionStrategy;
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
   * Test {@link RStarTree} bulk loaded using {@link SortTileRecursiveBulkSplit}
   */
  @Test
  public void testSTRTree() {
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
}
