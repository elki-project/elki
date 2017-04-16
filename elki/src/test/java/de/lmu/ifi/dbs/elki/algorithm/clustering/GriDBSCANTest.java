/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test GriDBSCAN.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
public class GriDBSCANTest extends AbstractClusterAlgorithmTest {
  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testGriDBSCANResults() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 0.04);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 20);
    params.addParameter(GriDBSCAN.Parameterizer.GRID_ID, 0.08);
    GriDBSCAN<DoubleVector> dbscan = ClassGenericsUtil.parameterizeOrAbort(GriDBSCAN.class, params);
    testParameterizationOk(params);

    // run DBSCAN on database
    Clustering<Model> result = dbscan.run(db);

    testFMeasure(db, result, 0.996413);
    testClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard, with larger grid width (fewer cells, less redundancy).
   * 
   * @throws ParameterException
   */
  @Test
  public void testGriDBSCANWide() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 0.04);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 20);
    params.addParameter(GriDBSCAN.Parameterizer.GRID_ID, 0.4);
    GriDBSCAN<DoubleVector> dbscan = ClassGenericsUtil.parameterizeOrAbort(GriDBSCAN.class, params);
    testParameterizationOk(params);

    // run DBSCAN on database
    Clustering<Model> result = dbscan.run(db);

    testFMeasure(db, result, 0.996413);
    testClusterSizes(result, new int[] { 29, 50, 101, 150 });
  }

  /**
   * Run DBSCAN with fixed parameters and compare the result to a golden
   * standard.
   * 
   * @throws ParameterException
   */
  @Test
  public void testDBSCANOnSingleLinkDataset() {
    Database db = makeSimpleDatabase(UNITTEST + "single-link-effect.ascii", 638);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 11.5);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 120);
    params.addParameter(GriDBSCAN.Parameterizer.GRID_ID, 25.);
    GriDBSCAN<DoubleVector> dbscan = ClassGenericsUtil.parameterizeOrAbort(GriDBSCAN.class, params);
    testParameterizationOk(params);

    // run DBSCAN on database
    Clustering<Model> result = dbscan.run(db);
    testFMeasure(db, result, 0.954382);
    testClusterSizes(result, new int[] { 11, 200, 203, 224 });
  }
}