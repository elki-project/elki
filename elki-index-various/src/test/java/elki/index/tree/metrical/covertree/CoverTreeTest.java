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
package elki.index.tree.metrical.covertree;

import org.junit.Test;

import elki.distance.minkowski.EuclideanDistance;
import elki.index.AbstractIndexStructureTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the {@link CoverTree}.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CoverTreeTest extends AbstractIndexStructureTest {
  @Test
  public void testCovertree() {
    CoverTree.Factory<?> factory = new ELKIBuilder<>(CoverTree.Factory.class) //
        .with(CoverTree.Factory.Par.DISTANCE_FUNCTION_ID, EuclideanDistance.class).build();
    assertExactEuclidean(factory, CoverTree.CoverTreePrioritySearcher.class, CoverTree.CoverTreeRangeSearcher.class);
    assertPrioritySearchEuclidean(factory, CoverTree.CoverTreePrioritySearcher.class);
    assertSinglePoint(factory, CoverTree.CoverTreePrioritySearcher.class, CoverTree.CoverTreeRangeSearcher.class);
  }
}
