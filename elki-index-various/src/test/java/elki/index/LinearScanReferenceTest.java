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
package elki.index;

import org.junit.Test;

import elki.database.query.distance.LinearScanEuclideanPrioritySearcher;
import elki.database.query.knn.LinearScanEuclideanKNNByObject;
import elki.database.query.knn.LinearScanPrimitiveKNNByObject;
import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.LinearScanEuclideanRangeByObject;
import elki.database.query.range.LinearScanPrimitiveDistanceRangeByObject;
import elki.database.query.range.WrappedRangeDBIDByLookup;

/**
 * This unit test verifies that the linear scan produces the reference result.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class LinearScanReferenceTest extends AbstractIndexStructureTest {
  /**
   * Test exact query, also to validate the test is correct.
   */
  @Test
  public void testExact() {
    assertExactEuclidean(null, LinearScanEuclideanKNNByObject.class, LinearScanEuclideanRangeByObject.class);
    assertPrioritySearchEuclidean(null, LinearScanEuclideanPrioritySearcher.class);
    assertSinglePoint(null, WrappedKNNDBIDByLookup.Linear.class, WrappedRangeDBIDByLookup.Linear.class);
  }

  /**
   * Test exact query, also to validate the test is correct.
   */
  @Test
  public void testExactCosine() {
    assertExactCosine(null, LinearScanPrimitiveKNNByObject.class, LinearScanPrimitiveDistanceRangeByObject.class);
  }
}
