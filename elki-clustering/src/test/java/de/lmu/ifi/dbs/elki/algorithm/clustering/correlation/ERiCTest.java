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
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.ProgressiveEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.RelativeEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Perform a full ERiC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that ERiC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class ERiCTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testERiCResults() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-3d2d1d.csv", 600);
    Clustering<CorrelationModel> result = new ELKIBuilder<ERiC<DoubleVector>>(ERiC.class) //
        .with(DBSCAN.Parameterizer.MINPTS_ID, 30) //
        // ERiC Distance function in DBSCAN:
        .with(ERiC.Parameterizer.DELTA_ID, 0.20) //
        .with(ERiC.Parameterizer.TAU_ID, 0.04) //
        .with(ERiC.Parameterizer.K_ID, 50) //
        // PCA options:
        .with(PCARunner.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, RelativeEigenPairFilter.class) //
        .with(RelativeEigenPairFilter.Parameterizer.EIGENPAIR_FILTER_RALPHA, 1.60) //
        .build().run(db);
    testFMeasure(db, result, 0.728074); // Hierarchical pairs scored: 0.9204825
    testClusterSizes(result, new int[] { 109, 188, 303 });
  }

  @Test
  public void testERiCOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);
    Clustering<CorrelationModel> result = new ELKIBuilder<ERiC<DoubleVector>>(ERiC.class) //
        // ERiC
        .with(DBSCAN.Parameterizer.MINPTS_ID, 15) //
        // ERiC Distance function in DBSCAN:
        .with(ERiC.Parameterizer.DELTA_ID, 1.0) //
        .with(ERiC.Parameterizer.TAU_ID, 1.0) //
        .with(ERiC.Parameterizer.K_ID, 45) //
        // PCA options:
        .with(PCARunner.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class) //
        .with(PercentageEigenPairFilter.Parameterizer.ALPHA_ID, 0.6) //
        .build().run(db);
    testFMeasure(db, result, 0.831136946);
    testClusterSizes(result, new int[] { 29, 189, 207, 225 });
  }

  @Test
  public void testERiCSubspaceOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-4-5d.ascii", 1100);
    Clustering<CorrelationModel> result = new ELKIBuilder<ERiC<DoubleVector>>(ERiC.class) //
        // ERiC
        .with(DBSCAN.Parameterizer.MINPTS_ID, 20) //
        // ERiC Distance function in DBSCAN:
        .with(ERiC.Parameterizer.DELTA_ID, 1.0) //
        .with(ERiC.Parameterizer.TAU_ID, 1.0) //
        .with(ERiC.Parameterizer.K_ID, 100) //
        // PCA options:
        .with(PCARunner.Parameterizer.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Parameterizer.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, ProgressiveEigenPairFilter.class) //
        .build().run(db);
    testFMeasure(db, result, 0.732609);
    testClusterSizes(result, new int[] { 104, 188, 211, 597 });
  }
}
