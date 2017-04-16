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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractClusterAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Test the LMCLUS algorithm.
 *
 * @author Erich Schubert
 * @since 0.3
 */
public class LMCLUSTest extends AbstractClusterAlgorithmTest {
  /**
   * Run 4F with fixed parameters and compare the result to a golden standard.
   *
   * @throws ParameterException on errors.
   */
  @Test
  public void testLMCLUSResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600);

    ListParameterization params = new ListParameterization();
    params.addParameter(LMCLUS.Parameterizer.MINSIZE_ID, 100);
    params.addParameter(LMCLUS.Parameterizer.THRESHOLD_ID, 10);
    params.addParameter(LMCLUS.Parameterizer.RANDOM_ID, 2);

    LMCLUS lmclus = ClassGenericsUtil.parameterizeOrAbort(LMCLUS.class, params);
    testParameterizationOk(params);

    Clustering<Model> result = lmclus.run(db);
    testFMeasure(db, result, 0.49033868);
    testClusterSizes(result, new int[] { 23, 577 });
  }

  /**
   * Run ERiC with fixed parameters and compare the result to a golden standard.
   *
   * @throws ParameterException on errors.
   */
  @Test
  public void testLMCLUSOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);

    ListParameterization params = new ListParameterization();
    params.addParameter(LMCLUS.Parameterizer.MINSIZE_ID, 100);
    params.addParameter(LMCLUS.Parameterizer.THRESHOLD_ID, 10);
    params.addParameter(LMCLUS.Parameterizer.RANDOM_ID, 0);

    LMCLUS lmclus = ClassGenericsUtil.parameterizeOrAbort(LMCLUS.class, params);
    testParameterizationOk(params);

    Clustering<Model> result = lmclus.run(db);
    testClusterSizes(result, new int[] { 200, 201, 249 });
    testFMeasure(db, result, 0.921865);
  }
}