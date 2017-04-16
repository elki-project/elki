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
import de.lmu.ifi.dbs.elki.data.model.CorrelationModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.RelativeEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Perform a full ERiC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that ERiC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.2
 */
public class ERiCTest extends AbstractClusterAlgorithmTest {
  /**
   * Run ERiC with fixed parameters and compare the result to a golden standard.
   *
   * @throws ParameterException on errors.
   */
  @Test
  public void testERiCResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600);

    // ERiC
    ListParameterization params = new ListParameterization();
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 30);
    // ERiC Distance function in DBSCAN:
    params.addParameter(ERiC.Parameterizer.DELTA_ID, 0.20);
    params.addParameter(ERiC.Parameterizer.TAU_ID, 0.04);
    params.addParameter(ERiC.Parameterizer.K_ID, 50);
    // PCA
    params.addParameter(PCARunner.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class);
    params.addParameter(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class);
    params.addParameter(EigenPairFilter.PCA_EIGENPAIR_FILTER, RelativeEigenPairFilter.class);
    params.addParameter(RelativeEigenPairFilter.Parameterizer.EIGENPAIR_FILTER_RALPHA, 1.60);

    ERiC<DoubleVector> eric = ClassGenericsUtil.parameterizeOrAbort(ERiC.class, params);
    testParameterizationOk(params);

    // run ERiC on database
    Clustering<CorrelationModel> result = eric.run(db);

    testFMeasure(db, result, 0.728074); // Hierarchical pairs scored: 0.9204825
    testClusterSizes(result, new int[] { 109, 188, 303 });
  }

  /**
   * Run ERiC with fixed parameters and compare the result to a golden standard.
   *
   * @throws ParameterException on errors.
   */
  @Test
  public void testERiCOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);

    // Setup algorithm
    ListParameterization params = new ListParameterization();
    // ERiC
    params.addParameter(DBSCAN.Parameterizer.MINPTS_ID, 15);
    // ERiC Distance function in DBSCAN:
    params.addParameter(ERiC.Parameterizer.DELTA_ID, 1.0);
    params.addParameter(ERiC.Parameterizer.TAU_ID, 1.0);
    params.addParameter(ERiC.Parameterizer.K_ID, 45);
    // PCA
    params.addParameter(PCARunner.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class);
    params.addParameter(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class);
    params.addParameter(EigenPairFilter.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class);
    params.addParameter(PercentageEigenPairFilter.Parameterizer.ALPHA_ID, 0.6);

    ERiC<DoubleVector> eric = ClassGenericsUtil.parameterizeOrAbort(ERiC.class, params);
    testParameterizationOk(params);

    // run ERiC on database
    Clustering<CorrelationModel> result = eric.run(db);
    testFMeasure(db, result, 0.831136946);
    testClusterSizes(result, new int[] { 29, 189, 207, 225 });
  }
}