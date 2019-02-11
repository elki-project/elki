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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

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
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.93866);
    testClusterSizes(clustering, new int[] { 200, 211, 227 });
  }

  @Test
  public void testRadius() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, RadiusCriterion.class) //
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.92082);
    testClusterSizes(clustering, new int[] { 82, 154, 200, 202 });
  }

  @Test
  public void testEuclideanDistance() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, EuclideanDistanceCriterion.class) //
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.93023);
    testClusterSizes(clustering, new int[] { 75, 161, 200, 202 });
  }

  @Test
  public void testCentroidEuclidean() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Parameterizer.DISTANCE_ID, CentroidEuclideanDistance.class) // d3
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.86062);
    testClusterSizes(clustering, new int[] { 102, 114, 200, 222 });
  }

  @Test
  public void testCentroidManhattan() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Parameterizer.DISTANCE_ID, CentroidManhattanDistance.class) // d3
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.92236);
    testClusterSizes(clustering, new int[] { 83, 154, 200, 201 });
  }

  @Test
  public void testAverageIntercluster() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Parameterizer.DISTANCE_ID, AverageInterclusterDistance.class) //
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.86062);
    testClusterSizes(clustering, new int[] { 102, 114, 200, 222 });
  }

  @Test
  public void testAverageIntracluster() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Parameterizer.DISTANCE_ID, AverageIntraclusterDistance.class) //
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.82023);
    testClusterSizes(clustering, new int[] { 158, 224, 256 });
  }

  @Test
  public void testOverflowing() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<?> clustering = new ELKIBuilder<>(BIRCHLeafClustering.class) //
        .with(CFTree.Factory.Parameterizer.ABSORPTION_ID, DiameterCriterion.class) //
        .with(CFTree.Factory.Parameterizer.BRANCHING_ID, 4) // Force branching
        .with(CFTree.Factory.Parameterizer.MAXLEAVES_ID, 4) //
        .build().run(db);
    testFMeasure(db, clustering, 0.93814);
    testClusterSizes(clustering, new int[] { 200, 218, 220});
  }
}
