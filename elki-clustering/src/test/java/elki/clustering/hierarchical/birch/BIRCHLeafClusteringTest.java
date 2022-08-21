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
package elki.clustering.hierarchical.birch;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Regression test for BIRCH clustering.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class BIRCHLeafClusteringTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testDiameter() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93789);
    assertClusterSizes(clustering, new int[] { 59, 176, 196, 207 });
  }

  @Test
  public void testRadius() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, RadiusCriterion.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.8619);
    assertClusterSizes(clustering, new int[] { 105, 112, 200, 221 });
  }

  @Test
  public void testEuclideanDistance() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, EuclideanDistanceCriterion.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.93866);
    assertClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testCentroidEuclidean() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.DISTANCE_ID, CentroidEuclideanDistance.class) // d3
        .with(CFTree.Factory.Par.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.86062);
    assertClusterSizes(clustering, new int[] { 102, 114, 200, 222 });
  }

  @Test
  public void testCentroidManhattan() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.DISTANCE_ID, CentroidManhattanDistance.class) // d3
        .with(CFTree.Factory.Par.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.92236);
    assertClusterSizes(clustering, new int[] { 83, 154, 200, 201 });
  }

  @Test
  public void testAverageIntercluster() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.DISTANCE_ID, AverageInterclusterDistance.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.86062);
    assertClusterSizes(clustering, new int[] { 102, 114, 200, 222 });
  }

  @Test
  public void testAverageIntracluster() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.DISTANCE_ID, AverageIntraclusterDistance.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.82023);
    assertClusterSizes(clustering, new int[] { 158, 224, 256 });
  }

  @Test
  public void testOverflowing() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Par.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Par.BRANCHING_ID, 4) // Force branching
        .with(CFTree.Factory.Par.MAXLEAVES_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, clustering, 0.89558);
    assertClusterSizes(clustering, new int[] { 65, 156, 198, 219 });
  }
}
