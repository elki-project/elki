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
 * Unit test for GNAT.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class GNATTest extends AbstractIndexStructureTest {
  @Test
  public void testGNAT() {
    GNAT.Factory<?> factory = new ELKIBuilder<>(GNAT.Factory.class) //
        .with(GNAT.Factory.Par.DISTANCE_FUNCTION_ID, EuclideanDistance.class)//
        .with(GNAT.Factory.Par.NUMBER_VANTAGE_POINTS_ID, 10)//
        .with(GNAT.Factory.Par.SEED_ID, 1234).build();
    assertExactEuclidean(factory, GNAT.GNATKNNObjectSearcher.class, GNAT.GNATRangeObjectSearcher.class);
    assertPrioritySearchEuclidean(factory, GNAT.GNATPrioritySearcher.class);
    assertSinglePoint(factory, GNAT.GNATKNNDBIDSearcher.class, GNAT.GNATRangeDBIDSearcher.class);
  }
}
