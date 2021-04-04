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

/**
 * Test the kd-tree accelerated EM clustering.
 *
 * @author Robert Gehde
 */
public class KDTreeEMTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testMultivariateGauss() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<KDTreeEM>(KDTreeEM.class) //
        .with(KMeans.SEED_ID, 1) //
        .with(KDTreeEM.Par.K_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.82040);
    assertClusterSizes(result, new int[] { 5, 102, 282, 321 });
  }

  @Test
  public void testInfinityCheatCase() {
    Database db = makeSimpleDatabase(UNITTEST + "hierarchical-2d.ascii", 710);
    Clustering<?> result = new ELKIBuilder<KDTreeEM>(KDTreeEM.class) //
        .with(KMeans.SEED_ID, 2) // on this seed, the singularity cheat reached
                                 // an infinite value!
        .with(KDTreeEM.Par.K_ID, 4) //
        .build().autorun(db);
    assertFMeasure(db, result, 0.64239);
    assertClusterSizes(result, new int[] { 5, 85, 205, 415 });
  }
}
