/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.clustering.em;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

public class EMKDTest extends AbstractClusterAlgorithmTest {

  @Test
  public void testMultivariateGauss() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EMKD<?>>(EMKD.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(EMKD.Par.K_ID, 4) //
        .with(EMKD.Par.INIT_ID, MultivariateGaussianModelFactory.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.8170718099);
    assertClusterSizes(result, new int[] { 5, 107, 290, 308 });
  }
  
  @Test
  public void testInfinityCheatCase() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EMKD<?>>(EMKD.class) //
        .with(KMeans.SEED_ID, 2) // on this seed, the singularity cheat reaches an infinite value!
        .with(EMKD.Par.K_ID, 4) //
        .with(EMKD.Par.INIT_ID, MultivariateGaussianModelFactory.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.45888107612);
    assertClusterSizes(result, new int[] { 0, 0, 0, 710 });
  }

  @Test
  public void testSphericalGauss() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EMKD<?>>(EMKD.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(EMKD.Par.K_ID, 4) //
        .with(EMKD.Par.INIT_ID, SphericalGaussianModelFactory.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.8136354671);
    assertClusterSizes(result, new int[] { 8, 96, 198, 408 });
  }

  @Test
  public void testDiagonalGauss() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<EMKD<?>>(EMKD.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(EMKD.Par.K_ID, 4) //
        .with(EMKD.Par.INIT_ID, DiagonalGaussianModelFactory.class) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.8334267105);
    assertClusterSizes(result, new int[] { 7, 99, 289, 315 });
  }

}
