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
package elki.clustering.em;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.database.Database;
import elki.utilities.ELKIBuilder;

/**
 * Test the kd-tree accelerated EM clustering.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class KDTreeEMTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testMultivariateGauss() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<KDTreeEM>(KDTreeEM.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(KDTreeEM.Par.K_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.82415);
    assertClusterSizes(result, new int[] { 5, 98, 289, 318 });
  }

  @Test
  public void testMultivariateGaussExact() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<KDTreeEM>(KDTreeEM.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(KDTreeEM.Par.K_ID, 4) //
        .with(KDTreeEM.Par.SOFT_ID) //
        .with(KDTreeEM.Par.EXACT_ASSIGN_ID) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.82908);
    assertClusterSizes(result, new int[] { 5, 98, 290, 317 });
  }

  @Test
  public void testInfinityCheatCase() {
    // This caused singularity issues before & cluster degeneration
    // But the random seeding was changed since, so it may need some attempts to
    // find a corner-case again.
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<KDTreeEM>(KDTreeEM.class) //
        .with(KMeans.SEED_ID, 2) //
        .with(KDTreeEM.Par.K_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.82415);
    assertClusterSizes(result, new int[] { 5, 98, 289, 318 });
  }
}
