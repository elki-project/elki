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
package elki.clustering.kmeans.spherical;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.datasource.AbstractDatabaseConnection;
import elki.datasource.filter.normalization.instancewise.LengthNormalization;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Regression test spherical Elkan k-means.
 * <p>
 * TODO: use a more appropriate data set
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class SphericalElkanKMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testSphericalElkanKMeans2() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000, new ListParameterization() //
        .addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, LengthNormalization.class));
    Clustering<?> result = new ELKIBuilder<SphericalElkanKMeans<DoubleVector>>(SphericalElkanKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.8637358);
    assertClusterSizes(result, new int[] { 149, 198, 200, 205, 248 });
    // Results should be identical to:
    result = new ELKIBuilder<SphericalKMeans<DoubleVector>>(SphericalKMeans.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 7) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.8637358);
    assertClusterSizes(result, new int[] { 149, 198, 200, 205, 248 });
  }
}
