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
package elki.index.tree.metrical.vptree;

import org.junit.Test;

import elki.distance.minkowski.EuclideanDistance;
import elki.index.AbstractIndexStructureTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the {@link VPTree}.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class VPTreeTest extends AbstractIndexStructureTest {
  @Test
  public void testVPTree() {
    VPTree.Factory<?> factory = new ELKIBuilder<>(VPTree.Factory.class) //
        .with(VPTree.Factory.Par.DISTANCE_FUNCTION_ID, EuclideanDistance.class)//
        .with(VPTree.Factory.Par.SAMPLE_SIZE_ID, 10)//
        .with(VPTree.Factory.Par.SEED_ID, 1234).build();
    assertExactEuclidean(factory, VPTree.VPTreeKNNSearcher.class, VPTree.VPTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, VPTree.VPTreePrioritySearcher.class);
    assertSinglePoint(factory, VPTree.VPTreeKNNSearcher.class, VPTree.VPTreeRangeSearcher.class);
  }

  /**
   * Test with samplesize 1, which has a special handling.
   */
  @Test
  public void testVPTreeOne() {
    VPTree.Factory<?> factory = new ELKIBuilder<>(VPTree.Factory.class) //
        .with(VPTree.Factory.Par.DISTANCE_FUNCTION_ID, EuclideanDistance.class)//
        .with(VPTree.Factory.Par.SAMPLE_SIZE_ID, 1)//
        .with(VPTree.Factory.Par.SEED_ID, 1234).build();
    assertExactEuclidean(factory, VPTree.VPTreeKNNSearcher.class, VPTree.VPTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, VPTree.VPTreePrioritySearcher.class);
    assertSinglePoint(factory, VPTree.VPTreeKNNSearcher.class, VPTree.VPTreeRangeSearcher.class);
  }
}
