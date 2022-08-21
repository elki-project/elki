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
package elki.clustering.kmeans;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for FCM clustering.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class FuzzyCMeansTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testFuzzyCMeans() {
    Database db = makeSimpleDatabase(UNITTEST + "constant-attribute.csv.gz", 200);
    Clustering<?> result = new ELKIBuilder<FuzzyCMeans<DoubleVector>>(FuzzyCMeans.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(FuzzyCMeans.Par.K_ID, 2) //
        .with(FuzzyCMeans.Par.M_ID, 2) //
        .with(FuzzyCMeans.Par.SOFT_ID, false) //
        .build().autorun(db);
    assertFMeasure(db, result, 1.);
    assertClusterSizes(result, new int[] { 100, 100 });
  }

  @Test
  public void testFuzzyCMeans2() {
    Database db = makeSimpleDatabase(UNITTEST + "constant-attribute.csv.gz", 200);
    Clustering<?> result = new ELKIBuilder<FuzzyCMeans<DoubleVector>>(FuzzyCMeans.class) //
        .with(KMeans.SEED_ID, 0) //
        .with(FuzzyCMeans.Par.K_ID, 3) //
        .with(FuzzyCMeans.Par.M_ID, 1.25) //
        .with(FuzzyCMeans.Par.SOFT_ID, true) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.85740402193);
    assertClusterSizes(result, new int[] { 48, 52, 100 });
  }
}
