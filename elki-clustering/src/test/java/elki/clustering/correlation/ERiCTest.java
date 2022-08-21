/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.correlation;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.dbscan.DBSCAN;
import elki.data.Clustering;
import elki.data.model.CorrelationModel;
import elki.database.Database;
import elki.math.linearalgebra.pca.PCARunner;
import elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.math.linearalgebra.pca.filter.ProgressiveEigenPairFilter;
import elki.math.linearalgebra.pca.filter.RelativeEigenPairFilter;
import elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import elki.utilities.ELKIBuilder;

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
    Clustering<CorrelationModel> result = new ELKIBuilder<>(ERiC.class) //
        .with(DBSCAN.Par.MINPTS_ID, 30) //
        // ERiC Distance function in DBSCAN:
        .with(ERiC.Par.DELTA_ID, 0.20) //
        .with(ERiC.Par.TAU_ID, 0.04) //
        .with(ERiC.Par.K_ID, 50) //
        // PCA options:
        .with(PCARunner.Par.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Par.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, RelativeEigenPairFilter.class) //
        .with(RelativeEigenPairFilter.Par.EIGENPAIR_FILTER_RALPHA, 1.60) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.728074); // Hierarchical pairs scored:
                                          // 0.9204825
    assertClusterSizes(result, new int[] { 109, 188, 303 });
  }

  @Test
  public void testERiCOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);
    Clustering<CorrelationModel> result = new ELKIBuilder<>(ERiC.class) //
        .with(DBSCAN.Par.MINPTS_ID, 15) //
        // ERiC Distance function in DBSCAN:
        .with(ERiC.Par.DELTA_ID, 1.0) //
        .with(ERiC.Par.TAU_ID, 1.0) //
        .with(ERiC.Par.K_ID, 45) //
        // PCA options:
        .with(PCARunner.Par.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Par.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class) //
        .with(PercentageEigenPairFilter.Par.ALPHA_ID, 0.6) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.831136946);
    assertClusterSizes(result, new int[] { 29, 189, 207, 225 });
  }

  @Test
  public void testERiCSubspaceOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "subspace-overlapping-4-5d.ascii", 1100);
    Clustering<CorrelationModel> result = new ELKIBuilder<>(ERiC.class) //
        .with(DBSCAN.Par.MINPTS_ID, 20) //
        // ERiC Distance function in DBSCAN:
        .with(ERiC.Par.DELTA_ID, 1.0) //
        .with(ERiC.Par.TAU_ID, 1.0) //
        .with(ERiC.Par.K_ID, 100) //
        // PCA options:
        .with(PCARunner.Par.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Par.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, ProgressiveEigenPairFilter.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.732609);
    assertClusterSizes(result, new int[] { 104, 188, 211, 597 });
  }
}
