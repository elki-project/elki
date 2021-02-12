/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.clustering.hierarchical;

import org.junit.Test;

import elki.Algorithm;
import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.extraction.ClustersWithNoiseExtraction;
import elki.data.Clustering;
import elki.database.Database;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.ManhattanDistance;
import elki.utilities.ELKIBuilder;

public class LingKClusteringTest extends AbstractClusterAlgorithmTest {

  @Test
  public void test() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(ClustersWithNoiseExtraction.class) //
        .with(ClustersWithNoiseExtraction.Par.K_ID, 3) //
        .with(ClustersWithNoiseExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, LingKClustering.class) //
        .with(LingKClustering.Par.DISTANCE_ID, EuclideanDistance.class) //
        .with(LingKClustering.Par.K_ID, 5) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.7783133984701753);
    assertClusterSizes(clustering, new int[] { 101, 132, 182, 223 });
  }

  @Test
  public void testnoneucl() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(ClustersWithNoiseExtraction.class) //
        .with(ClustersWithNoiseExtraction.Par.K_ID, 3) //
        .with(ClustersWithNoiseExtraction.Par.MINCLUSTERSIZE_ID, 50) //
        .with(Algorithm.Utils.ALGORITHM_ID, LingKClustering.class) //
        .with(LingKClustering.Par.DISTANCE_ID, ManhattanDistance.class) //
        .with(LingKClustering.Par.K_ID, 5) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.6601337716012936);
    assertClusterSizes(clustering, new int[] { 116, 155, 165, 202 });
  }
}
