package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

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
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.DimensionModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full COPAC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that COPAC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.3
 */
public class COPACTest extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  /**
   * Run COPAC with fixed parameters and compare the result to a golden
   * standard.
   *
   * @throws ParameterException on errors.
   */
  @Test
  public void testCOPACResults() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-hierarchy.csv", 450);

    // these parameters are not picked too smartly - room for improvement.
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 0.02);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 50);
    params.addParameter(COPAC.Parameterizer.K_ID, 15);

    COPAC<DoubleVector> copac = ClassGenericsUtil.parameterizeOrAbort(COPAC.class, params);
    testParameterizationOk(params);

    // run COPAC on database
    Clustering<DimensionModel> result = copac.run(db);

    testFMeasure(db, result, 0.8484056);
    testClusterSizes(result, new int[] { 54, 196, 200 });
  }

  /**
   * Run COPAC with fixed parameters and compare the result to a golden
   * standard.
   *
   * @throws ParameterException on errors.
   */
  @Test
  public void testCOPACOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.EPSILON_ID, 0.5);
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 20);
    params.addParameter(COPAC.Parameterizer.K_ID, 45);
    // PCA
    params.addParameter(PCARunner.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class);
    params.addParameter(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class);
    params.addParameter(EigenPairFilter.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class);
    params.addParameter(PercentageEigenPairFilter.Parameterizer.ALPHA_ID, 0.8);

    COPAC<DoubleVector> copac = ClassGenericsUtil.parameterizeOrAbort(COPAC.class, params);
    testParameterizationOk(params);

    Clustering<DimensionModel> result = copac.run(db);
    testFMeasure(db, result, 0.86505092);
    testClusterSizes(result, new int[] { 32, 172, 197, 249 });
  }
}