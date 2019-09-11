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
package elki.index.tree.metrical.mtreevariants.strategies.split;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.index.AbstractIndexStructureTest;
import elki.index.tree.metrical.mtreevariants.mtree.MTreeFactory;
import elki.index.tree.metrical.mtreevariants.query.MTreeKNNQuery;
import elki.index.tree.metrical.mtreevariants.query.MTreeRangeQuery;
import elki.persistent.AbstractPageFileFactory;
import elki.utilities.ELKIBuilder;

/**
 * Test the mM_rad split.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MMRadSplitTest extends AbstractIndexStructureTest {
  @Test
  public void testEuclidean() {
    MTreeFactory<DoubleVector> factory = new ELKIBuilder<>(MTreeFactory.class) //
        .with(AbstractPageFileFactory.Par.PAGE_SIZE_ID, 300) //
        .with(MTreeFactory.Par.SPLIT_STRATEGY_ID, MMRadSplit.class) //
        .build();
    testExactEuclidean(factory, MTreeKNNQuery.class, MTreeRangeQuery.class);
    testSinglePoint(factory, MTreeKNNQuery.class, MTreeRangeQuery.class);
  }
}
