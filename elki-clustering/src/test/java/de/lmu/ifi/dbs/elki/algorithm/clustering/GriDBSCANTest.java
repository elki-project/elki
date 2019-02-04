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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test GriDBSCAN.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class GriDBSCANTest extends AbstractClusterAlgorithmTest {
  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testGriDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<Model> result = new ELKIBuilder<GriDBSCAN<DoubleVector>>(GriDBSCAN.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 0.04) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 20) //
        .with(GriDBSCAN.Parameterizer.GRID_ID, 0.08) //
        .build().run(db);
    testFMeasure(db, result, 0.996413);
    testClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard, with larger grid width (fewer cells, less redundancy).
   */
  @Test
  public void testGriDBSCANWide() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);
    Clustering<Model> result = new ELKIBuilder<GriDBSCAN<DoubleVector>>(GriDBSCAN.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 0.04) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 20) //
        .with(GriDBSCAN.Parameterizer.GRID_ID, 0.4) //
        .build().run(db);
    testFMeasure(db, result, 0.996413);
    testClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   */
  @Test
  public void testDBSCANOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);
    Clustering<Model> result = new ELKIBuilder<GriDBSCAN<DoubleVector>>(GriDBSCAN.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 11.5) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 120) //
        .with(GriDBSCAN.Parameterizer.GRID_ID, 25.) //
        .build().run(db);
    testFMeasure(db, result, 0.954382);
    testClusterSizes(result, new int[] { 11, 200, 203, 224 });
  }
}
