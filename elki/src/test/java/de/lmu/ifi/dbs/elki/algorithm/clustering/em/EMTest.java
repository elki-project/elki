package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Performs a full EM run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that EM's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Katharina Rausch
 * @author Erich Schubert
 * @since 0.4.0
 */
public class EMTest extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run EM with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testEMResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.SEED_ID, 0);
    params.addParameter(EM.Parameterizer.K_ID, 6);
    EM<DoubleVector, ?> em = ClassGenericsUtil.parameterizeOrAbort(EM.class, params);
    testParameterizationOk(params);

    // run EM on database
    Clustering<?> result = em.run(db);
    testFMeasure(db, result, 0.780036);
    testClusterSizes(result, new int[] { 2, 5, 27, 171, 200, 305 });
  }

  /**
   * Run EM with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testEMResultsDiagonal() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.SEED_ID, 0);
    params.addParameter(EM.Parameterizer.K_ID, 6);
    params.addParameter(EM.Parameterizer.INIT_ID, DiagonalGaussianModelFactory.class);
    EM<DoubleVector, ?> em = ClassGenericsUtil.parameterizeOrAbort(EM.class, params);
    testParameterizationOk(params);

    // run EM on database
    Clustering<?> result = em.run(db);
    testFMeasure(db, result, 0.9302319);
    testClusterSizes(result, new int[] { 7, 22, 93, 97, 200, 291 });
  }

  /**
   * Run EM with fixed parameters and compare the result to a golden standard.
   */
  @Test
  public void testEMResultsSpherical() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(KMeans.SEED_ID, 0);
    params.addParameter(EM.Parameterizer.K_ID, 6);
    params.addParameter(EM.Parameterizer.INIT_ID, SphericalGaussianModelFactory.class);
    EM<DoubleVector, ?> em = ClassGenericsUtil.parameterizeOrAbort(EM.class, params);
    testParameterizationOk(params);

    // run EM on database
    Clustering<?> result = em.run(db);
    testFMeasure(db, result, 0.514850);
    testClusterSizes(result, new int[] { 0, 6, 53, 69, 191, 391 });
  }
}