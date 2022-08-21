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
package elki.index.idistance;

import org.junit.Test;

import elki.clustering.kmeans.initialization.FarthestPoints;
import elki.data.NumberVector;
import elki.database.query.knn.WrappedKNNDBIDByLookup;
import elki.database.query.range.WrappedRangeDBIDByLookup;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.AbstractIndexStructureTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the iDistance index.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class IDistanceTest extends AbstractIndexStructureTest {
  @Test
  public void testIDistance() {
    InMemoryIDistanceIndex.Factory<NumberVector> factory = new ELKIBuilder<>(InMemoryIDistanceIndex.Factory.class) //
        .with(InMemoryIDistanceIndex.Factory.Par.K_ID, 4) //
        .with(InMemoryIDistanceIndex.Factory.Par.DISTANCE_ID, EuclideanDistance.class) //
        .with(InMemoryIDistanceIndex.Factory.Par.REFERENCE_ID, FarthestPoints.class) //
        .build();
    assertExactEuclidean(factory, InMemoryIDistanceIndex.IDistanceKNNSearcher.class, InMemoryIDistanceIndex.IDistanceRangeSearcher.class);
    assertSinglePoint(factory, WrappedKNNDBIDByLookup.class, WrappedRangeDBIDByLookup.class);
  }
}
