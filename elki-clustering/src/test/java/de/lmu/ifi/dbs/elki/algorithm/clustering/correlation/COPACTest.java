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
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

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
public class COPACTest extends AbstractClusterAlgorithmTest {
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
    COPAC<DoubleVector> copac = new ELKIBuilder<COPAC<DoubleVector>>(COPAC.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 0.02) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 50) //
        .with(COPAC.Parameterizer.K_ID, 15) //
        .build();

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

    COPAC<DoubleVector> copac = new ELKIBuilder<COPAC<DoubleVector>>(COPAC.class) //
        .with(DBSCAN.Parameterizer.EPSILON_ID, 0.5) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 20) //
        .with(COPAC.Parameterizer.K_ID, 45) //
        // PCA
        .with(PCARunner.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class) //
        .with(PercentageEigenPairFilter.Parameterizer.ALPHA_ID, 0.8) //
        .build();

    Clustering<DimensionModel> result = copac.run(db);
    testFMeasure(db, result, 0.86505092);
    testClusterSizes(result, new int[] { 32, 172, 197, 249 });
  }
}
