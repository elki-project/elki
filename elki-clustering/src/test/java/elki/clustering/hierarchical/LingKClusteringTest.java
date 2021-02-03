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
import elki.clustering.hierarchical.extraction.CutDendrogramByNumberOfClusters;
import elki.data.Clustering;
import elki.database.Database;
import elki.distance.minkowski.EuclideanDistance;
import elki.distance.minkowski.ManhattanDistance;
import elki.utilities.ELKIBuilder;

public class LingKClusteringTest extends AbstractClusterAlgorithmTest {

  @Test
  public void test() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(LingKClustering.class) //
        .with(LingKClustering.Par.DISTANCE_ID, EuclideanDistance.class) //
        .with(LingKClustering.Par.K_ID, 5) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.20828105395232122);
    assertClusterSizes(clustering, new int[] { 0, 3, 5, 6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 16, 16, 19, 19, 22, 22, 23, 25, 30, 40, 40, 41, 42, 45, 52 });
  }

  @Test
  public void testnoneucl() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(LingKClustering.class) //
        .with(LingKClustering.Par.DISTANCE_ID, ManhattanDistance.class) //
        .with(LingKClustering.Par.K_ID, 5) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.20982427323317426);
    assertClusterSizes(clustering, new int[] { 0, 4, 6, 6, 6, 6, 6, 6, 6, 7, 7, 8, 8, 8, 8, 8, 9, 9, 10, 11, 12, 13, 13, 16, 20, 21, 24, 24, 26, 28, 29, 33, 34, 36, 38, 66, 66 });
  }
}
