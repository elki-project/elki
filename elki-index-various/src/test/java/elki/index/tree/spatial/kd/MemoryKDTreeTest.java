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
package elki.index.tree.spatial.kd;

import org.junit.Test;

import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.index.AbstractIndexStructureTest;
import elki.index.tree.spatial.kd.split.MedianSplit;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the {@link MemoryKDTree} index.
 *
 * @author Erich Schubert
 */
public class MemoryKDTreeTest extends AbstractIndexStructureTest {
  @Test
  public void testMemoryKDTree() {
    MemoryKDTree.Factory<?> factory = new ELKIBuilder<>(MemoryKDTree.Factory.class) //
        .with(MemoryKDTree.Factory.Par.SPLIT_P, MedianSplit.class) //
        .with(MemoryKDTree.Factory.Par.LEAFSIZE_P, 2) //
        .build();
    assertExactSqEuclidean(factory, MemoryKDTree.KDTreeKNNSearcher.class, MemoryKDTree.KDTreeRangeSearcher.class);
    assertExactEuclidean(factory, MemoryKDTree.KDTreeKNNSearcher.class, MemoryKDTree.KDTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, MemoryKDTree.KDTreePrioritySearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
