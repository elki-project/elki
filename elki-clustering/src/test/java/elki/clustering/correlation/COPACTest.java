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
import elki.data.model.DimensionModel;
import elki.database.Database;
import elki.math.linearalgebra.pca.PCARunner;
import elki.math.linearalgebra.pca.WeightedCovarianceMatrixBuilder;
import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import elki.utilities.ELKIBuilder;

/**
 * Perform a full COPAC run, and compare the result with a clustering derived
 * from the data set labels. This test ensures that COPAC performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Erich Schubert
 * @author Katharina Rausch
 * @since 0.7.0
 */
public class COPACTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testCOPACResults() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-hierarchy.csv", 450);
    // these parameters are not picked too well - room for improvement.
    Clustering<DimensionModel> result = new ELKIBuilder<>(COPAC.class) //
        .with(DBSCAN.Par.EPSILON_ID, 0.02) //
        .with(DBSCAN.Par.MINPTS_ID, 50) //
        .with(COPAC.Par.K_ID, 15) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.8484056);
    assertClusterSizes(result, new int[] { 54, 196, 200 });
  }

  @Test
  public void testCOPACOverlap() {
    Database db = makeSimpleDatabase(UNITTEST + "correlation-overlap-3-5d.ascii", 650);
    Clustering<DimensionModel> result = new ELKIBuilder<>(COPAC.class) //
        .with(DBSCAN.Par.EPSILON_ID, 0.5) //
        .with(DBSCAN.Par.MINPTS_ID, 20) //
        .with(COPAC.Par.K_ID, 45) //
        // PCA Options:
        .with(PCARunner.Par.PCA_COVARIANCE_MATRIX, WeightedCovarianceMatrixBuilder.class) //
        .with(WeightedCovarianceMatrixBuilder.Par.WEIGHT_ID, ErfcWeight.class) //
        .with(EigenPairFilter.PCA_EIGENPAIR_FILTER, PercentageEigenPairFilter.class) //
        .with(PercentageEigenPairFilter.Par.ALPHA_ID, 0.8) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.86505092);
    assertClusterSizes(result, new int[] { 32, 172, 197, 249 });
  }
}
