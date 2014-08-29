package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality.BayesianInformationCriterion;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Regression test for X-Means.
 * 
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 */
public class TestXMeans extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  @Test
  public void testXMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(XMeans.Parameterizer.K_MIN_ID, 2);
    params.addParameter(KMeans.K_ID, 20);
    params.addParameter(XMeans.Parameterizer.INNER_KMEANS_ID, KMeansLloyd.class);
    params.addParameter(XMeans.Parameterizer.INFORMATION_CRITERION_ID, BayesianInformationCriterion.class);
    params.addParameter(KMeans.SEED_ID, 0); // Initializer seed
    params.addParameter(XMeans.Parameterizer.SEED_ID, 0); // X-means seed

    XMeans<DoubleVector, ?> xmeans = ClassGenericsUtil.parameterizeOrAbort(XMeans.class, params);
    testParameterizationOk(params);

    // run XMeans on database
    Clustering<?> result = xmeans.run(db);
    testFMeasure(db, result, 0.95927231008);
    testClusterSizes(result, new int[] { 1, 2, 2, 2, 3, 5, 5, 51, 106, 153 });
  }
}
